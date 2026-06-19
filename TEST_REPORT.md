# Test Report — Landano-Veridian Prototype (Milestone 2)

**Project:** Implementing the Cardano Foundation Identity Wallet in Landano  
**Project ID:** 1200131  
**Milestone:** 2 — Testing and Refinement  
**Report date:** April 22, 2026  
**Lead tester:** Dorus van der Kroft  

---

## Test Environment

| Item | Details |
|------|---------|
| Mendix Studio Pro | 11.6.2 |
| Cardano network | Preprod testnet |
| KERIA instance | Cardano Foundation testnet |
| Blockfrost API | Preprod |
| Veridian wallet | Custom build — [landano/veridian-wallet](https://github.com/landano/veridian-wallet) |
| Test date | April 22, 2026 |

---

## Summary

All 15 test cases passed. The prototype successfully demonstrates all three functional use cases defined in the milestone requirements:

1. Verifying Landano user identity via ACDC credential issuance with Chief authorization
2. Verifying user ownership of a Landano NFT via dual-signature binding
3. Verifying that an ADA wallet balance belongs to an ID wallet holder

---

## Issues Found and Resolved

The following issues were identified and resolved during the testing phase:

| # | Issue | Severity | Resolution |
|---|-------|----------|------------|
| 1 | CIP-30 signature verification did not validate the signed payload against the expected value — a valid signature on a different payload would pass | Critical | Fixed in `JA_VerifyCardanoPayloadSignature`: payload is now extracted from the COSE_Sign1 structure and compared against the expected value before accepting the signature |
| 2 | Signer address was stored in raw hex format, making it unreadable and unusable for address comparison | Medium | Fixed by converting the raw bytes to bech32 format using the Cardano client library |
| 3 | Blockfrost import mapping treated `onchain_metadata` as a flat string, leaving all child fields empty | High | Replaced with a direct OkHttp + Jackson call in `JA_FetchNFTBindingMetadata` that parses the full JSON structure and reassembles 64-character chunked metadata fields |
| 4 | Asset unit passed to Blockfrost used the ASCII asset name instead of the hex-encoded form | High | Fixed by applying `JA_StringToHex` to the asset name before concatenating with the policy ID |
| 5 | `OnChainKERISAID` attribute auto-detected as `Long` by Studio Pro instead of `String` | Medium | Corrected attribute type to `String` in the domain model; proxy regenerated |
| 6 | `JA_FetchNFTBindingMetadata` created a new `NFTVerification` object internally, making it impossible to reuse the same object across the NFT and Balance verification flows | Medium | Refactored to accept an existing `NFTVerification` as input and mutate it in place, returning void |
| 7 | `IssuedCredential` association to issuer (Chief) was unnamed/missing, requiring XPath string matching on AIDPrefix for page navigation | Low | Added `IssuedCredential_WalletContact_Issuer` association; renamed existing association to `IssuedCredential_WalletContact_Receiver` for clarity |
| 8 | LEI was hardcoded in the credential issuance microflow, causing all credentials to share the same LEI regardless of issuer | Medium | Added `LEI` attribute to `WalletContact`; microflow now reads LEI from the Chief's WalletContact |
| 9 | CardaноScan URL defaulted to mainnet for the minted NFT, causing confusion during metadata verification | Low | Confirmed metadata is correct on preprod; Blockfrost preprod API used for all verification steps |

---

## Test Results

### UC1: Verify Landano User Identity (Credential Issuance)

| ID | Test Case | Result |
|----|-----------|--------|
| TC-01 | Chief can view their identity on the home page | **Pass** |
| TC-02 | Chief can initiate credential issuance to a Representative | **Pass** |
| TC-03 | Chief approves credential in Veridian wallet | **Pass** |
| TC-04 | Credential status updates to Issued after Chief approves | **Pass** |
| TC-05 | Representative can view received credential | **Pass** |
| TC-06 | Credential issuance fails gracefully if Representative has no AID | **Pass** |

**Notes:** TC-05 — LEI is not displayed directly on the Representative's credential screen but is present in the credential data and matches the Chief's LEI.

### UC2: Verify User Ownership of Landano NFT

| ID | Test Case | Result |
|----|-----------|--------|
| TC-07 | Land Owner can sign and mint NFT | **Pass** |
| TC-08 | NFT metadata contains correct binding structure | **Pass** |
| TC-09 | Verifier can verify NFT ownership — all checks pass | **Pass** |
| TC-10 | NFT verification fails if NFT moved to different wallet | **Pass** |
| TC-11 | NFT verification fails if KERI challenge not approved | **Pass** |

**Notes:** TC-08 — CardanoScan URL defaults to mainnet; verified on preprod via Blockfrost directly. TC-10 — Status correctly shows as Failed when NFT is no longer held by the original wallet.

### UC3: Verify ADA Wallet Balance belongs to ID Wallet Holder

| ID | Test Case | Result |
|----|-----------|--------|
| TC-12 | Land Owner can create wallet binding | **Pass** |
| TC-13 | Verifier can verify wallet ownership and view ADA balance | **Pass** |
| TC-14 | Wallet verification fails if Cardano signature invalid | **Pass** |

### Regression

| ID | Test Case | Result |
|----|-----------|--------|
| TC-15 | Existing credential issuance unaffected by domain model changes | **Pass** |

---

## Outstanding Items and Known Limitations

- **Property transfer** (via smart contract with identity verification and metadata update) is out of scope for this prototype and noted as a future enhancement.
- **Presentation flow** (Representative presents ACDC credential to Verifier via IPEX Disclose) is not implemented in this milestone and noted as a future enhancement.
- **Key State** display on WalletContact is not implemented; removed from UI as it is not required for any of the three use cases.
- **Root of trust bootstrapping** — chiefs act as self-sovereign roots of trust in this prototype. Anchoring to a government registry or trusted attestation party is noted as a future requirement for broader scalability.
- The Veridian wallet used is a custom Landano build; behaviour may differ from the production Cardano Foundation Identity Wallet release.

---

## Conclusion

The prototype meets all predefined functional requirements as outlined in the initial design document. All critical bugs identified during testing have been resolved. The prototype is ready for demonstration.

---

## Sign-off

| Name | Role | Date | Sign-off |
|------|------|------|----------|
| Dorus van der Kroft | Lead Developer, Landano | April 22, 2026 | |
| TBD | Veridian / Cardano Foundation Representative | | Via e-mail confirmation following demo |
