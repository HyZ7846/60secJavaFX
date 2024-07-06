import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimpleGame extends Application {
    private double playerX;
    private double playerY;
    private final double playerSpeed = 5;
    private final double playerWidth = 20; // Player size
    private final double playerHeight = 20; // Player size

    private Direction currentDirection = Direction.UP;

    private final int canvasWidth = 800;
    private final int canvasHeight = 800;

    private List<Rectangle> terrains = new ArrayList<>();
    private List<Rectangle> walls = new ArrayList<>();
    private List<Zombie> zombies = new ArrayList<>();

    private boolean gameOver = false; // Game over flag
    private boolean victory = false; // Victory flag
    private long startTime; // Start time for the timer
    private AnimationTimer gameLoop;
    private Button playAgainButton;
    private long lastZombieSpawnTime = 0; // Last zombie spawn time
    private final long zombieSpawnInterval = 10000; // 10 seconds in milliseconds
    private Text survivalTimeText;

    private final long frameTime = 1000000000 / 120; // 120 FPS

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("60 SECONDS");

        Canvas canvas = new Canvas(canvasWidth, canvasHeight);
        GraphicsContext gc = canvas.getGraphicsContext2D();

        Text titleText = new Text("60 SECONDS");
        titleText.setFont(Font.font("Comic Sans MS", FontWeight.BOLD, FontPosture.ITALIC, 60));
        titleText.setFill(Color.RED);

        Text subtitleText = new Text("by TREZ");
        subtitleText.setFont(Font.font("Impact", FontPosture.ITALIC, 30));
        subtitleText.setFill(Color.YELLOW);

        Button startButton = new Button("Start Game");
        startButton.setStyle("-fx-font-size: 20px; -fx-background-color: #0000ff; -fx-text-fill: #ffffff; -fx-border-color: #000000; -fx-border-width: 2px;");
        startButton.setOnAction(e -> startGame(gc, startButton, titleText, subtitleText));

        playAgainButton = new Button("Play Again");
        playAgainButton.setStyle("-fx-font-size: 20px; -fx-background-color: #0000ff; -fx-text-fill: #ffffff; -fx-border-color: #000000; -fx-border-width: 2px;");
        playAgainButton.setVisible(false);
        playAgainButton.setOnAction(e -> restartGame(gc));

        survivalTimeText = new Text();
        survivalTimeText.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        survivalTimeText.setFill(Color.YELLOW);
        survivalTimeText.setVisible(false);

        StackPane root = new StackPane();
        root.getChildren().addAll(canvas, titleText, subtitleText, startButton, playAgainButton, survivalTimeText);
        root.setStyle("-fx-background-color: #000000;");
        StackPane.setAlignment(titleText, javafx.geometry.Pos.TOP_CENTER);
        StackPane.setMargin(titleText, new Insets(40, 10, 10, 10));
        StackPane.setAlignment(subtitleText, javafx.geometry.Pos.TOP_CENTER);
        StackPane.setMargin(subtitleText, new Insets(120, 10, 10, 10));
        StackPane.setAlignment(startButton, javafx.geometry.Pos.CENTER);
        StackPane.setAlignment(playAgainButton, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setAlignment(survivalTimeText, javafx.geometry.Pos.BOTTOM_CENTER);
        StackPane.setMargin(survivalTimeText, new Insets(50, 10, 10, 10));

        Scene scene = new Scene(root, canvasWidth, canvasHeight);

        scene.setOnKeyPressed(this::handleKeyPress);
        scene.setOnKeyReleased(this::handleKeyRelease);

        primaryStage.setScene(scene);
        primaryStage.show();

        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= frameTime) {
                    if (!gameOver && !victory) {
                        update();
                        if (System.currentTimeMillis() - lastZombieSpawnTime >= zombieSpawnInterval) {
                            spawnNewZombie();
                            lastZombieSpawnTime = System.currentTimeMillis();
                        }
                    }
                    draw(gc);
                    if (gameOver || victory) {
                        stop();
                    }
                    lastUpdate = now;
                }
            }
        };
    }

    private void startGame(GraphicsContext gc, Button startButton, Text titleText, Text subtitleText) {
        // Hide the start button and title text
        startButton.setVisible(false);
        titleText.setVisible(false);
        subtitleText.setVisible(false);

        resetGame();
        startTime = System.currentTimeMillis(); // Initialize the start time

        gameLoop.start();
    }

    private void restartGame(GraphicsContext gc) {
        playAgainButton.setVisible(false);
        survivalTimeText.setVisible(false);
        resetGame();
        startTime = System.currentTimeMillis(); // Initialize the start time
        gameLoop.start();
    }

    private void resetGame() {
        // Reset game state
        gameOver = false;
        victory = false;
        lastZombieSpawnTime = System.currentTimeMillis();

        // Clear previous game objects
        terrains.clear();
        walls.clear();
        zombies.clear();

        // Generate boundary walls
        generateBoundaryWalls();

        // Generate random terrains and walls
        generateRandomTerrainsAndWalls();

        // Generate initial zombies
        generateRandomZombies();

        // Ensure player does not spawn inside a wall or terrain
        placePlayer();
    }

    private void generateRandomTerrainsAndWalls() {
        Random random = new Random();

        // Generate terrains
        while (terrains.size() < 20) {
            int width = random.nextInt(80 - 30) + 30;
            int height = random.nextInt(80 - 30) + 30;
            int x = random.nextInt(canvasWidth - width - 30 * 2) + 30;
            int y = random.nextInt(canvasHeight - height - 30 * 2) + 30;

            Rectangle newTerrain = new Rectangle(x, y, width, height);
            if (isNonOverlapping(newTerrain, terrains) && isNonOverlapping(newTerrain, walls)) {
                terrains.add(newTerrain);
            }
        }

        // Generate walls
        while (walls.size() < 10) {
            int width = random.nextInt(80 - 30) + 30;
            int height = random.nextInt(80 - 30) + 30;
            if (random.nextBoolean()) { // Randomly decide to create a longer wall
                if (random.nextBoolean()) {
                    width = 30;
                } else {
                    height = 30;
                }
            }
            int x = random.nextInt(canvasWidth - width - 30 * 2) + 30;
            int y = random.nextInt(canvasHeight - height - 30 * 2) + 30;

            Rectangle newWall = new Rectangle(x, y, width, height);
            if (isNonOverlapping(newWall, terrains) && isNonOverlapping(newWall, walls)) {
                walls.add(newWall);
            }
        }
    }

    private void generateBoundaryWalls() {
        // Top wall
        walls.add(new Rectangle(0, 0, canvasWidth, 30));
        // Bottom wall
        walls.add(new Rectangle(0, canvasHeight - 30, canvasWidth, 30));
        // Left wall
        walls.add(new Rectangle(0, 0, 30, canvasHeight));
        // Right wall
        walls.add(new Rectangle(canvasWidth - 30, 0, 30, canvasHeight));
    }

    private void generateRandomZombies() {
        for (int i = 0; i < 3; i++) {
            spawnNewZombie();
        }
    }

    private boolean isNonOverlapping(Rectangle newRect, List<?> existingRects) {
        for (Object obj : existingRects) {
            Rectangle rect;
            if (obj instanceof Rectangle) {
                rect = (Rectangle) obj;
            } else if (obj instanceof Zombie) {
                rect = ((Zombie) obj).getBounds();
            } else {
                continue;
            }

            if (newRect.getBoundsInLocal().intersects(rect.getBoundsInLocal())) {
                return false;
            }
        }
        return true;
    }

    private void placePlayer() {
        Random random = new Random();
        boolean validPosition;
        do {
            playerX = random.nextInt(canvasWidth - (int) playerWidth - 30 * 2) + 30;
            playerY = random.nextInt(canvasHeight - (int) playerHeight - 30 * 2) + 30;
            Rectangle playerRect = new Rectangle(playerX, playerY, playerWidth, playerHeight);
            validPosition = walls.stream().noneMatch(wall -> wall.getBoundsInLocal().intersects(playerRect.getBoundsInLocal()))
                    && terrains.stream().noneMatch(terrain -> terrain.getBoundsInLocal().intersects(playerRect.getBoundsInLocal()))
                    && zombies.stream().noneMatch(zombie -> zombie.getBounds().intersects(playerRect.getBoundsInLocal()));
        } while (!validPosition);
    }

    private void update() {
        // Store the current position
        double newX = playerX;
        double newY = playerY;

        // Update player position
        if (up) {
            newY -= playerSpeed;
            currentDirection = Direction.UP;
        }
        if (down) {
            newY += playerSpeed;
            currentDirection = Direction.DOWN;
        }
        if (left) {
            newX -= playerSpeed;
            currentDirection = Direction.LEFT;
        }
        if (right) {
            newX += playerSpeed;
            currentDirection = Direction.RIGHT;
        }

        // Prevent player from moving outside the window
        if (newX < 30) newX = 30;
        if (newX + playerWidth > canvasWidth - 30) newX = canvasWidth - playerWidth - 30;
        if (newY < 30) newY = 30;
        if (newY + playerHeight > canvasHeight - 30) newY = canvasHeight - playerHeight - 30;

        // Check for collisions with walls and terrains
        Rectangle playerRect = new Rectangle(newX, newY, playerWidth, playerHeight);
        boolean collision = walls.stream().anyMatch(wall -> wall.getBoundsInLocal().intersects(playerRect.getBoundsInLocal()))
                || terrains.stream().anyMatch(terrain -> terrain.getBoundsInLocal().intersects(playerRect.getBoundsInLocal()));

        // Update player position if no collision
        if (!collision) {
            playerX = newX;
            playerY = newY;
        }

        // Check for collision with zombies
        if (zombies.stream().anyMatch(zombie -> zombie.getBounds().intersects(playerRect.getBoundsInLocal()))) {
            gameOver = true;
        }

        // Update zombies
        updateZombies();

        // Check for victory
        long elapsedTime = System.currentTimeMillis() - startTime;
        if (elapsedTime >= 60000 && !gameOver) {
            victory = true;
        }
    }

    private void updateZombies() {
        for (Zombie zombie : zombies) {
            zombie.update(playerX, playerY, walls, terrains, zombies);

            // Check if zombie is stuck and hasn't moved for a certain amount of time
            if (zombie.isStuck()) {
                zombies.remove(zombie); // Remove the stuck zombie
                spawnNewZombie(); // Spawn a new zombie
                break;
            }
        }
    }

    private void spawnNewZombie() {
        Random random = new Random();
        boolean validPosition;
        double x, y;

        do {
            x = random.nextInt(canvasWidth - (int) playerWidth - 30 * 2) + 30;
            y = random.nextInt(canvasHeight - (int) playerHeight - 30 * 2) + 30;
            Rectangle zombieRect = new Rectangle(x, y, playerWidth, playerHeight);
            validPosition = isNonOverlapping(zombieRect, terrains) && isNonOverlapping(zombieRect, walls) && isNonOverlapping(zombieRect, zombies)
                    && !isNearPlayer(zombieRect);
        } while (!validPosition);

        zombies.add(new Zombie(x, y));
    }

    private boolean isNearPlayer(Rectangle rect) {
        double buffer = 50;
        Rectangle playerRect = new Rectangle(playerX - buffer, playerY - buffer, playerWidth + buffer * 2, playerHeight + buffer * 2);
        return playerRect.getBoundsInLocal().intersects(rect.getBoundsInLocal());
    }

    private void draw(GraphicsContext gc) {
        // Clear the canvas
        gc.clearRect(0, 0, canvasWidth, canvasHeight);

        // Draw the player
        gc.setFill(Color.RED);
        gc.fillRect(playerX, playerY, playerWidth, playerHeight);

        // Draw the walls
        gc.setFill(Color.ORANGE);
        for (Rectangle wall : walls) {
            gc.fillRect(wall.getX(), wall.getY(), wall.getWidth(), wall.getHeight());
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2);
            for (int i = 0; i < wall.getWidth(); i += 20) {
                for (int j = 0; j < wall.getHeight(); j += 10) {
                    gc.strokeRect(wall.getX() + i, wall.getY() + j, 20, 10);
                }
            }
        }

        // Draw the terrains
        gc.setFill(Color.LIGHTGREEN);
        for (Rectangle terrain : terrains) {
            gc.fillRect(terrain.getX(), terrain.getY(), terrain.getWidth(), terrain.getHeight());
        }

        // Draw zombies
        gc.setFill(Color.AQUAMARINE);
        for (Zombie zombie : zombies) {
            gc.fillRect(zombie.getX(), zombie.getY(), zombie.getWidth(), zombie.getHeight());
        }

        // Draw timer
        if (!gameOver && !victory) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingTime = (60000 - elapsedTime) / 1000; // Convert to seconds
            gc.setFill(Color.WHITE);
            gc.setFont(new Font("Arial", 40)); // Increased font size for visibility
            gc.fillText("Time: " + remainingTime, 10, 40);
        }

        // Draw "Game Over" text if game is over
        if (gameOver) {
            gc.setFill(Color.RED);
            gc.setFont(new Font("Arial", 50));
            gc.fillText("Game Over", canvasWidth / 2 - 150, canvasHeight / 2);

            long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
            survivalTimeText.setText("You have survived " + elapsedTime + " sec");
            survivalTimeText.setVisible(true);
            playAgainButton.setVisible(true);
            StackPane.setMargin(playAgainButton, new Insets(0, 0, 100, 0)); // Adjust margin
        }

        // Draw "Victory" text if player wins
        if (victory) {
            gc.setFill(Color.GREEN);
            gc.setFont(new Font("Arial", 50));
            gc.fillText("Victory", canvasWidth / 2 - 150, canvasHeight / 2);

            survivalTimeText.setText("You have survived 60 sec!!!");
            survivalTimeText.setVisible(true);
            playAgainButton.setVisible(true);
            StackPane.setMargin(playAgainButton, new Insets(0, 0, 100, 0)); // Adjust margin
        }
    }

    private boolean up, down, left, right;

    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case W -> up = true;
            case S -> down = true;
            case A -> left = true;
            case D -> right = true;
        }
    }

    private void handleKeyRelease(KeyEvent event) {
        switch (event.getCode()) {
            case W -> up = false;
            case S -> down = false;
            case A -> left = false;
            case D -> right = false;
        }
    }

    private enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private class Zombie {
        private double x, y;
        private final double width = playerWidth;
        private final double height = playerHeight;
        private final double speed = 3;
        private Direction direction;
        private Random random = new Random();
        private double lastX, lastY;
        private long stuckTime;

        Zombie(double x, double y) {
            this.x = x;
            this.y = y;
            this.direction = Direction.values()[random.nextInt(Direction.values().length)];
            this.lastX = x;
            this.lastY = y;
            this.stuckTime = System.currentTimeMillis();
        }

        void update(double playerX, double playerY, List<Rectangle> walls, List<Rectangle> terrains, List<Zombie> zombies) {
            if (random.nextDouble() < 0.01) {
                this.direction = Direction.values()[random.nextInt(Direction.values().length)];
            }

            double newX = x;
            double newY = y;

            // Seek behavior
            if (random.nextDouble() < 0.5) {
                if (Math.abs(playerX - x) > Math.abs(playerY - y)) {
                    if (playerX > x) {
                        newX += speed;
                        direction = Direction.RIGHT;
                    } else {
                        newX -= speed;
                        direction = Direction.LEFT;
                    }
                } else {
                    if (playerY > y) {
                        newY += speed;
                        direction = Direction.DOWN;
                    } else {
                        newY -= speed;
                        direction = Direction.UP;
                    }
                }
            } else { // Wander behavior
                switch (direction) {
                    case UP -> newY -= speed;
                    case DOWN -> newY += speed;
                    case LEFT -> newX -= speed;
                    case RIGHT -> newX += speed;
                }
            }

            // Prevent zombie from moving outside the window
            if (newX < 30) newX = 30;
            if (newX + width > canvasWidth - 30) newX = canvasWidth - width - 30;
            if (newY < 30) newY = 30;
            if (newY + height > canvasHeight - 30) newY = canvasHeight - height - 30;

            // Check for collisions with walls, terrains, and other zombies
            Rectangle zombieRect = new Rectangle(newX, newY, width, height);
            boolean collision = walls.stream().anyMatch(wall -> wall.getBoundsInLocal().intersects(zombieRect.getBoundsInLocal()))
                    || terrains.stream().anyMatch(terrain -> terrain.getBoundsInLocal().intersects(zombieRect.getBoundsInLocal()))
                    || zombies.stream().anyMatch(z -> z != this && z.getBounds().intersects(zombieRect.getBoundsInLocal()));

            // Update zombie position if no collision
            if (!collision) {
                x = newX;
                y = newY;
                stuckTime = System.currentTimeMillis(); // Reset stuck time
            }
        }

        boolean isStuck() {
            long currentTime = System.currentTimeMillis();
            return currentTime - stuckTime > 1500; // Check if stuck for more than 2 seconds
        }

        double getX() {
            return x;
        }

        double getY() {
            return y;
        }

        double getWidth() {
            return width;
        }

        double getHeight() {
            return height;
        }

        Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
    }
}
