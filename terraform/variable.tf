variable "aws_region" {
  description = "The aws region"
  type        = string
  default     = "eu-north-1"
}

variable "zip_path" {
  description = "Zip file dployed as a lambda function"
  type        = string
  default     = "target/aws-api-gateway-dynamodb-sample.zip"
}

variable "aws_bucket_name" {
  description = "Name of the aws bucket, need to be unique"
  type        = string
  default     = "my-bucket-moz-61717"
}

variable "integration_http_method" {
  description = "integration_http_method"
  type        = string
  default     = "POST"
}

variable "integration_http_method_type" {
  description = "integration_http_method"
  type        = string
  default     = "AWS_PROXY"
}

variable "authorization_none" {
  type    = string
  default = "NONE"
}

variable "stage_name" {
  type    = string
  default = "Prod"
}

variable "http_methods" {
  type    = map(string)
  default = {
    GET  = "GET"
    POST = "POST"
  }
}

variable "paths" {
  type    = map(string)
  default = {
    events    = "events"
    locations = "locations"
  }
}