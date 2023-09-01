/* 
Code zur Programmierung eines hardwarebasierten Timers auf den ESP32.
Basierend auf dem Beispielcode "RepeatTimer.ino" aus Arduino.

Von Maximilian Lippmann und Juliana Kühn (Beide MI)
 */

#include <Wire.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <WiFi.h>
#include <PubSubClient.h>

//Definition WiFi Daten(Name und Passwort)
const char* ssid     = "ki-lokal";
const char* password = "dc-ki-2022+";

//Definition MQTT Broker (IP-Adresse und Port)

//OTH
const char* mqtt_broker = "192.168.1.4";
int mqttPort = 1883;

Adafruit_MPU6050 mpu;
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

portMUX_TYPE timerMux = portMUX_INITIALIZER_UNLOCKED;

//Variablen zum Speichern der Zeit
volatile uint32_t gameTimer = 0;
volatile uint32_t lastIsrAt = 0;
volatile uint32_t gameCount = 0;
volatile uint32_t saveGameTime = 0;

//Variablen zum Steuern von Funktionen
volatile bool startTimer = false;
volatile bool timerActive = false;
volatile bool ledFinish = false;

//Variable für die eingebaute LED-Pin, die aufleuchten soll,
//wenn das Ziel erreicht wurde
const int ledPin = 22;

hw_timer_t * timer = NULL;
volatile SemaphoreHandle_t timerSemaphore;

//Funktion onTimer, in der die Zeitvariable immer um 1 erhöht wird
void ARDUINO_ISR_ATTR onTimer(){

    portENTER_CRITICAL_ISR(&timerMux);
    gameTimer++;
    lastIsrAt = millis() / 1000;
    portEXIT_CRITICAL_ISR(&timerMux);

    xSemaphoreGiveFromISR(timerSemaphore, NULL);

}

//Funktion, um sich mit dem WLAN zu verbinden
void connectToWiFi() {
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }

  //erfolgreich mit WLAN verbunden
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

//Funktion, um Verbindung mit MQTT-Broker herzustellen
void setupMQTT() {

  mqttClient.setServer(mqtt_broker, 1883);

  while (!mqttClient.connected()) {
    Serial.println("Connecting to MQTT...");
    if (mqttClient.connect("ESP32Client")) {
      Serial.println("connected");

      //Nachrichten werden hier subscribed
      mqttClient.subscribe("start/M01");
      mqttClient.subscribe("finished/M01");

    } else {
      Serial.print("failed with state ");
      Serial.print(mqttClient.state());
      delay(2000);
    }
  }
}

//Funktion, um die Nachrichten, die subscribed wurden, zu verarbeiten
void callback(char* topic, byte* message, unsigned int length) {
  
  Serial.print("Message arrived in topic: ");
  Serial.print(topic);
  Serial.print(". Message: ");
  String messageTemp; //String um die empfangene Nachricht dort zu speichern

  //Inhalt der empfangenen Nachricht wird durchlaufen und die Zeichen in
  //chars umgewandelt, da diese als Byte empfangen werden,
  //diese werden anschließend in den String nacheinander einfügt
  for (int i = 0; i < length; i++) {
    Serial.print((char)message[i]);
    messageTemp += (char)message[i];
  }
  Serial.println();

  //Bei Empfang der Nachricht START, soll der Timer aktiviert werden
  if (String(topic) == "start/M01") {
    if(strcmp(messageTemp.c_str(), "START") == 0) {
      if(!startTimer)
      {
        startTimer = true;
        startTimerGo();
      }
      //Falls Nachricht bereits aktiviert wurde, soll der Timer zurückgesetzt
      //und danach wieder gestartet werden
      else
      {
        xSemaphoreGiveFromISR(timerSemaphore, NULL);
        resetTimer();
        startTimerGo();
      }
      
    }
  }
  //Bei Empfang des topics finished, soll der Timer gestoppt werden
  else if (String(topic) == "finished/M01") {
    ledFinish = true;
    stopTimer();
  }
}

//Funktion um den Timer und dazugehörige Variablen zurückzusetzen
void resetTimer() {

  if (timer) { 
    timerDetachInterrupt(timer);
    timerAlarmDisable(timer); 
    timerEnd(timer); 
    timer = NULL; //Null um kennzuzeichnen, dass kein Timer mehr aktiv ist
  }

  timerActive = false;
  gameTimer = 0;
  lastIsrAt = 0;
}

