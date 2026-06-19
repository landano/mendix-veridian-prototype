# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Mendix Low-Code Application prototype for the Landano-Veridian integration, implementing decentralized identity verification and credentialing using KERI (Key Event Receipt Infrastructure) and ACDC (Authentic Chained Data Containers) protocols. The app provides secure, edge-based key management for both web and native mobile deployments.

### Requirements
Full requirements are in [REQUIREMENTS.md](REQUIREMENTS.md). Key use cases:

1. **Issue ACDC Credential with Chief Authorization** (Completed)
   - Chief authorizes credential in wallet, backend issues to Representative
   - Flow: JA_RequestCredentialSignature → JA_VerifyChallengeResponse → JA_IssueCredential → JA_GrantCredentialToWallet

2. **Verify User Ownership of Landano NFT** (Next)
   - Verify user controls both a KERI AID and the Cardano wallet holding an NFT
   - Dual-signature binding: AID wallet signature + Cardano wallet signature embedded in NFT metadata at mint time
   - Verification: query blockchain for NFT metadata, extract AID + binding signatures, request fresh AID signature, confirm NFT still in original wallet, validate stored signatures
   - Property transfers require smart contract with identity verification and metadata updates

3. **Verify ADA Wallet Balance belongs to ID Wallet Holder**
   - Prove Cardano wallet ownership via dual-signature challenge (AID + Cardano wallet)
   - Verifier generates challenge message with AID, wallet address, and timestamp
   - Both signatures verified, then ADA balance queried via Cardano Mendix Plugin

## Essential Commands

### Build Commands
```bash
# Clean and build the project
gradlew clean build

# Build deployment package
gradlew build

# Run locally (requires Mendix Studio Pro)
# Open the .mpr file in Mendix Studio Pro and use Run Locally (F5)
```

### Development Workflow
1. Model changes: Edit `LandanoVeridianPrototype.mpr` in Mendix Studio Pro
2. Custom Java: Add/modify Java actions in `javasource/[module]/actions/`
3. JavaScript: Add/modify JS actions in `javascriptsource/[module]/actions/`
4. Styling: Edit SCSS files in `theme/web/` or native styles in `theme/native/`

## Architecture

