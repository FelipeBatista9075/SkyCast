package com.batist.SkyCast.dto;

import java.util.List;

public record ForecastResponse(
    String city,
    String country,
    List<DailyForecast> days
) {
    public record DailyForecast(
            String date,
            double minTemp,
            double maxTemp,
            String description,
            String icon
    ){

    }
}
