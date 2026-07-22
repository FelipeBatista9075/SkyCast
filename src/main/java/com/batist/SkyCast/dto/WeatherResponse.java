package com.batist.SkyCast.dto;

public record WeatherResponse(
    String city,
    String country,
    double temperature,
    double feelsLike,
    int humidity,
    double windSpeedKmh,
    String description,
    String icon
) {
}