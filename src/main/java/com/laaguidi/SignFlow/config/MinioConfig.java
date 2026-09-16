package com.laaguidi.SignFlow.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class MinioConfig {

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name:signflow-buckets}") // <--- زدنا الـ Bucket Name مع Default Value
    private String bucketName;

    @Value("${minio.connect-timeout:10000}")
    private int connectTimeout;

    @Value("${minio.write-timeout:10000}")
    private int writeTimeout;

    @Value("${minio.read-timeout:10000}")
    private int readTimeout;

    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .writeTimeout(writeTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    @Bean
    public MinioClient minioClient(OkHttpClient okHttpClient) {
        try {
            log.info("Initializing MinIO Client with endpoint: {}", minioUrl);

            MinioClient client = MinioClient.builder()
                    .endpoint(minioUrl)
                    .credentials(accessKey, secretKey)
                    .httpClient(okHttpClient)
                    .build();

            // Auto-create bucket if missing
            boolean found = client.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!found) {
                log.info("Bucket '{}' not found. Creating it...", bucketName);
                client.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Bucket '{}' created successfully.", bucketName);
            } else {
                log.info("Bucket '{}' already exists.", bucketName);
            }

            return client;

        } catch (Exception e) {
            log.error("Failed to initialize MinIO client: {}", e.getMessage(), e);
            throw new RuntimeException("MinIO client initialization failed", e);
        }
    }
}