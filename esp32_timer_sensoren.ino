/* 
Code for programming a hardware timers on a ESP32.
based on example code "RepeatTimer.ino" from Arduino.

from teammember1 and Juliana Kühn
 */

#include <Wire.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <WiFi.h>
#include <PubSubClient.h>

//definition WiFi name and password
const char* ssid     = "xxx"
const char* password = "xxx";

//definition MQTT Broker (IP-address and port)

const char* mqtt_broker = "xxx";
int mqttPort = 1883;

Adafruit_MPU6050 mpu;
WiFiClient wifiClient;
PubSubClient mqttClient(wifiClient);

portMUX_TYPE timerMux = portMUX_INITIALIZER_UNLOCKED;

//variables for saving the time
volatile uint32_t gameTimer = 0;
volatile uint32_t lastIsrAt = 0;
volatile uint32_t gameCount = 0;
volatile uint32_t saveGameTime = 0;

//variables to control the functions
volatile bool startTimer = false;
volatile bool timerActive = false;
volatile bool ledFinish = false;

//variable for the build-in LED-pin, 
//which has to light up if the player has reached the goal
const int ledPin = 22;

hw_timer_t * timer = NULL;
volatile SemaphoreHandle_t timerSemaphore;

//function onTimer, where the timervariable always increments by one
void ARDUINO_ISR_ATTR onTimer(){

    portENTER_CRITICAL_ISR(&timerMux);
    gameTimer++;
    lastIsrAt = millis() / 1000;
    portEXIT_CRITICAL_ISR(&timerMux);

    xSemaphoreGiveFromISR(timerSemaphore, NULL);

}

//function to connect with wifi
void connectToWiFi() {
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.print(".");
  }

  //if connected with wifi
  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

//function to connect with mqtt broker
void setupMQTT() {

  mqttClient.setServer(mqtt_broker, 1883);

  while (!mqttClient.connected()) {
    Serial.println("Connecting to MQTT...");
    if (mqttClient.connect("ESP32Client")) {
      Serial.println("connected");

      //subscribing messages here
      mqttClient.subscribe("start/M01");
      mqttClient.subscribe("finished/M01");

    } else {
      Serial.print("failed with state ");
      Serial.print(mqttClient.state());
      delay(2000);
    }
  }
}

//function to process the received essages
void callback(char* topic, byte* message, unsigned int length) {
  
  Serial.print("Message arrived in topic: ");
  Serial.print(topic);
  Serial.print(". Message: ");
  String messageTemp; //string to save the incoming message

//topic of the received message will be runned through
//signs are converted to chars, because they are received as bytes
//after that, the chars will be inserted into the string
  for (int i = 0; i < length; i++) {
    Serial.print((char)message[i]);
    messageTemp += (char)message[i];
  }
  Serial.println();

//if we receive the message START, timer will be activated
  if (String(topic) == "start/M01") {
    if(strcmp(messageTemp.c_str(), "START") == 0) {
      if(!startTimer)
      {
        startTimer = true;
        startTimerGo();
      }
        //if the timer is already activated, the timer will be reset and restarted
      else
      {
        xSemaphoreGiveFromISR(timerSemaphore, NULL);
        resetTimer();
        startTimerGo();
      }
      
    }
  }
    //if we receive the message "finished", timer will be stopped
  else if (String(topic) == "finished/M01") {
    ledFinish = true;
    stopTimer();
  }
}

//function to reset the timer and the variables
void resetTimer() {

  if (timer) { 
    timerDetachInterrupt(timer);
    timerAlarmDisable(timer); 
    timerEnd(timer); 
    timer = NULL; //"null" for no more activated timer
  }

  timerActive = false;
  gameTimer = 0;
  lastIsrAt = 0;
}

//function to stop the timer if the goal has reached
void stopTimer() {

  if (timer) {
    timerDetachInterrupt(timer);
    timerAlarmDisable(timer);
    timerEnd(timer);

    //time will be saved in a variables for publishing in the leaderboard
    saveGameTime = gameCount; 
    Serial.println("Gametime:");
    Serial.println(saveGameTime);

    //saving in a string for publishing
    String saveTime = String(saveGameTime);
    mqttClient.publish("endTime/M01", saveTime.c_str());
  }
}

//function to start the timer
void startTimerGo() {

  if (!timerActive) {
    //initializing the timer
    timer = timerBegin(0, 80, true);
    timerAttachInterrupt(timer, &onTimer, true);
    timerAlarmWrite(timer, 1000000, true);
    timerAlarmEnable(timer);
    
    timerActive = true;
  }
}

//connecting to wifi and sensors in the setup function and setting the mqtt broker and port with variables
void setup() {
  Serial.begin(115200);
  connectToWiFi();
  Wire.begin();

  mqttClient.setServer(mqtt_broker, mqttPort);
  mqttClient.setCallback(callback);

  mpu.begin();
  delay(1000);

  pinMode(ledPin, OUTPUT);

  //connecting to MPU6050 sensors
  while (!mpu.begin()) {
    Serial.println("Could not find a valid MPU6050 sensor, check wiring!");
    delay(500);
  }

  //creating semaphore to inform when the timer has started
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
        digitalWrite(ledPin, LOW); //LED at 22 off -> on with LOW
        delay(500);
        ledFinish = false;
      }

//sensors as variables for accelerometer, gyro and temperature
  sensors_event_t a, g, temp;
  mpu.getEvent(&a, &g, &temp);

//check if timer is activated
  if (startTimer) {
      if (xSemaphoreTake(timerSemaphore, 0) == pdTRUE){

        portENTER_CRITICAL(&timerMux);
        gameCount = gameTimer;
        portEXIT_CRITICAL(&timerMux);

    //saving the time and temperature in a string
      String tempTime = "Spielzeit: ";
      tempTime += String(gameCount);
      tempTime += " Sek ";
      tempTime += "Temperatur: ";
      tempTime += String(temp.temperature);
      tempTime += " °C";

      //String temptime will be published to topic "temp/M01"
      mqttClient.publish("temp/M01", tempTime.c_str());
      delay(500);

      // Serial.print("Spielzeit: ");
      // Serial.print(gameTimer);
      // Serial.print(" sekunden ");
      // Serial.print("Temperatur: ");
      // Serial.println(temp.temperature);
    }
  }

  //variables for roll, pitch und yaw
  float roll = g.gyro.x;
  float pitch = g.gyro.y;
  float yaw = g.gyro.z;

//saving the values with comma in a string
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

  //publishing accOrient to topic "mpu/M01"
  mqttClient.publish("mpu/M01", accOrient.c_str());
  delay(50);
}

