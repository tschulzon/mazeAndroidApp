package de.othaw.labyrinthion;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

/**
 *  MainActivity Class
 *  player can start here the game
 *  but only if a ip address has been entered
 */
public class MainActivity extends AppCompatActivity {
    int soundOn = 0;
    boolean IpFlag;
    private static final String TAG = "Test";

    public static String BROKER = "0";
    public static int PORT = 0;

    public static boolean useMQTT = false;
    public static boolean useSmartphone = true;

    /**
     * button to open the game activity
     * if no ip address has entered, a message will be shown
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        getData();

        MqttManager mqttManager = new MqttManager();
        mqttManager.connectToBroker();

        Button button = findViewById(R.id.button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IpFlag = IpActivity.IpFlag;
                if(IpFlag){
                    openLabActivity();
                }
                else {
                    Toast.makeText(MainActivity.this, "Bitte fügen sie erst eine IP-Adresse hinzu!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 
     *getting data from broker from ipActivity
     */
    public void getData() {
        BROKER = IpActivity.BROKER;
        PORT = IpActivity.PORT;
    }

    /**
     * open LabActivity
     */
    public void openLabActivity() {
        Intent intent = new Intent(this, LabActivity.class);
        startActivity(intent);
    }
    /**
     * opens the IPActivity where IP address can be entered
     */
    public void openIpActivity() {
        Intent intent = new Intent(this, IpActivity.class);
        startActivity(intent);
    }

    /**
     * open leaderboard
     */
    public void openBestenlisteActivity() {
        Intent intent = new Intent(this, BestenlisteActivity.class);
        startActivity(intent);
    }

    /**
     * building header
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.example_menu, menu);
        return true;
    }

    /**
     * defining header menue functions
     * 
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
            IpFlag = IpActivity.IpFlag;
            if (IpFlag) {
                if (!useMQTT) {
                item.setTitle("Steuerung: MQTT");
                useSmartphone = false;
                useMQTT = true;
                Log.d(TAG, "useSmartphone is: " + useSmartphone);
                Toast.makeText(this, "MQTT als Steuergerät gewählt", Toast.LENGTH_SHORT).show();
                    MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.this,R.raw.winsound);
                    mediaPlayer.start();
            }
            else {
                item.setTitle("Steuerung: Smartphone");
                useSmartphone = true;
                useMQTT = false;
                Log.d(TAG, "useSmartphone is: " + useSmartphone);
                Toast.makeText(this, "Smartphone als Steuergerät gewählt", Toast.LENGTH_SHORT).show();
                    MediaPlayer mediaPlayer = MediaPlayer.create(MainActivity.this,R.raw.winsound);
                    mediaPlayer.start();
            }
            }else {
                Toast.makeText(this, "Bitte fügen sie erst einen Broker hinzu", Toast.LENGTH_SHORT).show();
            }


            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
