package sample.Converters;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;

import static org.opencv.imgproc.Imgproc.*;

public class Rotater {
    Mat om, sm; //om - oryginalny obraz, sm - przeskalowany obraz
    GraphicsContext gc;
    MatOfByte matOfByte;
    Canvas canvas;
    Slider slider;
    Label rotationLabel;
    Button okButton;
    CheckBox noCropCheckBox;
    Stage window;
    Pane pane;
    ComboBox<String> combobox;
    public boolean rotated;

    public Rotater (Mat m)
    {
        rotated = false;
        om = m;
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Obróć obraz o wybrany kąt");
        window.setWidth(800);
        window.setHeight(800);
        double scale;
        if (m.width()>m.height())
            scale = (double)(m.width())/500;
        else
            scale = (double)(m.height())/500;
        Size displaySize = new Size(m.width()/scale, m.height()/scale);
        matOfByte = new MatOfByte();
        canvas = new Canvas();
        sm = new Mat(displaySize, om.type());
        Imgproc.resize(om, sm, displaySize);
        rotationLabel = new Label("0 stopni");
        noCropCheckBox = new CheckBox();
        noCropCheckBox.setText("Nie kadruj obrazu");
        noCropCheckBox.setOnMouseClicked(event -> draw(getRotatedMat(sm.clone())));
        slider = new Slider(-179, 180, 0);
        slider.setPrefWidth(360);
        slider.setOnMouseReleased(event -> draw(getRotatedMat(sm.clone())));
        slider.setOnMouseDragged( event -> rotationLabel.setText((int)slider.getValue()+" stopni"));
        combobox = new ComboBox<>();
        combobox.getItems().addAll("Linear", "Nearest neighbor", "Area", "Cubic", "Lanczos4");
        combobox.setValue("Linear");
        combobox.setPromptText("Rodzaj interpolacji");
        combobox.setOnAction( event -> draw(getRotatedMat(sm.clone())));
        okButton = new Button("Ok");
        okButton.setOnAction(event -> {
            if (slider.getValue()!=0) {
                rotated = true;
                om = getRotatedMat(om);
            }
            window.close();
        });
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10,10,10,10));
        HBox sliderBox = new HBox(10);
        HBox optionsBox = new HBox(10);
        sliderBox.setAlignment(Pos.TOP_CENTER);
        optionsBox.setAlignment(Pos.TOP_CENTER);
        pane = new Pane(canvas);
        gc = canvas.getGraphicsContext2D();
        sliderBox.getChildren().addAll(rotationLabel, slider);
        optionsBox.getChildren().addAll(noCropCheckBox, combobox);
        layout.getChildren().addAll(sliderBox, optionsBox, okButton, pane);
        layout.setAlignment(Pos.TOP_CENTER);
        Scene scene = new Scene(layout);
        window.setScene(scene);
    }

    private void draw(Mat m)
    {
        canvas.setWidth(m.width());
        pane.setMaxWidth(canvas.getWidth());
        canvas.setHeight(m.height());
        Imgcodecs.imencode(".bmp", m, matOfByte);
        gc.drawImage(new Image(new ByteArrayInputStream(matOfByte.toArray())),0,0);
    }

    private Mat getRotatedMat(Mat m)
    {
        if (slider.getValue()==0)
            return m;
        int interpolationFlag = 1;
        switch (combobox.getValue())
        {
            case "Nearest neighbor": interpolationFlag = INTER_NEAREST; break;
            case "Linear": interpolationFlag = INTER_LINEAR; break;
            case "Area": interpolationFlag = INTER_AREA; break;
            case "Cubic": interpolationFlag = INTER_CUBIC; break;
            case "Lanczos4": interpolationFlag = INTER_LANCZOS4; break;
        }
        if (noCropCheckBox.isSelected())
        {
            Size cropSize = Stitcher.getBoundingBoxSize(m, slider.getValue());
            double diagonal = Math.sqrt(m.width()*m.width()+m.height()*m.height());
            Size fitAllSize = new Size(diagonal,diagonal);
            Mat tempMat = new Mat(fitAllSize, m.type(), new Scalar(0));
            Point translateCoordinates = new Point((tempMat.width()-m.width())/2, (tempMat.height()-m.height())/2);
            Rect roi = new Rect(translateCoordinates, m.size());
            m.copyTo(new Mat(tempMat, roi));
            Point center = new Point(tempMat.width()/2, tempMat.height()/2);
            Imgproc.warpAffine(tempMat, tempMat, Imgproc.getRotationMatrix2D(center, slider.getValue(), 1.0), tempMat.size(),interpolationFlag);
            int cropRowStart = (int)((tempMat.height()-cropSize.height)/2);
            int cropRowEnd = cropRowStart+(int)cropSize.height;
            int cropColumnStart = (int)((tempMat.width()-cropSize.width)/2);
            int cropColumnEnd = cropColumnStart+(int)cropSize.width;
            tempMat = tempMat.submat(cropRowStart, cropRowEnd, cropColumnStart, cropColumnEnd);
            return tempMat;
        }
        else
        {
            Point center = new Point(m.width()/2, m.height()/2);
            Imgproc.warpAffine(m, m, Imgproc.getRotationMatrix2D(center, slider.getValue(), 1.0), m.size(),interpolationFlag);
            return m;
        }
    }

    public Mat rotate()
    {
        draw(sm);
        window.showAndWait();
        return om;
    }
}
