package org.example.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.ApiGatewayRequestEvent;
import org.example.entity.ApiGatewayResponseEvent;
import org.example.entity.WeatherEvent;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

public class CreateWeatherEventFunction {
  private final ObjectMapper objectMapper = new ObjectMapper();
  DynamoDbClient dynamoDbClient;
  private final String tableName = System.getenv("LOCATIONS_TABLE");
  private final String awsRegionEnv = System.getenv("AWS_REGION");
  private final DynamoDbEnhancedClient enhancedClient;
  private final DynamoDbTable<WeatherEvent> mappedTable;

  final Region region = Region.of(awsRegionEnv);

  public CreateWeatherEventFunction() {
    this.dynamoDbClient = DynamoDbClient.builder().region(region).build();
    this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    this.mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(WeatherEvent.class));
  }

  public CreateWeatherEventFunction(DynamoDbClient dynamoDbClient) {
    this.dynamoDbClient = dynamoDbClient;
    this.enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(this.dynamoDbClient).build();
    this.mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(WeatherEvent.class));
  }

  public ApiGatewayResponseEvent apply(ApiGatewayRequestEvent request) {
    try {
      WeatherEvent weatherEvent = putItem(request.body());
      String responseBody = objectMapper.writeValueAsString(weatherEvent);
      return new ApiGatewayResponseEvent(200, responseBody);
    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage());
    }
  }

  WeatherEvent putItem(String request) throws JsonProcessingException {
    var weatherEvent = objectMapper.readValue(request, WeatherEvent.class);
    mappedTable.putItem(r -> r.item(weatherEvent));
    return mappedTable.getItem(r -> r.key(k -> k.partitionValue(weatherEvent.getLocationName())));
  }
}
