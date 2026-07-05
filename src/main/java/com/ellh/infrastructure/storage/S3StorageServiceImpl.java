package com.ellh.infrastructure.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;

@Slf4j
@Service
public class S3StorageServiceImpl implements StorageService {

    private final S3Client s3Client;
    private final String bucketName;
    private final String publicCdnUrl;

    public S3StorageServiceImpl(
            @Value("${cloud.s3.endpoint}") String endpoint,
            @Value("${cloud.s3.region}") String region,
            @Value("${cloud.s3.access-key}") String accessKey,
            @Value("${cloud.s3.secret-key}") String secretKey,
            @Value("${cloud.s3.bucket-name}") String bucketName,
            @Value("${cloud.s3.cdn-url}") String publicCdnUrl) {
        
        this.bucketName = bucketName;
        this.publicCdnUrl = publicCdnUrl;

        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Override
    public String uploadAudio(MultipartFile file, String fileHash) throws IOException {
        String objectKey = "recordings/" + fileHash + ".aac";

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType("audio/aac")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        log.info("Successfully uploaded audio to S3: {}", objectKey);

        return publicCdnUrl + "/" + objectKey;
    }

    @Override
    public void deleteAudio(String fileHash) {
        String objectKey = "recordings/" + fileHash + ".aac";
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
                
        s3Client.deleteObject(deleteRequest);
        log.info("Hard-deleted audio from S3 (GDPR Compliance): {}", objectKey);
    }
}
