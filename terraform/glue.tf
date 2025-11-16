# AWS Glue Catalog Database
resource "aws_glue_catalog_database" "user_logs_db" {
  name = "${var.project_name}-db"
}

# AWS Glue Catalog Table for user history logs
resource "aws_glue_catalog_table" "user_history_logs" {
  name          = "user_history_logs"
  database_name = aws_glue_catalog_database.user_logs_db.name
  table_type    = "EXTERNAL_TABLE"

  parameters = {
    "EXTERNAL"                    = "TRUE"
    "parquet.compress"            = "SNAPPY"
    "projection.enabled"          = "true"
    "projection.dt.type"          = "date"
    "projection.dt.range"         = "2025-01-01,NOW"
    "projection.dt.format"        = "yyyy-MM-dd"
    "projection.dt.interval"      = "1"
    "projection.dt.interval.unit" = "DAYS"
    "storage.location.template"   = "s3://${aws_s3_bucket.log_bucket.bucket}/prefix/dt=$${dt}"
  }

  storage_descriptor {
    location      = "s3://${aws_s3_bucket.log_bucket.bucket}/prefix/"
    input_format  = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetInputFormat"
    output_format = "org.apache.hadoop.hive.ql.io.parquet.MapredParquetOutputFormat"

    ser_de_info {
      name                  = "ParquetHiveSerDe"
      serialization_library = "org.apache.hadoop.hive.ql.io.parquet.serde.ParquetHiveSerDe"
      parameters = {
        "serialization.format" = "1"
      }
    }

    columns {
      name = "account_id"
      type = "bigint"
    }
    columns {
      name = "type"
      type = "string"
    }
    columns {
      name = "created_at"
      type = "bigint"
    }
    columns {
      name = "context_data"
      type = "map<string,string>"
    }
  }

  partition_keys {
    name = "dt"
    type = "string"
  }

  depends_on = [
    aws_s3_bucket.log_bucket
  ]
}
