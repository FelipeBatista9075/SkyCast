package com.batist.SkyCast.service;

import com.batist.SkyCast.dto.ForecastResponse;
import com.batist.SkyCast.model.Item;
import com.batist.SkyCast.model.OpenWeahterCurrentResponse;
import com.batist.SkyCast.dto.WeatherResponse;
import com.batist.SkyCast.model.OpenWeatherForecastResponse;
import com.batist.SkyCast.model.Weather;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.slf4j.Logger;
import java.util.stream.Collectors;


@Service
public class WeatherService {
    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final DateTimeFormatter FORECAST_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public WeatherService(RestTemplate restTemplate,
                          @Value("${weather.api.key}") String apiKey,
                          @Value("${weather.api.base-url}") String baseUrl)
    {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }


    @Cacheable(value = "currentWeather", key = "#city.toLowerCase()")
    public WeatherResponse getCurrentWeather(String city) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/weather")
                .queryParam("q", city)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .queryParam("lang", "pt_br")
                .toUriString();

        OpenWeahterCurrentResponse raw = callApi(url, city, OpenWeahterCurrentResponse.class);
        return mapToWeatherResponse(raw);
    }

    @Cacheable(value = "forecast", key = "#city.toLowerCase()")
    public ForecastResponse getForecast(String city) {
        String url = UriComponentsBuilder.fromUriString(baseUrl + "/forecast")
                .queryParam("q", city)
                .queryParam("appid", apiKey)
                .queryParam("units", "metric")
                .queryParam("lang", "pt_br")
                .toUriString();

        OpenWeatherForecastResponse raw = callApi(url, city, OpenWeatherForecastResponse.class);
        return mapToForecastResponse(raw);
    }

    private <T> T callApi(String url, String city, Class<T> responseType) {
        try{
            return restTemplate.getForObject(url, responseType);
        } catch (HttpClientErrorException e) {
            throw new HttpClientErrorException(e.getStatusCode());
        } catch (RestClientException e) {
            throw new RestClientException("Error occurred while fetching weather data for city: " + city, e);
        }
    }

    private WeatherResponse mapToWeatherResponse(OpenWeahterCurrentResponse raw) {
        Weather weather = raw.getWeather() != null && !raw.getWeather().isEmpty()
                ? raw.getWeather().get(0) : null;

        return new WeatherResponse(
                raw.getName(),
                raw.getSys() != null ? raw.getSys().getCountry() : null,
                raw.getMain().getTemp(),
                raw.getMain().getFeels_like(),
                raw.getMain().getHumidity(),
                round(raw.getWind() != null ? raw.getWind().getSpeed() * 3.6 : 0), // m/s -> km/h
                weather != null ? weather.getDescription() : "N/A",
                weather != null ? weather.getIcon() : null
        );
    }

    private ForecastResponse mapToForecastResponse(OpenWeatherForecastResponse raw) {
        // Agrupa os blocos de 3 em 3 horas por dia (yyyy-MM-dd)
        Map<String, List<Item>> byDay = raw.getItems().stream()
                .collect(Collectors.groupingBy(
                        item -> LocalDateTime.parse(item.getDt_txt(), FORECAST_DATE_FORMAT).toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ForecastResponse.DailyForecast> days = byDay.entrySet().stream()
                .map(entry -> summarizeDay(entry.getKey(), entry.getValue()))
                .toList();

        return new ForecastResponse(
                raw.getCity().getName(),
                raw.getCity().getCountry(),
                days
        );
    }

    private ForecastResponse.DailyForecast summarizeDay(String date, List<Item> items) {
        double min = items.stream().mapToDouble(i -> i.getMain().getTemp()).min().orElse(0);
        double max = items.stream().mapToDouble(i -> i.getMain().getTemp()).max().orElse(0);

        Item midday = items.stream()
                .min(Comparator.comparingInt(i -> Math.abs(
                        LocalDateTime.parse(i.getDt_txt(), FORECAST_DATE_FORMAT).getHour() - 12
                )))
                .orElse(items.get(0));

        String description = midday.getWeather() != null && !midday.getWeather().isEmpty()
                ? midday.getWeather().get(0).getDescription()
                : "N/A";
        String icon = midday.getWeather() != null && !midday.getWeather().isEmpty()
                ? midday.getWeather().get(0).getIcon()
                : null;

        return new ForecastResponse.DailyForecast(date, round(min), round(max), description, icon);
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}

