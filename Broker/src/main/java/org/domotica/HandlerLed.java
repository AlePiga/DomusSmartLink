package org.domotica;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

class HandlerLed implements HttpHandler {
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try (
                InputStream is = exchange.getRequestBody();
                OutputStream os = exchange.getResponseBody()
        ) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonToArduino d = new Gson().fromJson(json, JsonToArduino.class);
            
            // Crea il JSON nel formato atteso da Arduino
            String mqttMessage = String.format("{\"led\": %d, \"stato\": %d}", d.led, d.stato);
            
            // Pubblica sul topic MQTT
            BrokerMQTTT.pubblica("progettoIOT/led", mqttMessage);
            
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            os.write(response.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }
}
