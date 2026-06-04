package com.priyan.maven.aws;

import com.priyan.maven.config.S3FileConfig;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * {@link S3ObjectResolver} backed by AWS S3 (SDK v2).
 *
 * <p>Credentials are resolved from the environment, never from plugin configuration:
 * a named profile when {@code awsProfile} is set, otherwise the {@link DefaultCredentialsProvider}
 * chain (env vars, SSO, IAM instance/task roles, GitHub OIDC, ...). The plugin stores no keys.
 *
 * <p>Never logs object contents â€” callers receive bytes; failures throw with the bucket/key only.
 */
public class AwsS3ObjectResolver implements S3ObjectResolver {

    private final S3Client client;

    /** Wraps an existing client (used by tests). */
    public AwsS3ObjectResolver(S3Client client) {
        this.client = client;
    }

    /**
     * Builds a resolver from plugin configuration.
     *
     * @param region     AWS region; when {@code null}/blank the SDK default region chain is used
     * @param awsProfile named profile for local credentials; when {@code null}/blank the
     *                   {@link DefaultCredentialsProvider} chain is used
     */
    public static AwsS3ObjectResolver fromConfig(String region, String awsProfile) {
        AwsCredentialsProvider credentials = (awsProfile == null || awsProfile.isBlank())
                ? DefaultCredentialsProvider.create()
                : ProfileCredentialsProvider.create(awsProfile);

        S3ClientBuilder builder = S3Client.builder().credentialsProvider(credentials);
        if (region != null && !region.isBlank()) {
            builder.region(Region.of(region));
        }
        return new AwsS3ObjectResolver(builder.build());
    }

    @Override
    public byte[] fetch(S3FileConfig config) {
        GetObjectRequest.Builder request = GetObjectRequest.builder()
                .bucket(config.getBucket())
                .key(config.getKey());
        if (config.getVersionId() != null && !config.getVersionId().isBlank()) {
            request.versionId(config.getVersionId());
        }

        try {
            ResponseBytes<GetObjectResponse> object = client.getObjectAsBytes(request.build());
            return object.asByteArray();
        } catch (NoSuchKeyException e) {
            throw new S3FetchException("Object not found: " + config.location()
                    + " (check the <key>).", e);
        } catch (NoSuchBucketException e) {
            throw new S3FetchException("Bucket not found: " + config.getBucket()
                    + " (check the <bucket> and <region>).", e);
        } catch (S3Exception e) {
            // awsErrorDetails carries the service-side message without exposing object bytes
            String detail = e.awsErrorDetails() != null
                    ? e.awsErrorDetails().errorMessage()
                    : e.getMessage();
            throw new S3FetchException("Failed to download " + config.location() + ": " + detail, e);
        }
    }

    @Override
    public void close() {
        client.close();
    }
}
