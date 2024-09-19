package org.example.handler;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Arrays;
import java.util.List;

@ExtendWith(SystemStubsExtension.class)
public class GetAllWeatherEventFunctionTest {
  @SystemStub
  private final EnvironmentVariables tableName =
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

  private DynamoDbClient createClient() {
    String endpoint = String.format("http://localhost:%d", 8080);
    return DynamoDbClient.builder()
        .endpointOverride(URI.create(endpoint))
        .region(Region.EU_NORTH_1)
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create("dummykey", "dummysecret")))
        .build();
  }

  @AfterEach
  public void deleteTable() throws Exception {
    server.stop();
  }

  @Test
  void apply_ValidApiGatewayRequestEvent_Returns_Successfully() throws IOException {
    GetAllWeatherEventFunction getAllWeatherEventFunction =
        new GetAllWeatherEventFunction(dynamoDbClient);
    CreateWeatherEventFunction createWeatherEventFunction =
        new CreateWeatherEventFunction(dynamoDbClient);
    WeatherEvent weatherEvent1 = new WeatherEvent();
    weatherEvent1.setLocationName("Brooklyn");
    weatherEvent1.setTemperature(91.0);
    weatherEvent1.setTimestamp(1564428897L);
    weatherEvent1.setLatitude(40.70);
    weatherEvent1.setLongitude(-73.99);
    WeatherEvent weatherEvent2 = new WeatherEvent();
    weatherEvent2.setLocationName("Brooklyn1");
    weatherEvent2.setTemperature(11.0);
    weatherEvent2.setTimestamp(1564428899L);
    weatherEvent2.setLatitude(40.70);
    weatherEvent2.setLongitude(-73.99);
    ApiGatewayRequestEvent apiGatewayRequestEvent1 =
        new ApiGatewayRequestEvent(objectMapper.writeValueAsString(weatherEvent1));
    ApiGatewayRequestEvent apiGatewayRequestEvent2 =
        new ApiGatewayRequestEvent(objectMapper.writeValueAsString(weatherEvent2));

    createWeatherEventFunction.apply(apiGatewayRequestEvent1);
    createWeatherEventFunction.apply(apiGatewayRequestEvent2);
    ApiGatewayResponseEvent apiGatewayResponseEvent =
        getAllWeatherEventFunction.apply(new ApiGatewayRequestEvent(""));

    List<WeatherEvent> weatherEvents =
        objectMapper.readValue(apiGatewayResponseEvent.body(), new TypeReference<>() {});

    Assertions.assertEquals(2, weatherEvents.size());
    Assertions.assertIterableEquals(Arrays.asList(weatherEvent1, weatherEvent2), weatherEvents);
  }
}