//Funktion, um den Timer zu stoppen (wenn das Ziel im Labyrinth erreicht wurde)
void stopTimer() {

  if (timer) {
    timerDetachInterrupt(timer);
    timerAlarmDisable(timer);
    timerEnd(timer);

    //Abgelaufene Zeit wird in der Variable festgehalten, um diese
    //zu publishen für die Bestenliste
    saveGameTime = gameCount; 
    Serial.println("Gametime:");
    Serial.println(saveGameTime);

    //Wird in einen String zum publishen gespeichert
    String saveTime = String(saveGameTime);
    mqttClient.publish("endTime/M01", saveTime.c_str());
  }
}

//Funktion, um den Timer zu starten
void startTimerGo() {

  if (!timerActive) {
    //Timer wird initialisiert
    timer = timerBegin(0, 80, true);
    timerAttachInterrupt(timer, &onTimer, true);
    timerAlarmWrite(timer, 1000000, true);
    timerAlarmEnable(timer);
    
    timerActive = true;
  }
}

//In der Setup Funktion werden die Verbindungen zum WLAN und zu den
//Sensoren hergestellt, MQTT-Broker und Port werden mithilfe der Variablen
//eingestellt
void setup() {
  Serial.begin(115200);
  connectToWiFi();
  Wire.begin();

  mqttClient.setServer(mqtt_broker, mqttPort);
  mqttClient.setCallback(callback);

  mpu.begin();
  delay(1000);

  pinMode(ledPin, OUTPUT);

  //MPU6050 Sensor verbinden
  while (!mpu.begin()) {
    Serial.println("Could not find a valid MPU6050 sensor, check wiring!");
    delay(500);
  }

  //Semaphore wird erstellt, zum informieren, wenn der Timer startet
  timerSemaphore = xSemaphoreCreateBinary();
}

void loop() {

  if (!mqttClient.connected()) {
    setupMQTT();
  }

  mqttClient.loop();
  digitalWrite(ledPin, HIGH);

  if (ledFinish)
      {
        digitalWrite(ledPin, LOW); //LED bei 22 ist aus -> mit LOW geht es an
        delay(500);
        ledFinish = false;
      }

  //Sensoren als Variablen definieren für die Accelerometer, Gyro und
  //Temperatur Werte
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

  //Prüfung ob Timer aktiviert wurde
  if (startTimer) {
      if (xSemaphoreTake(timerSemaphore, 0) == pdTRUE){

        portENTER_CRITICAL(&timerMux);
        gameCount = gameTimer;
        portEXIT_CRITICAL(&timerMux);

      //Zeit vom Timer und die Temperatur werden in einem
      //String gespeichert
      String tempTime = "Spielzeit: ";
      tempTime += String(gameCount);
      tempTime += " Sek ";
      tempTime += "Temperatur: ";
      tempTime += String(temp.temperature);
      tempTime += " °C";

      //String temptime wird an das topic "temp/M01" gepublished
      mqttClient.publish("temp/M01", tempTime.c_str());
      delay(500);

      // Serial.print("Spielzeit: ");
      // Serial.print(gameTimer);
      // Serial.print(" sekunden ");
      // Serial.print("Temperatur: ");
      // Serial.println(temp.temperature);
    }
  }

  //Variaben für roll, pitch und yaw, die man durch die 
  //Gyro Sensorwerte erhält
  float roll = g.gyro.x;
  float pitch = g.gyro.y;
  float yaw = g.gyro.z;

  //Werte mit Kommatas in einem String speichern
  String accOrient = String (a.acceleration.x);
  accOrient += ",";
  accOrient += String(a.acceleration.y);
  accOrient += ",";
  accOrient += String(a.acceleration.z);
  accOrient += ",";
  accOrient += String(pitch);
  accOrient += ",";
  accOrient += String(roll);
  accOrient += ",";
  accOrient += String(yaw);

  //accOrient an topic "mpu/M01" publishen
  mqttClient.publish("mpu/M01", accOrient.c_str());
  delay(50);
}

