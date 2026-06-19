# Architecture

## Overview

The prototype consists of two Mendix modules that extend the base platform:

```
┌─────────────────────────────────────────────────────────────┐
│                      Mendix Runtime                          │
│                                                             │
│  ┌──────────────────────┐   ┌───────────────────────────┐  │
│  │  KERIAIntegration    │   │     CardanoWallet          │  │
│  │                      │   │                            │  │
│  │  Java Actions        │   │  Java Actions              │  │
│  │  ├─ KERIA comms      │   │  ├─ Wallet management      │  │
│  │  ├─ Credential issue │   │  ├─ NFT minting            │  │
│  │  ├─ Challenge/verify │   │  ├─ Signature sign/verify  │  │
│  │  └─ NFT binding      │   │  └─ Blockfrost queries     │  │
│  │                      │   │                            │  │
│  │  Domain Model        │   │  Domain Model              │  │
│  │  ├─ BackendIdentity  │   │  ├─ Wallet                 │  │
│  │  ├─ WalletContact    │   │  ├─ CardanoSignature        │  │
│  │  ├─ IssuedCredential │   │  ├─ Asset / Balance        │  │
│  │  ├─ NFTBinding       │   │  └─ NFTNP (mint params)    │  │
│  │  └─ NFTVerification  │   │                            │  │
│  └──────────┬───────────┘   └──────────────┬─────────────┘  │
│             │                              │               │
└─────────────┼──────────────────────────────┼───────────────┘
              │ cf-signify-java              │ cardano-client-lib
              ▼                              ▼
        KERIA Agent                   Cardano Network
        (keria.landano.io)            (via Blockfrost)
```

**cf-signify-java** (`userlib/cf-signify-java-*-all.jar`) handles all KERI protocol operations: AID management, OOBI resolution, exchange messages, IPEX grants.

**cardano-client-lib** handles Cardano transaction building, signing, and NFT minting. Blockchain queries go through Blockfrost.

---

## KERIAIntegration Module

### Domain Model

#### BackendIdentity
The Mendix backend's own KERI identity. There is exactly one active `BackendIdentity` per deployment.

| Attribute | Type | Description |
|-----------|------|-------------|
| `AIDPrefix` | String | The backend's AID (e.g. `EAbcd...`) |
| `IdentifierAlias` | String | Alias used in KERIA (matches `KERIA_CONTROLLER_NAME` constant) |
| `CreatedAt` | DateTime | When the AID was bootstrapped |
| `IsActive` | Boolean | Soft-delete flag |

#### WalletContact
Represents a user whose Veridian wallet has been connected (OOBI resolved). One per user that interacts with KERIA.

| Attribute | Type | Description |
|-----------|------|-------------|
| `AIDPrefix` | String | The user's AID |
| `Alias` | String | Display name |
| `OOBI` | String | The OOBI URL used to resolve this contact |
| `ResolvedAt` | DateTime | When `JA_ResolveWalletOOBI` succeeded |
| `KeyState` | String | Raw JSON key state (informational; not updated automatically) |
| `LEI` | String | Legal Entity Identifier — used as a credential claim |

**Association:** `WalletContact_Account` → `Administration.Account`

#### CredentialRegistry
The TEL (Transaction Event Log) registry in KERIA that tracks credential issuance and revocation status.

| Attribute | Type | Description |
|-----------|------|-------------|
| `RegistryID` | String | Registry SAID returned by KERIA |
| `RegistryName` | String | Human-readable name |
| `IdentifierAlias` | String | Issuer AID alias |
| `CreatedAt` | DateTime | |
| `IsActive` | Boolean | |

**Association:** `CredentialRegistry_BackendIdentity` → `BackendIdentity`

#### IssuedCredential
Tracks each ACDC credential issued by the backend.

| Attribute | Type | Description |
|-----------|------|-------------|
| `CredentialID` | String | SAID of the issued ACDC credential |
| `SchemaSAID` | String | Schema used (default: QVI schema) |
| `IssuerAIDPrefix` | String | Backend AID (cryptographic issuer) |
| `HolderAIDPrefix` | String | Recipient's AID |
| `AuthorizedByAIDPrefix` | String | Chief's AID (who approved) |
| `RequestSAID` | String | SAID of the Chief's signed exchange message (correlation ID) |
| `CredentialSubjectJSON` | String | The credential claims as JSON |
| `CredentialData` | String | Full ACDC credential JSON |
| `Status` | Enum | `Issued`, `Granted`, `Revoked` |
| `IssuedAt` | DateTime | |
| `GrantedAt` | DateTime | |
| `GrantSAID` | String | SAID of the IPEX Grant message |

