terraform {
  required_version = ">= 1.0.0, < 2.0.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

locals {
  object_source = "../${path.module}/${var.zip_path}"
}

resource "aws_s3_object" "file_upload" {
  bucket      = var.aws_bucket_name
  key         = var.zip_path
  source      = local.object_source
  source_hash = filemd5(local.object_source)
}

resource "aws_dynamodb_table" "locations_table" {
  name         = "LocationsTable"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "locationName"
  attribute {
    name = "locationName"
    type = "S"
  }
}

resource "aws_iam_role" "lambda_role" {
  name               = "lambda-execution-role"
  assume_role_policy = jsonencode({
    Version   = "2012-10-17"
    Statement = [
      {
        Action    = "sts:AssumeRole"
        Effect    = "Allow"
        Sid       = ""
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      },
    ]
  })
}

resource "aws_iam_policy" "dynamodb_crud_policy" {
  name   = "DynamoDBCrudPolicy"
  policy = jsonencode({
    Version   = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "dynamodb:PutItem",
          "dynamodb:GetItem",
          "dynamodb:UpdateItem",
          "dynamodb:DeleteItem",
          "dynamodb:Scan",
        ],
        Resource = aws_dynamodb_table.locations_table.arn
      }
    ]
  })
}

resource "aws_iam_policy_attachment" "lambda_attach_crud_policy" {
  policy_arn = aws_iam_policy.dynamodb_crud_policy.arn
  roles      = [aws_iam_role.lambda_role.name]
  name       = "lambda_attach_crud_policy"
}

resource "aws_lambda_function" "CreateWeatherEventFunction" {
  function_name = "CreateWeatherEventFunction"
  handler       = "org.example.handler.CreateWeatherEventFunction::apply"
  runtime       = "java17"
  memory_size   = 512
  timeout       = 20
  role          = aws_iam_role.lambda_role.arn
  s3_bucket     = aws_s3_object.file_upload.bucket
  s3_key        = aws_s3_object.file_upload.key
  environment {
    variables = {
      LOCATIONS_TABLE = aws_dynamodb_table.locations_table.name
    }
  }
}

resource "aws_lambda_function" "GetAllWeatherEventFunction" {
  function_name = "GetAllWeatherEventFunction"
  handler       = "org.example.handler.GetAllWeatherEventFunction::apply"
  runtime       = "java17"
  memory_size   = 512
  timeout       = 20
  role          = aws_iam_role.lambda_role.arn
  s3_bucket     = aws_s3_object.file_upload.bucket
  s3_key        = aws_s3_object.file_upload.key
  environment {
    variables = {
      LOCATIONS_TABLE = aws_dynamodb_table.locations_table.name
    }
  }
}

resource "aws_api_gateway_rest_api" "api" {
  name = "aws-api-gateway-dynamodb-sample"
}

resource "aws_api_gateway_resource" "events_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = var.paths.events
}

resource "aws_api_gateway_method" "events_resource_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.events_resource.id
  http_method   = var.http_methods.POST
  authorization = var.authorization_none
}

resource "aws_lambda_permission" "CreateWeatherEventFunction_permission" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.CreateWeatherEventFunction.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/${var.stage_name}/${var.http_methods.POST}/${var.paths.events}"
}

resource "aws_api_gateway_integration" "events_resource_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.events_resource.id
  http_method             = aws_api_gateway_method.events_resource_method.http_method
  integration_http_method = var.integration_http_method
  type                    = var.integration_http_method_type
  uri                     = aws_lambda_function.CreateWeatherEventFunction.invoke_arn
}

resource "aws_api_gateway_resource" "locations_resource" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  parent_id   = aws_api_gateway_rest_api.api.root_resource_id
  path_part   = var.paths.locations
}

resource "aws_api_gateway_method" "locations_method" {
  rest_api_id   = aws_api_gateway_rest_api.api.id
  resource_id   = aws_api_gateway_resource.locations_resource.id
  http_method   = var.http_methods.GET
  authorization = var.authorization_none
}

resource "aws_lambda_permission" "GetAllWeatherEventFunction_permission" {
  statement_id  = "AllowExecutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.GetAllWeatherEventFunction.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.api.execution_arn}/${var.stage_name}/${var.http_methods.GET}/${var.paths.locations}"
}

resource "aws_api_gateway_integration" "locations_resource_integration" {
  rest_api_id             = aws_api_gateway_rest_api.api.id
  resource_id             = aws_api_gateway_resource.locations_resource.id
  http_method             = aws_api_gateway_method.locations_method.http_method
  integration_http_method = var.integration_http_method
  type                    = var.integration_http_method_type
  uri                     = aws_lambda_function.GetAllWeatherEventFunction.invoke_arn
}

resource "aws_api_gateway_deployment" "api_deployment" {
  rest_api_id = aws_api_gateway_rest_api.api.id
  stage_name  = var.stage_name

  depends_on = [
    aws_api_gateway_integration.events_resource_integration,
    aws_api_gateway_integration.locations_resource_integration
  ]
}