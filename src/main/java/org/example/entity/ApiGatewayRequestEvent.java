package org.example.entity;

import java.util.HashMap;
import java.util.Map;

public record ApiGatewayRequestEvent(String body, Map<String, String> queryStringParameters) {
  public ApiGatewayRequestEvent(String body) {
    this(body, new HashMap<>());
  }
}
