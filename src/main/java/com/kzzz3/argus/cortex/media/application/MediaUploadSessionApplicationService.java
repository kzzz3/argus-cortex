package com.kzzz3.argus.cortex.media.application;

import com.kzzz3.argus.cortex.auth.domain.AccessTokenStore;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.auth.domain.InvalidCredentialsException;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import com.kzzz3.argus.cortex.media.web.CreateMediaUploadSessionRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MediaUploadSessionApplicationService {

    private static final String UPLOAD_BASE_URL = "https://uploads.argus.dev";

    private final AccessTokenStore accessTokenStore;

    public MediaUploadSessionApplicationService(AccessTokenStore accessTokenStore) {
        this.accessTokenStore = accessTokenStore;
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

        return new MediaUploadSession(
                sessionId,
                accountRecord.accountId(),
                attachmentType,
                uploadUrl,
                maxPayloadBytes,
                uploadToken,
                uploadHeaders,
                "Upload the requested payload with PUT using the provided headers."
        );
    }

    private String generateSessionId(String accountId, String fileName, MediaAttachmentType attachmentType) {
        String seed = accountId + ":" + attachmentType.name() + ":" + fileName.toLowerCase();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private String buildUploadUrl(String sessionId, MediaAttachmentType attachmentType) {
        return UPLOAD_BASE_URL + "/" + attachmentType.name().toLowerCase() + "/" + sessionId;
    }
}
