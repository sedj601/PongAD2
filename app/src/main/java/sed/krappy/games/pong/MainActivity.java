package sed.krappy.games.pong;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.security.Key;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    // desired fps
    private final static int    MAX_FPS = 50;
    // maximum number of frames to be skipped
    private final static int    MAX_FRAME_SKIPS = 5;
    // the frame period
    private final static int    FRAME_PERIOD = 1000 / MAX_FPS;
    // Stuff for stats */
    private DecimalFormat df = new DecimalFormat("0.##");  // 2 dp
    // we'll be reading the stats every second
    private final static int    STAT_INTERVAL = 1000; //ms
    // the average will be calculated by storing
    // the last n FPSs
    private final static int    FPS_HISTORY_NR = 10;
    // last time the status was stored
    private long lastStatusStore = 0;
    // the status time counter
    private long statusIntervalTimer    = 0l;
    // number of frames skipped since the game started
    private long totalFramesSkipped         = 0l;
    // number of frames skipped in a store cycle (1 sec)
    private long framesSkippedPerStatCycle  = 0l;

    // number of rendered frames in an interval
    private int frameCountPerStatCycle = 0;
    private long totalFrameCount = 0l;
    // the last FPS values
    private double  fpsStore[];
    // the number of times the stat has been read
    private long    statsCount = 0;
    // the average FPS since the game started
    private double  averageFps = 0.0;

    int ballXDirection = 1;
    int ballYDirection = 1;
    int ballXSpeed = 20;
    int ballYSpeed = 20;
    float maxBounceAngle = (float)( 5 * Math.PI / 12);
    float bounceAngle = 0;

    PongAIPlayer leftPlayer, rightPlayer;

    AsyncTask asyncTask;
    Runnable runnable;
    boolean running;

    Thread gameThread;
    ImageView ivBall, ivPaddleLeft, ivPaddleRight, ivUpArrow, ivDownArrow;
    Button btnNewGame;
    ConstraintLayout clMain, clField;
    Set<String> inputs;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        clMain = findViewById(R.id.clMain);
        clField = findViewById(R.id.clField);
        btnNewGame = findViewById(R.id.btnNewGame);
        ivBall = findViewById(R.id.ivBall);
        ivPaddleLeft = findViewById(R.id.ivPaddleLeft);
        ivPaddleRight = findViewById(R.id.ivPaddleRight);
        ivUpArrow = findViewById(R.id.ivUpArrow);
        ivDownArrow = findViewById(R.id.ivDownArrow);

        leftPlayer = new PongAIPlayer(ivPaddleLeft);
        rightPlayer = new PongAIPlayer(ivPaddleRight);

        running = false;
        inputs = new HashSet();

        runnable = new Runnable() {
            @Override
            public void run() {
                long beginTime;
                long timeDiff;
                int sleepTime = 0;
                int framesSkipped;
                synchronized (this) {
                    try {
                        while (running) {
                            beginTime = System.currentTimeMillis();
                            framesSkipped = 0;

                            moveBall();
                            //movePaddles();
                            aiPaddlesMove();

                            timeDiff = System.currentTimeMillis() - beginTime;
                            sleepTime = (int) ((1000 / 50) - timeDiff);
                            if (sleepTime > 0) {
                                try {
                                    Thread.sleep(sleepTime);
                                } catch (InterruptedException ex) {
                                    //Log.d("MainActivity", ex.toString());
                                    //Log.d("",Integer.toString(framesSkipped));
                                    //Log.d("MainActivity", "Thread shutting down as it was requested to stop.");
                                }
//                        finally {
//                            gameThread = null;
//                        }
                            }
                            if (!running) {
                                this.wait();
                            }

                            while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
                                sleepTime += FRAME_PERIOD;
                                framesSkipped++;
                            }

                            if (framesSkipped > 0) {
                                Log.d("MainActivity", Integer.toString(framesSkipped));
                            }

                            framesSkippedPerStatCycle += framesSkipped;
                        }
                    }catch (InterruptedException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        };



        btnNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!running) {
                    if(gameThread == null) {
                        gameThread = new Thread(runnable);
                        gameThread.start();
                        running  = true;
                        btnNewGame.setText("Stop");
                    }
                }
                 else if(running){
                    if(gameThread != null) {
                        gameThread.interrupt();
                        running = false;
                        btnNewGame.setText("End of Game");
                    }
                }
            }
        });

        ivUpArrow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent arg1) {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
                    inputs.add("UP_ARROW_PRESSED");
                }
                else if(arg1.getAction() == MotionEvent.ACTION_UP){
                    inputs.remove("UP_ARROW_PRESSED");
                }

                return true;
            }
        });

        ivDownArrow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent arg1) {
                if (arg1.getAction() == MotionEvent.ACTION_DOWN) {
                    inputs.add("DOWN_ARROW_PRESSED");
                }
                else if(arg1.getAction() == MotionEvent.ACTION_UP){
                    inputs.remove("DOWN_ARROW_PRESSED");
                }

                return true;
            }
        });

    }

    private void aiPaddlesMove()
    {
        if (ballXDirection < 0) {
            leftPlayer.MovePaddle(true, ivBall.getX(), ivBall.getY() + (ivBall.getHeight() / 2));
            rightPlayer.MovePaddle(false,  ivBall.getX(), ivBall.getY() + (ivBall.getHeight() / 2));
        }
        else {
            leftPlayer.MovePaddle(false,  ivBall.getX(), ivBall.getY() + (ivBall.getHeight() / 2));
            rightPlayer.MovePaddle(true,  ivBall.getX(), ivBall.getY() + (ivBall.getHeight() / 2));
        }
    }

    public void movePaddles()
    {
        if(inputs.contains("UP_ARROW_PRESSED")){
            ivPaddleLeft.setY(ivPaddleLeft.getY() - 10);
        }
        else if(inputs.contains("DOWN_ARROW_PRESSED")){
            ivPaddleLeft.setY(ivPaddleLeft.getY() + 10);
        }


    }

    public void checkPaddleCollision()
    {
        //Log.d("MainActivity",clField.getWidth() + "");
        Rect rectBall = new Rect();
        ivBall.getHitRect(rectBall);
        Rect rectPaddleLeft = new Rect();
        ivPaddleLeft.getHitRect(rectPaddleLeft);
        Rect rectPaddleRight = new Rect();
        ivPaddleRight.getHitRect(rectPaddleRight);

        if(Rect.intersects(rectBall, rectPaddleRight))
        {
            ballXDirection *= -1;
            double paddleIntersectY = (ivBall.getY() + (ivBall.getHeight() / 2)) - ivPaddleRight.getY();
            double relativeIntersectY = paddleIntersectY - (ivPaddleRight.getHeight() / 2);
            double normalizedRelativeIntersectionY = relativeIntersectY / (ivPaddleRight.getHeight() / 2);
            bounceAngle = (float)(normalizedRelativeIntersectionY * maxBounceAngle);

        }
        else if(Rect.intersects(rectBall, rectPaddleLeft))
        {
            ballXDirection *= -1;
            double paddleIntersectY = (ivBall.getY() + (ivBall.getHeight() / 2)) - ivPaddleLeft.getY();
            double relativeIntersectY = paddleIntersectY - (ivPaddleLeft.getHeight() / 2);
            double normalizedRelativeIntersectionY = relativeIntersectY / (ivPaddleLeft.getHeight() / 2);
            bounceAngle = (float)(normalizedRelativeIntersectionY * maxBounceAngle);
        }
    }

    public void checkWallCollision()
    {
        if(ivBall.getY() <= clField.getY()) {
            ballYDirection *= -1;
        }
        else if(ivBall.getY() + ivBall.getHeight() >= clField.getY() + clField.getHeight()){
            ballYDirection *= -1;
        }
        else if(ivBall.getX() <= ivPaddleLeft.getX())
        {
            //Todo player score!
            gameThread.interrupt();
        }
        else if(ivBall.getX() + ivBall.getWidth() >= ivPaddleRight.getX() + ivPaddleRight.getWidth()){
            //Todo player score!
            gameThread.interrupt();
        }
    }

    public void moveBall()
    {
        ivBall.setX((float) (ivBall.getX() + (ballXDirection * ballXSpeed * Math.cos(bounceAngle))));
        ivBall.setY((float)( ivBall.getY() + (ballYDirection * ballYSpeed * -Math.sin(bounceAngle))));
        Log.d("MainActivity",ivBall.getX() + " - " + ivBall.getY() + " - " + (clField.getY() + clField.getHeight()));
        checkPaddleCollision();
        checkWallCollision();
    }

}
