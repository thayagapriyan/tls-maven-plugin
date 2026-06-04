package com.yourcompany.maven;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.yourcompany.maven.aws.S3FetchException;
import com.yourcompany.maven.aws.S3ObjectResolver;
import com.yourcompany.maven.config.S3FileConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FetchTlsContextMojoTest {

    private static S3FileConfig file(String bucket, String key, String target) {
        S3FileConfig c = new S3FileConfig();
        c.setBucket(bucket);
        c.setKey(key);
        c.setTargetFileName(target);
        return c;
    }

    private FetchTlsContextMojo mojo(Path outputDir, S3ObjectResolver resolver,
            S3FileConfig... files) {
        FetchTlsContextMojo mojo = new FetchTlsContextMojo();
        mojo.setOutputDirectory(outputDir);
        mojo.setResolver(resolver);
        mojo.setFiles(List.of(files));
        return mojo;
    }

    @Test
    void writesDownloadedBytesToTargetUnderOutputDirectory(@TempDir Path outputDir)
            throws Exception {
        byte[] payload = "keystore-bytes".getBytes();
        S3FileConfig cfg = file("my-tls-bucket", "mule-app/dev/keystore.jks",
                "certificates/keystore.jks");

        mojo(outputDir, c -> payload, cfg).execute();

        Path written = outputDir.resolve("certificates/keystore.jks");
        assertTrue(Files.exists(written));
        assertArrayEquals(payload, Files.readAllBytes(written));
    }

    @Test
    void createsNestedParentDirectories(@TempDir Path outputDir) throws Exception {
        S3FileConfig cfg = file("b", "trust.pem", "a/b/c/trust.pem");

        mojo(outputDir, c -> new byte[] {1, 2, 3}, cfg).execute();

        assertTrue(Files.exists(outputDir.resolve("a/b/c/trust.pem")));
    }

    @Test
    void failsBuildWhenDownloadFailsAndFailOnMissingFileTrue(@TempDir Path outputDir) {
        S3FileConfig cfg = file("b", "missing.jks", "x.jks");
        FetchTlsContextMojo mojo = mojo(outputDir, c -> {
            throw new S3FetchException("boom");
        }, cfg);
        mojo.setFailOnMissingFile(true);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void continuesWhenDownloadFailsAndFailOnMissingFileFalse(@TempDir Path outputDir)
            throws Exception {
        S3FileConfig cfg = file("b", "missing.jks", "x.jks");
        FetchTlsContextMojo mojo = mojo(outputDir, c -> {
            throw new S3FetchException("boom");
        }, cfg);
        mojo.setFailOnMissingFile(false);

        mojo.execute(); // must not throw

        assertFalse(Files.exists(outputDir.resolve("x.jks")));
    }

    @Test
    void skipShortCircuitsExecution(@TempDir Path outputDir) throws Exception {
        S3FileConfig cfg = file("b", "k.jks", "x.jks");
        FetchTlsContextMojo mojo = mojo(outputDir, c -> {
            throw new AssertionError("resolver must not be called when skip=true");
        }, cfg);
        mojo.setSkip(true);

        mojo.execute();

        assertFalse(Files.exists(outputDir.resolve("x.jks")));
    }

    @Test
    void rejectsTargetThatEscapesOutputDirectory(@TempDir Path outputDir) {
        S3FileConfig cfg = file("b", "k.jks", "../escape.jks");
        FetchTlsContextMojo mojo = mojo(outputDir, c -> new byte[] {1}, cfg);
        mojo.setFailOnMissingFile(true);

        // path traversal surfaces as a build failure, not a silent write
        assertThrows(Exception.class, mojo::execute);
    }

    @Test
    void failsWhenBucketMissing(@TempDir Path outputDir) {
        S3FileConfig cfg = file(null, "k.jks", "x.jks");
        FetchTlsContextMojo mojo = mojo(outputDir, c -> new byte[] {1}, cfg);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void failsWhenKeyMissing(@TempDir Path outputDir) {
        S3FileConfig cfg = file("b", null, "x.jks");
        FetchTlsContextMojo mojo = mojo(outputDir, c -> new byte[] {1}, cfg);

        assertThrows(MojoExecutionException.class, mojo::execute);
    }
}
