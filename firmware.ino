#include <LiquidCrystal.h>
#include <SPI.h>
#include <WiFi.h>
#include <WiFiUdp.h>
#include <ArduinoJson.h>
#include <DHT.h>
#include <SevSeg.h>
#include <NTPClient.h>
#include <PubSubClient.h>

// Pin per lo schermo LCD
const int rs = 12, en = 11, d4 = 5, d5 = 4, d6 = 3, d7 = 2;
LiquidCrystal lcd(rs, en, d4, d5, d6, d7);

// Pin per il ventilatore
#define ENABLE 45
#define DIRA   43
#define DIRB   44

// Pin per i LED
#define LED1 8
#define LED2 6
#define LED3 31

// Pin per il sensore DHT11
#define DHTPIN  A3
#define DHTTYPE DHT11
DHT dht(DHTPIN, DHTTYPE);

// Pin per il sensore del livello dell'acqua
#define WATER_SENSOR_PIN A0

// Display a 7 segmenti
SevSeg sevseg;

// Credenziali Wi-Fi e MQTT
String WIFI_SSID = "iPhone di Alessandro";
String WIFI_PASS = "weirdfishes2";
String MQTT_BROKER = "";
const int   MQTT_PORT = 1883;

// Prefisso e topic MQTT
const char* MY_PREFIX = "progettoIOT";
char TOPIC_LED[60];
char TOPIC_VENTILATORE[60];
char TOPIC_TEMPERATURA[60];
char TOPIC_UMIDITA[60];
char TOPIC_ACQUA[60];
char TOPIC_STATUS[60];

// Client Wi-Fi e MQTT
WiFiClient   wifiClient;
PubSubClient mqttClient(wifiClient);

// Orario internet
WiFiUDP   ntpUDP;
NTPClient timeClient(ntpUDP, "europe.pool.ntp.org", 7200, 60000); // ora legale

// Stato del motore
int  motorSpeed     = 0;
bool direzioneMotore = true;   // true = avanti, false = indietro
bool motoreInMovimento   = false;

// Gestione dei delay e dei sensori
unsigned long lastSensorRead    = 0;
unsigned long lastLcdUpdate     = 0;
unsigned long lastDataSend      = 0;
unsigned long lastMqttReconnect = 0;
unsigned long lastWaterRead     = 0;

const long SENSOR_INTERVAL       = 2000;
const long LCD_INTERVAL          = 1000;
const long SEND_INTERVAL         = 10000;
const long MQTT_RECONNECT_INTERVAL = 5000;
const long WATER_INTERVAL        = 3000;

int temperatura  = 0;
int umidita = 0;
int livelloAcqua = 0;

// Funzione di supporto per leggere una stringa intera dal Monitor Seriale
String leggiSeriale() {
  while (!Serial.available()) {
    // Aspetta che l'utente scriva qualcosa
    delay(50); 
  }
  String input = Serial.readStringUntil('\n');
  input.trim(); // Rimuove spazi bianchi o caratteri di invio (\r, \n) extra
  return input;
}

