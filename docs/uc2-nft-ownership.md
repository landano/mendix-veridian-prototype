# UC2: Verify User Ownership of Landano NFT

## Overview

A Land Owner binds their KERI identity and Cardano wallet to a Landano NFT at mint time. Both signatures are stored immutably in the NFT metadata on-chain. A Verifier can later confirm the owner still controls both the AID and the wallet holding the NFT.

## Actors

| Actor | Role |
|-------|------|
| **Land Owner** | Binds their identity and mints the NFT |
| **Verifier** | Verifies NFT ownership at a later point in time |
| **Backend (Mendix)** | Orchestrates signing and minting; custodial Cardano wallet |

## Part A: Minting (Binding + Mint)

### Prerequisites

- Land Owner has a `WalletContact` with OOBI resolved (Veridian wallet connected)
- Land Owner has a custodial Cardano `Wallet` in the system with test ADA
- Backend AID bootstrapped

### Binding Flow

The binding proves at mint time that the same person controls both the KERI AID and the Cardano wallet. The binding message ties all four identifiers together: AID, Cardano address, NFT policy ID, and asset name.

**Key principle: KERI signs first.** The KERI exchange SAID is included in the binding before the Cardano wallet signs — the Cardano signature covers the full binding context including proof of AID control.

```
Land Owner (Mendix page)     Veridian Wallet       Backend / KERIA        Cardano
        │                          │                     │                   │
        │─ Create NFT Binding ─────────────────────────→ │                   │
        │  (select NFT type,        │                     │                   │
        │   select wallet,          │                     │                   │
        │   enter name, Save)       │                     │                   │
        │                           │                     │                   │
        │   JA_BuildBindingMessage  │                     │                   │
        │   └─ Returns SAIDified    │                     │                   │
        │      binding JSON         │                     │                   │
        │                           │                     │                   │
        │   JA_SendChallenge        │                     │                   │
        │   └─ Sends binding msg ──────────────────────→  │                   │
        │      Returns KERI SAID    │                     │                   │
        │                           │                     │                   │
        │                           │←─ Sign request ─────│                   │
        │                           │                     │                   │
        │                           │  [Owner signs]       │                   │
        │                           │                      │                   │
        │                           │── Signed response ──→│                   │
        │                           │                      │                   │
        │   JA_VerifyChallengeResponse                     │                   │
        │   └─ Returns KERI SAID                           │                   │
        │      (proof of AID control)                      │                   │
        │                           │                      │                   │
        │   JA_SignPayloadWithCardanoWallet                 │                   │
        │   └─ Cardano wallet signs binding message        │                   │
        │      (binding msg includes KERI SAID)            │                   │
        │                           │                      │                   │
        │   JA_NFT_Mint ──────────────────────────────────────────────────────→│
        │   └─ Metadata includes:   │                      │                   │
        │      - BindingMessage     │                      │                   │
        │      - KERIChallengeSAID  │                      │                   │
        │      - CardanoSignatureHex│                      │                   │
        │      - CardanoKeyHex      │                      │                   │
        │                           │                      │                   │
        │←─ TransactionID ──────────────────────────────────────────────────────│
```

### Microflow Sequence

#### Step 1 — Build the Binding Message

```
JA_BuildBindingMessage(
    AIDPrefix:     String   // Land Owner's AIDPrefix from WalletContact
    CardanoAddress: String  // bech32 address (NOT hex) of receiving wallet
    NFTPolicyId:   String   // policy ID (or empty string for auto-create)
    NFTAssetName:  String   // unique asset name entered by user
)
→ BindingMessageJSON: String
```

Store the result in `NFTBinding.BindingMessage`.

> **Note:** Pass the bech32 address (e.g. `addr_test1...`), not the hex address. Use `ConvertHexToBech32` if you only have the hex.

#### Step 2 — Send to KERI Wallet

```
JA_SendChallenge(
    BackendIdentity: BackendIdentity
    WalletContact:   WalletContact   // Land Owner's wallet contact
    ChallengePayload: String         // the binding message JSON from step 1
)
→ KERIChallengeSAID: String
```

Store in `NFTBinding.KERIChallengeSAID`. The Land Owner opens their Veridian wallet and approves the signing request.

#### Step 3 — Verify KERI Signature

```
JA_VerifyChallengeResponse(
    ChallengeSAID: String   // KERIChallengeSAID from step 2
)
→ Boolean
```

If `true`, the Land Owner has proven AID control. The `KERIChallengeSAID` stored in `NFTBinding` serves as the on-chain proof of this event.

#### Step 4 — Sign with Cardano Wallet

Create a `CardanoSignature` entity, set `Payload` = `NFTBinding.BindingMessage`, associate with the Land Owner's `Wallet`, then:

