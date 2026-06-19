# Test Cases — Landano-Veridian Prototype (Milestone 2)

## Test Environment
- Mendix Studio Pro version: TBD
- KERIA instance: Cardano Foundation testnet
- Cardano network: testnet (preprod)
- Blockfrost: preprod API
- Veridian wallet: TBD (version)
- Test date: TBD
- Testers: Dorus van der Kroft, Veridian representative

---

## UC1: Verify Landano User Identity (Credential Issuance)

### TC-01: Chief can view their identity on the home page
**Precondition:** Chief user account exists with WalletContact linked to a KERIA AID and LEI set  
**Steps:**
1. Log in as Chief
2. Navigate to Chief home page
**Expected:** AID prefix, LEI, and name are displayed correctly  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

### TC-02: Chief can initiate credential issuance to a Representative
**Precondition:** Representative WalletContact exists with AID prefix set  
**Steps:**
1. Log in as Chief
2. Press "Issue Credential"
3. Select the Representative
4. Confirm the credential details (LEI is populated from Chief's WalletContact)
5. Press submit
**Expected:** IPEX Apply message sent to Chief's Veridian wallet; credential appears with status "Pending Signature"  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

### TC-03: Chief approves credential in Veridian wallet
**Precondition:** TC-02 completed successfully; Chief has Veridian wallet running  
**Steps:**
1. Open Veridian wallet on Chief's device
2. Locate the incoming credential signing request notification
3. Review the credential details
4. Approve the request in the wallet
**Expected:** Wallet signs the credential; notification visible in wallet  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

### TC-04: Credential status updates to Issued after Chief approves
**Precondition:** TC-03 completed  
**Steps:**
1. Log in as Chief (or wait for poll cycle)
2. Check the credential and press verify
**Expected:** Credential status changes from "Pending Signature" to "Issued"; credential delivered to Representative's wallet via IPEX Grant  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

### TC-05: Representative can view received credential
**Precondition:** TC-04 completed  
**Steps:**
1. Log in as Representative
2. Navigate to the credential overview page
**Expected:** Issued credential is visible with correct LEI, issuer AID, and holder AID  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**
LEI not visible on screen but matches LEI of Chief and LEI in Credential
---

### TC-06: Credential issuance fails gracefully if Representative has no AID
**Precondition:** A WalletContact exists with no AIDPrefix set  
**Steps:**
1. Log in as Chief
2. Attempt to issue credential to the AID-less contact
**Expected:** Clear error message shown; no credential created  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

## UC2: Verify User Ownership of Landano NFT

### TC-07: Land Owner can sign and mint NFT
**Precondition:** Land Owner account exists with WalletContact (AID), Cardano wallet connected  
**Steps:**
1. Log in as Land Owner
2. Navigate to NFT minting page
3. Enter NFT details (asset name)
4. Press "Sign & Mint"
5. Approve signing request in Veridian wallet
6. Confirm Cardano wallet signing (CIP-30)
**Expected:** NFT minted on Cardano testnet; binding message signed by both KERI AID and Cardano wallet; both signatures stored in NFT metadata  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

### TC-08: NFT metadata contains correct binding structure
**Precondition:** TC-07 completed  
**Steps:**
1. Query the minted NFT via Blockfrost using the asset unit or by checking cardanoscan 
2. Inspect `onchain_metadata`
**Expected:** Metadata contains `aid`, `cardanoAddress`, `bindingMessage`, `KERIChallengeSAID`, `CardanoSignatureHex`, `CardanoKeyHex`; chunked fields reassemble correctly  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**
The transaction showed mainnet in the url, after changing it to preprod I was able to verify the onchain data
---

### TC-09: Verifier can verify NFT ownership — all checks pass
**Precondition:** TC-07 completed; Land Owner is active and has Veridian wallet running  
**Steps:**
1. Log in as Verifier
2. Navigate to NFT verification page
3. Enter asset unit (policy ID + asset name hex)
4. Press "Verify Ownership"
5. Land Owner approves KERI challenge in Veridian wallet
**Expected:** All three checks pass — NFT held by original wallet, KERI challenge verified, Cardano signature valid; OverallResult = true; Status = Passed  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

### TC-10: NFT verification fails if NFT moved to different wallet
**Precondition:** NFT transferred to a different wallet address (manually via testnet)  
**Steps:**
1. Log in as Verifier
2. Run verification for the moved NFT
**Expected:** IsNFTHeldByOriginalWallet = false; OverallResult = false; Status = Failed  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**
Passed, status shows as failed
---

### TC-11: NFT verification fails if KERI challenge not approved
**Precondition:** TC-07 completed  
**Steps:**
1. Log in as Verifier
2. Start verification
3. Do NOT approve the KERI challenge in Veridian wallet (let it time out)
**Expected:** IsKERIChallengeVerified = false; OverallResult = false; Status = Failed  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

## UC3: Verify ADA Wallet Balance belongs to ID Wallet Holder

### TC-12: Land Owner can create wallet binding
**Precondition:** Land Owner account exists with WalletContact (AID) and Cardano wallet connected  
**Steps:**
1. Log in as Land Owner
2. Navigate to wallet balance verification page
3. Press "Bind Wallet"
4. Approve KERI signing request in Veridian wallet
5. Confirm Cardano wallet signing (CIP-30)
**Expected:** Binding message signed by both KERI AID and Cardano wallet; NFTBinding created with BindingType = Balance  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

### TC-13: Verifier can verify wallet ownership and view ADA balance
**Precondition:** TC-12 completed  
**Steps:**
1. Log in as Verifier
2. Navigate to wallet verification page
3. Select the Land Owner's binding
4. Press "Verify"
**Expected:** Both KERI SAID and Cardano signature verified; ADA balance queried from Blockfrost and displayed; OverallResult = true  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

### TC-14: Wallet verification fails if Cardano signature invalid
**Precondition:** A binding exists with a tampered/incorrect CardanoSignatureHex  
**Steps:**
1. Log in as Verifier
2. Run verification against the tampered binding
**Expected:** IsCardanoSignatureValid = false; OverallResult = false  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

## Regression

### TC-15: Existing credential issuance unaffected by domain model changes
**Precondition:** A credential was previously issued  
**Steps:**
1. Verify IssuedCredential has both WalletContact_Receiver and WalletContact_Issuer set correctly
**Expected:** Receiver = Representative, Issuer = Chief  
**Result:** [ x ] Pass / [ ] Fail  
**Notes:**

---

## Sign-off

| Tester | Role | Date | Signature |
|--------|------|------|-----------|
| Dorus van der Kroft | Lead Developer | | |
| TBD | Veridian Representative | | |