void setup() {
  Serial.begin(9600);

  // LCD
  lcd.begin(16, 2);
  // lcd.print("Esegui config su");
  // lcd.setCursor(0, 1);
  // lcd.print("monitor seriale");
  // Aspetta che la porta seriale sia pronta (utile su alcune schede come Arduino Leonardo/Mega/ESP32)
  while (!Serial) { ; } 

  snprintf(TOPIC_LED,         sizeof(TOPIC_LED),         "%s/led",         MY_PREFIX);
  snprintf(TOPIC_VENTILATORE, sizeof(TOPIC_VENTILATORE), "%s/ventilatore",  MY_PREFIX);
  snprintf(TOPIC_TEMPERATURA, sizeof(TOPIC_TEMPERATURA), "%s/temperatura",  MY_PREFIX);
  snprintf(TOPIC_UMIDITA,     sizeof(TOPIC_UMIDITA),     "%s/umidita",      MY_PREFIX);
  snprintf(TOPIC_ACQUA,       sizeof(TOPIC_ACQUA),       "%s/acqua",        MY_PREFIX);
  snprintf(TOPIC_STATUS,      sizeof(TOPIC_STATUS),      "%s/stato",        MY_PREFIX);

  // Richiesta input credenziali da Monitor Seriale
  // Serial.println("\n--- CONFIGURAZIONE INIZIALE ---");
  
  Serial.print("SSID Wi-Fi: ");
  // WIFI_SSID = leggiSeriale();
  Serial.println(WIFI_SSID); // Mostra di conferma il testo inserito
  
  Serial.print("Password: ");
  // WIFI_PASS = leggiSeriale();
  Serial.println(WIFI_PASS); // Nasconde la password a schermo per sicurezza
  
  Serial.print("Broker MQTT: ");
  MQTT_BROKER = "wireself.duckdns.org";
  Serial.println(MQTT_BROKER);
  // Serial.println("-------------------------------\n");

  // Ventilatore
  pinMode(ENABLE, OUTPUT);
  pinMode(DIRA,   OUTPUT);
  pinMode(DIRB,   OUTPUT);
  digitalWrite(ENABLE, LOW);
  digitalWrite(DIRA,   LOW);
  digitalWrite(DIRB,   LOW);

  // LED
  pinMode(LED1, OUTPUT);  digitalWrite(LED1, LOW);
  pinMode(LED2, OUTPUT);  digitalWrite(LED2, LOW);
  pinMode(LED3, OUTPUT);  digitalWrite(LED3, LOW);

  // Sensore temperatura
  dht.begin();

  // Sensore acqua
  pinMode(WATER_SENSOR_PIN, INPUT);

  // Display 7 segmenti
  byte numDigits = 4;
  byte digitPins[] = {26, 25, 24, 23};
  byte segmentPins[] = {21, 22, 17, 15, 14, 20, 18, 16};
  sevseg.begin(COMMON_CATHODE, numDigits, digitPins, segmentPins, false, false, false, false);
  sevseg.setBrightness(90);

  // Wi-Fi
  if (WiFi.status() == WL_NO_SHIELD) {
    Serial.println("ERRORE: Wi-Fi Shield non rilevata!");
    lcd.clear();
    lcd.print("ERRORE Wi-Fi");
    lcd.setCursor(0, 1);
    lcd.print("Shield non trovata");
    while (true) {
      sevseg.setNumber(8888, 0);
      sevseg.refreshDisplay();
    }
  }

  lcd.clear();
  lcd.print("Connessione WiFi");
  lcd.setCursor(0, 1);
  lcd.print("in corso...");

  // Avvio connessione usando le stringhe ricevute in input (.c_str() serve a convertirle nel formato accettato dalla libreria)
  WiFi.begin(WIFI_SSID.c_str(), WIFI_PASS.c_str());

  int timeout = 0;
  while (WiFi.status() != WL_CONNECTED && timeout < 30) {
    delay(500);
    Serial.print(".");
    timeout++;
    sevseg.setNumber(timeout, 0);
    sevseg.refreshDisplay();
  }

  if (WiFi.status() == WL_CONNECTED) {
    IPAddress ip = WiFi.localIP();
    Serial.println("Connesso al Wi-Fi!");
    Serial.print("IP: ");  Serial.println(ip);

    byte mac[6];
    WiFi.macAddress(mac);
    Serial.print("MAC: "); stampaMAC(mac);

    lcd.clear();
    lcd.print("Wi-Fi connesso!  ");
    lcd.setCursor(0, 1);
    lcd.print(ip);

    timeClient.begin();
    timeClient.update();
    Serial.println("Orologio internet (NTP) sincronizzato.");
    delay(2000);
  } else {
    Serial.println("\nConnessione al Wi-Fi fallita.");
    lcd.clear();
    lcd.print("ERRORE Wi-Fi");
    lcd.setCursor(0, 1);
    lcd.print("Conn. fallita");
  }

  // Configurazione MQTT utilizzando l'IP ricevuto in input
  mqttClient.setServer(MQTT_BROKER.c_str(), MQTT_PORT);
  mqttClient.setCallback(mqttCallback);
  mqttClient.setKeepAlive(60);
  mqttClient.setBufferSize(512); // Buffer per JSON

  riconnettiMQTT();

  lcd.clear();
  aggiornaLCD();
}

