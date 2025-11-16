# IAM User for the Spring Boot application
resource "aws_iam_user" "app_user" {
  name = "${var.project_name}-app-user"
  tags = local.common_tags
}

# IAM Policy for the application user
resource "aws_iam_policy" "app_policy" {
  name   = "${var.project_name}-app-policy"
  policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Effect   = "Allow",
        Action   = [
          "firehose:PutRecord",
          "firehose:PutRecordBatch"
        ],
        Resource = [
          "arn:aws:firehose:${var.aws_region}:${data.aws_caller_identity.current.account_id}:deliverystream/${var.project_name}-stream"
        ]
      },
      {
        Effect = "Allow",
        Action = [
          "athena:StartQueryExecution",
          "athena:GetQueryExecution",
          "athena:GetQueryResults",
          "athena:GetWorkGroup"
        ],
        Resource = [
          aws_athena_workgroup.app_workgroup.arn
        ]
      },
      {
        Effect = "Allow",
        Action = [
          "glue:GetDatabase",
          "glue:GetTable"
        ],
        Resource = [
          "arn:aws:glue:${var.aws_region}:${data.aws_caller_identity.current.account_id}:catalog",
          aws_glue_catalog_database.user_logs_db.arn,
          aws_glue_catalog_table.user_history_logs.arn
        ]
      },
      {
        Effect = "Allow",
        Action = [
          "s3:GetObject",
          "s3:GetBucketLocation",
          "s3:ListBucket"
        ],
        Resource = [
          aws_s3_bucket.athena_results_bucket.arn,
          aws_s3_bucket.log_bucket.arn
        ]
      },
      {
        Effect = "Allow",
        Action = [
          "s3:PutObject",
          "s3:GetObject"
        ],
        Resource = [
          "${aws_s3_bucket.athena_results_bucket.arn}/*",
          "${aws_s3_bucket.log_bucket.arn}/*"
        ]
      }
    ]
  })
  tags = local.common_tags
}

# Attach the policy to the user
resource "aws_iam_user_policy_attachment" "app_attach" {
  user       = aws_iam_user.app_user.name
  policy_arn = aws_iam_policy.app_policy.arn
}

# Create an access key for the user
resource "aws_iam_access_key" "app_access_key" {
  user = aws_iam_user.app_user.name
}