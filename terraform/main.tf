# Terraform configuration for TLS Maven Plugin E2E testing infrastructure
#
# Creates:
# 1. S3 bucket with a test keystore object
# 2. IAM OIDC provider for GitHub Actions
# 3. IAM Role that GitHub Actions assumes via OIDC
# 4. IAM Policy granting s3:GetObject on the test bucket

terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "~> 4.0"
    }
  }

  # Uncomment and configure for remote state
  # backend "s3" {
  #   bucket = "your-terraform-state-bucket"
  #   key    = "tls-maven-plugin/terraform.tfstate"
  #   region = "us-east-1"
  # }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project   = "tls-maven-plugin"
      ManagedBy = "terraform"
    }
  }
}
