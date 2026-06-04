variable "aws_region" {
  description = "AWS region to deploy resources"
  type        = string
  default     = "us-east-1"
}

variable "github_org" {
  description = "GitHub organization or username (e.g., 'yourcompany' or 'your-username')"
  type        = string
}

variable "github_repo" {
  description = "GitHub repository name (e.g., 'tls-maven-plugin')"
  type        = string
  default     = "tls-maven-plugin"
}

variable "bucket_name" {
  description = "Name for the S3 test bucket. Must be globally unique."
  type        = string
  default     = ""
}
