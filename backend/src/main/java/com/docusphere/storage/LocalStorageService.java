package com.docusphere.storage;

import com.docusphere.common.exception.InvalidFileException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;
    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(properties.getLocation()).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            log.info("Storage initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage location.", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename) {
        if (inputStream == null) {
            throw new InvalidFileException("File is empty.");
        }

        // Sécurité : Générer un nom de fichier unique et sûr
        String extension = getExtension(originalFilename).toLowerCase();
        String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

        try {
            Path destinationFile = this.rootLocation.resolve(storedFilename).normalize();

            // Protection CRITIQUE contre le Path Traversal
            if (!destinationFile.startsWith(this.rootLocation)) {
                throw new InvalidFileException("Cannot store file outside current directory.");
            }

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file.", e);
        }
    }

    @Override
    public InputStream load(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            if (!Files.exists(file)) {
                throw new RuntimeException("File not found: " + filename);
            }
            return Files.newInputStream(file);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file.", e);
        }
    }

    @Override
    public void delete(String filename) {
        try {
            Path file = rootLocation.resolve(filename).normalize();
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filename, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1);
    }
}
