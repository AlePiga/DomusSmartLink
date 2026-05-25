![alt text](https://github.com/AlePiga/DomusSmartLink/blob/main/DomusSmartLink.jpg)

# Domus Smart Link

Domus Smart Link è un sistema completo di domotica IoT che integra:

- **Microcontrollore Arduino** per la gestione dell'hardware
- **Broker MQTT Java** per la comunicazione centralizzata
- **Applicazione Android** per il controllo remoto tramite UI

Il sistema consente di monitorare e controllare dispositivi smart home tramite protocollo MQTT, con visualizzazione real-time dei dati su un'app Android moderna.

---

## Architettura del Sistema

```
┌─────────────────┐         ┌──────────────────┐         ┌──────────────┐
│   Arduino Mega  │◄───────►│  Broker MQTT     │◄───────►│ App Android  │
│                 │  WiFi   │  (Java Server)   │  MQTT   │  (Kotlin)    │
│ - DHT11         │  MQTT   │                  │         │              │
│ - Sensori       │         │ - Database       │         │ - UI Modern  │
│ - LED           │         │ - HTTP REST API  │         │ - Control UI │
│ - Ventilatore   │         │ - MQTT Broker    │         │ - Sensors    │
└─────────────────┘         └──────────────────┘         └──────────────┘
```

---

## Componenti principali

### Sketch arduino

| Componente              | Tipo             | Pin Arduino                                            |
| ----------------------- | ---------------- | ------------------------------------------------------ |
| **LCD Display**         | 16x2             | RS:12, EN:11, D4:5, D5:4, D6:3, D7:2                   |
| **Ventilatore**         | Motor Controller | ENABLE:45, DIRA:43, DIRB:44                            |
| **LED 1**               | Digitale         | 8                                                      |
| **LED 2**               | Digitale         | 6                                                      |
| **LED 3**               | Digitale         | 31                                                     |
| **Sensore Temperatura** | DHT11            | A3                                                     |
| **Sensore Acqua**       | Analogico        | A0                                                     |
| **Display 7-Segmenti**  | Digitale         | 26,25,24,23 (digit), 21,22,17,15,14,20,18,16 (segment) |

#### Funzionalità principali:

- Lettura temperatura e umidità (DHT11)
- Monitoraggio livello acqua
- Controllo ventilatore (velocità e direzione)
- Controllo 3 LED
- Display LCD in tempo reale
- Orologio Internet (NTP)
- Comunicazione MQTT bidirezionale

#### Topic MQTT Arduino:

- **Ricezione**: `progettoIOT/led`, `progettoIOT/ventilatore`
- **Invio**: `progettoIOT/temperatura`, `progettoIOT/umidita`, `progettoIOT/acqua`, `progettoIOT/stato`

---

### **Broker MQTT Java**

Server centrale che gestisce la comunicazione tra Arduino e App Android.

#### Architettura:

```
Server.java (HttpServer porta 8080)
    ├─ HandlerStart.java → /sensori (GET sensori)
    ├─ HandlerLed.java → /led (POST controlla LED)
    ├─ HandlerVentilatore.java → /ventilatore (POST controlla ventilatore)
    ├─ BrokerMQTTT.java (connessione MQTT)
    ├─ Database.java (persistenza dati)
    └─ JsonArduino.java (serializzazione JSON)
```

#### Classi principali:

**`Server.java`**

- Avvia HttpServer sulla porta 8080
- Espone REST API per LED e ventilatore
- Gestisce le richieste dall'app Android

**`BrokerMQTTT.java`**

- Connessione al broker MQTT remoto (wireself.duckdns.org:1883)
- Sottoscrizione ai topic Arduino
- Ricezione messaggi dai sensori

**`Database.java`**

- Gestione persistenza dati sensori
- Memorizzazione stato dispositivi
- Query sensori in tempo reale

**`HandlerLed.java` / `HandlerVentilatore.java`**

- Elaborazione comandi da app Android
- Pubblicazione messaggi MQTT verso Arduino

---

### **Applicazione Android**

Interfaccia moderna in **Kotlin con Jetpack Compose**.

#### Funzionalità:

- Visualizzazione sensori in tempo reale
- Controllo LED on/off
- Controllo ventilatore (velocità 0-100%)
- Display temperatura, umidità, livello acqua
- Refresh automatico dati
- Interfaccia moderna con Material Design 3

#### Schermata Principale:

```
┌─────────────────────────────────┐
│ Domus Smart Link             🔄 |
├─────────────────────────────────┤
│                                 │
│  CONTROLLI                      │
│  ├─ Luce gialla    [Toggle]     │
│  ├─ Luce verde     [Toggle]     │
│  ├─ Luce rossa     [Toggle]     │
|  └─ Ventilatore    [Slider]     │
│                                 |
│  SENSORI                        │
│  ├─ Temperatura    25°C         │
│  ├─ Umidità        60%          │
│  └─ Livello Acqua  256          │
│                                 │
└─────────────────────────────────┘
```

---

## Protocollo MQTT

### Configurazione:

- **Broker**: `wireself.duckdns.org`
- **Porta**: 1883
- **Prefisso**: `progettoIOT`

### Topic e messaggi:

#### Temperatura

**Topic**: `progettoIOT/temperatura`

```json
{
	"temperatura": 25
}
```

#### Umidità

**Topic**: `progettoIOT/umidita`

```json
{
	"umidita": 60
}
```

#### Livello dell'acqua

**Topic**: `progettoIOT/acqua`

```json
{
	"livello_acqua": 456
}
```

#### LED (ricevuto da Arduino)

**Topic**: `progettoIOT/led`

```json
{
	"led": 8,
	"stato": 1
}
```

**Valori**: `led`: 8|6|31, `stato`: 0|1

#### Ventilatore (ricevuto da Arduino)

**Topic**: `progettoIOT/ventilatore`

```json
{
	"velocita": 128,
	"stato": "avanti"
}
```

**Valori** `stato`: "avanti"|"indietro"|"stop"
**Velocità**: 0-255

#### Stato completo controlli / sensori

**Topic**: `progettoIOT/stato`

```json
{
	"ventilatore": {
		"stato": "avanti",
		"velocita": 128
	},
	"leds": {
		"led1": 0,
		"led2": 1,
		"led3": 0
	},
	"temperatura": 25,
	"umidita": 60,
	"livello": 456
}
```

---

## Installazione e configurazione

### 1. Arduino

**Librerie Richieste:**

```cpp
#include <LiquidCrystal.h>      // LCD Display
#include <WiFi.h>               // Connessione WiFi
#include <PubSubClient.h>       // Client MQTT
#include <ArduinoJson.h>        // Serializzazione JSON
#include <DHT.h>                // Sensore temperatura
#include <SevSeg.h>             // Display 7-segmenti
#include <NTPClient.h>          // Sincronizzazione orario
```

**Installazione Librerie:**

1. IDE Arduino → Sketch → Includi librerie → Gestisci librerie
2. Cercare e installare le librerie sopra indicate

**Configurazione WiFi:**

```cpp
String WIFI_SSID = "<SSID_RETE>";
String WIFI_PASS = "<PASS_RETE>";
String MQTT_BROKER = "wireself.duckdns.org";
```

**Upload:**

1. Selezionare scheda: Arduino Mega 2560
2. Selezionare porta seriale corretta
3. Caricamento sketch

### 2. Broker Java

**Prerequisiti:**

- Java JDK 21+
- Maven 3.6+

**Build:**

```bash
cd Broker
mvn clean package
```

**Esecuzione:**

```bash
java -jar target/server.jar
```

**Output atteso:**

```
[SERVER] Server avviato sulla porta 8080!
[MQTT] Connessione al broker MQTT...
```

### 3. App Android

**Prerequisiti:**

- Android Studio 2022+
- SDK 33+ (Tiramisu)
- Kotlin 1.9+

**Build e Run:**

```bash
cd DomusSmartLink_App
./gradlew build
./gradlew installDebug
```

**Oppure da Android Studio:**

1. File → Open → DomusSmartLink_App
2. Run → Run 'app'

---

## Intervalli di Aggiornamento

| Componente                   | Intervallo |
| ---------------------------- | ---------- |
| Lettura DHT11                | 2000 ms    |
| Aggiornamento LCD            | 1000 ms    |
| Lettura sensore acqua        | 3000 ms    |
| Invio dati sensori           | 10000 ms   |
| Tentativo riconnessione MQTT | 5000 ms    |

---

## Struttura file progetto

```
DomusSmartLink/
├── README.md (questo file)
├── firmware.ino (firmware Arduino)
├── Broker/ (server MQTT Java)
│   ├── pom.xml
│   ├── src/main/java/org/domotica/
│   │   ├── Main.java
│   │   ├── Server.java
│   │   ├── BrokerMQTTT.java
│   │   ├── Database.java
│   │   ├── HandlerStart.java
│   │   ├── HandlerLed.java
│   │   ├── HandlerVentilatore.java
│   │   ├── JsonArduino.java
│   │   ├── JsonToArduino.java
│   │   ├── JsonVentilatore.java
│   │   ├── Sensore.java
│   │   └── Temperatura.java
│   └── target/ (build output)
└── DomusSmartLink_App/ (app Android)
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── app/
        ├── build.gradle.kts
        └── src/
            ├── main/
            │   ├── java/com/example/myapplication/
            │   │   └── MainActivity.kt
            │   └── res/
            └── test/
```

---

## Flusso di comunicazione

### Scenario: accensione LED da app Android

```
1. Utente tocca toggle LED su app
2. MainActivity.kt chiama sendLed(led_id, stato)
3. App invia richiesta HTTP POST a Broker (porta 8080)
4. HandlerLed.java riceve richiesta
5. Broker pubblica su topic MQTT: progettoIOT/led
6. Arduino riceve messaggio MQTT nel callback
7. Arduino accende/spegne LED e pubblica stato
8. App legge nuovo stato dal Broker tramite /sensori API
```

### Scenario: lettura sensore da Arduino

```
1. Arduino legge temperatura DHT11
2. Arduino pubblica su progettoIOT/temperatura
3. Broker MQTT riceve messaggio
4. Database.java salva nuovo valore
5. App Android chiama API /sensori (GET)
6. Server restituisce JSON con sensori
7. MainActivity.kt aggiorna UI
```

---

## Troubleshooting

### Arduino non si connette al WiFi

- ✅ Verificare SSID e password WiFi
- ✅ Controllare shield WiFi collegata
- ✅ Controllare monitor seriale per debug

### Broker non riceve messaggi MQTT

- ✅ Verificare connessione internet
- ✅ Verificare IP del broker: `wireself.duckdns.org`
- ✅ Controllare firewall porta 1883

### App Android non legge sensori

- ✅ Verificare Broker è in esecuzione (porta 8080)
- ✅ Verificare dispositivo Android è sulla stessa rete/internet
- ✅ Controllare in logcat errori di connessione

### Display LCD non mostra nulla

- ✅ Verificare contrasto LCD (potenziometro)
- ✅ Verificare pin collegati correttamente
- ✅ Testare pin con blink test

---

## Dipendenze

### Arduino

- LiquidCrystal
- WiFi
- ArduinoJson (v6.x)
- DHT (Adafruit)
- SevSeg
- NTPClient
- PubSubClient

### Broker Java

- Maven (build)
- JDK 15+ (runtime)
- PubSubClient (libreria MQTT Java)

### App Android

- Kotlin 1.9+
- Jetpack Compose
- Material Design 3
- Android SDK 33+

---

## Autori

**Marco Bedin e Alessandro Pigaiani**
