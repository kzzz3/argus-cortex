# Argus Cortex Technical Plan

## 1. Repository Positioning

Argus Cortex is the cloud hub of the full system. It is the only repository that should own account identity, social graph, message routing, sync state, payment orchestration, AI intent parsing orchestration, and cross-device auditability.

This repo is **not** the place for heavy media processing, direct sensor access, UI logic, or low-level cryptographic key handling. Those belong to Argus Lens and Argus Retina.

## 2. Product Strategy Split

The overall product is developed in two stages:

### Stage 1 — WeChat-like Baseline
Deliver the standard communication and payment backbone first:

- account and device login
- direct chat and friend-request workflow
- text, voice, image, and video message delivery
- call signaling and session coordination
- message sync and offline recovery
- QR-based payment and transfer orchestration

### Stage 2 — AI Glasses Enhancement
After the baseline is stable, Cortex expands into the cloud brain for smart-wear scenarios:

- multimodal intent parsing from FPV audio/video streams
- policy-aware wearable device coordination
- AI action routing for hands-free messaging and payment intent confirmation
- first-person live session coordination and visual collaboration metadata

## 3. Core Responsibilities

### 3.1 Social and Identity Core
- user account lifecycle
- device registration and session management
- contact graph, block list, and relationship data
- multi-device trust model for phone-as-glasses simulation

### 3.2 IM Core
- Netty-based long connection gateway
- user session routing and presence state
- timeline sequence allocation
- Sync-Key or timeline-diff based message reconciliation
- delivery acknowledgement and retry semantics

### 3.3 Media and Realtime Coordination
- upload-session negotiation for voice/image/video assets
- WebRTC signaling via WebSocket or IM side-channel
- call invitation, accept/reject, reconnect, and session teardown control
- media metadata indexing and delivery status tracking

### 3.4 Payment and Risk Orchestration
- wallet QR task creation and wallet session validation
- transfer and payment workflow orchestration
- risk control, antifraud policy checks, and audit timeline
- integration boundary for signed transaction tokens from Retina-backed paths

### 3.5 AI and Wearable Orchestration
- ingest pre-filtered visual/audio segments from Lens
- forward multimodal payloads to model services
- convert model outputs into typed actions
- hand off typed actions to IM, call, payment, and policy subdomains

## 4. Stage-Oriented Architecture

## 4.1 Stage 1 Cloud Baseline Architecture

Primary target: reliable WeChat-like core messaging and payment workflows.

### Main subsystems
- **Gateway layer**: Netty TCP gateway for IM long connections and command frames
- **API layer**: Spring Boot HTTP/WebSocket APIs for auth, profile, upload, payment, and signaling
- **Message domain**: chat session, message envelope, receipt, recall, unread cursor
- **Sync domain**: timeline allocator, diff sync, reconnect reconciliation
- **Payment domain**: wallet summary, QR transfer session, transfer orchestration, receipt state machine
- **Infra layer**: Redis, MySQL, Kafka, object storage

### Stage 1 data flow
1. Lens establishes authenticated long connection.
2. Cortex maintains session, route, and heartbeat state.
3. New messages receive timeline IDs and are persisted.
4. Offline clients recover by sending latest Sync-Key / timeline cursor.
5. Voice/image/video payloads are uploaded through negotiated asset channels and referenced by message envelopes.
6. Payment requests are validated, risk checked, persisted, then settled through payment services.

## 4.2 Stage 2 AI Wearable Enhancement Architecture

Primary target: convert first-person audio/video and sensor-confirmed behavior into server-side action orchestration.

### Added subsystems
- **Multimodal ingress**: receive pre-filtered FPV audio/video chunks from Lens
- **Intent orchestration**: model prompting, function calling, and typed action validation
- **Wearable policy engine**: decide what actions are allowed hands-free
- **Action dispatch layer**: route validated actions into IM, call, wallet pay / collect, or alert workflows
- **Audit intelligence layer**: persist model decisions, confidence, evidence pointers, and operator-review traces

