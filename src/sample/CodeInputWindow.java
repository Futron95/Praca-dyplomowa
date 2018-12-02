package sample;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CodeInputWindow
{
    public static void display(Controller ctrl)
    {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setMinWidth(300);
        window.setMinHeight(180);
        Label label = new Label("Wprowadź kod odszyfrujący:");
        TextField codeField = new TextField();
        Button okButton = new Button("Ok");
        okButton.setOnAction(e->{
            String code = codeField.getText();
            if (code.length()==20)
                ctrl.decodeImg(code);
            else
                label.setText("Poprawny kod ma 20 znaków!");
            window.close();
        });
        VBox layout = new VBox(20);
        layout.getChildren().addAll(label, codeField, okButton);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(20,20,20,20));
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }
}
