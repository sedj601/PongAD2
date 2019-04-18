package sed.krappy.games.pong;

import android.widget.ImageView;

import java.util.concurrent.ThreadLocalRandom;

/**
 *
 * @author blj0011
 */
public class PongAIPlayer
{
    private final ImageView paddle;
    private boolean isHitLocationCalculated = false;
    private double paddleHitLocation;
    private int ySpeed;

    public PongAIPlayer(ImageView paddle)
    {
        this.paddle = paddle;
        ySpeed = 12;
    }

    public void MovePaddle(boolean isBallComingAtPaddle, double ballLocationX, double ballLocationY)
    {

        if (isBallComingAtPaddle) {

            if (!isHitLocationCalculated) {
                paddleHitLocation = this.paddle.getHeight() * ThreadLocalRandom.current().nextDouble(.25,.75);
                isHitLocationCalculated = true;
            }

            if (paddle.getY() + paddleHitLocation > ballLocationY) {
                this.paddle.setY(this.paddle.getY() - ySpeed);
            }
            else if (paddle.getY() + paddle.getHeight() - paddleHitLocation < ballLocationY) {
                this.paddle.setY(this.paddle.getY() + ySpeed);
            }
        }
        else {
            isHitLocationCalculated = false;
        }
    }

}
