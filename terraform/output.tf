output "get_url" {
  value       = "${aws_api_gateway_deployment.api_deployment.invoke_url}/locations"
  description = "GET URL"
}

output "post_url" {
  value       = "${aws_api_gateway_deployment.api_deployment.invoke_url}/events"
  description = "POST URL"
}

