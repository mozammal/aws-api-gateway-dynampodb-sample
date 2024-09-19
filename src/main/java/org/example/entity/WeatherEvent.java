package org.example.entity;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.Objects;

@DynamoDbBean
public class WeatherEvent {
  private String locationName;
  private Double temperature;
  private Long timestamp;
  private Double longitude;
  private Double latitude;

  @DynamoDbPartitionKey
  public String getLocationName() {
    return locationName;
  }

  public void setLocationName(String locationName) {
    this.locationName = locationName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WeatherEvent that = (WeatherEvent) o;
    return Objects.equals(locationName, that.locationName)
        && Objects.equals(temperature, that.temperature)
        && Objects.equals(timestamp, that.timestamp)
        && Objects.equals(longitude, that.longitude)
        && Objects.equals(latitude, that.latitude);
  }

  @Override
  public int hashCode() {
    return Objects.hash(locationName, temperature, timestamp, longitude, latitude);
  }

  public Double getTemperature() {
    return temperature;
  }

  public void setTemperature(Double temperature) {
    this.temperature = temperature;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Long timestamp) {
    this.timestamp = timestamp;
  }

  public Double getLongitude() {
    return longitude;
  }

  public void setLongitude(Double longitude) {
    this.longitude = longitude;
  }

  public Double getLatitude() {
    return latitude;
  }

  public void setLatitude(Double latitude) {
    this.latitude = latitude;
  }

  @Override
  public String toString() {
    return "WeatherEvent{"
        + "locationName='"
        + locationName
        + '\''
        + ", temperature="
        + temperature
        + ", timestamp="
        + timestamp
        + ", longitude="
        + longitude
        + ", latitude="
        + latitude
        + '}';
  }
}
