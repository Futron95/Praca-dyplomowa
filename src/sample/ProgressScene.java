package sample;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

public class ProgressScene {
    Scene scene;
    Label progressLabel;
    Label processLabel;
    ProgressBar progressBar;
    double progressPoints, progressPointsLimit;
    VBox layout;

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
    }

    public void updateProgressBar()
    {
        double progress = progressPoints/progressPointsLimit;
        progressBar.setProgress(progress);
        String percentStr = String.format("%.2f", progress*100);
        progressLabel.setText(percentStr+"%");
    }
}
