package com.jobboard.api.service;

import com.jobboard.api.config.MinioProperties;
import com.jobboard.api.dto.response.FileUploadResponse;
import com.jobboard.api.exception.BadRequestException;
import com.jobboard.api.exception.ForbiddenException;
import com.jobboard.api.exception.StorageException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.Result;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "doc", "docx");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Value("${app.minio.max-files-per-user:20}")
    private int maxFilesPerUser;

    @Value("${app.minio.max-total-size-bytes:104857600}")
    private long maxTotalSizeBytes;

    public FileUploadResponse uploadCv(MultipartFile file, Long userId) {
        validate(file);
        enforceStorageQuota(userId, file.getSize());
        String objectKey = buildObjectKey(file.getOriginalFilename(), userId);

        try {
            ensureBucket();
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(properties.bucket())
                        .object(objectKey)
                        .stream(inputStream, file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
            }
            String url = presignedUrl(objectKey);
            return new FileUploadResponse(
                    objectKey,
                    url,
                    file.getOriginalFilename(),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (Exception exception) {
            throw new StorageException("CV yüklenemedi", exception);
        }
    }

    public String presignedUrl(String objectKey) {
        if (objectKey == null || !objectKey.startsWith("cvs/") || objectKey.contains("..")) {
            throw new BadRequestException("Geçersiz dosya anahtarı");
        }

        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .expiry(properties.presignedUrlExpiration(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception exception) {
            throw new StorageException("Dosya bağlantısı oluşturulamadı", exception);
        }
    }

    public String presignedUrlForUser(String objectKey, Long userId) {
        String expectedPrefix = "cvs/" + userId + "/";
        if (objectKey == null || !objectKey.startsWith(expectedPrefix)
                || objectKey.length() == expectedPrefix.length() || objectKey.contains("..")) {
            throw new ForbiddenException("Bu dosyaya erişim yetkiniz yok");
        }
        return presignedUrl(objectKey);
    }

    public void validateObjectKeyForUser(String objectKey, Long userId) {
        String expectedPrefix = "cvs/" + userId + "/";
        if (objectKey == null || !objectKey.startsWith(expectedPrefix)
                || objectKey.length() == expectedPrefix.length() || objectKey.contains("..")) {
            throw new BadRequestException("Geçersiz CV anahtarı");
        }
    }

    private void enforceStorageQuota(Long userId, long additionalBytes) {
        String prefix = "cvs/" + userId + "/";
        int fileCount = 0;
        long totalBytes = 0;

        try {
            for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(properties.bucket())
                    .prefix(prefix)
                    .recursive(true)
                    .build())) {
                Item item = result.get();
                if (item != null && !item.isDir()) {
                    fileCount++;
                    totalBytes += item.size();
                }
            }
        } catch (Exception exception) {
            throw new StorageException("Depolama kotası kontrol edilemedi", exception);
        }

        if (fileCount >= maxFilesPerUser) {
            throw new BadRequestException("Dosya sayısı sınırına ulaştınız");
        }
        if (totalBytes + additionalBytes > maxTotalSizeBytes) {
            throw new BadRequestException("Depolama alanı sınırına ulaştınız");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Dosya boş olamaz");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("Dosya boyutu 5 MB'ı aşamaz");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BadRequestException("Dosya adı geçersiz");
        }

        String safeName = safeFileName(filename);
        String extension = safeName.substring(safeName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Yalnızca PDF, DOC ve DOCX yüklenebilir");
        }
        if (file.getContentType() == null || !ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Geçersiz dosya türü");
        }
        try (InputStream input = file.getInputStream()) {
            byte[] signature = input.readNBytes(8);
            boolean validContent = switch (extension) {
                case "pdf" -> startsWith(signature, new byte[]{'%', 'P', 'D', 'F', '-'});
                case "doc" -> startsWith(signature,
                        new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
                                (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
                case "docx" -> startsWith(signature, new byte[]{'P', 'K', 0x03, 0x04});
                default -> false;
            };
            if (!validContent) {
                throw new BadRequestException("Dosya içeriği uzantı ile eşleşmiyor");
            }
        } catch (java.io.IOException exception) {
            throw new BadRequestException("Dosya okunamadı");
        }
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private String buildObjectKey(String originalFilename, Long userId) {
        return "cvs/%d/%s-%s".formatted(userId, UUID.randomUUID(), safeFileName(originalFilename));
    }

    private String safeFileName(String originalFilename) {
        String name = originalFilename.replace('\\', '/');
        int slashIndex = name.lastIndexOf('/');
        if (slashIndex >= 0) {
            name = name.substring(slashIndex + 1);
        }
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private void ensureBucket() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(properties.bucket())
                .build());

        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(properties.bucket())
                    .build());
        }
    }
}
