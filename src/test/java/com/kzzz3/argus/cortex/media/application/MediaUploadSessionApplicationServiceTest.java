package com.kzzz3.argus.cortex.media.application;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kzzz3.argus.cortex.auth.application.AuthenticatedAccountResolver;
import com.kzzz3.argus.cortex.auth.domain.AccountRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentRecord;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentStore;
import com.kzzz3.argus.cortex.media.domain.MediaAttachmentType;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSession;
import com.kzzz3.argus.cortex.media.domain.MediaUploadSessionStore;
import com.kzzz3.argus.cortex.media.infrastructure.MediaContentStorage;
import com.kzzz3.argus.cortex.media.infrastructure.MediaStorageProperties;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MediaUploadSessionApplicationServiceTest {

    @Test
    void uploadContent_rejectsSessionOwnedByAnotherAccount() {
        MediaUploadSessionStore uploadSessionStore = mock(MediaUploadSessionStore.class);
        MediaAttachmentStore attachmentStore = mock(MediaAttachmentStore.class);
        MediaContentStorage mediaContentStorage = mock(MediaContentStorage.class);
        MediaStorageProperties storageProperties = new MediaStorageProperties();
        storageProperties.setPublicBaseUrl("https://media.example.com");

        when(uploadSessionStore.findBySessionId("session-1")).thenReturn(Optional.of(mediaSession("other-account", false)));

        MediaUploadSessionApplicationService service = new MediaUploadSessionApplicationService(
                resolver("tester"),
                uploadSessionStore,
                attachmentStore,
                storageProperties,
                mediaContentStorage
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.uploadContent("session-1", new byte[] {1, 2, 3})
        );

        assertEquals("Upload session does not belong to the authenticated account.", error.getMessage());
    }

    @Test
    void uploadContent_storesPayloadForOwnedSession() {
        MediaUploadSessionStore uploadSessionStore = mock(MediaUploadSessionStore.class);
        MediaAttachmentStore attachmentStore = mock(MediaAttachmentStore.class);
        MediaContentStorage mediaContentStorage = mock(MediaContentStorage.class);
        MediaStorageProperties storageProperties = new MediaStorageProperties();
        storageProperties.setPublicBaseUrl("https://media.example.com");

        when(uploadSessionStore.findBySessionId("session-1")).thenReturn(Optional.of(mediaSession("tester", false)));

        MediaUploadSessionApplicationService service = new MediaUploadSessionApplicationService(
                resolver("tester"),
                uploadSessionStore,
                attachmentStore,
                storageProperties,
                mediaContentStorage
        );

        byte[] content = new byte[] {1, 2, 3};
        service.uploadContent("session-1", content);

        verify(mediaContentStorage).store("media/object-key", content);
        verify(uploadSessionStore).markUploaded("session-1", "media/object-key");
    }

    private AuthenticatedAccountResolver resolver(String accountId) {
        AuthenticatedAccountResolver resolver = mock(AuthenticatedAccountResolver.class);
        when(resolver.resolveCurrent()).thenReturn(new AccountRecord(accountId, "Tester", "hash"));
        return resolver;
    }

    private MediaUploadSession mediaSession(String accountId, boolean uploaded) {
        return new MediaUploadSession(
                "session-1",
                accountId,
                MediaAttachmentType.IMAGE,
                "media/object-key",
                "https://media.example.com/upload/session-1",
                4096L,
                "upload-token",
                Map.of("X-Upload-Session", "session-1"),
                "Upload the requested payload with PUT using the provided headers.",
                uploaded
        );
    }
}
