package sed.krappy.games.pong;

import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.security.Key;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity  implements View.OnKeyListener {

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


    AsyncTask asyncTask;
    Runnable runnable;
    boolean running;

    Thread gameThread;
    ImageView ivBall, ivPaddleLeft, ivPaddleRight;
    Button btnNewGame;
    ConstraintLayout clMain;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        clMain = findViewById(R.id.clMain);
        btnNewGame = findViewById(R.id.btnNewGame);
        ivBall = findViewById(R.id.ivBall);
        ivPaddleLeft = findViewById(R.id.ivPaddleLeft);
        ivPaddleRight = findViewById(R.id.ivPaddleRight);
        running = false;


        ivPaddleRight.setOnKeyListener(this);
        ivPaddleLeft.setOnKeyListener(this);
        clMain.setOnKeyListener(this);

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

                            ivBall.setX(ivBall.getX() + 5);

                            timeDiff = System.currentTimeMillis() - beginTime;
                            sleepTime = (int) ((1000 / 50) - timeDiff);
                            if (sleepTime > 0) {
                                try {
                                    Thread.sleep(sleepTime);
                                } catch (InterruptedException ex) {
                                    Log.d("MainActivity", ex.toString());
                                    //Log.d("",Integer.toString(framesSkipped));
                                    Log.d("MainActivity", "Thread shutting down as it was requested to stop.");
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
                        Log.d("MainActivity", "Here");
                        gameThread = new Thread(runnable);
                        gameThread.start();
                        running  = true;
                        btnNewGame.setText("Stop");
                        Log.d("MainActivity", "New Game Pressed!");
                    }
                }
                 else if(running){
                    if(gameThread != null) {
                        gameThread.interrupt();
                        running = false;
                        btnNewGame.setText("End of Game");
                        Log.d("MainActivity", "Pause Pressed!");
                    }
                }
            }
        });


    }

    @Override
    public boolean onKey(View view, int keyCode, KeyEvent keyEvent)
    {
        Log.d("MainActivity", "keycode: " + keyCode);
        Log.d("MainActivity", "KeyEvent: " + keyEvent.getKeyCode());
        if(keyCode == KeyEvent.KEYCODE_A)
        {
            Log.d("MainActivity", "Up Pressed!");
        }

        return true;
    }
}
