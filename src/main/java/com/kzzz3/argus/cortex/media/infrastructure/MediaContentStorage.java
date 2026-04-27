package com.kzzz3.argus.cortex.media.infrastructure;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.springframework.stereotype.Component;

@Component
public class MediaContentStorage {

    private final Path rootPath;

    public MediaContentStorage(MediaStorageProperties properties) {
        this.rootPath = Paths.get(properties.getRootDir()).toAbsolutePath().normalize();
    }

    public void store(String objectKey, byte[] content) {
        Path destination = resolvePath(objectKey, "storing");
        if (content == null) {
            throw new IllegalArgumentException("Content bytes are required for storing media content.");
        }
        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(destination, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist uploaded media content.", ex);
        }
    }

    public void store(String objectKey, InputStream content) {
        Path destination = resolvePath(objectKey, "storing");
        if (content == null) {
            throw new IllegalArgumentException("Content stream is required for storing media content.");
        }
        try {
            Path parent = destination.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.copy(content, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to persist uploaded media content.", ex);
        }
    }

    public byte[] read(String objectKey) {
        Path destination = resolvePath(objectKey, "reading");
        try {
            return Files.readAllBytes(destination);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read media content.", ex);
        }
    }

    public boolean exists(String objectKey) {
        Path destination = resolvePath(objectKey, "checking existence of");
        return Files.exists(destination);
    }

    private Path resolvePath(String objectKey, String operation) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new IllegalArgumentException("Object key is required for " + operation + " media content.");
        }
        Path destination = rootPath.resolve(objectKey).normalize();
        if (!destination.startsWith(rootPath)) {
            throw new IllegalArgumentException("Invalid object key path.");
        }
        return destination;
    }
}
