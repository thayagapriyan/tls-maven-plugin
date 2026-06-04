locals {
  bucket_name = var.bucket_name != "" ? var.bucket_name : "tls-maven-plugin-test-${data.aws_caller_identity.current.account_id}"
}

data "aws_caller_identity" "current" {}

# ─── S3 Bucket ───────────────────────────────────────────────────────────────────

resource "aws_s3_bucket" "test" {
  bucket = local.bucket_name

  tags = {
    Purpose = "E2E testing for tls-maven-plugin"
  }
}

resource "aws_s3_bucket_versioning" "test" {
  bucket = aws_s3_bucket.test.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "test" {
  bucket = aws_s3_bucket.test.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_public_access_block" "test" {
  bucket                  = aws_s3_bucket.test.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Upload a dummy keystore file for integration tests
resource "aws_s3_object" "test_keystore" {
  bucket  = aws_s3_bucket.test.id
  key     = "test/dummy-keystore.jks"
  content = "DUMMY-KEYSTORE-FOR-TESTING-ONLY-NOT-REAL-CRYPTO-MATERIAL"

  tags = {
    Purpose = "Integration test fixture"
  }
}

resource "aws_s3_object" "test_truststore" {
  bucket  = aws_s3_bucket.test.id
  key     = "test/dummy-truststore.pem"
  content = "DUMMY-TRUSTSTORE-FOR-TESTING-ONLY-NOT-REAL-CRYPTO-MATERIAL"

  tags = {
    Purpose = "Integration test fixture"
  }
}
