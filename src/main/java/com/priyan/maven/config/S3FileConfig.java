package com.priyan.maven.config;

/**
 * Describes a single TLS file to download from an S3 bucket and where to write it locally.
 *
 * <p>Bound from a {@code <file>} element in the plugin {@code <configuration>}:
 *
 * <pre>{@code
 * <file>
 *     <bucket>my-tls-bucket</bucket>
 *     <key>mule-app/dev/keystore.jks</key>
 *     <targetFileName>certificates/keystore.jks</targetFileName>
 * </file>
 * }</pre>
 *
 * <p>S3 objects are returned as raw bytes, so {@code .jks} and {@code .pem} files both stream
 * straight to disk â€” no Base64 decoding or binary/string distinction is needed.
 */
public class S3FileConfig {

    /** The S3 bucket name. Required. */
    private String bucket;

    /** The S3 object key (path within the bucket). Required. */
    private String key;

    /** Path, relative to the configured output directory, the file is written to. Required. */
    private String targetFileName;

    /** Optional object version id (for buckets with versioning enabled). */
    private String versionId;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getTargetFileName() {
        return targetFileName;
    }

    public void setTargetFileName(String targetFileName) {
        this.targetFileName = targetFileName;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /** Convenience for log/error messages: {@code s3://bucket/key}. Never logs file contents. */
    public String location() {
        return "s3://" + bucket + "/" + key;
    }

    @Override
    public String toString() {
        return "S3FileConfig{" + location()
                + ", targetFileName='" + targetFileName + '\''
                + (versionId != null ? ", versionId='" + versionId + '\'' : "") + '}';
    }
}
