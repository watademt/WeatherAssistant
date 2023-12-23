package com.example.Weather.Assistant.model;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

@Slf4j
public class Weather {
    private String weatherText;
    private String APIkey = "c8a1d7c2a45230f4ac2186453868d259";
    /**
     * получение url
     * @param urlAddress
     */
    public String getUrlContent(String urlAddress) {
        StringBuffer content = new StringBuffer();
        try {
            URL url = new URL(urlAddress);
            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                content.append(line + "\n");
            }
            bufferedReader.close();
        } catch (Exception e) {
            log.info("Город не найден!");
        }

        return content.toString();
    }

    /**
     * получаение данных с OpenWeather
     * @param city нужный город
     */
    public String getWeather(String city) {
        String output = getUrlContent(
                "https://api.openweathermap.org/data/2.5/weather?q="
                        + city
                        + "&appid=" + APIkey + "&units=metric"
        );

        if (!output.isEmpty()) {
            JSONObject object = new JSONObject(output);
            weatherText = "Погода в городе " + city + ":"
                    + "\n\nТемпература: " + object.getJSONObject("main").getDouble("temp")
                    + "\nОщущается: " + object.getJSONObject("main").getDouble("feels_like")
                    + "\nВлажность: " + object.getJSONObject("main").getDouble("humidity")
                    + "\nДавление: " + object.getJSONObject("main").getDouble("pressure");
        }
        return weatherText;
    }
}
