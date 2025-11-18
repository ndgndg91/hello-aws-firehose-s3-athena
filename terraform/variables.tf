variable "aws_region" {
  description = "The AWS region to deploy resources in."
  type        = string
  default     = "ap-northeast-2"
}

variable "project_name" {
  description = "A name for the project to prefix resources."
  type        = string
  default     = "excel-demo-logs-new"
}
