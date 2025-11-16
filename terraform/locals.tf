resource "random_string" "bucket_suffix" {
  length  = 8
  special = false
  upper   = false
}

locals {
  # Common tags to be applied to all resources
  common_tags = {
    Project   = var.project_name
    ManagedBy = "Terraform"
  }

  # Bucket names
  log_bucket_name          = "${var.project_name}-logs-${random_string.bucket_suffix.result}"
  athena_results_bucket_name = "${var.project_name}-athena-results-${random_string.bucket_suffix.result}"
}
