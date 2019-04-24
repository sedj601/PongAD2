package sed.krappy.games.pong;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

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
    int ballXSpeed = 27;
    int ballYSpeed = 27;
    float maxBounceAngle = (float)( 5 * Math.PI / 12);
    float bounceAngle = 0;

    PongAIPlayer leftPlayer, rightPlayer;

    Thread gameThread;
    Runnable runnable;
    boolean running, newRound;

    Handler handler;
    ImageView ivBall, ivPaddleLeft, ivPaddleRight, ivUpArrow, ivDownArrow;
    Button btnNewGame;
    ConstraintLayout clMain, clField;
    Set<String> inputs;
    TextView tvCpuScore, tvPlayerScore;
    ConstraintLayout.LayoutParams layoutParams;

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

        tvCpuScore = findViewById(R.id.tvCpuScore);
        tvPlayerScore = findViewById(R.id.tvPlayerScore);

        running = false;
        newRound = false;
        inputs = new HashSet();
        
        layoutParams = (ConstraintLayout.LayoutParams)clField.getLayoutParams();
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                double next_game_tick = System.currentTimeMillis();
                int loops;

                while (running)
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




    }



    public void moveBall()
    {
        ivBall.setX((float) (ivBall.getX() + (ballXDirection * ballXSpeed * Math.cos(bounceAngle))));
        ivBall.setY((float)( ivBall.getY() + (ballYDirection * ballYSpeed * -Math.sin(bounceAngle))));
        checkPaddleCollision();
        checkWallCollision();
        checkIfScored();
    }

    public void aiPaddlesMove()
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

    private void checkPaddleCollision()
    {
        Rect rectBall = new Rect();
        ivBall.getHitRect(rectBall);
        Rect rectPaddleLeft = new Rect();
        ivPaddleLeft.getHitRect(rectPaddleLeft);
        Rect rectPaddleRight = new Rect();
        ivPaddleRight.getHitRect(rectPaddleRight);

        if(Rect.intersects(rectBall, rectPaddleRight))
        {
            float ballCurrentLocation = ivBall.getX() + ivBall.getWidth();
            float distanceBallPassedPaddle = ballCurrentLocation - ivPaddleRight.getX();
            ivBall.setX(ballCurrentLocation - (distanceBallPassedPaddle + ivBall.getWidth()));
            ballXDirection *= -1;
            double paddleIntersectY = (ivBall.getY() + (ivBall.getHeight() / 2)) - ivPaddleRight.getY();
            double relativeIntersectY = paddleIntersectY - (ivPaddleRight.getHeight() / 2);
            double normalizedRelativeIntersectionY = relativeIntersectY / (ivPaddleRight.getHeight() / 2);
            bounceAngle = (float)(normalizedRelativeIntersectionY * maxBounceAngle);

        }
        else if(Rect.intersects(rectBall, rectPaddleLeft))
        {
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
        if(ivBall.getY() <= clField.getY() + layoutParams.topMargin) {
            ballYDirection *= -1;
        }
        else if(ivBall.getY() + ivBall.getHeight() >= clField.getY() + clField.getHeight() - layoutParams.bottomMargin){
            ballYDirection *= -1;
        }
    }

    private void checkIfScored()
    {
        if(ivBall.getX() <= clField.getX() + layoutParams.leftMargin)
        {
            float boardCenterX = (clField.getWidth() + layoutParams.rightMargin) / 2;
            float boardCenterY = (clField.getHeight() + layoutParams.height) / 2;

            gameThread.interrupt();
            running = false;
            bounceAngle = 0;


            ivPaddleRight.setY(boardCenterY);
            ivPaddleLeft.setY(boardCenterY);
            ivBall.setX(boardCenterX);
            ivBall.setY(boardCenterY);

            handler.post(()-> {
                btnNewGame.setText("Next Round");
                tvCpuScore.setText(Integer.toString(Integer.parseInt(tvCpuScore.getText().toString()) + 1));
            });
        }

        if(ivBall.getX() + ivBall.getWidth() >= clField.getX() + clField.getWidth() - layoutParams.rightMargin)
        {
            float boardCenterX = (clField.getWidth() + layoutParams.rightMargin) / 2;
            float boardCenterY = (clField.getHeight() + layoutParams.height) / 2;

            gameThread.interrupt();
            running = false;
            bounceAngle = 0;

            ivBall.setX(boardCenterX);
            ivBall.setY(boardCenterY);
            ivPaddleRight.setY(boardCenterY);
            ivPaddleLeft.setY(boardCenterY);

            handler.post(()->{
                btnNewGame.setText("Next Round");
                tvPlayerScore.setText(Integer.toString(Integer.parseInt(tvPlayerScore.getText().toString()) + 1));
            });
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        btnNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                if(!running) {
                    gameThread = new Thread(runnable);
                    gameThread.start();
                    running  = true;
                    btnNewGame.setText("Stop");
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
}
