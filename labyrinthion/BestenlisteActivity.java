package de.othaw.labyrinthion;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * page with leaderboard
 */
public class BestenlisteActivity extends AppCompatActivity {
    int soundOn = 0;
    RecyclerView recyclerView;
    Button addData , deleteData , newGame;
    EditText editName;
    TextView showTime;
    DatabaseHelper DB;
    ArrayList<String> id,name ,time;
    CustomAdapter customAdapter;
    public static boolean useSmartphone;
    public static String zeitBestenliste;

    /**
     * 
     *function to show the controls, which was chosen
     *
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem controlItem = menu.findItem(R.id.steuerung);
        if (useSmartphone) {
            controlItem.setTitle("Steuerung: Smartphone");
        } else {
                controlItem.setTitle("Steuerung: MQTT");
        }
        return super.onPrepareOptionsMenu(menu);
    }


    /**
     *
     *building the header like example_menu.xml file
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.example_menu, menu);

        return true;
    }

    /**
     * defining menue functions in the header
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
            openMainActivity();
        } else if (itemId == R.id.broker) {
            Toast.makeText(this, "IP-Adresse kann nur im Hauptmenü geändert werden", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.steuerung) {
            Toast.makeText(this, "Kann nur im Menü oder beim Spiel geändert werden", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.restart) {

            Intent intent = new Intent(BestenlisteActivity.this, LabActivity.class);
            finish();
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * OnCreate method
     * ids will be assigned
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bestenliste);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        recyclerView = findViewById(R.id.recyclerView);
        editName = findViewById(R.id.editName);
        showTime = findViewById(R.id.showGameTime);
        addData = findViewById(R.id.addData);
        deleteData = findViewById(R.id.deleteData);
        newGame = findViewById(R.id.newGame);

        getTime();
        useSmartphone = LabActivity.useSmartphone;
        invalidateOptionsMenu();

        addData.setOnClickListener(new View.OnClickListener() {
            /**
             * Add-Button OnClick event
             */
            @Override
            public void onClick(View view) {
                DatabaseHelper myDB = new DatabaseHelper(BestenlisteActivity.this);
                myDB.addData(editName.getText().toString().trim(),
                        showTime.getText().toString().trim());
                recreate();
            }


        });
        deleteData.setOnClickListener((new View.OnClickListener() {
            /**
             * Delete-Button OnClick event
             */
            @Override
            public void onClick(View view) {
                DatabaseHelper myDB = new DatabaseHelper(BestenlisteActivity.this);
                myDB.deleteAll();
                recreate();
            }
        }));


        newGame.setOnClickListener(new View.OnClickListener() {
            /**
             * New-Game-Button OnClick event
             */
            @Override
            public void onClick(View view) {
                openLabActivity();
            }


        });



        DB = new DatabaseHelper(BestenlisteActivity.this);
        id = new ArrayList<>();
        name = new ArrayList<>();
        time = new ArrayList<>();

        storeData();

        customAdapter = new CustomAdapter(BestenlisteActivity.this,id ,name , time);
        recyclerView.setAdapter(customAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(BestenlisteActivity.this));


    }

    /**
     * saving the values in the leaderboard
     *saving id,name, time
     */
    void storeData() {
        Cursor cursor = DB.readAllData();
        if(cursor.getCount() == 0) {
            Toast.makeText(this, "Keine Daten", Toast.LENGTH_SHORT).show();
        } else {
                while (cursor.moveToNext()) {
                 id.add(cursor.getString(0));
                 name.add(cursor.getString(1));
                 time.add(cursor.getString(2));
                }
    }

}

    /**
     * getting the {@link LabActivity#finalTime} in "zeitBestenliste"
     * and showing it
     */
    private void getTime() {
        zeitBestenliste = LabActivity.finalTime;
        showTime.setText(zeitBestenliste);
    }


    /**
     * open MainActivity
     */
    public void openMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }


    /**
     * open LabActivity
     */
    public void openLabActivity() {
        Intent intent = new Intent(this, LabActivity.class);
        startActivity(intent);
    }
}



   
