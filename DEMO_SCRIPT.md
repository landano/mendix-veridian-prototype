# Demo Script — Landano-Veridian Prototype (Milestone 2)

**Target duration:** ~7 minutes  
**Format:** Screen recording with narration  
**Narrator:** Dorus van der Kroft  
**Presentation date:** 23 April 2026

---

## Slide 1: Title (~15 seconds)

**Screen:** Title slide — "Property rights, secured. For everyone."

**Narration:**
> "Welcome. I'm Dorus van der Kroft, and today I'll be demonstrating the Landano-Veridian proof of concept — our Milestone 2 submission for the Catalyst Fund 12 project implementing the Cardano Foundation Veridian Wallet in Landano."

---

## Slide 2: Agenda (~20 seconds)

**Screen:** Agenda slide

**Narration:**
> "We'll briefly cover the pre-requisites for this integration, what was delivered in Milestone 2, the components that make up the system, and then move straight into the live demonstration."

---

## Slide 3: Pre-requisites (~30 seconds)

**Screen:** Pre-requisites slide

**Narration:**
> "Before we start, a few things are already in place for this demo. Each user has set up the customized Veridian wallet, connecting to the Landano KERIA instance running at keria.landano.io. The Mendix backend has resolved each user's OOBI — establishing trusted connections between all participants. And the land owner has a custodial Cardano wallet connected in the Mendix backend."

---

## Slide 4: Milestone 2 (~30 seconds)

**Screen:** Steps Milestone 2 slide

**Narration:**
> "Milestone 2 focused on three things: replacing the earlier REST endpoint approach with cf-signify-java on the Mendix backend for proper KERIA integration, implementing the three use cases defined in our design document, and completing testing. All three are done."

---

## Slide 5: Components (~45 seconds)

**Screen:** Components diagram — Mendix, KERIA, Veridian Wallet, Cardano Blockchain with connecting libraries

**Narration:**
> "The system is built from four layers. Mendix is the orchestration layer — it coordinates all business logic and user interfaces without ever touching a private key. KERIA, developed by the Cardano Foundation and hosted by Landano, is the cloud agent that relays messages between the backend and users' wallets. The Veridian wallet, customized by Landano, runs on the user's device and holds their KERI Autonomic Identifier — all signing happens here at the edge. And the Cardano blockchain provides the immutable on-chain record, accessed via the Cardano client library and Blockfrost.
>
> The backend connects to KERIA via cf-signify-java, and the wallet connects via signify-ts — both developed by the Cardano Foundation."

---

## Slide 6: Demonstration intro (~15 seconds)

**Screen:** Demonstration slide — three use cases listed

**Narration:**
> "Now let's demonstrate the three use cases. First, verifying a Landano user's identity through credential issuance. Second, verifying ownership of a Landano NFT. And third, verifying that an ADA wallet balance belongs to an ID wallet holder."

---

## Scene 1: UC1 — Credential Issuance (~2 minutes)

### 1a — Chief home page

**Screen:** Log in as Chief → Chief home page

**Narration:**
> "We start as the village chief. The chief has a Mendix account linked to their KERIA Autonomic Identifier — their AID — and a Legal Entity Identifier, or LEI, that identifies their community authority. Both are visible here on the home page."

---

### 1b — Issue credential

**Screen:** Press "Issue Credential" → select Representative → review credential details → submit

**Narration:**
> "The chief wants to issue a verifiable credential to their representative — authorizing them to act on the community's behalf in land transactions. We select the representative, review the credential details — the LEI is automatically populated from the chief's profile — and submit."

---

### 1c — Chief approves in Veridian wallet

**Screen:** Switch to Veridian wallet on mobile — show incoming notification — approve

**Narration:**
> "Mendix sends an IPEX Apply message to the chief's Veridian wallet. The chief receives a notification, reviews the credential, and approves it directly on their device. The private key never leaves the device — signing happens at the edge using the KERI protocol."

---

### 1d — Credential delivered

**Screen:** Back in Mendix — credential status updates to Issued → switch to Representative view — credential visible

**Narration:**
> "Back in Mendix, we verify the chief's signed response. Once confirmed, the backend issues the ACDC credential in KERIA and delivers it to the representative's wallet via IPEX Grant. The representative can now see the credential in their account."

---

## Scene 2: UC2 — NFT Ownership Verification (~2 minutes)

