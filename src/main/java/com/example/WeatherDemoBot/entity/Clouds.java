package com.example.WeatherDemoBot.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Clouds {
    private int all;

    @JsonProperty("all")
    public int getAll() {
        return all;
    }

    public void setAll(int all) {
        this.all = all;
    }
}
