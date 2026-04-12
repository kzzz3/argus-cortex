package com.kzzz3.argus.cortex.media.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSessionStore;
import com.kzzz3.argus.cortex.media.web.CreateMediaUploadSessionRequest;
import com.kzzz3.argus.cortex.media.web.FinalizeMediaUploadRequest;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MediaUploadSessionApplicationService {

    private static final String UPLOAD_BASE_URL = "https://uploads.argus.dev";

    private final AccessTokenStore accessTokenStore;
    private final MediaUploadSessionStore mediaUploadSessionStore;
    private final MediaAttachmentStore mediaAttachmentStore;

    public MediaUploadSessionApplicationService(
            AccessTokenStore accessTokenStore,
            MediaUploadSessionStore mediaUploadSessionStore,
            MediaAttachmentStore mediaAttachmentStore
    ) {
        this.accessTokenStore = accessTokenStore;
        this.mediaUploadSessionStore = mediaUploadSessionStore;
        this.mediaAttachmentStore = mediaAttachmentStore;
    }

    public MediaUploadSession createUploadSession(String accessToken, CreateMediaUploadSessionRequest request) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        String normalizedFileName = request.fileName().trim();
        MediaAttachmentType attachmentType = request.attachmentType();
        long normalizedEstimatedBytes = Math.max(1, request.estimatedBytes());
        long maxPayloadBytes = Math.max(attachmentType.defaultMaxPayloadBytes(), normalizedEstimatedBytes);
        String sessionId = generateSessionId(accountRecord.accountId(), normalizedFileName, attachmentType);
        String uploadUrl = buildUploadUrl(sessionId, attachmentType);
        String uploadToken = Base64.getUrlEncoder().withoutPadding().encodeToString(
                (accountRecord.accountId() + ":" + sessionId).getBytes(StandardCharsets.UTF_8)
        );
        Map<String, String> uploadHeaders = Map.of(
                "X-Upload-Session", sessionId,
                "X-Attachment-Type", attachmentType.name()
        );
        MediaUploadSession session = new MediaUploadSession(
                sessionId,
                accountRecord.accountId(),
                attachmentType,
                uploadUrl,
                maxPayloadBytes,
                uploadToken,
                uploadHeaders,
                "Upload the requested payload with PUT using the provided headers."
        );

        return mediaUploadSessionStore.save(session);
    }

    public MediaAttachmentRecord finalizeUploadSession(
            String accessToken,
            String sessionId,
            FinalizeMediaUploadRequest request
    ) {
        AccountRecord accountRecord = accessTokenStore.findByToken(accessToken)
                .orElseThrow(InvalidCredentialsException::new);
        MediaUploadSession session = mediaUploadSessionStore.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found."));
        if (!session.accountId().equals(accountRecord.accountId())) {
            throw new IllegalArgumentException("Upload session does not belong to the authenticated account.");
        }

        MediaAttachmentRecord attachmentRecord = buildAttachmentRecord(session, request);
        return mediaAttachmentStore.save(attachmentRecord);
    }

    private MediaAttachmentRecord buildAttachmentRecord(MediaUploadSession session, FinalizeMediaUploadRequest request) {
        return new MediaAttachmentRecord(
                UUID.randomUUID().toString(),
                session.sessionId(),
                session.accountId(),
                normalizeConversationId(request.conversationId()),
                session.attachmentType(),
                request.fileName(),
                request.contentType(),
                request.contentLength(),
                request.objectKey(),
                session.uploadUrl(),
                LocalDateTime.now()
        );
    }

    private String normalizeConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return null;
        }
        return conversationId;
    }

    private String generateSessionId(String accountId, String fileName, MediaAttachmentType attachmentType) {
        String seed = accountId + ":" + attachmentType.name() + ":" + fileName.toLowerCase();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String buildUploadUrl(String sessionId, MediaAttachmentType attachmentType) {
        return UPLOAD_BASE_URL + "/" + attachmentType.name().toLowerCase() + "/" + sessionId;
    }
}
