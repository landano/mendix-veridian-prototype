# Landano-Veridian Prototype

**Implementing the Cardano Foundation Identity Wallet in Landano**  
Project Catalyst Fund 12 — Project ID: 1200131

---

## What This Is

This is a Mendix low-code application prototype demonstrating how [Landano](https://landano.io) integrates the [Veridian Identity Wallet](https://github.com/landano/veridian-wallet) (based on the Cardano Foundation Identity Wallet) with land administration workflows.

The prototype proves that Mendix can orchestrate decentralized identity verification using KERI (Key Event Receipt Infrastructure) and the Cardano blockchain — without ever handling private keys. All signing happens at the edge in the user's wallet.

This is a proof-of-concept (PoC) submission, not a production-ready plugin.

---

## Three Use Cases

### Use Case 1: Verify Landano User Identity (Credential Issuance)

In the jurisdictions where Landano operates, a village chief and their representatives have the right to approve land administration transactions for their community.

This use case demonstrates that a particular user holds the credentials to act as a representative for their chief:

1. The **Chief** has a Mendix account linked to their KERI Autonomic Identifier (AID) and Legal Entity Identifier (LEI)
2. The Chief initiates credential issuance to a **Representative** via Mendix
3. Mendix sends an IPEX Apply message to the Chief's Veridian wallet
4. The Chief reviews and approves in their wallet — private key never leaves the device
5. The backend issues the ACDC credential and delivers it to the Representative's wallet via IPEX Grant

### Use Case 2: Verify User Ownership of Landano NFT

Landano NFTs represent rights to specific plots of land. This use case verifies that a user controls both the KERI identity and the Cardano wallet holding the NFT.

**At minting time:** A dual-signature binding is embedded in the NFT metadata — the same binding message is signed by both the user's KERI identity wallet (proving AID control) and their Cardano wallet (proving wallet control). Both signatures are stored immutably on-chain.

**At verification time:** A third-party verifier can confirm:
1. The NFT is still held by the original wallet address
2. The user can still prove live control of their AID (fresh KERI challenge)
3. The stored Cardano signature remains cryptographically valid

### Use Case 3: Verify ADA Wallet Balance belongs to an ID Wallet Holder

Demonstrates how to prove control over a Cardano wallet and its ADA balance in relation to a KERI-based identity. Useful for scenarios where users must prove they have sufficient ADA to complete a transaction.

1. The ID holder binds their Cardano wallet by signing a challenge message with both their identity wallet and Cardano wallet
2. A verifier triggers verification — both signatures are validated against the binding message
3. If verification passes, the ADA balance is queried via Blockfrost and displayed

---

## System Architecture

```
User Devices                    Mendix Backend              Infrastructure
──────────────                  ──────────────              ──────────────
Veridian Wallet                 Mendix Runtime              KERIA (Landano)
 ├─ KERI AID                    ├─ KERIAIntegration         ├─ Cloud Agent
 ├─ Private Keys (edge only)    │   ├─ cf-signify-java      ├─ Message relay
 └─ signify-ts ──────────────────→  └─ Java Actions         └─ Witness coord.
                                │
Cardano Wallet                  ├─ CardanoWallet Module      Cardano Blockchain
 └─ Custodial (platform)        │   ├─ cardano-client-lib   ├─ Preprod testnet
                                │   └─ Blockfrost            └─ NFT metadata
                                └─ Mendix pages/microflows
```

**Key libraries:**
- [`cf-signify-java`](https://github.com/cardano-foundation/cf-signify-java) — Cardano Foundation's Java KERI client (backend ↔ KERIA)
- [`signify-ts`](https://github.com/cardano-foundation/signify-ts) — TypeScript KERI client (Veridian wallet ↔ KERIA)
- [`cardano-client-lib`](https://github.com/bloxbean/cardano-client-lib) — Cardano transaction construction and signing
- [Blockfrost](https://blockfrost.io) — Cardano blockchain API

---

## Prerequisites

To run this prototype locally, you need:

### Software
- **Mendix Studio Pro 11.6.2** — [download from Mendix](https://marketplace.mendix.com/link/studiopro/)
- **Java 21** (for building cf-signify-java) — [Eclipse Adoptium JDK 21](https://adoptium.net/)
- Java 11 is used by Mendix runtime (typically bundled with Studio Pro)

### Accounts and Services
- **KERIA instance** — the prototype connects to `keria.landano.io`. For your own deployment, run a KERIA instance from [cardano-foundation/keria](https://github.com/cardano-foundation/keria)
- **Blockfrost API key** — [Blockfrost preprod project](https://blockfrost.io) (free tier sufficient)
- **Cardano preprod testnet** — all blockchain interactions use the preprod testnet

### Mobile Device
- **Veridian Wallet** — customized build for Landano, connecting to `keria.landano.io`
  - Source: [landano/veridian-wallet](https://github.com/landano/veridian-wallet)
  - Requires building and sideloading the app onto a physical device

---

## Demo User Accounts

The prototype uses four roles. Before running a demo, each user needs:
- A Mendix account with the appropriate role
- A Veridian wallet installed and connected to the Landano KERIA instance
- Their OOBI resolved in the Mendix backend (establishing trusted connection)

| Role | Description |
|------|-------------|
| **Chief** | Village chief — root of trust, issues credentials to Representatives |
| **Representative** | Receives credentials from the Chief, can act on behalf of the community |
| **Land Owner** | Holds Landano NFTs representing land rights |
| **Verifier** | Third party verifying identity, NFT ownership, or ADA wallet balance |

The Land Owner additionally requires a custodial Cardano wallet connected in the Mendix backend.

---

## Running Locally

1. Clone this repository
2. Open `LandanoVeridianPrototype.mpr` in Mendix Studio Pro 11.6.2
3. Configure the following constants in Studio Pro (App → Active Profiles → Constants):

| Constant | Description |
|----------|-------------|
| `KERIAIntegration.KERIA_URL` | KERIA agent URL (e.g. `https://keria.landano.io/agent`) |
| `KERIAIntegration.KERIA_BOOT_URL` | KERIA boot URL (e.g. `https://keria.landano.io/boot`) |
| `KERIAIntegration.KERIA_CONTROLLER_BRAN` | 21-character seed for the backend AID |
| `KERIAIntegration.KERIA_SALTER_TIER` | Key security tier (`low`, `med`, or `high`) |
| `CardanoWallet.BlockfrostApiKey` | Your Blockfrost preprod API key |
| `CardanoWallet.BlockfrostBaseUrl` | `https://cardano-preprod.blockfrost.io/api/v0` |

4. Press **F5** (Run Locally) in Studio Pro
5. The app starts at `http://localhost:8080`

> **Note:** On first run, the backend AID is bootstrapped automatically via the after-startup microflow `ASE_BootstrapBackendAID`. This takes a few seconds and requires the KERIA instance to be reachable.

---

## Repository Structure

```
├── javasource/
│   ├── keriaintegration/actions/   # KERIA + credential issuance Java actions
│   ├── cardanowallet/actions/      # Cardano wallet + NFT Java actions
│   └── communitycommons/           # Utility functions
├── javascriptsource/
│   ├── cardanowallet/actions/      # CIP-30 and wallet JS actions
│   └── signifynative/              # signify-ts for native mobile
├── userlib/
│   └── cf-signify-java-*-all.jar  # cf-signify-java fat jar
├── theme/                          # Web and native styling (SCSS)
├── REQUIREMENTS.md                 # Full functional requirements
├── TEST_CASES.md                   # 15 test cases across all three use cases
├── TEST_REPORT.md                  # Test results — all 15 cases passed
└── DEMO_SCRIPT.md                  # Script for the milestone demo video
```

---

## Building cf-signify-java

The backend KERIA integration depends on a fat jar built from source:

```bash
# Requires Java 21
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot
cd C:\path\to\cf-signify-java
gradlew shadowJar -x test
```

Copy the resulting `build/libs/cf-signify-java-*-all.jar` into `userlib/` and clean the deployment directory in Studio Pro before running.

---

## Known Limitations

This is a proof-of-concept. The following are acknowledged limitations and out-of-scope items for this milestone:

- **Property transfer** via smart contract (with identity verification and metadata updates) is not implemented
- **Credential presentation** (Representative presents ACDC to Verifier via IPEX Disclose) is not implemented
- **Key rotation** — signatures stored in NFT metadata cannot be re-verified after the signer's AID key is rotated; requires metadata update mechanism in a future version
- **Root of trust bootstrapping** — chiefs act as self-sovereign roots of trust; anchoring to a government registry or trusted attestation party is a future requirement
- **CIP-170 metadata standard** — NFT metadata follows a custom `landano` namespace; alignment with CIP-170 is planned for a future iteration
- The Veridian wallet used is a custom Landano build; behavior may differ from the production Cardano Foundation Identity Wallet

---

## Documentation

### Developer Guides
- [docs/setup.md](docs/setup.md) — Prerequisites, constants, first run, user setup, troubleshooting
- [docs/architecture.md](docs/architecture.md) — Module architecture, domain model, Java action reference
- [docs/uc1-credential-issuance.md](docs/uc1-credential-issuance.md) — Credential issuance with Chief authorization
- [docs/uc2-nft-ownership.md](docs/uc2-nft-ownership.md) — NFT dual-signature binding and ownership verification
- [docs/uc3-wallet-balance.md](docs/uc3-wallet-balance.md) — ADA wallet balance verification with dual-signature proof

### Project Documentation
- [REQUIREMENTS.md](REQUIREMENTS.md) — Functional requirements for Milestone 2 and 3
- [TEST_CASES.md](TEST_CASES.md) — Detailed test cases
- [TEST_REPORT.md](TEST_REPORT.md) — Test execution results
- [DEMO_SCRIPT.md](DEMO_SCRIPT.md) — Demo video script

---

## Project Links

- **Landano website:** [landano.io](https://landano.io)
- **Veridian wallet (Landano build):** [github.com/landano/veridian-wallet](https://github.com/landano/veridian-wallet)
- **Project Catalyst proposal:** [Project 1200131](https://milestones.projectcatalyst.io/projects/1200131)
- **Contact:** info@landano.io | [@landanodapp](https://twitter.com/landanodapp)

---

## License

This prototype is published as evidence for the Catalyst Fund 12 milestone submission. See [LICENSE](LICENSE) for terms.