### Key Directories
- **javasource/**: Custom Java actions organized by module
  - signifyweb/signifynative: Identity verification (backend uses cf-signify-java)
  - keriaintegration: KERIA agent integration module
  - communitycommons: Utility functions
- **javascriptsource/**: Client-side JavaScript/TypeScript code (native uses signify-ts)
- **widgets/**: UI components (.mpk files) for web and native
- **deployment/**: Build outputs and runtime files

### Important Integration Points
- **Signify Integration**: 
  - Configuration in javasource/signifyweb/config/Constants.java and signifynative/config/Constants.java
  - URLs: BootURL and ConnectURL for identity services
- **QR Code**: Multiple QR widgets for scanning/generating codes
- **Native Features**: Camera, notifications, and other device capabilities

### Custom Modules
- SignifyWeb/SignifyNative: Core identity verification
- CommunityCommons: Common utilities (file handling, string operations, etc.)
- NanoflowCommons: Client-side logic utilities
- DataWidgets: Data visualization components

## Development Notes

- This is a Mendix project - most business logic is modeled visually in Studio Pro
- Custom Java actions extend platform capabilities
- Native mobile uses React Native under the hood
- Web deployment uses standard Mendix runtime
- No automated testing framework present - testing typically done in Studio Pro or manually

## Common Tasks

### Adding Custom Java Action
1. Create in Studio Pro: Right-click module → Add other → Java action
2. Implement in `javasource/[module]/actions/[ActionName].java`
3. Use executeAction() method for logic

### Modifying Styling
- Web: Edit SCSS in `theme/web/`
- Native: Edit styles in `theme/native/`
- Run build after changes

### Working with Widgets
- Place .mpk files in `widgets/`
- Restart Studio Pro to recognize new widgets
- Configure in page editor

## Mendix Development Patterns

### Domain Model Design
Mendix uses visual domain modeling in Studio Pro. Key concepts:
- **Entities**: Database tables (stored in PostgreSQL at runtime)
- **Associations**: Relationships between entities (1-*, *-*)
- **Attributes**: Entity properties with types (String, Integer, DateTime, etc.)
- **Generalization**: Inheritance between entities
- **Non-persistable entities**: In-memory only, ideal for sensitive data

### Microflows vs Nanoflows
- **Microflows**: Server-side logic, full database access, synchronous
- **Nanoflows**: Client-side logic, offline-capable, better for mobile
- **JavaScript Actions**: Custom client-side code for native APIs and complex operations

### Native Mobile Architecture
- Built on React Native
- Offline-first data synchronization
- Local SQLite database (can be encrypted)
- Access to device APIs via JavaScript actions
- Mendix Client manages data sync and conflict resolution

### Security Layers
1. **Module Security**: Role-based access control
2. **Entity Access**: CRUD permissions per user role
3. **Page Access**: Restrict pages by role
4. **Microflow Access**: Control who can execute server logic
5. **Attribute Security**: Field-level permissions

### JavaScript Action Structure
```javascript
// Generated wrapper
export async function ActionName(param1, param2) {
    // BEGIN USER CODE
    // Your implementation here
    // Access Mendix APIs via mx.* 
    // Return promises for async operations
    // END USER CODE
}
```

### Local Storage in Native Apps
- **AsyncStorage**: Basic key-value storage (unencrypted)
- **iOS Keychain**: Secure, hardware-encrypted storage via Secure Enclave
- **Android Keystore**: Hardware-backed key storage via TEE/StrongBox
- **Mendix Encryption**: Automatic database encryption with keys in OS secure storage

## Veridian Platform Integration

### Overview
Veridian is a decentralized identity platform built on KERI (Key Event Receipt Infrastructure) providing:
- Self-sovereign identity management
- Verifiable credentials
- Quantum-resistant cryptography
- Key compromise recovery

### Core Concepts

#### AIDs (Autonomous Identifiers)
- Self-certifying identifiers derived from cryptographic keys
- Controlled solely by the holder of the private keys
- Can be rotated for key compromise recovery

#### KERI (Key Event Receipt Infrastructure)
- Distributed ledger for key events
- Provides duplicity detection
- Enables key rotation without losing identity
- Witnesses provide consensus without blockchain

#### ACDC (Authentic Chained Data Containers)
- Verifiable credential format
- Supports selective disclosure
- Chain-linked for provenance
- Schema-based validation

### Veridian Components

#### 1. Wallet (Mobile)
- Manages DIDs and private keys
- Signs credentials and presentations
- Biometric authentication
- Edge-only key storage

#### 2. Cloud Agents (KERIA)
- Hosted identity agents
- Message relay and storage
- Witness network coordination
- No access to private keys

#### 3. Witnesses
- Distributed verification nodes
- Receipt logs for key events
- Duplicity detection
- High availability infrastructure

### Integration Architecture

```
Native Mobile App              Backend (Mendix Runtime)
       ↓                              ↓
SignifyTS (JavaScript)         cf-signify-java (Java)
       ↓                              ↓
       └──────→ KERIA Agent ←─────────┘
                    ↓
              Witness Network
```

- **Native path**: Nanoflows → JavaScript Actions → signify-ts
- **Backend path**: Microflows → Java Actions → cf-signify-java

### Key Security Features
- **Edge-Only Keys**: Private keys never leave device
- **Hardware Security**: Keychain/Keystore integration
- **Quantum Resistance**: Pre-rotation commitments
- **Recovery**: Key rotation preserves identity

## SignifyTS Integration (Native Mobile Only)

> **Note**: SignifyTS is exclusively for native mobile development (nanoflows and JavaScript actions). For backend/server-side KERI operations, use cf-signify-java (see below).

### Key Components
The app uses SignifyTS (TypeScript implementation of KERI) located in:
`javascriptsource/signifynative/node_modules/signify-ts/`

### Core Classes
- **SignifyClient**: Main client for KERIA interaction
- **Identifier**: AID management
- **Credentials**: Issue and verify credentials
- **KeyManager**: Cryptographic operations
- **Salter**: Key derivation from passwords/PINs

### Cryptographic Stack
- **Ed25519**: Signing algorithm
- **X25519**: Encryption (ECDH)
- **Blake3**: Hashing
- **LibSodium**: Core crypto library
- **PBKDF2**: Key derivation from PINs

### Implementation Pattern
```javascript
// 1. Initialize client with PIN-derived seed
const bran = deriveFromPIN(userPIN); // 21 chars
const client = new SignifyClient(url, bran, tier);

// 2. Create/recover identity
await client.boot(); // First time
await client.connect(); // Subsequent

// 3. Create AID
const aid = await client.identifiers().create({
    name: "user-identity",
    transferable: true
});

// 4. Sign data
const sig = await client.sign(data);

// 5. Issue credential
const cred = await client.credentials().issue({
    schema: "EBxxx...",
    recipient: "did:keri:...",
    data: claimData
});
```

## cf-signify-java Backend Integration

### Overview
For server-side KERI operations in microflows and Java actions, use the **cf-signify-java** module from Cardano Foundation.

### Version & Build
- **Version**: 0.1.2-c2d9024-SNAPSHOT (fat jar via shadow plugin)
- **Fat jar**: `userlib/cf-signify-java-0.1.2-c2d9024-SNAPSHOT-all.jar` (~17MB)
- **Build source**: `C:\Mendix\Dev\cf-signify-java`
- **Build command**: `set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.4.7-hotspot && gradlew shadowJar -x test`
- Requires Java 21 for building (Mendix runtime uses Java 11)

### API Changes in 0.1.2 (Important!)
Several methods now return `Optional<T>` instead of `T` directly:
- `client.identifiers().get(name)` → `Optional<HabState>` (use `.orElseThrow()`)
- `client.oobis().get(alias, role)` → `Optional<Map>` (unwrap before parsing)
- `client.credentials().get(id)` → `Optional<Object>` (use `.orElseThrow()`)
- `client.oobis().resolve(url, alias)` — requires TWO params (alias can be `""`)
- `RegistryResult.op()` → returns String (raw body), need `Operation.fromObject()`
- `IssueCredentialResult.getOp()` → returns `Operation<?>` directly (no fromObject needed)

### Key Classes
- **SignifyClient**: Main entry point for KERIA agent communication
- **Identifier**: AID creation, rotation, and management
- **Credentials**: ACDC credential issuance and verification
- **Exchanging**: Exchange message creation and sending (used for remote signing)
- **Saider**: SAIDification of payloads (required for all exchange payloads)
- **Manager**: Key management with configurable security tiers
- **Salter/Signer**: Cryptographic primitives (Ed25519, PBKDF2)

### Basic Usage Pattern
```java
// Initialize client with bran (21-char seed) and security tier
SignifyClient client = new SignifyClient(url, bran, Tier.low, bootUrl, null);
client.boot();    // First time setup
client.connect(); // Subsequent connections

// Create identifier
var result = client.identifiers().create(CreateIdentifierArgs.builder()
    .name("backend-identity")
    .transferable(true)
    .build());

// Wait for operation completion
client.operations().wait(result, WaitOptions.defaults());
```

### When to Use
- **Use cf-signify-java**: Server-side validation, credential issuance by backend, witness coordination, remote signing orchestration
- **Use signify-ts**: Mobile wallet operations, client-side signing, biometric-protected keys

## Implemented Java Actions (KERIAIntegration module)

### Core Actions
- **JA_BootstrapBackendAID**: Creates backend AID in KERIA on first run
- **JA_GetBackendOOBI**: Retrieves backend's OOBI URL for wallet resolution
- **JA_ResolveWalletOOBI**: Resolves a wallet's OOBI in the backend KERIA agent
- **JA_CreateCredentialRegistry**: Creates TEL registry for credential status tracking
- **JA_IssueCredential**: Issues ACDC credential in KERIA (backend as cryptographic issuer)
- **JA_GrantCredentialToWallet**: Delivers credential to wallet via IPEX Grant

### Remote Signing Actions
- **JA_SendChallenge**: Sends arbitrary payload to wallet for signing (generic remote signing)
- **JA_VerifyChallengeResponse**: Polls for wallet's signed response (reusable for any signing flow)
- **JA_RequestCredentialSignature**: Builds ACDC structure, sends to Chief's wallet for authorization

### Credential Issuance Flow (with Chief Authorization)
The full flow for issuing credentials where the Chief (village chief) authorizes but the backend issues:

```
1. JA_RequestCredentialSignature  → Builds ACDC, sends to Chief's wallet
2. JA_VerifyChallengeResponse     → Polls until Chief signs in wallet
3. JA_IssueCredential             → Backend issues credential in KERIA
4. JA_GrantCredentialToWallet     → IPEX Grant delivers to Representative
```

The IssuedCredential entity tracks:
- `AuthorizedByAIDPrefix`: Chief's AID (who authorized)
- `IssuerAIDPrefix`: Backend's AID (cryptographic issuer)
- `RequestSAID`: Correlation ID linking to Chief's signed exchange message

### Remote Signing Pattern
Exchange messages use route `/remotesign/ixn/req` for requests and `/remotesign/ixn/ref` for responses.

**Critical**: Must use `/remotesign/ixn/req` — the Veridian wallet filters notifications by known routes and ignores custom routes.

Correlation: `request.exn.d == response.exn.p`

All payloads must be SAIDified using `Saider.saidify()` before sending.

### KERIA Notification Structure
When polling notifications:
- Top-level `r` = read status (boolean), NOT the route
- Route is at `a.r` (inside attributes)
- `a.d` = exchange SAID → use `client.exchanges().get(a.d)`
- Exchange result is nested: `{exn: {...}, pathed: {...}}` — access `.get("exn")` first

## Implementation Guidelines

### For AID Creation
1. Collect PIN (minimum 5 digits) via Mendix page
2. Use JavaScript action to initialize SignifyClient
3. Store encrypted salt in local storage (Keychain/Keystore)
4. Save public AID in Mendix entity
5. Enable biometric for future authentication

### For Document Signing
1. Authenticate user (biometric or PIN)
2. Retrieve encrypted keys from secure storage
3. Use SignifyTS to create signature
4. Store signature with document reference
5. Broadcast to witness network if needed

### For Credential Verification
1. Parse credential and extract issuer AID
2. Fetch issuer's key event log
3. Verify signature chain
4. Check witness receipts
5. Display verification status

### Security Best Practices
- Never store PINs - derive keys immediately and discard
- Use biometric authentication when available
- Implement rate limiting on PIN attempts
- Clear sensitive data from memory after use
- Use non-persistable entities for transient sensitive data
- Enable Mendix database encryption for offline storage
- Implement session timeouts for identity operations

### Testing Approach
1. Use Mendix Studio Pro debugger for microflows
2. Chrome DevTools for nanoflow debugging
3. React Native Debugger for mobile JavaScript
4. Test offline scenarios with airplane mode
5. Verify secure storage using device security settings

## Common Integration Patterns

### Pattern 1: Initialize Identity
```
Mendix Page → Nanoflow → JS Action (SignifyClient) → Secure Storage
                    ↓
            Update Domain Model
```

### Pattern 2: Sign Transaction
```
Mendix Page → Biometric Check → Nanoflow → JS Action (Sign)
                                      ↓
                            Microflow (Store Signature)
```

### Pattern 3: Verify Credential
```
QR Scanner → Parse DID → Nanoflow → JS Action (Verify)
                              ↓
                    Update UI with Status
```

## Troubleshooting

### Common Issues
1. **"Agent does not exist"**: Need to boot agent first
2. **"Commitment mismatch"**: Agent/controller sync issue
3. **Biometric fails**: Check device settings and permissions
4. **Keys not found**: Verify secure storage persistence
5. **Witness timeout**: Check network connectivity

### Debug Commands
```javascript
// Check if AID exists in storage
const exists = await StorageItemExists("aid_salt_" + aid);

// Verify SignifyClient connection
console.log(await client.state());

// Test biometric availability
const supported = await IsBiometricAuthenticationSupported();
```