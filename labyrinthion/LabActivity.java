package de.othaw.labyrinthion;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.TextUtils;
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

public class LabActivity extends AppCompatActivity {

    private static final float MIN_X_AXIS = -2.0f;
    private static final float MAX_X_AXIS = 2.0f;
    private static final float MIN_Y_AXIS = -2.0f;
    private static final float MAX_Y_AXIS = 2.0f;


    private SensorManager sensorManager;
    private MazeView mazeView;
    int soundOn = 0;
    static int maxDifference = 10;

    static int min = 10;
    static int maxWid = 20;

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

    private String topicStart = "start/M01";
    private String topicFinish = "finished/M01";
    private String msgStart = "START";
    private String msgFinish = "FINISH";

    private static String BROKER = "192.168.1.32";
    private static int PORT = 1883;

    public static String endTime;

    private static float mqttSensorX;
    private static float mqttSensorY;

    private boolean useMQTT = false;
    public static boolean useSmartphone = true;

    public static String endZeit;

    public static String zeitBestenliste;

    public static String finalTime;

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem controlItem = menu.findItem(R.id.steuerung);
        if (useSmartphone) {
            controlItem.setTitle("Steuerung: Smartphone");
        } else
        {
            controlItem.setTitle("Steuerung: MQTT");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.example_menu, menu);
        return true;
    }

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
        invalidateOptionsMenu();
        Log.d(TAG, "useMQTT:" + useMQTT + "---- useSmartphone" + useSmartphone);

        sub = findViewById(R.id.subText);

        mqttManager = new MqttManager();

        // Erzeuge eine Instanz der MazeView und füge sie zum Layout hinzu
        mazeView = new MazeView(this);

        mainLayout.addView(mazeView);
        useMQTT = MainActivity.useMQTT;

        initActivity();


    }


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
                        mazeView.invalidate(); // Redraw the maze view

                    }
                });
                useMQTT = true;

        }

    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                float xAxis = event.values[0];
                float yAxis = event.values[1];
                float zAxis = event.values[2];
                String data = "X: " + xAxis + "Y: " + yAxis + "Z: " + zAxis;

                mazeView.updateBallPosition(xAxis, yAxis);
                mazeView.invalidate(); // Redraw the maze view
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Wird aufgerufen, wenn sich die Genauigkeit des Sensors ändert
        }
    };

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

    @Override
    protected void onPause() {
        super.onPause();
        mqttManager.disconnect();
        Log.d(TAG, "Broker ist nicht mehr verbunden");
    }


    public static class MazeGenerator {
        private int width;
        private int height;
        private int[][] maze;

        public MazeGenerator(int width, int height) {
            this.width = width;
            this.height = height;
            maze = new int[width][height];
            generateMaze(0, 0); //Labyrinth-Generierung wird gestartet
            setGoal();
        }

        private void generateMaze(int x, int y) {
            maze[x][y] = 1; //Weg bzw. ist besucht
            maze[0][0] = 2; //Beginn
            maze[MAZE_WIDTH - 1][MAZE_HEIGHT - 1] = 3; //Ziel


            //Zufällige Reihenfolge der Richtungen
            //oben, unten, links, rechts
            int[] directions = {0, 1, 2, 3};
            shuffleArray(directions);

            //Richtungen werden durchlaufen
            for (int direction : directions) {
                int nx = x;
                int ny = y;

                //Koordinaten neu berechnen basierend auf der Richtung
                if (direction == 0 && y > 1) {
                    ny -= 2;
                } else if (direction == 1 && y < height - 2) {
                    ny += 2;
                } else if (direction == 2 && x > 1) {
                    nx -= 2;
                } else if (direction == 3 && x < width - 2) {
                    nx += 2;
                }

                //unbesuchte Felder als besucht markieren
                if (maze[nx][ny] == 0){
                    maze[nx][ny] = 1;
                    //Feld zwischen aktuellen und nächsten Coords als besucht markieren
                    maze[(x + nx) / 2][(y + ny) / 2] = 1;
                    generateMaze(nx, ny); //Generierung rekursiv für das nächste Feld aufrufen
                }
            }
        }

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



        public void updateBallPosition(float xAxis, float yAxis) {
            // Begrenzt die Achsenwerte auf den definierten Wertebereich
            xAxis = Math.max(Math.min(xAxis, 95.0f), -85.0f);
            yAxis = Math.max(Math.min(yAxis, 2.0f), -2.0f);

            // Adjust the ball position based on accelerometer data
            float acceleration = 0.2f; // You can adjust this value for sensitivity
            float newBallX = ballX - xAxis * acceleration;
            float newBallY = ballY + yAxis * acceleration;

            // Check if the new position collides with walls
            if (!checkWallCollision(ballX, ballY, newBallX, newBallY)) {
                // Calculate the new potential cell position of the ball
                int newCellX = Math.round(newBallX);
                int newCellY = Math.round(newBallY);

                // Check if the new cell is within the maze boundaries and is a valid path cell
                if (newCellX >= 0 && newCellX < MAZE_WIDTH && newCellY >= 0 && newCellY < MAZE_HEIGHT && maze[newCellX][newCellY] != 0) {
                    ballX = newBallX;
                    ballY = newBallY;
                }
            } 

            // Keep the ball within the maze boundaries
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

            // Check if the ball reaches the finish point
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

        private boolean checkWallCollision(float startX, float startY, float endX, float endY) {
            int cellX1 = Math.round(startX);
            int cellY1 = Math.round(startY);
            int cellX2 = Math.round(endX);
            int cellY2 = Math.round(endY);

            // Check if the ball is moving horizontally
            if (cellY1 == cellY2) {
                // Check for left collision
                if (endX < startX) {
                    for (int x = cellX2; x <= cellX1; x++) {
                        if (x >= 0 && maze[x][cellY2] == 0) {
                            return true;
                        }
                    }
                }
                // Check for right collision
                else if (endX > startX) {
                    for (int x = cellX1; x <= cellX2; x++) {
                        if (x < MAZE_WIDTH && maze[x][cellY2] == 0) {
                            return true;
                        }
                    }
                }
            }
            // Check if the ball is moving vertically
            else if (cellX1 == cellX2) {
                // Check for top collision
                if (endY < startY) {
                    for (int y = cellY2; y <= cellY1; y++) {
                        if (y >= 0 && maze[cellX2][y] == 0) {
                            return true;
                        }
                    }
                }
                // Check for bottom collision
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
                        //canvas.drawBitmap(imgWall, null, rect, null);
                    }
                    else if (maze[x][y] == 3) {
                        canvas.drawRect(left, top, right, bottom, finishPaint);
                    }
                    else if (maze[x][y] == 2) {
                        canvas.drawRect(left, top, right, bottom, beginPaint);
                    }
                    else {
                        //canvas.drawRect(left, top, right, bottom, pathPaint);
                        canvas.drawBitmap(imgPath, null, rect, null);
                    }
                }
            }

            // Draw the ball
            float ballRadius = cellSize / 2f;
            float ballCenterX = ballX * cellSize + ballRadius;
            float ballCenterY = ballY * cellSize + ballRadius;
            Paint ballPaint = new Paint();
            ballPaint.setColor(Color.BLUE);
            canvas.drawCircle(ballCenterX, ballCenterY, ballRadius, ballPaint);

            invalidate();
        }
    }

    public void goalfunction() {
        mqttManager.publishToBroker(topicFinish, msgFinish);
        MediaPlayer mediaPlayer = MediaPlayer.create(LabActivity.this,R.raw.winsound);
        mediaPlayer.start();

        zeitBestenliste = endZeit;

        String[] timeArr = zeitBestenliste.split(" ");
        finalTime = timeArr[1];

        Log.d(TAG, "Zeit Bestenliste: " + finalTime);
        openBestenlisteActivity();
    }
    public void openBestenlisteActivity() {
        Intent intent = new Intent(this, BestenlisteActivity.class);
        startActivity(intent);
    }
}