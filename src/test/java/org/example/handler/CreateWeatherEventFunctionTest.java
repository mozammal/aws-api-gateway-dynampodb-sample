package org.example.handler;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.entity.ApiGatewayRequestEvent;
import org.example.entity.ApiGatewayResponseEvent;
import org.example.entity.WeatherEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;
import java.net.URI;

@ExtendWith(SystemStubsExtension.class)
public class CreateWeatherEventFunctionTest {
  @SystemStub
  private EnvironmentVariables tableName =
      new EnvironmentVariables("LOCATIONS_TABLE", "fake_table");

  @SystemStub
  private EnvironmentVariables awsRegion = new EnvironmentVariables("AWS_REGION", "eu-north-1");

  private DynamoDbEnhancedClient enhancedClient;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private DynamoDBProxyServer server;
  private DynamoDbTable<WeatherEvent> mappedTable;

  private DynamoDbClient dynamoDbClient;

  @BeforeEach
  void setup() throws Exception {
    server =
        ServerRunner.createServerFromCommandLineArgs(new String[] {"-inMemory", "-port", "8080"});
    server.start();
    dynamoDbClient = createClient();
    enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    mappedTable =
        enhancedClient.table(
            tableName.getVariables().get("LOCATIONS_TABLE"),
            TableSchema.fromBean(WeatherEvent.class));
    mappedTable.createTable();
  }

  @AfterEach
  public void deleteTable() throws Exception {
    server.stop();
  }

  private DynamoDbClient createClient() {
    String endpoint = String.format("http://localhost:%d", 8080);
    return DynamoDbClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.EU_NORTH_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("dummykey", "dummysecret")))
        .build();
  }

  @Test
  void putItem_saveItems_Returns_Successfully() throws IOException {
    CreateWeatherEventFunction createWeatherEventFunction =
        new CreateWeatherEventFunction(dynamoDbClient);
    WeatherEvent expectedWeatherEvent = getWeatherEvent();
    String json = objectMapper.writeValueAsString(getWeatherEvent());

    WeatherEvent actualWeatherEvent = createWeatherEventFunction.putItem(json);

    Assertions.assertEquals(actualWeatherEvent, expectedWeatherEvent);
  }

  @Test
  void apply_ValidApiGatewayRequestEvent_returns_successfully() throws IOException {
    CreateWeatherEventFunction weatherEventLambda = new CreateWeatherEventFunction(dynamoDbClient);
    WeatherEvent weatherEvent = getWeatherEvent();
    String json = objectMapper.writeValueAsString(weatherEvent);
    ApiGatewayRequestEvent apiGatewayRequestEvent = new ApiGatewayRequestEvent(json);

    ApiGatewayResponseEvent apiGatewayResponseEvent =
        weatherEventLambda.apply(apiGatewayRequestEvent);

    Assertions.assertEquals(json, apiGatewayResponseEvent.body());
    Assertions.assertEquals(200, apiGatewayResponseEvent.statusCode());
  }

  @Test
  void apply_InValidApiGatewayRequestEvent_throws_exception() throws IOException {
    CreateWeatherEventFunction weatherEventLambda = new CreateWeatherEventFunction(dynamoDbClient);
    WeatherEvent weatherEvent = getWeatherEvent();
    String json = objectMapper.writeValueAsString(weatherEvent);
    ApiGatewayRequestEvent apiGatewayRequestEvent = new ApiGatewayRequestEvent(json.substring(5));

    Assertions.assertThrows(
        RuntimeException.class,
        () -> {
          weatherEventLambda.apply(apiGatewayRequestEvent);
        });
  }

  private static WeatherEvent getWeatherEvent() {
    WeatherEvent weatherEvent = new WeatherEvent();
    weatherEvent.setLocationName("Brooklyn");
    weatherEvent.setTemperature(91.0);
    weatherEvent.setTimestamp(1564428897L);
    weatherEvent.setLatitude(40.70);
    weatherEvent.setLongitude(-73.99);
    return weatherEvent;
  }
}
