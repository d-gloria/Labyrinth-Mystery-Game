import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class LabyrinthGame {
    public static void main(String[] args) {
        GameController controller = new GameController();
        controller.startGame();
    }
}





class Player {
    private int x, y;
    private int treasuresCollected;
    private int score;

    public Player(int startX, int startY) {
        this.x = startX;
        this.y = startY;
        this.treasuresCollected = 0;
        this.score = 0;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getTreasuresCollected() { return treasuresCollected; }
    public int getScore() { return score; }

    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
    }

    public void collectTreasure() {
        treasuresCollected++;
        score += 10;
    }
}




class Labyrinth {
    private char[][] grid;
    private int rows, cols;
    private int exitX, exitY;

    public Labyrinth(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        grid = new char[rows][cols];
        generateLabyrinth();
    }

    private void generateLabyrinth() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = (Math.random() < 0.2) ? 'W' : ' ';
            }
        }
        grid[0][0] = ' ';
        exitX = rows - 1;
        exitY = cols - 1;
        grid[exitX][exitY] = 'E';

        for (int i = 0; i < rows * cols / 10; i++) {
            int tx = (int) (Math.random() * rows);
            int ty = (int) (Math.random() * cols);
            if (grid[tx][ty] == ' ') grid[tx][ty] = 'T';
        }
    }

    public boolean isWall(int x, int y) {
        return grid[x][y] == 'W';
    }

    public boolean isTreasure(int x, int y) {
        return grid[x][y] == 'T';
    }

    public boolean isExit(int x, int y) {
        return grid[x][y] == 'E';
    }

    public void collectTreasure(int x, int y) {
        if (grid[x][y] == 'T') {
            grid[x][y] = ' ';
        }
    }

    public char[][] getGrid() {
        return grid;
    }
}




class GameController {
    private Player player;
    private Labyrinth labyrinth;
    private JFrame frame;
    private JPanel gridPanel;
    private JLabel infoLabel;
    private Connection connection;

    public GameController() {
        player = new Player(0, 0);
        labyrinth = new Labyrinth(10, 10);
        connectToDatabase();
    }

    public void startGame() {
        setupGUI();
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/labyrinth_game", "root", "root1234");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Lidhja me databazen deshtoi! Kontrolloni konfigurimet.");
            System.exit(1);
        }
    }

    private void setupGUI() {
        frame = new JFrame("Loja e Labirintit");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        gridPanel = new JPanel(new GridLayout(10, 10));
        drawLabyrinth();

        JPanel controls = new JPanel();
        JButton up = new JButton("Mbrapa");
        JButton down = new JButton("Drejt");
        JButton left = new JButton("Lart");
        JButton right = new JButton("Poshte");
        JButton save = new JButton("Ruaj Lojen");
        JButton load = new JButton("Ngarko Lojen");

        up.addActionListener(e -> movePlayer(0, -1));
        down.addActionListener(e -> movePlayer(0, 1));
        left.addActionListener(e -> movePlayer(-1, 0));
        right.addActionListener(e -> movePlayer(1, 0));
        save.addActionListener(e -> saveGame());
        load.addActionListener(e -> loadGame());

        controls.add(up);
        controls.add(down);
        controls.add(left);
        controls.add(right);
        controls.add(save);
        controls.add(load);

        infoLabel = new JLabel("Pike: 0 | Thesare: 0");

        frame.add(gridPanel, BorderLayout.CENTER);
        frame.add(controls, BorderLayout.SOUTH);
        frame.add(infoLabel, BorderLayout.NORTH);

        frame.setSize(600, 600);
        frame.setVisible(true);
    }

    private void drawLabyrinth() {
        gridPanel.removeAll();
        char[][] grid = labyrinth.getGrid();

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                JLabel cell = new JLabel("", SwingConstants.CENTER);
                cell.setOpaque(true);
                if (player.getX() == i && player.getY() == j) {
                    cell.setBackground(Color.BLUE);
                } else if (grid[i][j] == 'W') {
                    cell.setBackground(Color.BLACK);
                } else if (grid[i][j] == 'T') {
                    cell.setBackground(Color.YELLOW);
                } else if (grid[i][j] == 'E') {
                    cell.setBackground(Color.GREEN);
                } else {
                    cell.setBackground(Color.WHITE);
                }
                gridPanel.add(cell);
            }
        }

        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private void movePlayer(int dx, int dy) {
        int newX = player.getX() + dx;
        int newY = player.getY() + dy;

        if (newX < 0 || newY < 0 || newX >= 10 || newY >= 10 || labyrinth.isWall(newX, newY)) {
            JOptionPane.showMessageDialog(frame, "Levizje e pavlefshme!");
            return;
        }

        player.move(dx, dy);

        if (labyrinth.isTreasure(newX, newY)) {
            player.collectTreasure();
            labyrinth.collectTreasure(newX, newY);
        }

        if (labyrinth.isExit(newX, newY)) {
            JOptionPane.showMessageDialog(frame, "Urime! Ju gjetet daljen!");
            System.exit(0);
        }

        infoLabel.setText("Pike: " + player.getScore() + " | Thesare: " + player.getTreasuresCollected());
        drawLabyrinth();
    }

    private void saveGame() {
        try {
            System.out.println("Saving: " + player.getX() + ", " + player.getY() + ", " + player.getTreasuresCollected() + ", " + player.getScore());
            String query = "REPLACE INTO game_state (player_x, player_y, treasures, score) VALUES (?, ?, ?, ?)";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, player.getX());
            statement.setInt(2, player.getY());
            statement.setInt(3, player.getTreasuresCollected());
            statement.setInt(4, player.getScore());
            statement.executeUpdate();
            JOptionPane.showMessageDialog(frame, "Loja u ruajt me sukses!");
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Ruajtja e lojes deshtoi. Provoni perseri.");
        }
    }

    private void loadGame() {
        try {
            String query = "SELECT * FROM game_state LIMIT 1";
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            if (resultSet.next()) {
                int x = resultSet.getInt("player_x");
                int y = resultSet.getInt("player_y");
                int treasures = resultSet.getInt("treasures");
                int score = resultSet.getInt("score");

                player = new Player(x, y);
                infoLabel.setText("Pike: " + score + " | Thesare: " + treasures);
                drawLabyrinth();
            } else {
                JOptionPane.showMessageDialog(frame, "Nuk ka loje te ruajtur.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Ngarkimi i lojes deshtoi. Provoni perseri.");
        }
    }
}
