package com.batist.SkyCast.controller;

import com.batist.SkyCast.dto.ForecastResponse;
import com.batist.SkyCast.dto.WeatherResponse;
import com.batist.SkyCast.service.WeatherService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/skycast")
@Validated
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/{city}")
    public ResponseEntity<WeatherResponse> getCurrentWeather(
            @PathVariable @NotBlank(message = "o nome da cidade não pode ser vazio") String city
    ) {
        return ResponseEntity.ok(weatherService.getCurrentWeather(city));
    }

    @GetMapping("/{city}/forecast")
    public ResponseEntity<ForecastResponse> getForecast(
            @PathVariable @NotBlank(message = "o nome da cidade não pode ser vazio") String city
    ) {
        return ResponseEntity.ok(weatherService.getForecast(city));
    }
}
