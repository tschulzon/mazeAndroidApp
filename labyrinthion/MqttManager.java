package de.othaw.labyrinthion;

import android.util.Log;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import java.util.concurrent.atomic.AtomicBoolean;

public class MqttManager {
    private static final String TAG = "Test";

    public static String BROKER = "192.168.1.32";
    public static int PORT = 1883;

    private Mqtt3AsyncClient client;

    public MqttManager() {
        client = MqttClient.builder()
                .useMqttVersion3()
                .identifier("android-client")
                //.serverHost("192.168.178.193")
                //.serverHost("192.168.1.32")
                .serverHost(BROKER)
                .serverPort(PORT)
                .buildAsync();
    }

    public void setBrokerAndPort(String broker, int port) {
        this.BROKER = broker;
        this.PORT = port;
    }

    public void connectToBroker() {

        AtomicBoolean connected = new AtomicBoolean(false);
        //while (connected.get() == false) {
            try {
                client.connectWith()
                        .simpleAuth()
                        .username("my-user")
                        .password("my-password".getBytes())
                        .applySimpleAuth()
                        .send()
                        .whenComplete((connAck, throwable) -> {
                            if (throwable != null) {
                                // Verbindungsfehler behandeln
                                Log.e(TAG, "Fehler bei der Verbindung zum MQTT-Broker: " + throwable.getMessage());
                            } else {
                                Log.d(TAG, "Verbunden mit MQTT-Broker");
                                connected.set(true); // Erfolgreich verbunden
                            }
                        });
                //.get(); // Blockiert die Schleife, bis der Verbindungsvorgang abgeschlossen ist
            } catch (Exception e) {
                Log.e(TAG, "Fehler bei der Verbindung zum MQTT-Broker: " + e.getMessage());
            }

            // Eine kurze Wartezeit, bevor ein erneuter Verbindungsversuch unternommen wird
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        //}

    }

    public void subscribeToBroker(String topic, MqttMessageListener listener) {
        client.subscribeWith()
                .topicFilter(topic)
                .callback(publish -> {
                    String message = new String(publish.getPayloadAsBytes());
                    listener.onMessageReceived(message);
                })
                .send()
                .whenComplete((subAck, throwable) -> {
                    if (throwable != null) {
                        // Handle failure to subscribe
                    } else {
                        Log.d(TAG, "Subscribed to topic:" + topic);
                    }
                });
    }

    public void unsubscribeFromBroker(String topic) {
        client.unsubscribeWith()
                .topicFilter(topic)
                .send()
                .whenComplete((unsubAck, throwable) -> {
                    if (throwable != null) {
                        // Fehler beim Abmelden behandeln
                        Log.e(TAG, "Fehler beim Abmelden von Topic: " + topic);
                    } else {
                        Log.d(TAG, "Abgemeldet von Topic: " + topic);
                    }
                });
    }

    public void publishToBroker(String topic, String msg){
        client.publishWith()
                .topic(topic)
                .payload(msg.getBytes())
                .send()
                .whenComplete((publish, throwable) -> {
                    if (throwable != null) {
                        Log.e(TAG, "Fehler beim Publishen: " + throwable.getMessage());
                    } else {
                        Log.d(TAG, "Published to topic:" + topic);
                    }
                });
    }

    public void disconnect() {
        client.disconnect();
    }

    public interface MqttMessageListener {
        void onMessageReceived(String message);
    }
}