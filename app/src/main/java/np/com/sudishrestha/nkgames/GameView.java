package np.com.sudishrestha.nkgames;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;
import android.content.SharedPreferences; // Add this import at the top

public class GameView extends View {

    private Paint paint, scorePaint;
    private Handler handler;
    private Runnable runnable;
    private int screenWidth, screenHeight;
    private float ballX, ballY, ballRadius;
    private float ballSpeedY;
    private final float gravity = 2.5f;
    private final float offScreenMargin = 100; // Reset margin off-screen

    // Background and obstacles
    private Bitmap mountainBackground;
    private Bitmap birdObstacle, cowObstacle;
    private float backgroundX;
    private ArrayList<Obstacle> obstacles;
    private Random random;

    // Score
    private int score;
    private long lastUpdateTime;

    private int highScore = 0; // Store high score
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private boolean gameStarted = false; // Game starts only when player taps to jump

    public GameView(Context context) {
        super(context);

        // Initialize shared preferences to save the high score
        sharedPreferences = context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        // Load the high score from previous sessions
        highScore = sharedPreferences.getInt("high_score", 0);


        paint = new Paint();
        scorePaint = new Paint();
        handler = new Handler();

        // Initialize game loop
        runnable = new Runnable() {
            @Override
            public void run() {
                invalidate();
            }
        };

        // Initialize ball properties
        ballRadius = 50;
        ballSpeedY = 0;

        // Load assets
        mountainBackground = BitmapFactory.decodeResource(getResources(), R.drawable.mountain); // Mountain background image
        birdObstacle = BitmapFactory.decodeResource(getResources(), R.drawable.coronavirus); // Bird obstacle image
        cowObstacle = BitmapFactory.decodeResource(getResources(), R.drawable.cow); // Cow obstacle image

        // Background movement
        backgroundX = 0;

        // Obstacle list
        obstacles = new ArrayList<>();
        random = new Random();

        // Initialize score
        score = 0;
        lastUpdateTime = System.currentTimeMillis();

        // Setup paint for score display
        scorePaint.setColor(Color.BLACK);
        scorePaint.setTextSize(80);
        scorePaint.setTextAlign(Paint.Align.LEFT);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // Position the ball at the center initially
        ballX = screenWidth / 2;
        ballY = screenHeight / 2;

        // Resize the background to fit the screen
        mountainBackground = Bitmap.createScaledBitmap(mountainBackground, screenWidth, screenHeight, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (!gameStarted) {
            // Draw instructions or waiting screen
            paint.setColor(Color.BLACK);
            paint.setTextSize(100);
            canvas.drawText("Tap to start", screenWidth / 2 - 200, screenHeight / 2, paint);
            return; // Don't update the game until it's started
        }

        // Update ball position and speed
        ballSpeedY += gravity;
        ballY += ballSpeedY;

        // Scroll the background horizontally
        backgroundX -= 5;  // Speed of background scroll
        if (backgroundX < -screenWidth) {
            backgroundX = 0;
        }

        // Draw the background (repeat it to make the scroll continuous)
        canvas.drawBitmap(mountainBackground, backgroundX, 0, paint);
        canvas.drawBitmap(mountainBackground, backgroundX + screenWidth, 0, paint);

        // Prevent ball from falling below the screen
        if (ballY + ballRadius >= screenHeight) {
            resetGame();
            gameStarted = false; // Stop the game after reset
            return;
        }
        // Check if ball goes off the screen (above or below) with a margin
        if (ballY - ballRadius < -offScreenMargin || ballY + ballRadius > screenHeight + offScreenMargin) {
            resetGame();
        }

        // Draw the ball
        paint.setColor(Color.RED);
        canvas.drawCircle(ballX, ballY, ballRadius, paint);

        // Add random obstacles periodically
        if (random.nextInt(100) < 3) { // 3% chance to spawn an obstacle per frame
            addObstacle();
        }

        // Update and draw obstacles
        for (int i = obstacles.size() - 1; i >= 0; i--) {
            Obstacle obstacle = obstacles.get(i);
            obstacle.update();
            obstacle.draw(canvas);

            // Remove obstacles that go off the screen
            if (obstacle.x + obstacle.width < 0) {
                obstacles.remove(i);
            }

            // Check for collisions with the ball
            if (isCollision(obstacle)) {
                resetGame(); // Reset the game if collision occurs
                return;
            }
        }

        // Update score based on time
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastUpdateTime > 1000) { // Update score every second
            score++;
            lastUpdateTime = currentTime;
        }

        // Draw the score at the top of the screen
        canvas.drawText("Score: " + score, 50, 100, scorePaint);
// Draw the high score at the top right
        canvas.drawText("High Score: " + highScore, screenWidth - 500, 100, scorePaint);
        // Redraw the canvas after delay
        handler.postDelayed(runnable, 16);

        // Safely request the next frame
        if (!handler.hasCallbacks(runnable)) {
            handler.postDelayed(runnable, 16);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (!gameStarted) {
                gameStarted = true; // Start the game on first tap
                handler.postDelayed(runnable, 16); // Start the game loop
            }
            // Make the ball move upwards when the screen is tapped
            ballSpeedY = -40;
        }
        return true;
    }

    // Reset ball and score when a reset condition is met
    private void resetGame() {
        // Stop any ongoing updates while resetting
        handler.removeCallbacks(runnable);

        // Update high score if current score is greater
        if (score > highScore) {
            highScore = score;
            editor.putInt("high_score", highScore);
            editor.apply(); // Save the high score
        }

        // Reset the ball position and speed
        ballX = screenWidth / 2;
        ballY = screenHeight / 2;
        ballSpeedY = 0;

        // Reset the score
        score = 0;

        // Clear all obstacles safely
        obstacles.clear();

        // Reset game state so it waits for the player to start again
        gameStarted = false;

        // Remove the current game loop and wait for player input to restart
        handler.removeCallbacks(runnable);
    }

    // Add a random obstacle
    private void addObstacle() {
        // Randomly choose between bird and cow
        Bitmap obstacleBitmap = random.nextBoolean() ? birdObstacle : cowObstacle;
        int obstacleHeight = random.nextInt(screenHeight - 200) + 100; // Random height
        Obstacle obstacle = new Obstacle(obstacleBitmap, screenWidth, obstacleHeight);
        obstacles.add(obstacle);
    }

    // Check if the ball collides with an obstacle
    private boolean isCollision(Obstacle obstacle) {
        float distX = Math.abs(ballX - (obstacle.x + obstacle.width / 2));
        float distY = Math.abs(ballY - (obstacle.y + obstacle.height / 2));

        if (distX <= (ballRadius + obstacle.width / 2) && distY <= (ballRadius + obstacle.height / 2)) {
            return true;
        }
        return false;
    }

    // Inner class for obstacles
    private class Obstacle {
        Bitmap bitmap;
        int x, y, width, height;
        int speed;

        public Obstacle(Bitmap bitmap, int startX, int startY) {
            this.bitmap = bitmap;
            this.x = startX;
            this.y = startY;
            this.width = bitmap.getWidth();
            this.height = bitmap.getHeight();
            this.speed = 10; // Speed of obstacle movement
        }

        public void update() {
            x -= speed; // Move obstacle to the left
        }

        public void draw(Canvas canvas) {
            canvas.drawBitmap(bitmap, x, y, null);
        }
    }
}