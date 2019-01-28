package sample;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CloseBox
{
    private static Boolean shouldClose;

    public static Boolean show()
    {
        shouldClose = true;
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Potwierdź zamknięcie");
        window.setWidth(320);
        window.setHeight(160);
        window.setResizable(false);
        window.setOnCloseRequest( event -> shouldClose = false);
        Label label = new Label("Czy na pewno chcesz wyjść bez zapisywania?");
        Button saveAndExitButton = new Button("Zapisz i wyjdź");
        saveAndExitButton.setOnAction( event -> {
            Controller.currentController.saveImage();
            window.close();
        });
        Button exitButton = new Button("Wyjdź");
        exitButton.setOnAction(event -> window.close());
        Button cancelButton = new Button("Anuluj");
        cancelButton.setOnAction(event -> {
            shouldClose = false;
            window.close();
        });
        VBox layout = new VBox(10);
        HBox hBox = new HBox(20);
        hBox.getChildren().addAll(saveAndExitButton, exitButton, cancelButton);
        hBox.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(label, hBox);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
        return shouldClose;
    }
}