void loop() {
  if (!mqttClient.connected()) {
    riconnettiMQTT();
  }
  mqttClient.loop();

  timeClient.update();
  aggiornaSevenSegDisplay();

  unsigned long now = millis();

  if (now - lastSensorRead >= SENSOR_INTERVAL) {
    leggiDHT11();
    lastSensorRead = now;
  }

  if (now - lastWaterRead >= WATER_INTERVAL) {
    leggiLivelloAcqua();
    lastWaterRead = now;
  }

  if (now - lastLcdUpdate >= LCD_INTERVAL) {
    aggiornaLCD();
    lastLcdUpdate = now;
  }

  if (now - lastDataSend >= SEND_INTERVAL) {
    inviaTemperatura();
    inviaUmidita();
    inviaLivelloAcqua();
    lastDataSend = now;
  }
}

// Riconnessione MQTT
void riconnettiMQTT() {
  unsigned long now = millis();
  if (now - lastMqttReconnect < MQTT_RECONNECT_INTERVAL) return;
  lastMqttReconnect = now;

  Serial.print("\nConnessione al broker MQTT...");

  // Client ID univoco basato sul MAC
  byte mac[6];
  WiFi.macAddress(mac);
  char clientId[30];
  snprintf(clientId, sizeof(clientId), "Arduino-%02X%02X", mac[5], mac[4]);

  if (mqttClient.connect(clientId)) {
    Serial.println(" CONNESSO!");
    Serial.print("Client ID: "); Serial.println(clientId);

    // Iscrizione ai topic
    mqttClient.subscribe(TOPIC_LED);
    mqttClient.subscribe(TOPIC_VENTILATORE);
    Serial.print("Sottoscritto a: "); Serial.println(TOPIC_LED);
    Serial.print("Sottoscritto a: "); Serial.println(TOPIC_VENTILATORE);

    pubblicaStato();

    lcd.clear();
    lcd.setCursor(0, 0);
    lcd.print("Connesso a MQTT");
    lcd.setCursor(0, 1);
    lcd.print(MY_PREFIX);
    delay(2000);
    aggiornaLCD();
  } else {
    int rc = mqttClient.state();
    Serial.print(" FALLITO! rc=");
    Serial.println(rc);
    /*
      Codici di errore PubSubClient:
      -4 = MQTT_CONNECTION_TIMEOUT
      -3 = MQTT_CONNECTION_LOST
      -2 = MQTT_CONNECT_FAILED
      -1 = MQTT_DISCONNECTED
       1 = MQTT_CONNECT_BAD_PROTOCOL
       2 = MQTT_CONNECT_BAD_CLIENT_ID
       3 = MQTT_CONNECT_UNAVAILABLE
       4 = MQTT_CONNECT_BAD_CREDENTIALS
       5 = MQTT_CONNECT_UNAUTHORIZED
    */
    lcd.clear();
    lcd.print("ERRORE MQTT");
    lcd.setCursor(0, 1);
    lcd.print("rc="); lcd.print(rc);
    delay(2000);
    aggiornaLCD();
  }
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  Serial.print("\n[MQTT IN] topic: ");
  Serial.println(topic);

  // Converti payload in stringa
  char message[length + 1];
  memcpy(message, payload, length);
  message[length] = '\0';
  Serial.print("payload: ");
  Serial.println(message);

  StaticJsonDocument<300> doc;
  DeserializationError error = deserializeJson(doc, message);

  if (error) {
    Serial.print("JSON error: ");
    Serial.println(error.c_str());
    return;
  }

  // Handler LED
  if (strcmp(topic, TOPIC_LED) == 0) {
    if (doc.containsKey("led") && doc.containsKey("stato")) {
      int ledNum = doc["led"];
      int stato  = doc["stato"];
      int pin = -1;
      
      if      (ledNum == 8)  pin = LED1;
      else if (ledNum == 6)  pin = LED2;
      else if (ledNum == 31) pin = LED3;

      if (pin != -1) {
        digitalWrite(pin, stato ? HIGH : LOW);
        Serial.print("LED pin "); Serial.print(ledNum);
        Serial.println(stato ? " ACCESO" : " SPENTO");
      } else {
        Serial.println("Errore: Il numero ricevuto non corrisponde a nessun LED mappato!");
      }
      pubblicaStato();
    }
  }

  // Handler ventilatore
  if (strcmp(topic, TOPIC_VENTILATORE) == 0) {
    if (doc.containsKey("velocita")) {
      int newSpeed = doc["velocita"];
      if (newSpeed >= 0 && newSpeed <= 255) {
        motorSpeed = newSpeed;
        Serial.print("Velocita impostata: "); Serial.println(motorSpeed);
        if (motoreInMovimento) impostaVelocitaMotore(motorSpeed);
      }
    }

    if (doc.containsKey("stato")) {
      String cmd = doc["stato"].as<String>();
      if (cmd == "avanti") {
        direzioneMotore = true;
        motoreInMovimento   = true;
        impostaDirezioneMotore(true);
        impostaVelocitaMotore(motorSpeed > 0 ? motorSpeed : 128);  // default 50% se non impostata
        Serial.println("Ventilatore AVANTI");
      } else if (cmd == "indietro") {
        direzioneMotore = false;
        motoreInMovimento   = true;
        impostaDirezioneMotore(false);
        impostaVelocitaMotore(motorSpeed > 0 ? motorSpeed : 128);
        Serial.println("Ventilatore INDIETRO");
      } else if (cmd == "stop") {
        motoreInMovimento = false;
        fermaMotore();
        Serial.println("Ventilatore STOP");
      } else {
        Serial.print("Comando sconosciuto: "); Serial.println(cmd);
      }
    }
    pubblicaStato();
  }
}