### 2a — Create binding, sign with KERI, and mint

**Screen:** Log in as Land Owner → press "Create Binding" → select NFT binding type → select Cardano wallet → enter unique NFT name → press Save → binding appears on dashboard → press "Sign with KERI" → open Veridian wallet and approve → return to platform → press "Verify KERI" → status updates → press "Sign & Mint" → NFT minted

**Narration:**
> "Now we switch to the land owner. Landano NFTs represent rights to specific plots of land. The land owner starts by creating a binding — selecting the NFT type, the Cardano wallet they want to receive the NFT in, and entering a unique name for the NFT. Once saved, the binding appears on their dashboard.
>
> They then sign the binding message with their KERI identity wallet — opening the Veridian app and approving the request. Back in the platform, they press Verify KERI, which confirms the signature was received. The status updates accordingly.
>
> Finally, they press Sign & Mint. This signs the same binding message with their custodial Cardano wallet and mints the NFT on the blockchain. Both signatures — KERI and Cardano — are embedded directly in the NFT metadata, creating an immutable dual-signature proof of ownership."

---

### 2b — Verify on-chain metadata

**Screen:** Blockfrost preprod — show NFT metadata with AID, cardanoAddress, KERIChallengeSAID, CardanoSignatureHex, CardanoKeyHex

**Narration:**
> "We can verify this directly on-chain. Here is the NFT metadata on the Cardano preprod testnet — the AID, the wallet address, the KERI exchange SAID, and the Cardano signature and key, all stored immutably."

---

### 2c — Verifier verifies ownership

**Screen:** Log in as Verifier → NFT verification page → enter asset unit → press Verify → Land Owner approves KERI challenge in Veridian wallet → result shows Passed

**Narration:**
> "A third party — the verifier — wants to confirm the land owner still controls this NFT. They enter the asset identifier and start the verification. The system queries the blockchain, retrieves the metadata, checks the NFT is still held by the original wallet address, and sends a fresh KERI challenge to the land owner's Veridian wallet to prove live control of their AID.
>
> The land owner approves in their wallet. All three checks pass — the NFT is in the right wallet, the fresh KERI challenge is verified, and the stored Cardano signature is valid. Ownership confirmed."

---

## Scene 3: UC3 — ADA Wallet Balance Verification (~1 minute)

### 3a — Bind wallet

**Screen:** Log in as Land Owner → wallet binding page → press "Bind Wallet" → approve in Veridian wallet → Cardano wallet signs → success

**Narration:**
> "Finally, wallet balance verification. A land owner may need to prove they control a Cardano wallet and that it holds sufficient ADA. The binding works the same way as NFT minting — the same message is signed by both the KERI identity wallet and the Cardano wallet."

---

### 3b — Verifier confirms ownership and balance

**Screen:** Log in as Verifier → wallet verification page → select binding → press Verify → result shows Passed with ADA balance displayed

**Narration:**
> "The verifier selects the binding and runs the verification. The stored KERI signature and Cardano signature are both validated against the original binding message. The system then queries the Cardano blockchain via Blockfrost and displays the confirmed ADA balance. The verifier can be certain this wallet belongs to the identity holder they are dealing with."

---

## Slide 7: Thank you (~20 seconds)

**Screen:** Thank you slide — @landanodapp, info@landano.io, www.landano.io

**Narration:**
> "This prototype demonstrates that Mendix can orchestrate decentralized identity verification using KERI and the Cardano blockchain — without ever handling private keys. All signing happens at the edge in the user's wallet.
>
> For more information, visit us at landano.io, follow us at @landanodapp, or reach out at info@landano.io. Thank you."

---

## Recording Notes

- Use preprod testnet throughout — confirm Blockfrost and CardanoScan URLs point to preprod before recording
- Have the Veridian wallet open and logged in on a second device (or screen share from phone) before starting
- Pre-create all four demo user accounts (Chief, Representative, Land Owner, Verifier) with WalletContacts and AIDs linked
- Do a full dry run before recording — wallet notifications and polling timers need to fire reliably
- Keep a pre-minted NFT asset unit ready for the on-chain metadata scene to avoid waiting for blockchain confirmation mid-recording
- Cut between slides and screen recording smoothly — slides act as chapter markers
- Remove the roadmap content from slide 4 — it references Q1 2025 dates and a different product (the Cardano Mendix Plugin), which is out of context for this milestone demo
