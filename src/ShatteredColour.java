import javafx.application.Application;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Random;

public class ShatteredColour extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    private boolean result = false;
    private final int timeout = 5;
    private final int lineBound = 8; // stack overflow problems from 7/8 upwards
    private final int circleBound = lineBound * 2;
    private final int sizeBound = 500;
    private final Pane canvas = new Pane();
    private final ArrayList<PairedCircle> circles = new ArrayList<>();
    private final ArrayList<Line> lines = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Shattered Colour");

        // board
        Rectangle board = new Rectangle();
        board.setFill(Color.WHITE);
        board.setHeight(sizeBound);
        board.setWidth(sizeBound);

        // canvas
        canvas.setMaxWidth(sizeBound);
        canvas.setMaxHeight(sizeBound);
        canvas.getChildren().add(board);

        // lines
        createLines();

        // title
        Label title = new Label("Shattered Colour");
        title.setFont(new Font(40));

        // buttons
        Button reset = new Button("Reset Points");
        Button clear = new Button("Clear Points");

        // button functions
        reset.setOnMouseClicked(e -> createLines());
        clear.setOnMouseClicked(e -> clearCircles());

        // top menu
        HBox topMenu = new HBox();
        topMenu.setSpacing(10);
        topMenu.getChildren().add(title);
        topMenu.getChildren().add(reset);
        topMenu.getChildren().add(clear);

        // main pane
        BorderPane mainPane = new BorderPane();
        mainPane.setPadding(new Insets(10, 10, 10, 10));
        mainPane.setPrefSize(sizeBound + 100, sizeBound + 100);
        mainPane.setTop(topMenu);
        mainPane.setCenter(canvas);

        // scene
        Scene scene = new Scene(mainPane);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.show();
    }

    // attempts to create a valid set of lines
    public void createLines() {
        LocalDateTime timeoutTime = LocalDateTime.now().plusSeconds(timeout);
        do {
            if (!LocalDateTime.now().isAfter(timeoutTime)) {
                result = createCircles();
            } else {
                break;
            }
        } while (!result);

        // displays timeout error
        if (!result) {
            Alert timeoutAlert = new Alert(Alert.AlertType.ERROR);
            timeoutAlert.setTitle("Timeout!");
            timeoutAlert.show();
        }
    }

    // clears the lines and circles
    public void clearCircles() {
        canvas.getChildren().removeAll(circles);
        circles.clear();
        canvas.getChildren().removeAll(lines);
        lines.clear();
    }

    // creates the lines and circles
    public boolean createCircles() {
        clearCircles();
        Random rand = new Random();
        ArrayList<PairedCircle> uncolouredCircles = new ArrayList<>();

        // creates circles
        for (int i = 0; i < circleBound; i++) {
            PairedCircle circle = new PairedCircle();
            circle.setFill((Color.DARKGRAY));
            circle.setRadius(5);
            circle.setCenterX(rand.nextInt(sizeBound));
            circle.setCenterY(rand.nextInt(sizeBound));
            circle.setIndex(i);

            uncolouredCircles.add(circle);
        }

        // pairs up circles
        for (PairedCircle circle1 : uncolouredCircles) {
            if (!circle1.isPaired()) {
                ArrayList<NeighbourCheck> potentials = new ArrayList<>();

                for (PairedCircle circle2 : uncolouredCircles) {
                    if (!circle2.isPaired() && circle2.getIndex() != circle1.getIndex()) {
                        double cSquared = Math.pow(Math.abs(circle2.getCenterX() - circle1.getCenterY()), 2)
                                          + Math.pow(Math.abs(circle2.getCenterY() - circle1.getCenterY()), 2);
                        potentials.add(new NeighbourCheck(circle2.getIndex(), Math.sqrt(cSquared)));
                    }
                }
                Comparator<Object> byDistance = Comparator.comparingInt(neighbourCheck -> (int) ((NeighbourCheck) neighbourCheck).getDistance());
                potentials.sort(byDistance);

                PairedCircle circle2 = uncolouredCircles.get(potentials.get(0).getIndex());
                circle1.setNeighbourIndex(circle2.getIndex());
                circle2.setNeighbourIndex(circle1.getIndex());

                circle1.setPaired(true);
                circle2.setPaired(true);
            }
        }

        // pair colouring
        for (int i = 0; i < circleBound; i++) {
            PairedCircle circle1 = uncolouredCircles.get(i);

            if (!circle1.isColoured()) {
                PairedCircle circle2 = uncolouredCircles.get(circle1.getNeighbourIndex());

                Color custom = Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
                circle1.setFill(custom);
                circle2.setFill(custom);
                circle1.setColoured(true);
                circle2.setColoured(true);

                Line line = new Line(circle1.getCenterX(), circle1.getCenterY(), circle2.getCenterX(), circle2.getCenterY());
                line.setStrokeWidth(2);
                line.setStroke(custom);
                lines.add(line);

                circles.add(circle1);
                circles.add(circle2);
            }
        }

        // creates lines
        for (Line line1 : lines) {
            for (Line line2 : lines) {
                if (line1 != line2) {
                    if (doIntersect(line1, line2)) {
                        clearCircles();
                        return false;
                    }
                }
            }
        }

        // displays the lines
        canvas.getChildren().addAll(circles);
        canvas.getChildren().addAll(lines);
        return true;
    }

    public boolean doIntersect(Line line1, Line line2) {
        // gradients
        double line1Grad = (line1.getStartY() - line1.getEndY()) / (line1.getStartX() - line1.getEndX());
        double line2Grad = (line2.getStartY() - line2.getEndY()) / (line2.getStartX() - line2.getEndX());

        // check if parallel
        if (line1Grad == line2Grad) {
            return false;
        }

        // y intercepts
        double line1Intercept = line1.getStartY() - (line1Grad * line1.getStartX());
        double line2Intercept = line2.getStartY() - (line2Grad * line2.getStartX());

        // x and y
        double x = (line1Intercept - line2Intercept) / (line2Grad - line1Grad);
        double y = (line1Grad * x) + line1Intercept;

        // check if segments intersect
        if (checkIntersect(line1, x, y)) return false;
        if (checkIntersect(line2, x, y)) return false;
        return true;
    }

    private boolean checkIntersect(Line line2, double x, double y) {
        if (line2.getStartX() > line2.getEndX()) {
            if (x > line2.getStartX() || x < line2.getEndX()) {
                return true;
            }
        } else {
            if (x < line2.getStartX() || x > line2.getEndX()) {
                return true;
            }
        }
        if (line2.getStartY() > line2.getEndY()) {
            if (y > line2.getStartY() || y < line2.getEndY()) {
                return true;
            }
        } else {
            if (y < line2.getStartY() || y > line2.getEndY()) {
                return true;
            }
        }
        return false;
    }
}

class PairedCircle extends Circle {
    private int index;
    private int neighbourIndex;
    private boolean paired;
    private boolean coloured;

    public PairedCircle() {
        super();
        this.index = -1;
        this.neighbourIndex = -1;
        this.paired = false;
        this.coloured = false;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getNeighbourIndex() {
        return neighbourIndex;
    }

    public void setNeighbourIndex(int neighbourIndex) {
        this.neighbourIndex = neighbourIndex;
    }

    public boolean isPaired() {
        return paired;
    }

    public void setPaired(boolean paired) {
        this.paired = paired;
    }

    public boolean isColoured() {
        return coloured;
    }

    public void setColoured(boolean coloured) {
        this.coloured = coloured;
    }
}

class NeighbourCheck {
    private int index;
    private double distance;

    public NeighbourCheck(int index, double distance) {
        this.index = index;
        this.distance = distance;
    }

    public int getIndex() {
        return index;
    }

    public double getDistance() {
        return distance;
    }
}
