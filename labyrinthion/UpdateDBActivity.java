package de.othaw.labyrinthion;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

/**
 * Die OnCreate Methode wird überschrieben und ein Layout wird festgelegt
 */
public class UpdateDBActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_dbactivity);
    }
}