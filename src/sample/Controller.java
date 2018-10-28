package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;

public class Controller {
    @FXML
    private Canvas canvas;
    private Image image;
    private GraphicsContext gc;

    public void FileChooseAction(ActionEvent event)
    {
        FileChooser fc = new FileChooser();
        File selectedFile = fc.showOpenDialog(null);

        if(selectedFile != null){
            System.out.println("Sciezka: "+selectedFile.getAbsolutePath());
            try {
                image = new Image(selectedFile.toURI().toURL().toExternalForm());
                canvas.setHeight(image.getHeight());
                canvas.setWidth(image.getWidth());
                gc = canvas.getGraphicsContext2D();
                gc.drawImage(image,0,0);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Plik niepoprawny!");
        }
    }
}
