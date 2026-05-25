package org.domotica;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class BrokerMQTTT {

    private static final String BROKER    = "tcp://localhost:1883";
    private static final String MY_PREFIX = "progettoIOT";

    private static final String TOPIC_LED         = MY_PREFIX + "/led";
    private static final String TOPIC_VENTILATORE = MY_PREFIX + "/ventilatore";
    private static final String TOPIC_TEMPERATURA = MY_PREFIX + "/temperatura";
    private static final String TOPIC_UMIDITA     = MY_PREFIX + "/umidita";
    private static final String TOPIC_ACQUA       = MY_PREFIX + "/acqua";
    private static final String TOPIC_STATUS      = MY_PREFIX + "/stato";

    private static final String CLIENT_ID = "JavaCLI-" + System.currentTimeMillis();

    private static MqttClient client;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static volatile String ultimoSensori = "(nessun dato)";
    private static volatile String ultimaAcqua   = "(nessun dato)";
    private static volatile boolean connected    = false;

    public static void main(String[] args) {
        System.out.println("DOMOTICA CLI - Broker: " + BROKER + "  Prefisso: " + MY_PREFIX);
        connetti();
    }

    public static void connetti() {
        try {
            System.out.println("[MQTT] Connessione al broker " + BROKER + " ...");
            client = new MqttClient(BROKER, CLIENT_ID, new MemoryPersistence());

            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            opts.setConnectionTimeout(10);
            opts.setKeepAliveInterval(30);
            opts.setAutomaticReconnect(true);

            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    connected = false;
                    System.out.println("[MQTT] Connessione persa: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    String ora = java.time.LocalTime.now()
                            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

                    if (topic.equals(TOPIC_STATUS)) {
                        System.out.println("[" + ora + "] [STATO] " + formatJson(payload));

                    } else if (topic.equals(TOPIC_ACQUA)) {
                        ultimaAcqua = payload;
                        System.out.println("[" + ora + "] [ACQUA] " + formatJson(payload));
                        try {
                            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
                            Database.update(53, obj.get("livello_acqua").getAsInt());
                        } catch (Exception e) {
                            System.err.println("[ERR] Parsing acqua: " + e.getMessage());
                        }

                    } else if (topic.equals(TOPIC_UMIDITA)) {
                        ultimoSensori = payload;
                        System.out.println("[" + ora + "] [UMIDITA] " + formatJson(payload));
                        try {
                            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
                            Database.update(10, obj.get("umidita").getAsInt());
                        } catch (Exception e) {
                            System.err.println("[ERR] Parsing umidita: " + e.getMessage());
                        }

                    } else if (topic.equals(TOPIC_TEMPERATURA)) {
                        ultimoSensori = payload;
                        System.out.println("[" + ora + "] [TEMPERATURA] " + formatJson(payload));
                        try {
                            JsonObject obj = JsonParser.parseString(payload).getAsJsonObject();
                            Database.update(13, obj.get("temperatura").getAsInt());
                        } catch (Exception e) {
                            System.err.println("[ERR] Parsing temperatura: " + e.getMessage());
                        }

                    } else {
                        System.out.println("[" + ora + "] [IN] " + topic + " -> " + payload);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            client.connect(opts);
            connected = true;

            client.subscribe(TOPIC_STATUS, 1);
            client.subscribe(TOPIC_ACQUA, 1);
            client.subscribe(TOPIC_UMIDITA, 1);
            client.subscribe(TOPIC_TEMPERATURA, 1);

            Database.save("Umidità",      0, 10, "HUMIDITY");
            Database.save("Temperatura",  0, 13, "TEMPERATURE");
            Database.save("Livello dell'acqua",0, 53, "WATER_LEVEL");

            System.out.println("[MQTT] Connesso. ClientID: " + CLIENT_ID);
            System.out.println("[MQTT] Topic in ascolto: " + TOPIC_STATUS
                    + ", " + TOPIC_ACQUA + ", " + TOPIC_UMIDITA + ", " + TOPIC_TEMPERATURA);

        } catch (MqttException e) {
            System.err.println("[ERR] Connessione fallita: " + e.getMessage());
            System.err.println("      Broker: " + BROKER + " | Porta: 1883");
            System.exit(1);
        }
    }

    public static void pubblica(String topic, String payload) {
        if (client == null || !client.isConnected()) {
            System.err.println("[ERR] Non connesso al broker. Tentativo di riconnessione...");
            try {
                connetti();
            } catch (Exception e) {
                System.err.println("[ERR] Riconnessione fallita: " + e.getMessage());
                return;
            }
        }
        try {
            MqttMessage msg = new MqttMessage(payload.getBytes());
            msg.setQos(1);
            msg.setRetained(false);
            client.publish(topic, msg);
            System.out.println("[OUT] " + topic + " -> " + payload);
        } catch (MqttException e) {
            System.err.println("[ERR] Publish fallito: " + e.getMessage());
        }
    }

    private static String formatJson(String jsonString) {
        if (jsonString == null || jsonString.equals("(nessun dato)")) return jsonString;
        try {
            JsonObject json = JsonParser.parseString(jsonString).getAsJsonObject();
            return gson.toJson(json);
        } catch (Exception e) {
            return jsonString;
        }
    }
}