# Create a new Athena workgroup for this project
resource "aws_athena_workgroup" "app_workgroup" {
  name = var.project_name

  configuration {
    result_configuration {
      output_location = "s3://${aws_s3_bucket.athena_results_bucket.bucket}/"
    }
  }

  tags = local.common_tags
}