### Stage 2 constraints
- Cortex may interpret intent, but it must not own camera/audio cleanup logic.
- Cortex may orchestrate payment completion, but it must not generate secure signatures from raw local secrets.
- Cortex must treat Retina outputs as signed or trusted engine artifacts, not ad-hoc text blobs.

## 5. Recommended Domain Map

- `identity` — account, token, device enrollment, trust
- `social` — contacts, friend requests, presence, relation graph
- `im` — sessions, messages, receipts, unread state, recall
- `sync` — timeline IDs, diff sync, offline recovery
- `media` — upload sessions, asset descriptors, metadata
- `rtc` — call session metadata, signaling, ICE/SDP exchange state
- `payment` — wallet summary, QR transfer, receipt orchestration, risk gate
- `agent` — multimodal ingest, intent parsing, function routing, safety validation
- `audit` — immutable action and risk history
- `integration` — adapters for Lens, Retina, Redis, Kafka, storage, model providers

## 6. External Interfaces

### 6.1 Lens -> Cortex
- login and token refresh
- contact and conversation sync
- message send / receive ack / diff pull
- upload session creation for media payloads
- WebRTC signaling exchange
- wallet summary, transfer initiation, receipt lookup, and history sync
- wearable intent upload in Stage 2

### 6.2 Cortex -> Retina
- job contracts for edge-assisted verification or server-hosted native processing
- QR / vision / voice-derived structured payload validation
- signed engine result ingestion
- secure payment token verification support

See also: `docs/retina-cortex-contract.md`

### 6.3 Internal platform dependencies
- **MySQL**: account, social graph, conversation metadata, transaction records
- **Redis**: session routing, rate limits, locks, hot presence state
- **Kafka**: message fan-out, async notifications, analytics, risk pipelines
- **Object storage**: voice, image, video, thumbnails, evidence snapshots

## 7. Reliability and Security Model

### Stage 1 focus
- reliable heartbeats and reconnects
- monotonic sync cursor design
- idempotent message send and payment callbacks
- receipt, retry, and duplicate suppression
- audit logs for payment and account events

### Stage 2 focus
- typed and policy-checked AI action execution
- model output validation before side effects
- signed native-engine evidence for high-risk actions
- strong traceability for hands-free messaging and payment decisions

## 8. Phase Mapping by Repo Ownership

The global project is split into eight execution phases, but Cortex only owns the cloud-facing parts:

### Pair A — Phase 1 / Phase 2
- **Phase 1 primary**: IM gateway, timeline sequencing, reconnect sync, text chat
- **Phase 2 support**: media upload negotiation and metadata plumbing for voice/image flows

### Pair B — Phase 3 / Phase 4
- **Phase 3 primary**: WebRTC signaling, call session lifecycle, presence coordination
- **Phase 4 support**: first-person stream session coordination and remote annotation metadata transport

### Pair C — Phase 5 / Phase 6
- **Phase 5 support**: accept only filtered edge data, define ingestion schema
- **Phase 6 primary**: multimodal intent orchestration and function routing

### Pair D — Phase 7 / Phase 8
- **Phase 7 primary**: baseline wallet QR pay / collect / transfer orchestration and receipt state machine
- **Phase 8 support**: consume trusted confirmation and signing artifacts from Lens + Retina for zero-trust visual pay

## 9. Stage 1 Milestones for This Repo

1. establish Netty-based IM gateway and binary protocol envelope
2. implement conversation/message/timeline model and reconnect diff sync
3. add asset upload session APIs for image, voice, and video messages
4. ship signaling APIs for 1v1 audio/video calls
5. ship wallet QR pay / collect and transfer orchestration APIs with audit trail

## 10. Stage 2 Milestones for This Repo

