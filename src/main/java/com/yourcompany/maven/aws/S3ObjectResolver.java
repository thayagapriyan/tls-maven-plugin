package com.yourcompany.maven.aws;

import com.yourcompany.maven.config.S3FileConfig;

/**
 * Resolves the raw bytes of a TLS file from S3. Implemented against the AWS SDK in production
 * ({@link AwsS3ObjectResolver}); a seam that lets the Mojo be unit-tested with a fake.
 */
public interface S3ObjectResolver extends AutoCloseable {

    /**
     * Downloads the object described by {@code config} and returns its raw bytes.
     *
     * @throws S3FetchException if the object cannot be retrieved
     */
    byte[] fetch(S3FileConfig config);

    /** No-op by default; AWS-backed implementations close the underlying client. */
    @Override
    default void close() {
        // no-op
    }
}
