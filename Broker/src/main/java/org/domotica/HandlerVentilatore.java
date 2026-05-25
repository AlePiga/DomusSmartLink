package org.domotica;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

class HandlerVentilatore implements HttpHandler {
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

            // Parsing del JSON ricevuto dal client
            JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
            
            String stato = jsonObject.get("stato").getAsString();
            int velocita = jsonObject.has("velocita") ? jsonObject.get("velocita").getAsInt() : 128;
            
            // Crea il JSON nel formato atteso da Arduino
            String mqttMessage = String.format("{\"stato\": \"%s\", \"velocita\": %d}", stato, velocita);
            
            // Pubblica sul topic MQTT
            BrokerMQTTT.pubblica("progettoIOT/ventilatore", mqttMessage);
            
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            os.write(response.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(500, -1);
        }
    }
}