// Invio della temperatura
void inviaTemperatura() {
  if (!mqttClient.connected()) return;

  StaticJsonDocument<200> doc;
  doc["temperatura"] = temperatura;

  char payload[200];
  serializeJson(doc, payload);

  if (mqttClient.publish(TOPIC_TEMPERATURA, payload)) {
    Serial.print("[MQTT OUT] temperatura: "); Serial.println(payload);
  } else {
    Serial.println("Errore invio temperatura");
  }
}

// Invio dell'umidità
void inviaUmidita() {
  if (!mqttClient.connected()) return;

  StaticJsonDocument<200> doc;
  doc["umidita"]     = umidita;

  char payload[200];
  serializeJson(doc, payload);

  if (mqttClient.publish(TOPIC_UMIDITA, payload)) {
    Serial.print("[MQTT OUT] umidita: "); Serial.println(payload);
  } else {
    Serial.println("Errore invio umidita");
  }
}

// Invio del livello dell'acqua
void inviaLivelloAcqua() {
  if (!mqttClient.connected()) return;

  StaticJsonDocument<100> doc;
  doc["livello_acqua"] = livelloAcqua;

  char payload[100];
  serializeJson(doc, payload);

  if (mqttClient.publish(TOPIC_ACQUA, payload)) {
    Serial.print("[MQTT OUT] acqua: "); Serial.println(payload);
  } else {
    Serial.println("Errore invio livello acqua");
  }
}

