# S3 bucket for storing raw logs from Kinesis Firehose
resource "aws_s3_bucket" "log_bucket" {
  bucket = local.log_bucket_name

  tags = local.common_tags
}

# S3 bucket for storing Athena query results
resource "aws_s3_bucket" "athena_results_bucket" {
  bucket = local.athena_results_bucket_name

  tags = local.common_tags
}

# Block public access for both buckets
resource "aws_s3_bucket_public_access_block" "log_bucket_pac" {
  bucket                  = aws_s3_bucket.log_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_public_access_block" "athena_results_bucket_pac" {
  bucket                  = aws_s3_bucket.athena_results_bucket.id
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Set ownership controls for Athena
resource "aws_s3_bucket_ownership_controls" "log_bucket_oc" {
  bucket = aws_s3_bucket.log_bucket.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_ownership_controls" "athena_results_bucket_oc" {
  bucket = aws_s3_bucket.athena_results_bucket.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

# Apply server-side encryption
resource "aws_s3_bucket_server_side_encryption_configuration" "log_bucket_sse" {
  bucket = aws_s3_bucket.log_bucket.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "athena_results_bucket_sse" {
  bucket = aws_s3_bucket.athena_results_bucket.id
  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Lifecycle rule for the log bucket to transition old data and eventually expire it
resource "aws_s3_bucket_lifecycle_configuration" "log_bucket_lifecycle" {
  bucket = aws_s3_bucket.log_bucket.id

  rule {
    id     = "log-lifecycle"
    status = "Enabled"

    filter {} # Apply to the entire bucket

    transition {
      days          = 30
      storage_class = "STANDARD_IA" # Infrequent Access
    }

    transition {
      days          = 90
      storage_class = "GLACIER" # Long-term archive
    }

    expiration {
      days = 365 # Expire logs after one year
    }
  }
}
