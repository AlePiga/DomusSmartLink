package org.domotica;

public class JsonArduino {

    public static void sendLed(int id,int stato) {
        String jsonPayload = "{\"led\": \"" + id + "\", \"stato\": "+ stato +"}";
        BrokerMQTTT.pubblica("progettoIOT/led", jsonPayload);
    }

    public static void sendVentilatore(int velocitaValue) {
        String jsonPayload = String.format("""
            {
                "ventola": "avanti",
                "velocita": %d
            }
            """, velocitaValue);
        BrokerMQTTT.pubblica("progettoIOT/ventilatore", jsonPayload);
    }
}
