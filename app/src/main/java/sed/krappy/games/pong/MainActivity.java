package sed.krappy.games.pong;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    double interpolation = 0;
    int TICKS_PER_SECONDS = 50;
    int SKIP_TICKS = 1000 / TICKS_PER_SECONDS;
    int MAX_FRAME_SKIPS = 10;

    int ballXDirection = 1;
    int ballYDirection = 1;
    int ballXSpeed = 20;
    int ballYSpeed = 20;
    float maxBounceAngle = (float)( 5 * Math.PI / 12);
    float bounceAngle = 0;

    PongAIPlayer leftPlayer, rightPlayer;

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
        getSupportActionBar().hide();


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
                double next_game_tick = System.currentTimeMillis();
                int loops;

                while (true)
                {
                    loops = 0;
                    while(System.currentTimeMillis() > next_game_tick && loops < MAX_FRAME_SKIPS)
                    {
                        moveBall();
                        movePaddles();
                        aiPaddlesMove();
                        checkIfScored();

                        next_game_tick += SKIP_TICKS;
                        loops++;
                    }

                    interpolation = System.currentTimeMillis() + SKIP_TICKS - next_game_tick  / (double)SKIP_TICKS;
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

    public void checkIfScored()
    {

    }

    public void moveBall()
    {
        ivBall.setX((float) (ivBall.getX() + (ballXDirection * ballXSpeed * Math.cos(bounceAngle))));
        ivBall.setY((float)( ivBall.getY() + (ballYDirection * ballYSpeed * -Math.sin(bounceAngle))));
        //Log.d("MainActivity",ivBall.getX() + " - " + ivBall.getY() + " - " + (clField.getY() + clField.getHeight()));
        checkPaddleCollision();
        checkWallCollision();
    }

    public void aiPaddlesMove()
    {
        if (ballXDirection < 0) {
            //leftPlayer.MovePaddle(true, ivBall.getX(), ivBall.getY() + (ivBall.getHeight() / 2));
            rightPlayer.MovePaddle(false,  ivBall.getX(), ivBall.getY() + (ivBall.getHeight() / 2));
        }
        else {
           // leftPlayer.MovePaddle(false,  ivBall.getX(), ivBall.getY() + (ivBall.getHeight() / 2));
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

    private void checkPaddleCollision()
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
            Log.d("MainActivity", "paddle right: " + ivBall.getX() + " :: " + ivPaddleRight.getX());
            //ivBall.setX(ivBall.getX() + ivBall.getWidth() - (ivBall.getX() + ivBall.getWidth() - ivPaddleRight.getX()));
            ballXDirection *= -1;
            double paddleIntersectY = (ivBall.getY() + (ivBall.getHeight() / 2)) - ivPaddleRight.getY();
            double relativeIntersectY = paddleIntersectY - (ivPaddleRight.getHeight() / 2);
            double normalizedRelativeIntersectionY = relativeIntersectY / (ivPaddleRight.getHeight() / 2);
            bounceAngle = (float)(normalizedRelativeIntersectionY * maxBounceAngle);

        }
        else if(Rect.intersects(rectBall, rectPaddleLeft))
        {
            //Log.d("MainActivity", "paddle left: " + ivBall.getX() + " :: " + (ivPaddleLeft.getX() + ivPaddleLeft.getWidth()));
            ivBall.setX(ivBall.getX() + (ivPaddleLeft.getX() + ivPaddleLeft.getWidth() - ivBall.getX()));
            ballXDirection *= -1;
            double paddleIntersectY = (ivBall.getY() + (ivBall.getHeight() / 2)) - ivPaddleLeft.getY();
            double relativeIntersectY = paddleIntersectY - (ivPaddleLeft.getHeight() / 2);
            double normalizedRelativeIntersectionY = relativeIntersectY / (ivPaddleLeft.getHeight() / 2);
            bounceAngle = (float)(normalizedRelativeIntersectionY * maxBounceAngle);
        }
    }

    private void checkWallCollision()
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
}
