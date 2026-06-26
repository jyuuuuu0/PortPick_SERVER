package com.example.PortPick_SERVER.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    public String store(MultipartFile file, Path directory, String urlPrefix,
                        Set<String> allowedExtensions, String uploadErrorMessage) {
        String extension = extractExtension(file.getOriginalFilename(), allowedExtensions);
        try {
            Files.createDirectories(directory);
            String savedFilename = UUID.randomUUID() + "." + extension;
            Path target = directory.resolve(savedFilename).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return urlPrefix + "/" + savedFilename;
        } catch (IOException e) {
            throw new IllegalStateException(uploadErrorMessage, e);
        }
    }

    public void deleteIfManaged(String url, Path directory, String urlPrefix) {
        if (!StringUtils.hasText(url) || !url.startsWith(urlPrefix + "/")) {
            return;
        }

        String filename = url.substring((urlPrefix + "/").length());
        if (!StringUtils.hasText(filename)) {
            return;
        }

        Path target = directory.resolve(filename).normalize();
        if (!target.startsWith(directory)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException ignored) {
        }
    }

    public void registerRollbackCleanup(String url, Path directory, String urlPrefix) {
        if (!StringUtils.hasText(url) || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    deleteIfManaged(url, directory, urlPrefix);
                }
            }
        });
    }

    public void registerDeletionAfterCommit(String url, Path directory, String urlPrefix) {
        if (!StringUtils.hasText(url)) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteIfManaged(url, directory, urlPrefix);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteIfManaged(url, directory, urlPrefix);
            }
        });
    }

    private String extractExtension(String filename, Set<String> allowedExtensions) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new IllegalArgumentException("파일 형식이 올바르지 않습니다.");
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다. 허용: " + allowedExtensions);
        }

        return extension;
    }
}
