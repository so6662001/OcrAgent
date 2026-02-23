package com.ocr.file.storage;

import com.ocr.common.exception.OcrException;
import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "ocr.minio", name = "endpoint")
public class MinioStorageService {

    @Value("${ocr.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${ocr.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${ocr.minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${ocr.minio.bucket:ocr-files}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("创建MinIO Bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO初始化失败，文件存储功能不可用: {}", e.getMessage());
        }
    }

    public String upload(MultipartFile file, String tenantId) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String ext = getFileExtension(file.getOriginalFilename());
        String objectName = tenantId + "/" + datePath + "/" + UUID.randomUUID() + "." + ext;

        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            return objectName;
        } catch (Exception e) {
            throw new OcrException("文件上传失败: " + e.getMessage(), e);
        }
    }

    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .method(Method.GET)
                    .expiry(2, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            throw new OcrException("获取文件URL失败: " + e.getMessage(), e);
        }
    }

    public InputStream getFile(String objectName) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            throw new OcrException("获取文件失败: " + e.getMessage(), e);
        }
    }

    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName)
                    .build());
        } catch (Exception e) {
            log.error("删除文件失败: {}", objectName, e);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "bin";
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(idx + 1).toLowerCase() : "bin";
    }
}
