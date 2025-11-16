# Kinesis Data Firehose Delivery Stream
resource "aws_kinesis_firehose_delivery_stream" "user_history_stream" {
  name        = "${var.project_name}-stream"
  destination = "extended_s3"

  extended_s3_configuration {
    role_arn   = aws_iam_role.firehose_role.arn
    bucket_arn = aws_s3_bucket.log_bucket.arn

    # Dynamic partitioning based on the 'dt' field in the data
    dynamic_partitioning_configuration {
      enabled = true
    }

    # This processing configuration extracts the 'dt' field from the JSON record
    # and makes it available for dynamic partitioning.
    processing_configuration {
      enabled = true
      processors {
        type = "MetadataExtraction"
        parameters {
          parameter_name  = "JsonParsingEngine"
          parameter_value = "JQ-1.6"
        }
        parameters {
          parameter_name  = "MetadataExtractionQuery"
          parameter_value = "{dt: .dt}"
        }
      }
      processors {
        type = "AppendDelimiterToRecord"
        parameters {
          parameter_name  = "Delimiter"
          parameter_value = "\\n"
        }
      }
    }

    # S3 object prefix using the dynamically extracted 'dt' partition key
    prefix              = "prefix/dt=!{partitionKeyFromQuery:dt}/"
    error_output_prefix = "error/!{firehose:error-output-type}/"

    # Buffer settings
    buffering_size     = 128
    buffering_interval = 900

    cloudwatch_logging_options {
      enabled         = true
      log_group_name  = aws_cloudwatch_log_group.firehose_log_group.name
      log_stream_name = "S3Delivery"
    }

    # Data format conversion from JSON to Parquet
    data_format_conversion_configuration {
      enabled = true
      input_format_configuration {
        deserializer {
          open_x_json_ser_de {}
        }
      }
      output_format_configuration {
        serializer {
          parquet_ser_de {}
        }
      }
      schema_configuration {
        database_name = aws_glue_catalog_database.user_logs_db.name
        table_name    = aws_glue_catalog_table.user_history_logs.name
        role_arn      = aws_iam_role.firehose_role.arn
      }
    }
  }

  tags = local.common_tags

  depends_on = [
    aws_iam_role_policy_attachment.firehose_attach,
    aws_cloudwatch_log_group.firehose_log_group
  ]
}
