package org.simpmc.simppay.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.util.stream.Collectors;

public class HttpUtils {
    // TODO: Recode this for Card to be like banking, utlize Gson
    public static JsonObject getJsonResponse(String url) {
        try {
            CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));
            HttpURLConnection connection = (HttpURLConnection) (new URL(url)).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.setDoInput(true);
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(7000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response = reader.lines().collect(Collectors.joining());
            reader.close();
            connection.disconnect();
            return (JsonObject) (new JsonParser()).parse(response);
        } catch (Exception exception) {
            return null;
        }
    }
}
