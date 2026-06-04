package com.priyan.maven.aws;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.priyan.maven.config.S3FileConfig;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ExtendWith(MockitoExtension.class)
class AwsS3ObjectResolverTest {

    @Mock
    private S3Client client;

    private static S3FileConfig cfg() {
        S3FileConfig c = new S3FileConfig();
        c.setBucket("my-tls-bucket");
        c.setKey("mule-app/dev/keystore.jks");
        c.setTargetFileName("certificates/keystore.jks");
        return c;
    }

    @Test
    void returnsObjectBytesUnchanged() {
        byte[] raw = "keystore-contents".getBytes(StandardCharsets.UTF_8);
        when(client.getObjectAsBytes((GetObjectRequest) any()))
                .thenReturn(ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), raw));

        byte[] result = new AwsS3ObjectResolver(client).fetch(cfg());

        assertArrayEquals(raw, result);
    }

    @Test
    void wrapsNoSuchKeyAsS3FetchException() {
        when(client.getObjectAsBytes((GetObjectRequest) any()))
                .thenThrow(NoSuchKeyException.builder().message("nope").build());

        assertThrows(S3FetchException.class, () -> new AwsS3ObjectResolver(client).fetch(cfg()));
    }

    @Test
    void wrapsNoSuchBucketAsS3FetchException() {
        when(client.getObjectAsBytes((GetObjectRequest) any()))
                .thenThrow(NoSuchBucketException.builder().message("nope").build());

        assertThrows(S3FetchException.class, () -> new AwsS3ObjectResolver(client).fetch(cfg()));
    }
}
