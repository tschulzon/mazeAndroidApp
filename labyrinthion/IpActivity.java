package de.othaw.labyrinthion;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Activity der Ip-Adressen Eingabe
 */
public class IpActivity extends AppCompatActivity {
    int soundOn = 0;
    public static boolean IpFlag = false;
    private EditText brokerEditText;
    private EditText portEditText;

    public static String BROKER = "";
    public static int PORT = 0;

    private MqttManager mqttManager;


    /** OnCreate-Methode
     *Eingabe Texte werden per ID zugewiesen
     *Button wird zugewiesen
     *Hier wird nach dem Click auf den SpeicherButton ein Flag auf true gesetzt, und das Spielfeld wird freigeschaltet.
     *Text wird aus EditText genommen und in String umgewandelt
     * Werden in public Variabeln gespeichert und verbindet sich mit Broker
     * Danach schließt sich die Seite
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip);

        brokerEditText = findViewById(R.id.edit_IPAdresse);
        portEditText = findViewById(R.id.edit_port);
        Button saveButton = findViewById(R.id.btn_speichern);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IpFlag = true;
                String broker = brokerEditText.getText().toString();
                int port = Integer.parseInt(portEditText.getText().toString());

                BROKER = broker;
                PORT = port;

                mqttManager = new MqttManager();
                mqttManager.setBrokerAndPort(BROKER,PORT);
                Log.d("test:", "BROKER: " + BROKER + "PORT: " + PORT);
                finish();
            }
        });
    }

    /**
     * Baut den Header nach der example_menu.xml Datei auf
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.example_menu, menu);
        return true;
    }


    /**
     * IPActivity wird aufgemacht
     */
    public void openIpActivity() {
        Intent intent = new Intent(this, IpActivity.class);
        startActivity(intent);
    }

    /**
     * Menuefunktionen im Header werden hier deklariert
     * je nach ID in der example_menu.xml datei wird die funktion aufgerufen
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.sound) {

            if (soundOn == 1){
                Toast.makeText(this, "Sound On", Toast.LENGTH_SHORT).show();
                soundOn = 0;
                item.setIcon(R.drawable.volume_up);
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
            }
            else if (soundOn == 0) {
                Toast.makeText(this, "Sound Off", Toast.LENGTH_SHORT).show();
                soundOn = 1;
                item.setIcon(R.drawable.volume_off);
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);

            }
            return true;
        } else if (itemId == R.id.bestenliste) {
            openBestenlisteActivity();
            return true;
        } else if (itemId == R.id.broker) {
            openIpActivity();
            return true;
        } else if (itemId == R.id.steuerung) {
            Toast.makeText(this, "Steuerung kann nur im Hauptmenü oder im Spiel geändert werden", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * BestenlisteActivity wird aufgemacht
     */
    public void openBestenlisteActivity() {
        Intent intent = new Intent(this, BestenlisteActivity.class);
        startActivity(intent);
    }
}