// Pubblica uno stato completo del sistema
void pubblicaStato() {
  if (!mqttClient.connected()) return;

  StaticJsonDocument<400> doc;

  // Ventilatore
  JsonObject ventola = doc.createNestedObject("ventilatore");
  ventola["stato"]    = motoreInMovimento ? (direzioneMotore ? "avanti" : "indietro") : "stop";
  ventola["velocita"] = motorSpeed;

  // LED
  JsonObject leds = doc.createNestedObject("leds");
  leds["led1"] = digitalRead(LED1);
  leds["led2"] = digitalRead(LED2);
  leds["led3"] = digitalRead(LED3);

  // Sensori
  doc["temperatura"] = temperatura;
  doc["umidita"]     = umidita;
  doc["livello"]     = livelloAcqua;

  char payload[400];
  serializeJson(doc, payload);

  mqttClient.publish(TOPIC_STATUS, payload);
  Serial.print("[MQTT OUT] stato: "); Serial.println(payload);
}

void leggiDHT11() {
  float h = dht.readHumidity();
  float t = dht.readTemperature();
  if (isnan(h) || isnan(t)) {
    Serial.println("Errore lettura DHT11!");
    return;
  }
  umidita    = (int)h;
  temperatura = (int)t;
  Serial.print("Temp: "); Serial.print(temperatura);
  Serial.print("C, Umidita: "); Serial.println(umidita);
}

void leggiLivelloAcqua() {
  livelloAcqua = analogRead(WATER_SENSOR_PIN);
  Serial.print("Livello acqua: "); Serial.println(livelloAcqua);
}

void aggiornaSevenSegDisplay() {
  int ore     = timeClient.getHours();
  int minuti   = timeClient.getMinutes();
  int valore = ore * 100 + minuti;
  sevseg.setNumber(valore, 2);
  sevseg.refreshDisplay();
}

void aggiornaLCD() {
  lcd.clear();

  // Riga 0: stato ventilatore
  lcd.setCursor(0, 0);
  if (motoreInMovimento) {
    lcd.print("Ventilatore ");
    lcd.print(direzioneMotore ? ">" : "<");
    
    // Aggiunge gli zeri di formattazione (Padding)
    if (motorSpeed < 10) {
      lcd.print("00"); // Se è a una sola cifra, aggiunge due zeri
    } else if (motorSpeed < 100) {
      lcd.print("0");  // Se è a due cifre, aggiunge un solo zero
    }
    
    lcd.print(motorSpeed);
  } else {
    lcd.print("Ventilatore STOP");
  }

  // Riga 1: LED, temperatura e stato MQTT
  lcd.setCursor(0, 1);
  lcd.print("L:");
  lcd.print(digitalRead(LED3) ? "1" : "0");
  lcd.print(digitalRead(LED2) ? "1" : "0");
  lcd.print(digitalRead(LED1) ? "1" : "0");
  lcd.print(" T:");
  lcd.print(temperatura);
  lcd.print("C "); 
  lcd.print(umidita);
  lcd.print("%");

  lcd.setCursor(12, 1);
  lcd.print(mqttClient.connected() ? "MQTT" : "----");
}

void impostaDirezioneMotore(bool avanti) {
  digitalWrite(DIRA, avanti ? HIGH : LOW);
  digitalWrite(DIRB, avanti ? LOW  : HIGH);
}

void impostaVelocitaMotore(int velocita) {
  analogWrite(ENABLE, constrain(velocita, 0, 255));
}

void fermaMotore() {
  analogWrite(ENABLE, 0);
  digitalWrite(DIRA, LOW);
  digitalWrite(DIRB, LOW);
}

void stampaMAC(byte mac[]) {
  for (int i = 5; i >= 0; i--) {
    if (mac[i] < 16) Serial.print("0");
    Serial.print(mac[i], HEX);
    if (i > 0) Serial.print(":");
  }
  Serial.println();
}
