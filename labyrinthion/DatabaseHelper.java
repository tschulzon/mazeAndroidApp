package de.othaw.labyrinthion;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import androidx.annotation.Nullable;

/**
 * Klasse die fuer die Erstellung der Datenbank verantwortlich ist
 */
class DatabaseHelper extends SQLiteOpenHelper {


    private final Context context;
    private static final String DATABASE_NAME = "labyrinth.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_NAME = "bestenliste";
    private static final String COLUMN_ID = "_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_TIME = "time";

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;

    }

    /**
     *
     * Erstellt die Datenbank mit einem Tabellennamen einer ID, einem Namen und einer Zeit
     * db.execSQL(query) führt das ganze mit @param db anhand des query aus
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COLUMN_NAME + " TEXT, " + COLUMN_TIME + " TEXT);";
        db.execSQL(query);


    }


    /**
     *
     * Aktualisiert die Datenbank indem sie erst gedroppt wird und dann wieder in OnCreate aufgerufen wird
     *
     *
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME) ;
        onCreate(db);
    }


    /**
     *Fuegt der Datenbank einen Eintrag hinzu
     */
    void addData (String name, String time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();

        cv.put(COLUMN_NAME, name);
        cv.put(COLUMN_TIME, time);

        long result = db.insert(TABLE_NAME, null, cv);
        if (result == -1) {
            Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Added", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Liest alle Daten aus und gibt einen Cursor zurück
     * Erst wird diese Nach der Zeit geordnet
     * Danach wird sie mit getReadableDatabase() lesbar gemacht
     * Cursor wird zurückgegeben und ermöglicht den zugriff auf die Datenbank
     */
    Cursor readAllData() {
        String query = "SELECT * FROM " + TABLE_NAME + " ORDER BY " + COLUMN_TIME;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = null;
        if (db != null) {
            cursor = db.rawQuery(query, null);
        }
        return cursor;
    }


    /**
     * Loescht alle Daten aus der SQLite-Datenbanktabelle
     */
    void deleteAll(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
    }



}




