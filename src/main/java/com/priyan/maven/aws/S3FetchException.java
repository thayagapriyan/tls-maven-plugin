package com.priyan.maven.aws;

/** Thrown when a TLS file cannot be downloaded from S3. Never carries file contents. */
public class S3FetchException extends RuntimeException {

    public S3FetchException(String message) {
        super(message);
    }

    public S3FetchException(String message, Throwable cause) {
        super(message, cause);
    }
}
