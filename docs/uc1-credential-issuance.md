# UC1: Issue ACDC Credential with Chief Authorization

## Overview

A Chief (village chief) authorizes the issuance of a verifiable credential to a Representative. The credential proves the Representative is authorized to act on behalf of the chief's community. The backend issues the credential cryptographically, but only after the Chief has signed the credential payload in their Veridian wallet.

## Actors

| Actor | Role |
|-------|------|
| **Chief** | Approves issuance by signing the credential payload in their Veridian wallet |
| **Representative** | Receives the ACDC credential in their Veridian wallet |
| **Backend (Mendix)** | Orchestrates the flow; acts as cryptographic issuer in KERIA |

## Prerequisites

- Backend AID bootstrapped (`BackendIdentity` record exists)
- Credential registry created (`CredentialRegistry` record exists)
- Chief's wallet connected: `WalletContact` with `ResolvedAt` set for Chief
- Representative's wallet connected: `WalletContact` with `ResolvedAt` set for Representative
- Chief's `WalletContact.LEI` populated (used as credential claim)

## Flow

```
Mendix (Chief's page)          Chief's Veridian Wallet        Mendix Backend (KERIA)
        │                               │                              │
        │── ACT_IssueCredential ────────────────────────────────────→ │
        │                               │                              │
        │   JA_RequestCredentialSignature                              │
        │   ├─ Build ACDC structure + SAIDify                          │
        │   ├─ Send exchange msg (route: /remotesign/ixn/req) ────────→│
        │   └─ Returns ChallengeSAID                                   │
        │                               │                              │
        │                               │←─ Notification arrives ──────│
        │                               │   "Sign credential request"  │
        │                               │                              │
        │                               │  [Chief reviews & signs]     │
        │                               │                              │
        │                               │── Signed response ──────────→│
        │                               │   (route: /remotesign/ixn/ref│
        │                               │                              │
        │   JA_VerifyChallengeResponse                                 │
        │   ├─ Poll KERIA notifications                                 │
        │   ├─ Match: response.exn.p == ChallengeSAID                  │
        │   └─ Returns true                                            │
        │                               │                              │
        │   JA_IssueCredential                                         │
        │   ├─ Load QVI schema into KERIA (if not cached)             │
        │   ├─ Issue ACDC credential (backend AID as issuer)          │
        │   └─ Sets CredentialID, IssuedAt on IssuedCredential        │
        │                               │                              │
        │   JA_GrantCredentialToWallet                                 │
        │   ├─ Send IPEX Grant to Representative's wallet             │
        │   └─ Sets GrantSAID, GrantedAt, Status=Granted             │
        │                               │                              │
        │                               │←─ IPEX Grant arrives ───────│
        │                               │   (Representative's wallet)  │
```

## Microflow Sequence

Call these Java actions in order from a single microflow:

### Step 1 — Request Chief's Signature

```
JA_RequestCredentialSignature(
    Chief:              WalletContact   // Chief's wallet contact
    Representative:     WalletContact   // Representative's wallet contact
    CredentialRegistry: CredentialRegistry
    IssuedCredential:   IssuedCredential  // new, empty entity
    CredentialSubjectJSON: String       // see below
)
→ ChallengeSAID: String
```

**CredentialSubjectJSON format:**
```json
{
  "i": "<Representative AID>",
  "LEI": "<Chief LEI>",
  "dt": "<ISO 8601 timestamp>"
}
```

In the microflow, build this JSON by reading `Representative.AIDPrefix` and `Chief.LEI`, then serializing with `formatDateTime` and string concatenation or a community commons JSON helper.

After this action, `IssuedCredential` has:
- `RequestSAID` set to ChallengeSAID
- `IssuedCredential_WalletContact_Issuer` → Chief
- `IssuedCredential_WalletContact_Receiver` → Representative

Commit `IssuedCredential` at this point so progress is persisted.

### Step 2 — Wait for Chief's Response

```
JA_VerifyChallengeResponse(
    ChallengeSAID: String   // from step 1
)
→ Boolean
```

This action **blocks until** a matching signed response is found or times out. In a production flow, consider running this in a scheduled microflow or background task so the UI is not blocked.

If it returns `false`, the Chief has not yet signed (or timed out). You can retry or surface an error to the user.

### Step 3 — Issue Credential

```
JA_IssueCredential(
    BackendIdentity:      BackendIdentity
    WalletContact:        WalletContact   // the Representative
    CredentialRegistry:   CredentialRegistry
    IssuedCredential:     IssuedCredential
    CredentialSubjectJSON: String         // same JSON as step 1
)
→ void
```

After this action, `IssuedCredential` has `CredentialID`, `IssuerAIDPrefix`, `HolderAIDPrefix`, `IssuedAt`, and `CredentialData` populated.

Commit `IssuedCredential`.

### Step 4 — Deliver to Wallet

```
JA_GrantCredentialToWallet(
    BackendIdentity:  BackendIdentity
    IssuedCredential: IssuedCredential
    WalletContact:    WalletContact   // the Representative
)
→ void
```

After this action, `IssuedCredential.Status` = `Granted` and `GrantSAID`, `GrantedAt` are set.

Commit `IssuedCredential`. The Representative will see the credential in their Veridian wallet.

## Credential Claims (QVI Schema)

The credential uses the GLEIF vLEI QVI schema (`EBfdlu8R27Fbx-ehrqwImnK-8Cm79sqbAQ4MmvEAYqao`). The credential subject must include:

| Field | Value |
|-------|-------|
| `i` | Representative's AID prefix |
| `LEI` | Legal Entity Identifier (from Chief's `WalletContact.LEI`) |
| `dt` | ISO 8601 datetime |

## Error Handling

| Situation | What happens | Recommendation |
|-----------|-------------|----------------|
| Chief does not sign | `JA_VerifyChallengeResponse` returns `false` | Show error and allow retry; the `IssuedCredential` stays in `Requested` state |
| Schema not on vLEI server | `JA_IssueCredential` throws 400 | Check `QVI_SCHEMA_SAID` constant and `VLEI_SERVER_URL` |
| OOBI not resolved | Exchange message fails silently | Verify `WalletContact.ResolvedAt` is set before starting the flow |
| Duplicate registry name | `JA_CreateCredentialRegistry` fails | Change `CREDENTIAL_REGISTRY_NAME` constant |
