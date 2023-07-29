package com.example.WeatherDemoBot.service;

import com.example.WeatherDemoBot.config.BotConfig;
import com.example.WeatherDemoBot.entity.WeatherParams;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final String OPEN_WEATHER_MAP_API_KEY = /* your weather api key */;

    final BotConfig config;

    public TelegramBot(BotConfig config) {
        this.config = config;
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;

                case "/weather":
                    try {
                        getTodayWeatherData(chatId);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    break;

                default:
                    sendMessage(chatId, "Sorry, command was not recognized");
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = "Hi, " + name + ", nice to meet you!";
        log.info("Replied to user " + name);

        sendMessage(chatId, answer);
    }

    private void getTodayWeatherData(long chatId) throws Exception {

        String city = "Minsk";
        String apiUrl = "http://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + OPEN_WEATHER_MAP_API_KEY;
        URL url = new URL(apiUrl);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        String temperature = convertFromKelvinToCelsius(getTempFromJSON(builder.toString())) + " \uD83C\uDF21";
        String feelsLikeTemperature = convertFromKelvinToCelsius(getFeelsLikeTempFromJSON(builder.toString())) + " \uD83C\uDF21";
        String description = convertDescription(getDescriptionFromJSON(builder.toString()));
        String windSpeed = convertWindSpeed(getWindSpeedFromJSON(builder.toString())) + " km/h" + " \uD83D\uDCA8";

        String response =
                        "Weather for today" + "\n\n" +

                        "Temperature - " + temperature + "\n" +
                        "Feel's like - " + feelsLikeTemperature + "\n" +
                        "Description - " + description + "\n" +
                        "Wind - " + windSpeed;

        reader.close();
        connection.disconnect();

        sendMessage(chatId, response);
    }

    private String getDescriptionFromJSON(String jsonString) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        WeatherParams weatherParams = mapper.readValue(jsonString, WeatherParams.class);

        return weatherParams.getWeather()[0].getMain();
    }

    private Double getFeelsLikeTempFromJSON(String jsonString) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        WeatherParams weatherParams = mapper.readValue(jsonString, WeatherParams.class);

        return weatherParams.getMain().getFeelsLike();
    }

    private Double getTempFromJSON(String jsonString) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        WeatherParams weatherParams = mapper.readValue(jsonString, WeatherParams.class);

        return weatherParams.getMain().getTemp();
    }

    private Double getWindSpeedFromJSON(String jsonString) throws  JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        WeatherParams weatherParams = mapper.readValue(jsonString, WeatherParams.class);

        return weatherParams.getWind().getSpeed();
    }

    private Integer convertFromKelvinToCelsius(Double kelvinValueFromJSON) {

        return (int) Math.round(kelvinValueFromJSON - 273.15);
    }

    private String convertDescription(String descriptionStringFromJSON) {

        String returnDescription;

        switch (descriptionStringFromJSON) {
            case "Clouds":
                returnDescription = "cloudy \u2601";
                break;

            case "Rain":
                returnDescription = "rainy \uD83C\uDF27";
                break;

            default:
                returnDescription = descriptionStringFromJSON;
        }

        return returnDescription;
    }

    private Integer convertWindSpeed(Double windSpeedFromJson) {

        return (int) Math.round(windSpeedFromJson * 3.6);
    }

    private void sendMessage(long chatId, String textToSend) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }
}