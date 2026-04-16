package com.kzzz3.argus.cortex.media.application;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSessionStore;
import com.kzzz3.argus.cortex.media.infrastructure.MediaContentStorage;
import com.kzzz3.argus.cortex.media.infrastructure.MediaStorageProperties;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class MediaUploadSessionApplicationService {

	private final AuthenticatedAccountResolver authenticatedAccountResolver;
	private final MediaUploadSessionStore mediaUploadSessionStore;
    private final MediaAttachmentStore mediaAttachmentStore;
    private final MediaStorageProperties mediaStorageProperties;
    private final MediaContentStorage mediaContentStorage;

	public MediaUploadSessionApplicationService(
			AuthenticatedAccountResolver authenticatedAccountResolver,
			MediaUploadSessionStore mediaUploadSessionStore,
            MediaAttachmentStore mediaAttachmentStore,
            MediaStorageProperties mediaStorageProperties,
            MediaContentStorage mediaContentStorage
    ) {
		this.authenticatedAccountResolver = authenticatedAccountResolver;
        this.mediaUploadSessionStore = mediaUploadSessionStore;
        this.mediaAttachmentStore = mediaAttachmentStore;
        this.mediaStorageProperties = mediaStorageProperties;
        this.mediaContentStorage = mediaContentStorage;
    }

	public MediaUploadSession createUploadSession(CreateMediaUploadSessionCommand request) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        String normalizedFileName = request.fileName().trim();
        MediaAttachmentType attachmentType = request.attachmentType();
        long normalizedEstimatedBytes = Math.max(1, request.estimatedBytes());
        long maxPayloadBytes = Math.max(attachmentType.defaultMaxPayloadBytes(), normalizedEstimatedBytes);
        String sessionId = generateSessionId(accountRecord.accountId(), normalizedFileName, attachmentType);
        String objectKey = generateObjectKey(accountRecord.accountId(), attachmentType, sessionId, normalizedFileName);
        String uploadUrl = buildUploadUrl(sessionId);
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
                objectKey,
                uploadUrl,
                maxPayloadBytes,
                uploadToken,
                uploadHeaders,
                "Upload the requested payload with PUT using the provided headers.",
                false
        );

        return mediaUploadSessionStore.save(session);
    }

	public void uploadContent(String sessionId, byte[] content) {
        if (content == null) {
            throw new IllegalArgumentException("Upload payload is required.");
        }
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        MediaUploadSession session = mediaUploadSessionStore.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found."));
        if (!session.accountId().equals(accountRecord.accountId())) {
            throw new IllegalArgumentException("Upload session does not belong to the authenticated account.");
        }
        long contentLength = content.length;
        if (contentLength > session.maxPayloadBytes()) {
            throw new IllegalArgumentException("Content length exceeds the allowed upload size.");
        }
        String objectKey = session.objectKey();
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalStateException("Upload session object key is missing.");
        }
        mediaContentStorage.store(objectKey, content);
        mediaUploadSessionStore.markUploaded(sessionId, objectKey);
    }

	public MediaAttachmentRecord finalizeUploadSession(
			String sessionId,
			FinalizeMediaUploadCommand request
	) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        MediaUploadSession session = mediaUploadSessionStore.findBySessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Upload session not found."));
        if (!session.accountId().equals(accountRecord.accountId())) {
            throw new IllegalArgumentException("Upload session does not belong to the authenticated account.");
        }
        if (!session.uploaded()) {
            throw new IllegalStateException("Upload session content has not been uploaded.");
        }
        if (!session.objectKey().equals(request.objectKey())) {
            throw new IllegalArgumentException("Upload session object key mismatch.");
        }

        MediaAttachmentRecord attachmentRecord = buildAttachmentRecord(session, request);
        return mediaAttachmentStore.save(attachmentRecord);
    }

	public MediaAttachmentDownload loadAttachment(String attachmentId) {
		AccountRecord accountRecord = authenticatedAccountResolver.resolveCurrent();
        MediaAttachmentRecord record = mediaAttachmentStore.findByAttachmentId(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Media attachment not found."));
        if (!accountRecord.accountId().equals(record.accountId())) {
            throw new IllegalArgumentException("Attachment does not belong to the authenticated account.");
        }
        String objectKey = record.objectKey();
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalStateException("Attachment object key is missing.");
        }
        if (!mediaContentStorage.exists(objectKey)) {
            throw new IllegalStateException("Attachment content is missing.");
        }
        byte[] content = mediaContentStorage.read(objectKey);
        return new MediaAttachmentDownload(record, content);
    }

	private MediaAttachmentRecord buildAttachmentRecord(MediaUploadSession session, FinalizeMediaUploadCommand request) {
        return new MediaAttachmentRecord(
                UUID.randomUUID().toString(),
                session.sessionId(),
                session.accountId(),
                normalizeConversationId(request.conversationId()),
                session.attachmentType(),
                request.fileName(),
                request.contentType(),
                request.contentLength(),
                session.objectKey(),
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

    private String generateObjectKey(String accountId, MediaAttachmentType attachmentType, String sessionId, String normalizedFileName) {
        String sanitizedFileName = sanitizeFileName(normalizedFileName);
        return String.join("/", accountId, attachmentType.name().toLowerCase(), sessionId, sanitizedFileName);
    }

    private String sanitizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "upload";
        }
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String buildUploadUrl(String sessionId) {
        String baseUrl = mediaStorageProperties.getPublicBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("Media storage public base url is not configured.");
        }
        String normalizedBase = baseUrl.replaceAll("/+$", "");
        return normalizedBase + "/api/v1/media/upload-sessions/" + sessionId + "/content";
    }

    public static record MediaAttachmentDownload(MediaAttachmentRecord record, byte[] content) {
    }
}
