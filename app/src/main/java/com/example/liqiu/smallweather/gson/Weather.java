package com.example.liqiu.smallweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {
    public String status;
    public Basic basic;
    public AQI aqi;
    public Now now;
    public Suggestiom suggestion;
    @SerializedName("daily_forecast")
    public List<Forcast> forcastList;
}