```
JA_SignPayloadWithCardanoWallet(
    CardanoSignature: CardanoSignature
    Passphrase:       String   // wallet encryption passphrase
)
→ void
```

After this, `CardanoSignature` has `SignatureHex` (COSE_Sign1), `KeyHex` (COSE_Key), and `SignerAddress` set.

Copy `SignatureHex` → `NFTBinding.CardanoSignatureHex` and `KeyHex` → `NFTBinding.CardanoKeyHex`.

#### Step 5 — Mint the NFT

Build the metadata JSON and call `JA_NFT_Mint`. The metadata must include the binding proof under a `landano` namespace:

```json
{
  "721": {
    "<policyId>": {
      "<assetName>": {
        "name": "<display name>",
        "landano": {
          "bindingMessage": "<BindingMessage JSON>",
          "keriChallengeSAID": "<KERIChallengeSAID>",
          "cardanoSignature": "<CardanoSignatureHex>",
          "cardanoKey": "<CardanoKeyHex>"
        }
      }
    }
  }
}
```

Set this on `NFTNP.metadataJSON`, then:

```
JA_NFT_Mint(
    TransactionNP: NFTNP   // non-persistable transaction parameters
    Passphrase:    String
)
→ TransactionID: String
```

Store `TransactionID` in `NFTBinding.TransactionID` and set `Status = Signed`.

---

## Part B: Verification

### Prerequisites

- Verifier knows the asset unit (`{policyId}{assetNameHex}`)
- The NFT has been minted with the binding metadata structure above

### Verification Flow

```
Verifier (Mendix page)       Land Owner's Veridian Wallet      Backend / Blockfrost
        │                               │                              │
        │─ Enter asset unit ────────────────────────────────────────→  │
        │                               │                              │
        │   JA_FetchNFTBindingMetadata  │                              │
        │   └─ Query on-chain metadata ──────────────────────────────→ │
        │      Extract: BindingMessage, KERIChallengeSAID,             │
        │               CardanoSignatureHex, CardanoKeyHex             │
        │               Current holder address                         │
        │                               │                              │
        │   JA_VerifyCardanoPayloadSignature                           │
        │   └─ Verify stored Cardano signature                         │
        │      Confirm signer == original holder address               │
        │                               │                              │
        │   JA_SendChallenge (fresh)    │                              │
        │   └─ Send new challenge to Land Owner's AID ─────────────→  │
        │                               │                              │
        │                               │←─ Fresh sign request ────────│
        │                               │  [Owner signs live]          │
        │                               │── Response ─────────────────→│
        │                               │                              │
        │   JA_VerifyChallengeResponse  │                              │
        │   └─ Confirm live AID control │                              │
        │                               │                              │
        │   Confirm NFT still in original wallet address               │
        │   (compare current holder from Blockfrost                    │
        │    vs SignerAddress in stored Cardano signature)             │
        │                               │                              │
        │←─ Verification result ──────────────────────────────────────│
```

### Microflow Sequence

#### Step 1 — Fetch Metadata

```
JA_FetchNFTBindingMetadata(
    NFTVerification:  NFTVerification   // with AssetUnit set
    AssetUnit:        String
    BlockfrostAPIKey: String
    BlockfrostBaseUrl: String
)
→ void   // populates NFTVerification with metadata fields
```

#### Step 2 — Verify Cardano Signature

Create a `CardanoSignature` entity from the stored values, then:

```
JA_VerifyCardanoPayloadSignature(
    CardanoSignature: CardanoSignature  // with SignatureHex, KeyHex, Payload set
)
→ void   // sets IsVerified and SignerAddress
```

Check that `IsVerified = true` and that `SignerAddress` matches the current NFT holder address from Blockfrost.

#### Step 3 — Verify Live AID Control

Send a fresh challenge to the Land Owner's AID and verify the response (same as Steps 2–3 in Part A). This confirms the person still controls the AID, not just that they did at mint time.

#### Step 4 — Confirm NFT Not Transferred

Compare the current holder address (from `JA_FetchNFTBindingMetadata`) with the original signer address (from `JA_VerifyCardanoPayloadSignature`). If they differ, the NFT has been transferred and the binding is no longer valid.

---

## Known Limitations

- **Key rotation:** If the Land Owner rotates their KERI AID key after minting, the stored `KERIChallengeSAID` in the metadata still references the original key event. Fresh AID challenges will still succeed (the AID is preserved through rotation), but there is no mechanism to update the on-chain metadata. This is acknowledged as a limitation of this PoC.
- **NFT transfer:** The current implementation does not prevent the NFT from being transferred to another wallet. A smart contract with identity verification would be needed for transfers.
