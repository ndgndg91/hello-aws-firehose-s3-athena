output "app_aws_access_key_id" {
  description = "The access key ID for the application user. To be used in the Spring Boot application."
  value       = aws_iam_access_key.app_access_key.id
  sensitive   = true
}

output "app_aws_secret_access_key" {
  description = "The secret access key for the application user. To be used in the Spring Boot application."
  value       = aws_iam_access_key.app_access_key.secret
  sensitive   = true
}

output "athena_results_bucket_name" {
  description = "The name of the S3 bucket for storing Athena query results."
  value       = aws_s3_bucket.athena_results_bucket.bucket
}