1. define multimodal upload protocol from Lens
2. add model-facing intent orchestration pipeline with typed action schema
3. gate high-risk actions through policy and confidence checks
4. support FPV live-session metadata, remote markups, and action audit replay
5. integrate zero-trust visual pay confirmation flow with native signed evidence

## 11. Non-Goals

- no direct CameraX, AudioRecord, SensorManager, or HUD implementation here
- no JNI bridge logic here
- no low-level VAD, OpenCV filtering, or mbedtls signing here
- no heavy codec pipeline inside the cloud monolith

## 12. Current Progress Checklist

### 12.1 Completed Stage 1 backend foundation
- [x] Spring Boot baseline and module-first package layout are in place
- [x] auth module is structured as web / application / domain / infrastructure
- [x] validation and shared API error handling are in place
- [x] account registration and login endpoints exist
- [x] in-memory account storage is in place for the current stage
- [x] bearer token issuance is in place

### 12.2 Completed auth/session chain work
- [x] token issuance evolved into token-backed session lookup (`AccessTokenStore`)
- [x] `/api/v1/auth/session/me` exists for session restoration
- [x] backend auth now supports the Android client restoring a prior session instead of only fresh login

### 12.3 Completed first IM chain slices
- [x] `/api/v1/conversations` exists as the first remote conversation list endpoint
- [x] `/api/v1/conversations/{id}/messages` exists as the first remote message list endpoint
- [x] `/api/v1/conversations/{id}/messages` POST exists for remote message send
- [x] `/api/v1/conversations/{id}/messages/{messageId}/recall` POST exists for remote recall
- [x] conversation and message list endpoints now expose recent-window semantics instead of pretending to be unbounded full-history truth
- [x] `/api/v1/conversations/{id}/messages` now supports real cursor continuation across multiple reconnect pages
- [x] conversation messages can now carry structured attachment references instead of body-only file metadata

### 12.4 Completed repo-level validation work
- [x] backend test suite continues to pass after the auth/session/conversation endpoint additions
- [x] current Android remote auth and conversation slices can compile against these backend contracts
- [x] shared bearer-token parsing and authenticated-account resolution are centralized instead of being repeated across controllers and services
- [x] application services now accept application-layer commands rather than importing web request DTOs directly
- [x] development schema now uses a single v1 Flyway baseline migration instead of a forward-compatibility migration chain
- [x] conversation/friend stores no longer auto-seed demo contacts or sample threads; empty state now means truly empty persisted state

## 13. Next-Phase Checklist

### 13.1 IM backend priorities before calling the baseline complete
- [ ] replace seeded in-memory conversation/message responses with real structured persistence
- [x] add sync cursor / recent-window continuation semantics instead of static list-only responses
- [ ] add server-driven delivery receipt and read-state sync
- [x] add idempotent send semantics and duplicate suppression for message retries
- [ ] finalize friend-request workflow and direct-chat ownership rules for Stage 1

### 13.2 Media and realtime follow-up
- [x] SSE event streams now emit stable event ids, heartbeat frames, and reconnect resume support via Last-Event-ID
- [ ] add upload-session creation APIs for image / voice / video payloads
- [x] add media metadata records and attachment references in message envelopes
- [ ] add RTC signaling APIs for 1v1 audio/video sessions

### 13.3 Reliability and security follow-up
- [ ] define token lifecycle / expiry / refresh instead of current in-memory stage tokens
- [ ] move auth/session state from in-memory-only token store to a persistence-backed design
- [x] add reconnect-aware diff sync and cursor validation rules
- [ ] add explicit offline-window / retention policy beyond the current placeholder recent-window contract

## 14. Immediate Next Design Tasks

1. define structured persistence for conversation metadata, message envelopes, and recall/read-state transitions
2. define Sync-Key / timeline cursor semantics and conflict policy for bounded-window sync
3. define upload-session contracts for voice/image/video payloads
4. define WebRTC signaling schema and session lifecycle events
5. define typed action schema for Stage 2 AI orchestration once Stage 1 message sync semantics are stable
