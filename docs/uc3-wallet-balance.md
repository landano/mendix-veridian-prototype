# UC3: Verify ADA Wallet Balance belongs to an ID Wallet Holder

## Overview

A user proves they control a specific Cardano wallet and its ADA balance — and that this wallet is bound to their KERI identity. Useful when a user must demonstrate sufficient ADA for a transaction while also proving identity.

## Actors

| Actor | Role |
|-------|------|
| **ID Holder** | Proves control of both their KERI AID and Cardano wallet |
| **Verifier** | Requests proof and reads the verified balance |
| **Backend (Mendix)** | Orchestrates the dual-signature challenge |

## Prerequisites

- ID Holder has a `WalletContact` with OOBI resolved
- ID Holder has a custodial Cardano `Wallet` in the system
- Backend AID bootstrapped

## Flow

```
Verifier (Mendix page)      ID Holder's Veridian Wallet      Backend / Blockfrost
        │                            │                               │
        │─ Trigger verification ────────────────────────────────────→│
        │                            │                               │
        │   Build challenge message  │                               │
        │   {AID, walletAddress,     │                               │
        │    timestamp}              │                               │
        │                            │                               │
        │   JA_SendChallenge ────────────────────────────────────── →│
        │   └─ Sends challenge       │                               │
        │      Returns KERI SAID     │                               │
        │                            │                               │
        │                            │←─ Sign request ───────────────│
        │                            │  [ID Holder signs in wallet]  │
        │                            │── Signed response ───────────→│
        │                            │                               │
        │   JA_VerifyChallengeResponse                               │
        │   └─ Verifies KERI signature                               │
        │      Confirms AID control                                  │
        │                            │                               │
        │   JA_SignPayloadWithCardanoWallet                          │
        │   └─ Custodial wallet signs same challenge message         │
        │                            │                               │
        │   JA_VerifyCardanoPayloadSignature                         │
        │   └─ Verifies Cardano signature                            │
        │      Confirms wallet control                               │
        │                            │                               │
        │   JA_Account_GetBalances ──────────────────────────────── →│
        │   └─ Queries ADA balance                                   │
        │      via Blockfrost                                        │
        │                            │                               │
        │←─ Verified balance ─────────────────────────────────────── │
```

## Microflow Sequence

### Step 1 — Build the Challenge Message

Construct a challenge message that ties the AID, wallet address, and timestamp together:

```json
{
  "aid": "<WalletContact.AIDPrefix>",
  "cardanoAddress": "<Wallet.address (bech32)>",
  "timestamp": "<ISO 8601 now>"
}
```

Build this in the microflow using string operations or a CommunityCommons JSON helper. Store as a String variable `ChallengePayload`.

### Step 2 — KERI Challenge

```
JA_SendChallenge(
    BackendIdentity:  BackendIdentity
    WalletContact:    WalletContact   // ID Holder
    ChallengePayload: String          // challenge message from step 1
)
→ KERIChallengeSAID: String
```

The ID Holder opens their Veridian wallet and approves the signing request.

### Step 3 — Verify KERI Signature

```
JA_VerifyChallengeResponse(
    ChallengeSAID: String   // from step 2
)
→ Boolean
```

If `true`, the ID Holder has proven they control the AID.

### Step 4 — Cardano Wallet Signature

Create a `CardanoSignature` entity with `Payload` = same `ChallengePayload`, associated with the ID Holder's `Wallet`:

```
JA_SignPayloadWithCardanoWallet(
    CardanoSignature: CardanoSignature
    Passphrase:       String
)
→ void
```

### Step 5 — Verify Cardano Signature

```
JA_VerifyCardanoPayloadSignature(
    CardanoSignature: CardanoSignature
)
→ void   // sets IsVerified, SignerAddress
```

Confirm `IsVerified = true`. Optionally confirm that `SignerAddress` matches the wallet address in the challenge message (proves the signing key belongs to that address).

### Step 6 — Query ADA Balance

```
JA_Account_GetBalances(
    Wallet: Wallet
)
→ void   // populates Balance entities associated with Wallet
```

After this action, query `Balance` entities associated with the `Wallet` to read the ADA balance. Display to the Verifier.

## Challenge SAID Validation

As an additional integrity check, you can validate that the stored `KERIChallengeSAID` is non-empty and was recently created (check timestamp). This can be done in a Mendix microflow decision without calling a Java action:

```
$KERIChallengeSAID != empty
AND length($KERIChallengeSAID) > 10
AND $ChallengeCreatedAt > (now - 10 minutes)
```

This guards against replayed or stale challenges in the UI, though the cryptographic verification in steps 3 and 5 provides the actual security guarantee.

## What This Proves

When both steps 3 and 5 pass:

1. **AID control** — The holder signed the challenge with their KERI identity key (edge-only, in the Veridian wallet)
2. **Cardano wallet control** — The custodial wallet signed the same challenge
3. **Same message** — Both signatures cover identical payload, so they cannot be mixed with signatures from other sessions
4. **ADA balance** — Queried live from Blockfrost against the confirmed wallet address

## Limitations

- The Cardano wallet in this prototype is **custodial** (private key stored encrypted in Mendix). In a production system, users would sign with their own non-custodial wallet via a browser or mobile wallet connector.
- Balance is queried at verification time — it is not locked or reserved. A user could transfer ADA immediately after verification.
