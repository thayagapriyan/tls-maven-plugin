output "bucket_name" {
  description = "S3 bucket name for integration tests"
  value       = aws_s3_bucket.test.id
}

output "bucket_arn" {
  description = "S3 bucket ARN"
  value       = aws_s3_bucket.test.arn
}

output "test_object_key" {
  description = "Key of the test keystore object"
  value       = aws_s3_object.test_keystore.key
}

output "github_actions_role_arn" {
  description = "IAM Role ARN for GitHub Actions OIDC — set as repo secret AWS_ROLE_ARN"
  value       = aws_iam_role.github_actions.arn
}

output "aws_region" {
  description = "AWS region where resources are deployed"
  value       = var.aws_region
}
