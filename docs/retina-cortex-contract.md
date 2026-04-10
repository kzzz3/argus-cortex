# Argus Cortex ↔ Argus Retina Contract Draft

## 1. Purpose

This contract defines the long-term boundary between **Argus Cortex** and **Argus Retina**.

- **Cortex** owns cloud orchestration, policy, routing, persistence, and auditability.
- **Retina** owns native primitives, deterministic media/security routines, and high-trust local/native result generation.

This document exists to keep the two repositories **separate but tightly specified**.

---

## 2. Design Rules

- Cortex must never call ad-hoc native code paths without a versioned contract.
- Retina must never expose raw internal/native memory details or platform-specific implementation details across the boundary.
- All Cortex ↔ Retina calls must use **deterministic request/result payloads**.
- All result payloads returned by Retina must be suitable for **audit storage** in Cortex.
- Cortex owns final policy decisions; Retina may provide scores, signatures, or structured findings, but not business authorization.

---

## 3. Ownership Split

### Cortex owns
- job orchestration
- retry policy
- timeout policy
- persistence of requests/results
- policy decisions and authorization
- audit logs
- version compatibility enforcement

### Retina owns
- native media preprocessing
- native QR / image / audio analysis primitives
- secure payload canonicalization/signing substrate
- deterministic error/result construction
- capability reporting for supported native features

---

## 4. Contract Categories

## 4.1 Capability Probe

Purpose: let Cortex know what a given Retina build supports before scheduling work.

### Request
- none or local bootstrap call

### Response fields
- `engineVersion`
- `contractVersion`
- `supportedFeatures[]`
- `platform`
- `buildProfile`

### Notes
- Cortex must reject unknown `contractVersion` values.
- Feature checks should gate optional native jobs instead of failing at runtime later.

---

## 4.2 QR / Visual Parse Job

Purpose: parse QR or visual payloads through Retina when native image/security routines are justified.

### Request fields
- `jobId`
- `requestTimestamp`
- `inputType` (`qr-image`, `frame`, `merchant-visual`)
- `assetReference`
- `expectedFormat`
- `traceId`

### Response fields
- `jobId`
- `status` (`success`, `rejected`, `failed`)
- `parsedPayload`
- `confidence`
- `errorCode`
- `errorMessage`
- `evidenceReferences[]`

### Cortex expectations
- persist full response for audit
- never trust partial parse text without checking `status` and `errorCode`

---

## 4.3 Audio Preprocess / VAD Job

Purpose: accept pre-filtered audio chunks or raw stage-approved audio segments for native segmentation/validation.

### Request fields
- `jobId`
- `assetReference`
- `sampleRateHz`
- `channelCount`
- `segmentPolicy`
- `traceId`

### Response fields
- `jobId`
- `status`
- `segmentRanges[]`
- `vadScore`
- `recommendedUpload`
- `errorCode`
- `errorMessage`

### Notes
- Retina returns deterministic segment metadata, not user-facing conversation logic.

---

## 4.4 Secure Signing / Payment Support Job

Purpose: allow Cortex to verify or consume trusted signing-related artifacts without moving low-level secret logic into the cloud service.

### Request fields
- `jobId`
- `operationType`
- `canonicalPayload`
- `payloadHash`
- `traceId`

### Response fields
- `jobId`
- `status`
- `artifactType`
- `artifactReference`
- `signatureMetadata`
- `errorCode`
- `errorMessage`

### Rules
- Cortex must not receive raw secret material.
- Retina must return only high-trust artifacts or structured failure.

---

## 5. Required Common Envelope

Every Cortex ↔ Retina job should converge on a shared envelope shape:

### Required request metadata
- `jobId`
- `contractVersion`
- `traceId`
- `requestedAt`
- `requester`

### Required response metadata
- `jobId`
- `contractVersion`
- `completedAt`
- `status`
- `errorCode`
- `errorMessage`

This prevents each native capability from inventing a different top-level format.

---

## 6. Error Code Rules

- use stable machine-readable `errorCode`
- keep `errorMessage` human-readable but non-authoritative
- never use free-text-only failure reporting

### Example families
- `CONTRACT_VERSION_UNSUPPORTED`
- `ASSET_NOT_FOUND`
- `UNSUPPORTED_INPUT_TYPE`
- `NATIVE_VALIDATION_FAILED`
- `SIGNING_ARTIFACT_UNAVAILABLE`

---

## 7. Versioning Rules

- `contractVersion` must be explicit in every request/response family
- backward-incompatible changes require a new version
- Cortex must be able to reject unsupported Retina contract versions early

---

## 8. Security Rules

- no raw keys cross the boundary
- no untyped text blobs for high-risk operations
- all high-risk native outputs should be traceable and auditable
- Cortex logs should store structured result metadata, not just success/fail booleans

---

## 9. Near-Term Implementation Checklist

- [ ] define shared request/response envelope structs
- [ ] define contract version negotiation rules
- [ ] define capability probe payload
- [ ] define QR/visual parse request/result schema
- [ ] define audio preprocess/VAD request/result schema
- [ ] define secure signing artifact schema
- [ ] define shared error code families
- [ ] define audit persistence shape in Cortex for Retina results

---

## 10. Non-Goals

- this contract does not define Lens ↔ Retina JNI details
- this contract does not define user-facing UI behavior
- this contract does not move policy ownership into Retina
- this contract does not define all Stage 2 AI flows yet
