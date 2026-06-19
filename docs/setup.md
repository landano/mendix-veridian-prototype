# Setup Guide

This guide walks a Mendix developer through getting the Landano-Veridian Prototype running locally from scratch.

## Prerequisites

### Mendix Studio Pro
- Version **11.6.2** (exact version required — Mendix model files are version-locked)
- Download from [Mendix Marketplace](https://marketplace.mendix.com/link/studiopro/)

### Java
- **Java 21** is required to build `cf-signify-java` (the KERIA integration library)
- **Java 11** is used by the Mendix runtime (bundled with Studio Pro — no action needed)
- Install Java 21 from [Eclipse Adoptium](https://adoptium.net/) if you need to rebuild the library

### External Services
| Service | Purpose | Notes |
|---------|---------|-------|
| KERIA instance | Cloud agent for KERI identity operations | Landano runs one at `keria.landano.io`; or deploy your own from [cardano-foundation/keria](https://github.com/cardano-foundation/keria) |
| Blockfrost (preprod) | Cardano blockchain API | [Free tier](https://blockfrost.io) is sufficient |
| Cardano preprod testnet | All on-chain transactions | No setup needed — Blockfrost handles connectivity |

### Veridian Wallet
The prototype requires the Landano fork of the Veridian wallet, built for the `keria.landano.io` KERIA instance.

- Source: [github.com/landano/veridian-wallet](https://github.com/landano/veridian-wallet)
- Requires building and sideloading onto a physical iOS or Android device
- A separate KERIA account/bran is needed for each wallet user

---

## Opening the Project

1. Clone the repository
2. Open `LandanoVeridianPrototype.mpr` in Mendix Studio Pro 11.6.2
3. Studio Pro will ask to update the project — decline if prompted to upgrade to a newer version

---

## Configuring Constants

Open **App** → **App Settings** → **Configuration** → **Constants** in Studio Pro and set the following:

### KERIAIntegration Module

| Constant | Example Value | Description |
|----------|---------------|-------------|
| `KERIAIntegration.KERIA_URL` | `https://keria.landano.io/agent` | KERIA admin endpoint (port 3901) |
| `KERIAIntegration.KERIA_BOOT_URL` | `https://keria.landano.io/boot` | KERIA bootstrap endpoint (port 3903) |
| `KERIAIntegration.KERIA_CONTROLLER_BRAN` | `_0123456789abcdefghij` | 21-character base64url seed for the backend AID. **Must be unique per deployment.** Generate with any base64url-safe random string. |
| `KERIAIntegration.KERIA_CONTROLLER_NAME` | `landano-backend` | Alias for the backend AID in KERIA |
| `KERIAIntegration.BACKEND_IDENTIFIER_ALIAS` | `landano-backend` | Should match `KERIA_CONTROLLER_NAME` |
| `KERIAIntegration.KERIA_SALTER_TIER` | `low` | Key security tier. Use `low` for development, `med` or `high` for production |
| `KERIAIntegration.CREDENTIAL_REGISTRY_NAME` | `landano-registry` | Name for the TEL credential registry |
| `KERIAIntegration.QVI_SCHEMA_SAID` | `EBfdlu8R27Fbx-ehrqwImnK-8Cm79sqbAQ4MmvEAYqao` | SAID of the vLEI QVI schema used for ACDC credentials |
| `KERIAIntegration.VLEI_SERVER_URL` | `https://schema.testnet.gleif.org:7723` | Schema server for loading ACDC schemas into KERIA |
| `KERIAIntegration.LogNodeName` | `KERIAIntegration` | Log prefix for Java action output |

### CardanoWallet Module

| Constant | Example Value | Description |
|----------|---------------|-------------|
| `CardanoWallet.BlockfrostApiKey` | `preprod...` | Your Blockfrost preprod project API key |
| `CardanoWallet.BlockfrostBaseUrl` | `https://cardano-preprod.blockfrost.io/api/v0` | Blockfrost preprod base URL |

---

## First Run

### Backend AID Bootstrap

On first startup, the after-startup microflow `ASE_BootstrapBackendAID` runs automatically and:
1. Creates the backend AID in KERIA using the configured `KERIA_CONTROLLER_BRAN`
2. Creates a `BackendIdentity` entity record in the database
3. Creates the TEL credential registry in KERIA
4. Creates a `CredentialRegistry` entity record

This takes 5–15 seconds depending on KERIA latency. Check the Mendix log for `[KERIAIntegration]` entries to confirm success.

**If bootstrap fails:** Check that `KERIA_URL` and `KERIA_BOOT_URL` are reachable, and that `KERIA_CONTROLLER_BRAN` is exactly 21 characters. See [Troubleshooting](#troubleshooting).

On subsequent startups, the microflow detects the existing `BackendIdentity` record and skips creation — it only calls `connect()`, not `boot()`.

### Schema Loading

Before issuing ACDC credentials, the QVI schema must be loaded into KERIA from the vLEI schema server. This happens automatically the first time a credential is issued (the `JA_IssueCredential` action checks the KERIA schema cache and fetches it if missing).

---

## Setting Up Demo Users

Each demo requires user accounts in Mendix with wallets connected. The following roles are used in the prototype:

| Role | What they need |
|------|---------------|
| **Chief** | Mendix account + `WalletContact` record with their AID and OOBI resolved |
| **Representative** | Mendix account + `WalletContact` record with their AID and OOBI resolved |
| **Land Owner** | Mendix account + `WalletContact` + a custodial `Wallet` (Cardano) in the system |
| **Verifier** | Mendix account only |

### Connecting a Veridian Wallet

For each wallet user (Chief, Representative, Land Owner):

1. Open the Veridian wallet on their device and navigate to **Identifiers → Share OOBI**
2. Copy the OOBI URL
3. In the Mendix app, open the **Wallet Contacts** management page
4. Create a new `WalletContact` with the user's name, AID prefix, and OOBI URL
5. The app calls `JA_ResolveWalletOOBI` to establish trust between the backend KERIA agent and the wallet — the `WalletContact.ResolvedAt` field is set on success
6. Associate the `WalletContact` with the Mendix `Account` for that user

### Connecting a Cardano Wallet (Land Owner)

1. Open the **Cardano Wallets** section in the app
2. Create a new wallet using `JA_Account_GenerateMnemonics` (generates a new wallet) or `JA_Account_CreateFromMnemonic` (imports an existing one)
3. Fund the wallet address with test ADA from the [Cardano preprod faucet](https://docs.cardano.org/cardano-testnets/tools/faucet/)

---

## Rebuilding cf-signify-java

The KERIA integration depends on a fat jar at `userlib/cf-signify-java-0.1.2-c2d9024-SNAPSHOT-all.jar`. You only need to rebuild this if you are updating to a newer version of the library.

```bash
# Set JAVA_HOME to Java 21 (required — does not build on Java 11)
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot

cd C:\path\to\cf-signify-java
gradlew shadowJar -x test
```

Copy the resulting `build/libs/cf-signify-java-*-all.jar` into the `userlib/` folder.

> **Important:** After replacing the jar, use **App → Clean Deployment Directory** in Studio Pro before running. Stale jars in `deployment/model/lib/userlib/` cause `NoSuchMethodError` at runtime.

After replacing the jar, run **Deploy for Eclipse** (F6) in Studio Pro to regenerate Java proxy classes. A regular run (F5) does not regenerate proxies.

---

## Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `Agent does not exist` | Backend AID not bootstrapped | Check constants and restart; look for errors in the log during startup |
| `Commitment mismatch` | KERIA agent and local controller are out of sync | Delete the `BackendIdentity` record and restart to re-bootstrap (this creates a new AID — only do this in dev) |
| `NoSuchMethodError` on signify classes | Old jar in deployment directory | Clean deployment directory in Studio Pro |
| `cannot be resolved` in IDE | Stale IDE index | Expected — Mendix build works fine; ignore or run Deploy for Eclipse |
| `400 Bad Request` on credential issue | Schema not loaded in KERIA | Check `VLEI_SERVER_URL` is reachable and `QVI_SCHEMA_SAID` is correct |
| Wallet response never arrives in `JA_VerifyChallengeResponse` | Challenge route not recognized by wallet | Ensure the route is `/remotesign/ixn/req` — custom routes are silently ignored by the wallet |
| `Optional.empty()` on identifier lookup | Backend AID not yet created | Run after startup completes; check `BackendIdentity` record exists in database |
