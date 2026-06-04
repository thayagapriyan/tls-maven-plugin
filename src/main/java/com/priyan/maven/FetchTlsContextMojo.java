package com.priyan.maven;

import com.priyan.maven.aws.AwsS3ObjectResolver;
import com.priyan.maven.aws.S3FetchException;
import com.priyan.maven.aws.S3ObjectResolver;
import com.priyan.maven.config.S3FileConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Downloads TLS keystore/truststore files from an S3 bucket and writes them into the build
 * output directory, so certificates never need to be committed to version control.
 *
 * <p>Binds by default to the {@code generate-resources} phase, before packaging.
 *
 * <p>AWS credentials are resolved from the environment (profile, IAM role, OIDC, env vars) â€” the
 * plugin never accepts or stores raw access keys.
 */
@Mojo(name = "fetch-tls-context",
        defaultPhase = LifecyclePhase.GENERATE_RESOURCES,
        threadSafe = true)
public class FetchTlsContextMojo extends AbstractMojo {

    /** AWS region of the bucket. When unset, the AWS SDK default region chain is used. */
    @Parameter(property = "tls.region")
    private String region;

    /**
     * Named AWS profile for local credentials. When unset, the {@code DefaultCredentialsProvider}
     * chain is used (IAM roles, GitHub OIDC, ECS task roles, env vars, etc.). This is a non-secret
     * label only â€” never put access keys in the build.
     */
    @Parameter(property = "tls.awsProfile")
    private String awsProfile;

    /** Base directory the files are written under. */
    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    private Path outputDirectory;

    /** Fail the build if a file cannot be downloaded. */
    @Parameter(property = "tls.failOnMissingFile", defaultValue = "true")
    private boolean failOnMissingFile;

    /** Skip plugin execution entirely. */
    @Parameter(property = "tls.skip", defaultValue = "false")
    private boolean skip;

    /** TLS files to download from S3 and where to place them. */
    @Parameter(required = true)
    private List<S3FileConfig> files = new ArrayList<>();

    /** Test seam: injected resolver overrides the AWS-backed one. */
    private S3ObjectResolver resolver;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("tls:fetch-tls-context skipped (tls.skip=true).");
            return;
        }
        if (files == null || files.isEmpty()) {
            getLog().warn("No <files> configured; nothing to download.");
            return;
        }

        boolean ownsResolver = resolver == null;
        S3ObjectResolver activeResolver = ownsResolver
                ? AwsS3ObjectResolver.fromConfig(region, awsProfile)
                : resolver;

        try {
            for (S3FileConfig file : files) {
                fetchOne(activeResolver, file);
            }
        } finally {
            if (ownsResolver) {
                activeResolver.close();
            }
        }
    }

    private void fetchOne(S3ObjectResolver activeResolver, S3FileConfig file)
            throws MojoExecutionException {
        validate(file);
        try {
            byte[] bytes = activeResolver.fetch(file);
            Path target = resolveTarget(file);
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            // Never log file material â€” only source location, path and size.
            getLog().info("Downloaded " + file.location() + " (" + bytes.length
                    + " bytes) -> " + target);
        } catch (S3FetchException e) {
            handleFailure("Could not download " + file.location(), e);
        } catch (IOException e) {
            // A filesystem failure is always fatal regardless of failOnMissingFile.
            throw new MojoExecutionException(
                    "Failed to write " + file.location() + " to disk", e);
        }
    }

    private Path resolveTarget(S3FileConfig file) {
        Path base = outputDirectory.normalize();
        Path target = base.resolve(file.getTargetFileName()).normalize();
        if (!target.startsWith(base)) {
            throw new IllegalArgumentException("targetFileName '" + file.getTargetFileName()
                    + "' escapes the output directory");
        }
        return target;
    }

    private void validate(S3FileConfig file) throws MojoExecutionException {
        if (file.getBucket() == null || file.getBucket().isBlank()) {
            throw new MojoExecutionException("Each <file> requires a <bucket>.");
        }
        if (file.getKey() == null || file.getKey().isBlank()) {
            throw new MojoExecutionException("File in bucket '" + file.getBucket()
                    + "' requires a <key>.");
        }
        if (file.getTargetFileName() == null || file.getTargetFileName().isBlank()) {
            throw new MojoExecutionException(file.location() + " requires a <targetFileName>.");
        }
    }

    private void handleFailure(String message, Exception cause) throws MojoExecutionException {
        if (failOnMissingFile) {
            throw new MojoExecutionException(message, cause);
        }
        getLog().warn(message + " â€” continuing (failOnMissingFile=false): " + cause.getMessage());
    }

    // --- Test support -----------------------------------------------------------------------

    void setResolver(S3ObjectResolver resolver) {
        this.resolver = resolver;
    }

    void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    void setFiles(List<S3FileConfig> files) {
        this.files = files;
    }

    void setFailOnMissingFile(boolean failOnMissingFile) {
        this.failOnMissingFile = failOnMissingFile;
    }

    void setSkip(boolean skip) {
        this.skip = skip;
    }
}
