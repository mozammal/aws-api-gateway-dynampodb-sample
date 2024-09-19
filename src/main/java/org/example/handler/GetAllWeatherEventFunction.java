package org.example.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.ApiGatewayRequestEvent;
import org.example.entity.ApiGatewayResponseEvent;
import org.example.entity.WeatherEvent;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GetAllWeatherEventFunction {

  private static final String DEFAULT_LIMIT = "20";
  private final String tableName = System.getenv("LOCATIONS_TABLE");
  private final String awsRegionEnv = System.getenv("AWS_REGION");
  private final Region region = Region.of(awsRegionEnv);

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final DynamoDbClient dynamoDbClient;
  private final DynamoDbEnhancedClient enhancedClient;
  private final DynamoDbTable<WeatherEvent> mappedTable;

  public GetAllWeatherEventFunction() {
    this.dynamoDbClient = DynamoDbClient.builder().region(region).build();
    this.enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    this.mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(WeatherEvent.class));
  }

  public GetAllWeatherEventFunction(DynamoDbClient dynamoDbClient) {
    this.dynamoDbClient = dynamoDbClient;
    this.enhancedClient =
        DynamoDbEnhancedClient.builder().dynamoDbClient(this.dynamoDbClient).build();
    this.mappedTable = enhancedClient.table(tableName, TableSchema.fromBean(WeatherEvent.class));
  }

  public ApiGatewayResponseEvent apply(ApiGatewayRequestEvent request) {
    final String limitParam =
        request.queryStringParameters() == null
            ? DEFAULT_LIMIT
            : request.queryStringParameters().getOrDefault("limit", DEFAULT_LIMIT);
    final int limit = Integer.parseInt(limitParam);

    try {
      ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder().build();
      Iterator<WeatherEvent> results = mappedTable.scan(scanRequest).items().iterator();
      Iterable<WeatherEvent> iterable = () -> results;
      List<WeatherEvent> weatherEvents =
          StreamSupport.stream(iterable.spliterator(), false)
              .limit(limit)
              .collect(Collectors.toList());

      final String json = objectMapper.writeValueAsString(weatherEvents);

      return new ApiGatewayResponseEvent(200, json);
    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage());
    }
  }
}
