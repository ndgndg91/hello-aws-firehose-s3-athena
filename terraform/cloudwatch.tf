# CloudWatch Log Group for Kinesis Firehose error logging
resource "aws_cloudwatch_log_group" "firehose_log_group" {
  name              = "/aws/kinesisfirehose/${var.project_name}-stream"
  retention_in_days = 14

  tags = local.common_tags
}
