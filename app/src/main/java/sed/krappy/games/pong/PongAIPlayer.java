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

    public PongAIPlayer(ImageView paddle)
    {
        this.paddle = paddle;
    }

    public void MovePaddle(boolean isBallComingAtPaddle, double ballLocationX, double ballLocationY)
    {

        if (isBallComingAtPaddle) {

            if (!isHitLocationCalculated) {
                paddleHitLocation = this.paddle.getHeight() * ThreadLocalRandom.current().nextDouble(.40,.61);
                isHitLocationCalculated = true;
                System.out.println(paddleHitLocation);
            }

            if (paddle.getY() + paddleHitLocation > ballLocationY) {
                this.paddle.setY(this.paddle.getY() - 10);
            }
            else if (paddle.getY() + paddle.getHeight() - paddleHitLocation < ballLocationY) {
                this.paddle.setY(this.paddle.getY() + 10);
            }
        }
        else {
            isHitLocationCalculated = false;
        }
    }

}
