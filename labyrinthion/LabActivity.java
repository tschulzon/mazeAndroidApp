package de.othaw.labyrinthion;


import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

/**
 * Hier wird die Activity des eigentlichen Spiels aufgerufen
 * Am Anfang werden erstmal die random Werte fuer das spaetere Maze ermittelt
 * Verschiedene Topics werden initalisiert
 */
public class LabActivity extends AppCompatActivity {

    public static String endZeit;

    public static String zeitBestenliste;

    public static String finalTime;

    private SensorManager sensorManager;
    private MazeView mazeView;
    int soundOn = 0;
    boolean IpFlag;
    static int maxDifference = 5;

    static int min = 10;
    static int maxWid = 30;

    static int number1 = (int)Math.floor((Math.random() * (maxWid  - min +1 ) + min));
    static int minNumber2 = Math.max(min, number1 - maxDifference);
    static int maxNumber2 = Math.min(maxWid, number1 + maxDifference);


    static int number2 = (int)Math.floor((Math.random() * (maxNumber2 - minNumber2 +1 ) + minNumber2));

    static int MAZE_WIDTH = number1 ;
    static int MAZE_HEIGHT = number2;



    public boolean isGameFinished= false;

    private TextView sub;

    private MqttManager mqttManager;
    private static final String TAG = "Test";

    private final String topicStart = "start/M01";
    private final String topicFinish = "finished/M01";
    private final String msgStart = "START";
    private final String msgFinish = "FINISH";

    //Brokerdaten
    private static String BROKER = "192.168.1.32";
    private static int PORT = 1883;

    private static float mqttSensorX;
    private static float mqttSensorY;

    private boolean useMQTT = false;
    public static boolean useSmartphone = true;

    /**
     * Je nachdem was der Spieler ausgewaehlt hat wird als Steuerung angezeigt
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem controlItem = menu.findItem(R.id.steuerung);
        if (useSmartphone) {
            controlItem.setTitle("Steuerung: Smartphone");
        }
           else {
                controlItem.setTitle("Steuerung: MQTT");
           }


        return super.onPrepareOptionsMenu(menu);
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
            Toast.makeText(this, "IP-Adresse kann nur im Hauptmenü geändert werden", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.steuerung) {
            if (IpFlag) {
                if (!useMQTT) {
                useSmartphone = false;
                useMQTT = true;
                Log.d(TAG, "useMQtt is TRUE");
                initActivity();
                Toast.makeText(this, "MQTT als Steuergerät gewählt ", Toast.LENGTH_SHORT).show();
            }
            else {
                useSmartphone = true;
                useMQTT = false;
                Log.d(TAG, "useMQtt is FALSE");
                initActivity();
                Toast.makeText(this, "Smartphone als Steuergerät gewählt", Toast.LENGTH_SHORT).show();
            }
            } else {
                Toast.makeText(this, "Bitte fügen sie erst einen Broker hinzu", Toast.LENGTH_SHORT).show();
            }

            MediaPlayer mediaPlayer = MediaPlayer.create(LabActivity.this,R.raw.winsound);
            mediaPlayer.start();
            return true;
        } else if (itemId == R.id.restart) {

            Intent intent = new Intent(LabActivity.this, LabActivity.class);
            finish();
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * OnCreate Methode der LabActivity
     * Es werden ersteinmal die Variabel aus der MainActivity übernommen
     * Erzeugt dann eine Instanz der MazeView und füge sie zum Layout hinzu
     * Ruft dann initActivity auf
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lab);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        LinearLayout mainLayout = findViewById(R.id.main_layout);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        useSmartphone = MainActivity.useSmartphone;
        BROKER = MainActivity.BROKER;
        PORT = MainActivity.PORT;
        IpFlag = IpActivity.IpFlag;
        invalidateOptionsMenu();
        Log.d(TAG, "useMQTT:" + useMQTT + "---- useSmartphone" + useSmartphone);

        sub = findViewById(R.id.subText);

        mqttManager = new MqttManager();

        mazeView = new MazeView(this);

        mainLayout.addView(mazeView);
        useMQTT = MainActivity.useMQTT;

        initActivity();


    }


    /**
     * Ueberprueft erstmal was fuer eine Steuerung ausgewaehlt wurde
     * Je nachdem wird eine die Steuerung anderes ermoeglicht
     * Beim Handy wird die Funktion SensorEventListener bei einer bewegung verwendet
     * Beim Controller wird die Kugel in der Funktion on MessageReceived aktualisiert
     */
    private void initActivity(){

        if (useSmartphone) {
                mqttManager.unsubscribeFromBroker("mpu/M01");


                Sensor accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Sensor magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                sensorManager.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
                sensorManager.registerListener(sensorEventListener, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
                useMQTT = false;
        }
        else
        {
                // Handysteuerung deaktivieren
                sensorManager.unregisterListener(sensorEventListener);

                mqttManager.subscribeToBroker("mpu/M01", new MqttManager.MqttMessageListener() {
                    @Override
                    public void onMessageReceived(String message) {
                        String[] mqtt_coords = message.split(",");
                        float mqtt_x = Float.parseFloat(mqtt_coords[0]);
                        float mqtt_y = Float.parseFloat(mqtt_coords[1]);

                        mqttSensorX = mqtt_x;
                        mqttSensorY = mqtt_y;

                        // Kugelposition aktualisieren
                        mazeView.updateBallPosition(mqttSensorX, mqttSensorY);
                        mazeView.invalidate(); //Zeichnet das Maze

                    }
                });
                useMQTT = true;

        }

    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float xAxis = event.values[0];
                float yAxis = event.values[1];

                mazeView.updateBallPosition(xAxis, yAxis);
                mazeView.invalidate(); // Zeichnet das Maze neu
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Wird aufgerufen, wenn sich die Genauigkeit des Sensors ändert
        }
    };

