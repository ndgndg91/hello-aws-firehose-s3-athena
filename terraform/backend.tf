terraform {
  backend "s3" {
    bucket         = "giri91-terraform-state"
    key            = "excel-demo/terraform.tfstate"
    region         = "ap-northeast-2"
    dynamodb_table = "terraform-lock"
  }
}
