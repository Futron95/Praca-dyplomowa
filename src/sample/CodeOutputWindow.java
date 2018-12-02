package sample;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CodeOutputWindow
{
    public static void display(String code)
    {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setMinWidth(300);
        window.setMinHeight(180);
        Label label = new Label("Kod odszyfrujący:");
        TextField codeField = new TextField(code);
        codeField.setEditable(false);
        Button okButton = new Button("Ok");
        okButton.setOnAction(e->window.close());
        VBox layout = new VBox(20);
        layout.getChildren().addAll(label, codeField, okButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20,20,20,20));
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }
}
