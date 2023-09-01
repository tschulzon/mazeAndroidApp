package de.othaw.labyrinthion;

import androidx.appcompat.app.AppCompatActivity;

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

public class IpActivity extends AppCompatActivity {

    private EditText brokerEditText;
    private EditText portEditText;

    public static String BROKER = "";
    public static int PORT = 0;

    public static boolean IpFlag = false;

    private Button saveButton;

    private MqttManager mqttManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ip);

        brokerEditText = findViewById(R.id.edit_IPAdresse);
        portEditText = findViewById(R.id.edit_port);
        saveButton = findViewById(R.id.btn_speichern);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.example_menu, menu);
        return true;
    }

    public void openIpActivity() {
        Intent intent = new Intent(this, IpActivity.class);
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.sound) {

            Toast.makeText(this, "Sound turned off", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.bestenliste) {
            Toast.makeText(this, "Bestenliste", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.broker) {
            openIpActivity();
            finish();
            return true;
        } else if (itemId == R.id.steuerung) {
            Toast.makeText(this, "Wählen Sie ein Steuergerät ", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}