package sample.Interfaces;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ProgressScene {
    public Scene scene;
    public Label progressLabel;
    public Label processLabel;
    public ProgressBar progressBar;
    public double progressPoints, progressPointsLimit;
    VBox layout;
    Runnable updater;

    public ProgressScene()
    {
        progressBar = new ProgressBar();
        progressLabel = new Label();
        processLabel = new Label();
        progressPoints = 0;
        layout = new VBox(10);
        layout.getChildren().addAll(progressBar, progressLabel, processLabel);
        layout.setAlignment(Pos.CENTER);
        scene = new Scene(layout);
        updater = () -> {
            double progress = progressPoints/progressPointsLimit;
            progressBar.setProgress(progress);
            String percentStr = String.format("%.2f", progress*100);
            progressLabel.setText(percentStr+"%");
        };
    }

    public void updateProgressBar()
    {
        Platform.runLater(updater);
    }

    public void increaseProgress()
    {
        progressPoints++;
        updateProgressBar();
    }
}
