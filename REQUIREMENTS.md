# Landano-Veridian Prototype Requirements

## Milestone 2: Testing and Refinement

**Delivery Month:** January 2026
**Milestone Cost:** 30% — ADA 30,000

### Outputs
1. Comprehensive testing of the Mendix prototype to identify and resolve critical bugs.
2. Updated prototype that meets all functional requirements as outlined in the initial design document.

### Acceptance Criteria
- Resolution of all identified critical bugs during the testing phase.
- Confirmation from the technical team that the updated prototype meets the predefined functional requirements.

### Evidence of Completion
- Detailed test reports outlining outcomes, issues found during testing, and resolutions applied.
- Demonstration video of the updated prototype showcasing the refined functionalities and compliance with initial design specifications.

---

## Functional Requirements

### Use Case 1: Verify Landano User Identity

In the jurisdictions where we are piloting our solution, a village chief and their representatives have the right to approve land administration transactions for their community. This use case represents this scenario.

We want to demonstrate that a particular user has the credentials to act as a representative for their chief. The chief has the constitutional authority to approve land transactions and is considered to be the root of trust.

1\. Chief has an AID and is an issuer as the root of trust

The Chief has been given a Mendix account and uses their personal ID Wallet (Veridian or similar) to generate and manage their Autonomic Identifier (AID).

The wallet connects to the Cardano Foundation's KERIA instance via signify-ts, maintaining the Chief's cryptographic keys securely on their device.

**Note on Root of Trust Bootstrapping**

In the current pilot, each village chief acts as a root of trust by issuing credentials to their representatives. While this model works at a community level, it introduces a bootstrapping problem: how does a verifier know that a given AID truly belongs to a legitimate chief? For broader scalability, this root of trust may need to be anchored by a trusted attestation party, such as a government registry, traditional authority council, or an NGO that validates chiefs' AIDs. This ensures that verifiers can rely on a federated set of trusted issuers rather than individual chiefs alone.

2\. Chief representative creates AID

The Chief Representative creates their AID using signify-ts through the Cardano Foundation ID Wallet on their device.

3\. Mendix orchestrates credential issuance from Chief to Representative

The credential issuance follows the IPEX (Issuance and Presentation Exchange) protocol with Mendix acting as the orchestrator:

* Mendix prepares the credential details (representative's AID, attributes, permissions) based on business rules and data entry  
* Mendix sends an IPEX Apply message to the Chief's wallet requesting credential issuance  
* The Chief receives a notification in their Veridian wallet showing the proposed credential details  
* The Chief reviews and approves the issuance request directly in their wallet


### Use Case 2: Verify User Ownership of Landano NFT
In the Landano system, users own Cardano NFTs that prove their rights in relation to specific plots of land. This use case represents a scenario where a third party verifies that the user is in control of a Landano NFT.
The system performs comprehensive ownership verification to ensure the user controls both the identity and the blockchain asset through a dual-signature binding stored immutably in the NFT metadata:
Initial Binding (During NFT minting):
- When a Landano NFT is minted, the system generates a binding message containing the AID, Cardano wallet address, NFT identifier, and timestamp
- The user signs this identical message with both their ID wallet (proving AID control) and their Cardano wallet (proving wallet control)
- Both signatures are embedded directly in the NFT metadata along with the AID, creating an immutable record of the legitimate owner
- This dual-signature proof demonstrates that at minting time, the same person controlled both the AID and the Cardano wallet

Ownership Verification (Each time access is requested):
- Query the Cardano blockchain to retrieve the NFT and its complete metadata
- Extract the AID and both binding signatures from the NFT metadata
- Verify the user controls the claimed AID by requesting a fresh signature from their ID wallet
- Check that the NFT is still held by the original wallet address specified in the binding
- Validate both stored signatures against the binding message to ensure the cryptographic proof remains intact
Only grant access if the NFT is held by the original wallet and the user can prove control of the bound AID
Property Transfer Control: Since property rights cannot be transferred by simple wallet-to-wallet NFT transfers, any legitimate ownership change must go through a smart contract that:
- Verifies the current owner's identity through both wallet and AID signatures
- Updates the NFT metadata with the new owner's AID and fresh binding signatures
- Records the transfer authorization and relevant legal documentation
- Ensures the previous owner's binding is properly terminated
This approach ensures that property rights remain correctly tracked and legally compliant, preventing unauthorized transfers through simple wallet movements.

### Use Case 3: Verify ADA Wallet Balance belongs to an ID Wallet Holder
In addition to verifying identity and NFT ownership, the prototype also demonstrates how to prove control over an ADA wallet and its balance in relation to a KERI-based ID Wallet. This is useful in scenarios where users must show they have sufficient ADA to complete a transaction or service.

The system enables a verifier (e.g., government official) to confirm that a Cardano wallet belongs to a specific ID wallet holder:
Verification Process:
- ID holder claims ownership of a Cardano wallet address
- Verifier generates a challenge message: "I, holder of AID [AID], own Cardano wallet [address] [timestamp]"
- ID holder signs this message with their ID wallet (proving AID control)
- ID holder signs the same message with their Cardano wallet (proving wallet control)
- ID holder provides both signatures to the verifier
- Verifier validates both signatures are correct for the claimed AID and wallet address
- If both signatures verify, the verifier can query the ADA balance using the Cardano Mendix Plugin
- Verifier sees the confirmed balance of the verified wallet

This proves to the verifier that the ID holder actually controls the Cardano wallet they claim to own.

## Milestone 3: Final Validation and Documentation

### Outputs
1. Finalized and validated version of the Mendix plugin ready for deployment.
2. Comprehensive documentation and user guides for the Mendix plugin.

### Acceptance Criteria
- Positive feedback from key stakeholders and potential users confirming the viability and usability of the prototype.
- Detailed documentation that provides clear instructions and guidelines for users to effectively utilize the plugin.

### Evidence of Completion
- Publication of the prototype on the Landano GitHub repository (in lieu of Mendix Marketplace publication — the deliverable is a PoC, not a production-ready plugin).
- Availability of detailed documentation and a blog post on the Landano project website (landano.io).