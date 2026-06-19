# Developer Documentation

**Landano-Veridian Prototype** — Catalyst Fund 12, Project 1200131

This documentation is aimed at Mendix developers who want to understand, run, or extend the prototype. For a high-level project overview see the [repository README](../README.md).

---

## Getting Started

→ **[Setup Guide](setup.md)**  
Prerequisites, constants configuration, first-run bootstrap, connecting Veridian wallets and Cardano wallets, rebuilding cf-signify-java, and troubleshooting.

---

## How It Works

→ **[Architecture](architecture.md)**  
System diagram, module breakdown, full domain model (entities, attributes, associations), and a complete Java action reference for both the KERIAIntegration and CardanoWallet modules.

---

## Use Cases

→ **[UC1 — Issue ACDC Credential with Chief Authorization](uc1-credential-issuance.md)**  
A Chief approves issuance of a verifiable credential to a Representative by signing in their Veridian wallet. The Mendix backend issues the ACDC credential and delivers it via IPEX Grant.

→ **[UC2 — Verify User Ownership of Landano NFT](uc2-nft-ownership.md)**  
A Land Owner binds their KERI identity and Cardano wallet to an NFT at mint time using dual signatures stored in on-chain metadata. A Verifier can later confirm ownership is intact.

→ **[UC3 — Verify ADA Wallet Balance belongs to an ID Wallet Holder](uc3-wallet-balance.md)**  
A user proves control of both their KERI identity and Cardano wallet using a dual-signature challenge, then their live ADA balance is queried and displayed to a Verifier.

---

## Key Libraries

| Library | Purpose | Used by |
|---------|---------|---------|
| [cf-signify-java](https://github.com/cardano-foundation/cf-signify-java) | KERI protocol — AID management, OOBI, exchange messages, IPEX | KERIAIntegration Java actions |
| [cardano-client-lib](https://github.com/bloxbean/cardano-client-lib) | Cardano transactions, NFT minting, CIP-30 signing | CardanoWallet Java actions |
| [signify-ts](https://github.com/cardano-foundation/signify-ts) | KERI protocol client (TypeScript) | Veridian wallet (not Mendix) |
| [Blockfrost](https://blockfrost.io) | Cardano blockchain API | CardanoWallet Java actions |