    /**
     * OnResume ermoeglicht eine wiederaufnahme des App Betriebs
     */
    @Override
    protected void onResume(){
        super.onResume();
        BROKER = MainActivity.BROKER;
        PORT = MainActivity.PORT;
        mqttManager.connectToBroker();
        mqttManager.publishToBroker(topicStart, msgStart);
        mqttManager.subscribeToBroker("temp/M01", new MqttManager.MqttMessageListener() {
            @Override
            public void onMessageReceived(String message) {
                LabActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sub.setText(message);
                        endZeit = message;
                    }
                });
            }
        });
        Log.d(TAG, "Broker ist verbunden");
    }


    /**
     * Pausiert die App und disconnected vom MQTT Manager
     */
    @Override
    protected void onPause() {
        super.onPause();
        mqttManager.disconnect();
        Log.d(TAG, "Broker ist nicht mehr verbunden");
    }

    //Mit Hilfe von ChatGpt
    /**
     * In MazeGenerator wird generateMaze und setGoal aufgerufen
     * Mit Hilfe von ChatGpt
     */
    public static class MazeGenerator {
        private final int width;
        private final int height;
        private final int[][] maze;

        public MazeGenerator(int width, int height) {
            this.width = width;
            this.height = height;
            maze = new int[width][height];
            generateMaze(0, 0);
            setGoal();
        }

        /**
         * Das aktuelle Feld maze[x][y] wird auf 1 gesetzt, um es als besucht zu markieren
         * Der Startpunkt bei maze[0][0] wird auf 2 gesetzt
         * Als Ende nehmen wir maze[MAZE_WIDTH - 1][MAZE_HEIGHT - 1] und setzen es auf 3
         * Array direction wird erstellt und gemischt (Richtung)
         * In einer Schleife wird jede Richtung geprueft
         * Schaut ob das neue Feld maze[nx][ny] noch nicht besucht ist
         * Das neue Feld maze[nx][ny] und das Feld in der Mitte zwischen dem aktuellen Feld und dem neuen Feld maze[(x + nx) / 2][(y + ny) / 2]
         * werden auf 1 gesetzt, um sie als besucht zu markieren und eine Wand zu entfernen.
         * generate maze wird dann rekursive aufgerufen bis das Labyrinth vollstaendig generiert ist.
         */
        private void generateMaze(int x, int y) {
            maze[x][y] = 1;
            maze[0][0] = 2;
            maze[MAZE_WIDTH - 1][MAZE_HEIGHT - 1] = 3;


            int[] directions = {0, 1, 2, 3};
            shuffleArray(directions);

            for (int direction : directions) {
                int nx = x;
                int ny = y;

                if (direction == 0 && y > 1) {
                    ny -= 2;
                } else if (direction == 1 && y < height - 2) {
                    ny += 2;
                } else if (direction == 2 && x > 1) {
                    nx -= 2;
                } else if (direction == 3 && x < width - 2) {
                    nx += 2;
                }

                if (maze[nx][ny] == 0){
                    maze[nx][ny] = 1;
                    maze[(x + nx) / 2][(y + ny) / 2] = 1;
                    generateMaze(nx, ny);
                }
            }
        }

        /**
         * SetGoal Funktion die das Ziel festlegt und eine moegliche Wand daneben frei macht
         * Ausgang wird hier definiert
         * soll immer rechts unten in der Ecke sein
         */
        //Ohne ChatGpt
        private void setGoal(){
            //Ausgang wird hier definiert
            //soll immer rechts unten in der Ecke sein
            int goalX = width - 1;
            int goalY = height - 1;

            //hier wird überprüft ob das Feld über dem Ziel eine Wand ist
            //falls ja, soll das Feld links daneben ein Weg sein
            if (maze[goalX][goalY - 1] != 1) {
                maze[goalX - 1][goalY] = 1;
            }
            //hier wird überprüft ob das Feld links neben dem Ziel eine Wand ist
            //falls ja, ist das Feld über dem Ziel ein Weg
            else if(maze[goalX - 1][goalY] != 1) {
                maze[goalX][goalY - 1] = 1;
            }

            maze[goalX][goalY] = 3;

        }
        //Ohne ChatGpt

        /**
         * Array wird durchgemischt
         */
        private void shuffleArray(int[] array) {
            Random random = new Random();
            for (int i = array.length - 1; i > 0; i--) {
                int index = random.nextInt(i + 1);
                int temp = array[index];
                array[index] = array[i];
                array[i] = temp;
            }
        }

        public int[][] getMaze() {
            return maze;
        }

    }
    //Mit Hilfe von ChatGpt


    //Mit Hilfe von ChatGpt

    public class MazeView extends View {
        private int[][] maze;
        private Paint wallPaint;
        private Paint pathPaint;
        private Paint beginPaint;
        private Paint finishPaint;
        private Paint ballPaint;
        private int cellSize;
        private float ballX;
        private float ballY;


        public MazeView(Context context) {
            super(context);
            init();
        }

        public MazeView(Context context, AttributeSet attrs) {
            super(context, attrs); //view von xml wird geholt
            init();
        }

        /**
         * Wand und Wegfarben werden festgelegt
         * Ball wird auf 0,0 gesetzt
         */
        private void init() {

            String hexColor = "#9eb76b"; // Beispiel-Hex-Farbwert
            int colorWall = Color.parseColor(hexColor);

            MazeGenerator mazeGenerator = new MazeGenerator(MAZE_WIDTH, MAZE_HEIGHT);
            maze = mazeGenerator.getMaze();
            wallPaint = new Paint();
            wallPaint.setColor(colorWall);
            pathPaint = new Paint();
            pathPaint.setColor(Color.WHITE);

            beginPaint = new Paint();
            beginPaint.setColor(Color.GREEN);

            finishPaint = new Paint();
            finishPaint.setColor(Color.RED);

            ballPaint = new Paint();
            ballPaint.setColor(Color.BLUE);

            ballX = 0;
            ballY = 0;
        }


        /**
         * Ist für die eigentliche Bewegung des Ball verantwortlich
         * Begrenzt die Achsenwerte auf den definierten Wertebereich
         * Bestimmt die Geschwindigkeit des Balls
         * Fuehrt danach dann Kollisionskontrollen aus
         * Nach der Berechnung der neuen Zelle wird geschaut ob es ein Weg ist
         * Am Ende findet eine pruefung statt ob der Ball im Ziel ist
         */
        public void updateBallPosition(float xAxis, float yAxis) {
            // Begrenzt die Achsenwerte auf den definierten Wertebereich
            xAxis = Math.max(Math.min(xAxis, 95.0f), -85.0f);
            yAxis = Math.max(Math.min(yAxis, 2.0f), -2.0f);

            //Bestimmt die Geschwindigkeit des Balls
            float acceleration = 0.2f;
            float newBallX = ballX - xAxis * acceleration;
            float newBallY = ballY + yAxis * acceleration;

            // Prüft ob die neue Position mit den Wänden kolidiert
            if (!checkWallCollision(ballX, ballY, newBallX, newBallY)) {
                // Berechnet die neue Position der Zelle
                int newCellX = Math.round(newBallX);
                int newCellY = Math.round(newBallY);

                // Prüft ob die neue Zelle im Labyrinth ist und ein Weg
                if (newCellX >= 0 && newCellX < MAZE_WIDTH && newCellY >= 0 && newCellY < MAZE_HEIGHT && maze[newCellX][newCellY] != 0) {
                    ballX = newBallX;
                    ballY = newBallY;
                }
            }

            if (ballX < 0) {
                ballX = 0;
            } else if (ballX >= MAZE_WIDTH) {
                ballX = MAZE_WIDTH - 1;
            }

            if (ballY < 0) {
                ballY = 0;
            } else if (ballY >= MAZE_HEIGHT) {
                ballY = MAZE_HEIGHT - 1;
            }

            // Prüft ob der Ball im Ziel angelangt ist
            if (maze[(int) ballX][(int) ballY] ==  3 && !isGameFinished) {
                if (!useSmartphone) {
                    LabActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LabActivity.this, "You Won the Game", Toast.LENGTH_SHORT).show();
                            isGameFinished = true;
                            goalfunction();
                            invalidate();
                        }
                    });
                } else {
                    Toast.makeText(LabActivity.this, "You Won the Game", Toast.LENGTH_SHORT).show();
                    isGameFinished = true;
                    goalfunction();
                    invalidate();
                }
            }
        }

        /**
         * Kollisions pruefung
         * Prueft ob der Ball sich horizontal bewegt dann erst links und dann recht
         * Prueft ob der Ball sich vertikal bewegt dann erst oben und dann unten
         */
        private boolean checkWallCollision(float startX, float startY, float endX, float endY) {
            int cellX1 = Math.round(startX);
            int cellY1 = Math.round(startY);
            int cellX2 = Math.round(endX);
            int cellY2 = Math.round(endY);

            // Prüft ob der Ball sich horizontal bewegt
            if (cellY1 == cellY2) {
                // Prüft links
                if (endX < startX) {
                    for (int x = cellX2; x <= cellX1; x++) {
                        if (x >= 0 && maze[x][cellY2] == 0) {
                            return true;
                        }
                    }
                }
                // Prüft rechts
                else if (endX > startX) {
                    for (int x = cellX1; x <= cellX2; x++) {
                        if (x < MAZE_WIDTH && maze[x][cellY2] == 0) {
                            return true;
                        }
                    }
                }
            }
            // Prüft ob der Ball sich vertikal bewegt
            else if (cellX1 == cellX2) {
                // Prüft oben
                if (endY < startY) {
                    for (int y = cellY2; y <= cellY1; y++) {
                        if (y >= 0 && maze[cellX2][y] == 0) {
                            return true;
                        }
                    }
                }
                // Prüft unten
                else if (endY > startY) {
                    for (int y = cellY1; y <= cellY2; y++) {
                        if (y < MAZE_HEIGHT && maze[cellX2][y] == 0) {
                            return true;
                        }
                    }
                }
            }

            return false;
        }



        @Override
        protected void onSizeChanged(int w, int h, int oldW, int oldH) {
            super.onSizeChanged(w, h, oldW, oldH);
            cellSize = Math.min(w, h) / MAZE_WIDTH;
        }

        /**
         * OnDraw Funktion
         * Zeichnet das Maze auf den Canvas
         * Zeichnet den Ball
         */
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            Bitmap imgPath = BitmapFactory.decodeResource(getResources(), R.drawable.labrect);


            for (int y = 0; y < MAZE_HEIGHT; y++) {
                for (int x = 0; x < MAZE_WIDTH; x++) {
                    int left = x * cellSize;
                    int top = y * cellSize;
                    int right = left + cellSize;
                    int bottom = top + cellSize;

                    RectF rect = new RectF(left, top, right, bottom);


                    if (maze[x][y] == 0) {
                        canvas.drawRect(left, top, right, bottom, wallPaint);
                    }
                    else if (maze[x][y] == 3) {
                        canvas.drawRect(left, top, right, bottom, finishPaint);
                    }
                    else if (maze[x][y] == 2) {
                        canvas.drawRect(left, top, right, bottom, beginPaint);
                    }
                    else {
                        canvas.drawBitmap(imgPath, null, rect, null);
                    }
                }
            }

            // Zeichnet den Ball
            float ballRadius = cellSize / 2f;
            float ballCenterX = ballX * cellSize + ballRadius;
            float ballCenterY = ballY * cellSize + ballRadius;
            Paint ballPaint = new Paint();
            ballPaint.setColor(Color.BLUE);
            canvas.drawCircle(ballCenterX, ballCenterY, ballRadius, ballPaint);

            invalidate();
        }
    }
    //Mit Hilfe von ChatGpt

    /**
     * Wird ausgeloeslt wenn der Ball im Ziel ist
     * Schickt an den MQTT ein Finish Topic
     * Spielt mithilfe des MediaPlayers einen Sound ab
     * Speichert endZeit für die Bestenliste
     * Ruft zum schluss openBestenlisteActivity() auf um zur Bestenliste zu gelangen
     */
    public void goalfunction() {
        mqttManager.publishToBroker(topicFinish, msgFinish);

        //Aus dem Buch Kapitel 14 Multimedia
        MediaPlayer mediaPlayer = MediaPlayer.create(LabActivity.this,R.raw.winsound);
        mediaPlayer.start();
        //Aus dem Buch

        zeitBestenliste = endZeit;

        String[] timeArr = zeitBestenliste.split(" ");
        finalTime = timeArr[1];

        Log.d(TAG, "Zeit Bestenliste: " + finalTime);
        openBestenlisteActivity();
    }

    /**
     * BestenlisteActivity wird aufgemacht
     */
    public void openBestenlisteActivity() {
        Intent intent = new Intent(this, BestenlisteActivity.class);
        startActivity(intent);
    }
}