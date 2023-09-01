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
 * here is the activity where the maze is generated
 * at the beginning random values for the maze are generated
 * different topics will be initialized
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
    private static String BROKER = "xxx";
    private static int PORT = 1883;

    private static float mqttSensorX;
    private static float mqttSensorY;

    private boolean useMQTT = false;
    public static boolean useSmartphone = true;

    /**
     * showing the controls which the player has chosen
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
     * building the header
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
     * OnCreate method from LabActivity
     * getting the values from the MainActivity file
     * creating instance of the maze and insert it into the layout
     * open InitActivity
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
     * checking which controls has been chosen
     * depending on which was chosen, the other control is available
     * on the smartphone the function SensorEventListener will be used for movement
     * on the esp32 the ball will be moved with the function onMessageReceived 
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
                //deactivate smartphone control
                sensorManager.unregisterListener(sensorEventListener);

                mqttManager.subscribeToBroker("mpu/M01", new MqttManager.MqttMessageListener() {
                    @Override
                    public void onMessageReceived(String message) {
                        String[] mqtt_coords = message.split(",");
                        float mqtt_x = Float.parseFloat(mqtt_coords[0]);
                        float mqtt_y = Float.parseFloat(mqtt_coords[1]);

                        mqttSensorX = mqtt_x;
                        mqttSensorY = mqtt_y;

                        // update ballposition
                        mazeView.updateBallPosition(mqttSensorX, mqttSensorY);
                        mazeView.invalidate(); //drawing the maze

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
                mazeView.invalidate(); //new drawing of the maze
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //will be opened if the accuracy of the sensors has changed
        }
    };

    /**
     * OnResume opens the app again if it has been closed
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
     * pause the game and disconnected from mqtt broker
     */
    @Override
    protected void onPause() {
        super.onPause();
        mqttManager.disconnect();
        Log.d(TAG, "Broker ist nicht mehr verbunden");
    }

    //with help of chatGPT
    /**
     * In MazeGenerator the methods generateMaze und setGoal will be opened
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
         * current field maze[x][y] will be set on 1 to show it has been visited
         * beginning maze[0][0] will be set on 2
         * for the goal maze[MAZE_WIDTH - 1][MAZE_HEIGHT - 1] it will be set on 3
         * Array direction is creating and will be random mixed 
         * checking every direction in a for-loop
         * checking if the new field maze[nx][ny] has been visited yet
         * new field maze[nx][ny] and the field in the middle of the current and new field maze[(x + nx) / 2][(y + ny) / 2]
         * will be set on 1, for tagging it as visited and to remove a wall
         * generate maze will be opened recursive until the maze is completed 
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
         * SetGoal function for defining a goal
         * should be always in the corner right at the bottom
         * 
         */
        //without chatGPT
        private void setGoal(){
            int goalX = width - 1;
            int goalY = height - 1;

            //checking if field above the goal is a wall
            //if yes, field left of it should be a path
            if (maze[goalX][goalY - 1] != 1) {
                maze[goalX - 1][goalY] = 1;
            }

            //checking if field left from the goal is a wall
            //if yes, field above of it should be a path
            else if(maze[goalX - 1][goalY] != 1) {
                maze[goalX][goalY - 1] = 1;
            }

            maze[goalX][goalY] = 3;

        }

        /**
         * shuffle the array
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
    //with chatGPT

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
         * defining wall and path colors
         * ball position on 0,0
         */
        private void init() {

            String hexColor = "#9eb76b";
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
         * function for the movement of the ball
         * limit axis values based on definied range of values
         * defining speed of the ball
         * checking wall collision
         * after calculating new cell, checking if field is a path
         * at the end checking if ball is in the goal
         */
        public void updateBallPosition(float xAxis, float yAxis) {
            // limit axis values based on definied range of values
            xAxis = Math.max(Math.min(xAxis, 95.0f), -85.0f);
            yAxis = Math.max(Math.min(yAxis, 2.0f), -2.0f);

            //defining speed of the ball
            float acceleration = 0.2f;
            float newBallX = ballX - xAxis * acceleration;
            float newBallY = ballY + yAxis * acceleration;

            // checking if new position is a wall collision
            if (!checkWallCollision(ballX, ballY, newBallX, newBallY)) {
                // calculating new position of the cell
                int newCellX = Math.round(newBallX);
                int newCellY = Math.round(newBallY);

                // after calculating new cell, checking if field is a path and in the maze
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

            // checking if ball is in the goal
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
         * collision check
         * checking if ball is moving horizontal, then left and then right
         * checking if ball is moving , then top and then bottom
         */
        private boolean checkWallCollision(float startX, float startY, float endX, float endY) {
            int cellX1 = Math.round(startX);
            int cellY1 = Math.round(startY);
            int cellX2 = Math.round(endX);
            int cellY2 = Math.round(endY);

            // checking if ball is moving horizontal
            if (cellY1 == cellY2) {
                // check left movement
                if (endX < startX) {
                    for (int x = cellX2; x <= cellX1; x++) {
                        if (x >= 0 && maze[x][cellY2] == 0) {
                            return true;
                        }
                    }
                }
                // check right movement
                else if (endX > startX) {
                    for (int x = cellX1; x <= cellX2; x++) {
                        if (x < MAZE_WIDTH && maze[x][cellY2] == 0) {
                            return true;
                        }
                    }
                }
            }
            // checking if ball is moving vertikal
            else if (cellX1 == cellX2) {
                // check top movement
                if (endY < startY) {
                    for (int y = cellY2; y <= cellY1; y++) {
                        if (y >= 0 && maze[cellX2][y] == 0) {
                            return true;
                        }
                    }
                }
                //check bottom movement
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
         * OnDraw function
         * drawing maze on canvas
         * drawing the ball
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

            //drawing ball
            float ballRadius = cellSize / 2f;
            float ballCenterX = ballX * cellSize + ballRadius;
            float ballCenterY = ballY * cellSize + ballRadius;
            Paint ballPaint = new Paint();
            ballPaint.setColor(Color.BLUE);
            canvas.drawCircle(ballCenterX, ballCenterY, ballRadius, ballPaint);

            invalidate();
        }
    }
    //with help of chatgpt

    /**
     * will be opened if ball reached the goal
     * sending to mqtt a finish topic
     * playing a sound
     * saving endTime for leaderboard
     * at the end openBestenlisteActivity() will be opened for leaderboard 
     */
    public void goalfunction() {
        mqttManager.publishToBroker(topicFinish, msgFinish);

        //from a book chapter 14 Multimedia
        MediaPlayer mediaPlayer = MediaPlayer.create(LabActivity.this,R.raw.winsound);
        mediaPlayer.start();
        //from a book

        zeitBestenliste = endZeit;

        String[] timeArr = zeitBestenliste.split(" ");
        finalTime = timeArr[1];

        Log.d(TAG, "Zeit Bestenliste: " + finalTime);
        openBestenlisteActivity();
    }

    /**
     * open leaderboard
     */
    public void openBestenlisteActivity() {
        Intent intent = new Intent(this, BestenlisteActivity.class);
        startActivity(intent);
    }
}
