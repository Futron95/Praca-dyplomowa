package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.opencv.core.Core;
import sample.Interfaces.CloseBox;


public class Main extends Application {

    static{
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        //Controller.loadButtonImages();
        Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("Edytor obrazów");
        primaryStage.setScene(new Scene(root));
        primaryStage.setOnCloseRequest( event -> {
            if (Controller.currentController.unsavedChanges == true)
                if (CloseBox.show() != true)
                    event.consume();
        });
        Controller.currentController.setUp();
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
