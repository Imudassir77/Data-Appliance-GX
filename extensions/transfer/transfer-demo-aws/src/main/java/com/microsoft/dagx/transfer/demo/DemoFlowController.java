/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 */

package com.microsoft.dagx.transfer.demo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.dagx.schema.aws.S3BucketSchema;
import com.microsoft.dagx.spi.DagxException;
import com.microsoft.dagx.spi.monitor.Monitor;
import com.microsoft.dagx.spi.security.Vault;
import com.microsoft.dagx.spi.transfer.flow.DataFlowController;
import com.microsoft.dagx.spi.transfer.flow.DataFlowInitiateResponse;
import com.microsoft.dagx.spi.transfer.response.ResponseStatus;
import com.microsoft.dagx.spi.types.domain.transfer.DataRequest;
import com.microsoft.dagx.spi.types.domain.transfer.DestinationSecretToken;
import org.jetbrains.annotations.NotNull;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

public class DemoFlowController implements DataFlowController {
    private final Vault vault;
    private final Monitor monitor;

    public DemoFlowController(Vault vault, Monitor monitor) {
        this.vault = vault;
        this.monitor = monitor;
    }

    @Override
    public boolean canHandle(DataRequest dataRequest) {
        return true;
    }

    @Override
    public @NotNull DataFlowInitiateResponse initiateFlow(DataRequest dataRequest) {

        var awsSecretName = dataRequest.getDataDestination().getKeyName();
        var awsSecret = vault.resolveSecret(awsSecretName);
        var bucketName = dataRequest.getDataDestination().getProperty(S3BucketSchema.BUCKET_NAME);

        var region = dataRequest.getDataDestination().getProperty(S3BucketSchema.REGION);

        var dt = convertSecret(awsSecret);

        return copyToBucket(bucketName, region, dt);

    }

    @NotNull
    private DataFlowInitiateResponse copyToBucket(String bucketName, String region, DestinationSecretToken dt) {

        var wait = 2;//seconds
        var count = 0;
        var maxRetries = 3;

        try (S3Client s3 = S3Client.builder()
                .credentialsProvider(StaticCredentialsProvider.create(AwsSessionCredentials.create(dt.getAccessKeyId(), dt.getSecretAccessKey(), dt.getToken())))
                .region(Region.of(region))
                .build()) {

            var success = false;
            String etag = null;
            PutObjectRequest request = createRequest(bucketName, "demo-image");

            while (!success && count <= maxRetries) {
                try {
                    monitor.debug("Data request: begin transfer...");
                    var response = s3.putObject(request, RequestBody.fromBytes(createRandomContent()));
                    monitor.debug("Data request done.");
                    success = true;
                    etag = response.eTag();
                } catch (S3Exception tmpEx) {
                    monitor.info("Data request: transfer not successful, retrying after " + wait + " seconds...");
                    count++;
                    Thread.sleep(1000L * wait);
                    wait *= wait;

                }
            }
            return new DataFlowInitiateResponse(ResponseStatus.OK, etag);
        } catch (S3Exception | InterruptedException | DagxException ex) {
            monitor.severe("Data request: transfer failed after " + count + " attempts");
            return new DataFlowInitiateResponse(ResponseStatus.FATAL_ERROR, ex.getLocalizedMessage());
        }
    }

    private DestinationSecretToken convertSecret(String awsSecret) {
        try {
            var mapper = new ObjectMapper();
            return mapper.readValue(awsSecret, DestinationSecretToken.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    private PutObjectRequest createRequest(String bucketName, String objectKey) {
        return PutObjectRequest.builder()
                .bucket(bucketName)
                .metadata(Map.of("name", "demo_image.jpg"))
                .key(objectKey)
                .build();
    }

    private byte[] createRandomContent() {
        InputStream resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("demo_image.jpg");
        try {
            return Objects.requireNonNull(resourceAsStream).readAllBytes();
        } catch (IOException e) {
            throw new DagxException(e);
        }
    }
}