**Associations:**
- `IssuedCredential_WalletContact_Receiver` → the Representative (holder)
- `IssuedCredential_WalletContact_Issuer` → the Chief (authorizer)
- `IssuedCredential_CredentialRegistry` → the registry used
- `IssuedCredential_BackendIdentity` → the issuing backend identity

#### NFTBinding
Created when a Land Owner binds their KERI identity to a Cardano NFT.

| Attribute | Type | Description |
|-----------|------|-------------|
| `BindingMessage` | String | SAIDified JSON binding message |
| `CardanoAddressHex` | String | The Cardano wallet address in hex |
| `NFTPolicyId` | String | Policy ID of the NFT to be minted |
| `NFTAssetName` | String | Asset name (unique identifier) |
| `KERIChallengeSAID` | String | SAID of the KERI exchange message proving AID control |
| `CardanoSignatureHex` | String | COSE_Sign1 hex from the Cardano wallet |
| `CardanoKeyHex` | String | COSE_Key hex from the Cardano wallet |
| `TransactionID` | String | Cardano transaction ID of the mint |
| `Status` | Enum | `Pending`, `Signed`, `Verified` |
| `BindingType` | Enum | Type of NFT binding (e.g. land right) |

**Associations:**
- `NFTBinding_WalletContact` → Land Owner's wallet contact
- `NFTBinding_Wallet` → Cardano wallet used
- `NFTBinding_CardanoSignature` → Cardano signature record

#### NFTVerification
Used by the Verifier role to check NFT ownership.

| Attribute | Type | Description |
|-----------|------|-------------|
| `AssetUnit` | String | Combined `{policyId}{assetNameHex}` |
| `PolicyId` | String | |
| `AssetName` | String | |
| `Status` | Enum | Verification result |

---

### Java Actions Reference

All Java actions in the `KERIAIntegration` module are in `javasource/keriaintegration/actions/`.

#### JA_BootstrapBackendAID
Creates the backend AID in KERIA on first run. Called by the after-startup microflow.

```
Input:  BackendIdentity (empty entity to populate)
Output: void — populates AIDPrefix, IdentifierAlias, CreatedAt on the entity
```

Internally calls `client.boot()` then `client.connect()`. If the AID already exists in KERIA (identified by the bran), it skips `boot()`.

---

#### JA_GetBackendOOBI
Retrieves the backend's OOBI URL so wallet users can resolve the backend as a contact in their wallet.

```
Input:  BackendIdentity
Output: String — OOBI URL (share this with users so their wallets trust the backend)
```

---

#### JA_ResolveWalletOOBI
Resolves a user's wallet OOBI in the backend's KERIA agent. This establishes mutual trust and is required before any challenge/credential flow.

```
Input:  WalletContact (with AIDPrefix and OOBI set), WalletOOBI String
Output: void — sets WalletContact.ResolvedAt on success
```

Must be called once per wallet user before any other interaction.

---

#### JA_CreateCredentialRegistry
Creates the TEL registry in KERIA. Called by the after-startup microflow.

```
Input:  BackendIdentity, CredentialRegistry (empty entity to populate)
Output: void — populates RegistryID, RegistryName, CreatedAt
```

---

#### JA_SendChallenge
Sends an arbitrary payload to a wallet via KERIA exchange message. The wallet user sees a signing request in their Veridian wallet.

```
Input:  BackendIdentity, WalletContact (the target), ChallengePayload (String JSON)
Output: String — the exchange SAID (use as ChallengeSAID in JA_VerifyChallengeResponse)
```

The route used is `/remotesign/ixn/req` — this is the only route the Veridian wallet processes. Custom routes are silently ignored.

---

#### JA_VerifyChallengeResponse
Polls KERIA notifications to find the wallet's signed response to a challenge. Matches response to request via `exn.p == ChallengeSAID`.

```
Input:  ChallengeSAID (String — SAID returned by JA_SendChallenge)
Output: Boolean — true if a matching signed response was found
```

Fetches up to 1000 notifications (KERIA returns newest-first). Times out if no matching response is found after exhausting all notifications.

---

#### JA_RequestCredentialSignature
Builds a full ACDC credential structure, SAIDifies it, and sends it to the Chief's wallet for authorization. This is the first step in credential issuance.

```
Input:  Chief (WalletContact), Representative (WalletContact),
        CredentialRegistry, IssuedCredential (to populate),
        CredentialSubjectJSON (String — the credential claims)
Output: String — the challenge SAID to pass to JA_VerifyChallengeResponse
```

Also sets `IssuedCredential.RequestSAID`, `IssuedCredential_WalletContact_Issuer`, and `IssuedCredential_WalletContact_Receiver`.

---

#### JA_IssueCredential
Issues the ACDC credential in KERIA with the backend AID as cryptographic issuer.

```
Input:  BackendIdentity, WalletContact (the recipient/Representative),
        CredentialRegistry, IssuedCredential, CredentialSubjectJSON
Output: void — populates CredentialID, IssuerAIDPrefix, HolderAIDPrefix,
               CredentialData, IssuedAt on IssuedCredential
```

Loads the QVI schema from the vLEI server into KERIA cache if not already present.

---

#### JA_GrantCredentialToWallet
Delivers the issued credential to the Representative's wallet via IPEX Grant.

```
Input:  BackendIdentity, IssuedCredential, WalletContact (recipient)
Output: void — sets GrantSAID, GrantedAt, Status=Granted on IssuedCredential
```

---

#### JA_BuildBindingMessage
Creates the SAIDified binding message that both KERI and Cardano wallets will sign.

```
Input:  AIDPrefix (String), CardanoAddress (String — bech32 format),
        NFTPolicyId (String), NFTAssetName (String)
Output: String — SAIDified JSON binding message
```

The message includes a SAID (`d`) field as a self-addressing identifier, making it tamper-evident.

---

#### JA_FetchNFTBindingMetadata
Fetches NFT on-chain metadata via Blockfrost and populates the `NFTVerification` entity.

```
Input:  NFTVerification (with AssetUnit set), AssetUnit (String),
        BlockfrostAPIKey (String), BlockfrostBaseUrl (String)
Output: void — populates metadata fields on NFTVerification
```

---

## CardanoWallet Module

### Key Java Actions for This Prototype

The `CardanoWallet` module has 30+ Java actions. The ones directly used in the identity flows are:

| Action | Purpose |
|--------|---------|
| `JA_NFT_Mint` | Mints an NFT with custom metadata (used to embed binding signatures) |
| `JA_SignPayloadWithCardanoWallet` | Signs arbitrary payload with a custodial wallet (CIP-30 server-side) |
| `JA_VerifyCardanoPayloadSignature` | Verifies a CIP-30 signature and extracts the signer address |
| `JA_Account_GetBalances` | Queries ADA and native asset balances via Blockfrost |
| `JA_GenerateAssetUnitId` | Combines policy ID + asset name into a Blockfrost asset unit string |

### CIP-30 Signing Pattern

The prototype uses **custodial server-side signing** (not browser-based CIP-30). Private keys are stored encrypted in the Mendix database.

**Sign:**
```java
// CardanoSignature entity: set Payload, associate Wallet
JA_SignPayloadWithCardanoWallet(cardanoSignature, passphrase)
// → sets SignatureHex (COSE_Sign1 hex), KeyHex (COSE_Key hex), SignerAddress
```

**Verify:**
```java
JA_VerifyCardanoPayloadSignature(cardanoSignature)
// → sets IsVerified (boolean), SignerAddress (extracted from COSE headers)
```

### NFT Minting Pattern

```java
// 1. Create NFTNP (non-persistable) entity with:
//    - metadataJSON: custom JSON under "landano" namespace
//    - ReceiverAddress: destination wallet
//    - policyId: null for auto-create or set existing PolicyId
// 2. Call JA_NFT_Mint(transactionNP, passphrase)
// → returns transaction ID
```

---

## Microflow Naming Conventions

| Prefix | Usage |
|--------|-------|
| `ACT_` | UI button actions (called directly from pages) |
| `SUB_` | Sub-microflows (reusable logic, not called from UI directly) |
| `ASE_` | After-startup execution microflows |
| `DS_` | Data source microflows (for list views, data grids) |

---

## Security Notes

- The `KERIA_CONTROLLER_BRAN` constant is the seed for the backend AID's private key. Treat it as a secret — use environment-specific constants or Mendix secrets management in production.
- Cardano wallet mnemonics are stored **encrypted** in the database using `JA_EncryptMnemonic`. The passphrase is not stored — users must provide it at signing time.
- `WalletContact.OOBI` is stored in the database but is not sensitive — OOBIs are public by design.
- Private keys in the Veridian wallet **never leave the device** — KERIA only sees signed messages.
