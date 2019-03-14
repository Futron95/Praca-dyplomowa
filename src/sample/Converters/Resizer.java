package sample.Converters;

import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import static org.opencv.imgproc.Imgproc.*;

public class Resizer {
    Mat om;
    Button okButton;
    Stage window;
    TextField widthTextField, heightTextField;
    ComboBox<String> interpolationComboBox, unitsComboBox;
    Double aspectRatio;
    Boolean autoAdjust;
    public boolean resized;

    public int getPercentValue(String value, int base)
    {
        if (value.isEmpty())
            return 0;
        return (int)Double.parseDouble(value)/base*100;
    }

    public Resizer(Mat m)
    {
        resized = false;
        om = m;
        aspectRatio = (double)om.width()/om.height();
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Zmień rozmiar obrazu");
        window.setWidth(280);
        window.setHeight(360);
        window.setResizable(false);
        autoAdjust = false;
        Label interpolationLabel = new Label ("Metoda interpolacji");
        interpolationComboBox = new ComboBox<>();
        interpolationComboBox.getItems().addAll("Linear", "Nearest neighbor", "Area", "Cubic", "Lanczos4");
        interpolationComboBox.setValue("Linear");
        interpolationComboBox.setPromptText("Rodzaj interpolacji");
        Label widthLabel = new Label("Szerokość:");
        Label heightLabel = new Label("Wysokość:");
        widthTextField = new TextField(om.width()+"");
        widthTextField.setMaxWidth(100);
        Label unitsLabel = new Label("Jednostka:");
        unitsComboBox = new ComboBox<>();
        unitsComboBox.getItems().addAll("Piksele", "Procenty");
        unitsComboBox.setValue("Piksele");
        unitsComboBox.setOnAction( e ->
        {
            if (unitsComboBox.getValue().equals("Piksele"))
            {
                aspectRatio = (double)om.width()/om.height();
                autoAdjust = true;
                if (widthTextField.getText().isEmpty())
                    widthTextField.setText("0");
                else
                    widthTextField.setText(om.width()*Integer.parseInt(widthTextField.getText())/100+"");
                autoAdjust = true;
                if (heightTextField.getText().isEmpty())
                    heightTextField.setText("0");
                else
                    heightTextField.setText(om.height()*Integer.parseInt(heightTextField.getText())/100+"");
                return;
            }
            aspectRatio = 1.0;
            autoAdjust = true;
            widthTextField.setText(getPercentValue(widthTextField.getText(), om.width())+"");
            autoAdjust = true;
            heightTextField.setText(getPercentValue(heightTextField.getText(), om.height())+"");
        });

        CheckBox aspectRatioCheckBox = new CheckBox("Zachowaj proporcje");
        widthTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (autoAdjust == true) {
                autoAdjust = false;
                return;
            }
            if (!newValue.matches("\\d*")) {
                ((StringProperty) observable).setValue(oldValue);
                return;
            }
            if (!aspectRatioCheckBox.isSelected())
                return;
            autoAdjust = true;
            int width = 0;
            if (newValue.length() > 0)
                width = Integer.parseInt(newValue);
            int height = (int) (width / aspectRatio);
            heightTextField.setText(height + "");

        });
        heightTextField = new TextField(om.height()+"");
        heightTextField.setMaxWidth(100);
        heightTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (autoAdjust == true) {
                autoAdjust = false;
                return;
            }
            if (!newValue.matches("\\d*")) {
                ((StringProperty) observable).setValue(oldValue);
                return;
            }
            if (!aspectRatioCheckBox.isSelected())
                return;
            autoAdjust = true;
            int height = Integer.parseInt(newValue);
            int width = (int) (height * aspectRatio);
            widthTextField.setText(width + "");

        });
        okButton = new Button("Ok");
        okButton.setOnAction(event -> {
            changeSize();
            window.close();
        });
        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10,10,10,60));
        layout.getChildren().addAll(interpolationLabel, interpolationComboBox, unitsLabel, unitsComboBox, widthLabel, widthTextField, heightLabel, heightTextField, aspectRatioCheckBox, okButton);
        layout.setAlignment(Pos.TOP_LEFT);
        Scene scene = new Scene(layout);
        window.setScene(scene);
    }

    private void changeSize() {
        int width = 1, height = 1;
        if (widthTextField.getText().length() > 0) {
            width = Integer.parseInt(widthTextField.getText());
            if (unitsComboBox.getValue().equals("Procenty"))
                width = width * om.width() / 100;
            if (width == 0)
                width = 1;
        }
        if (heightTextField.getText().length() > 0) {
            height = Integer.parseInt(heightTextField.getText());
            if (unitsComboBox.getValue().equals("Procenty"))
                height = height * om.height() / 100;
            if (height == 0)
                height = 1;
        }

        int interpolationFlag = 1;
        switch (interpolationComboBox.getValue())
        {
            case "Nearest neighbor": interpolationFlag = INTER_NEAREST; break;
            case "Area": interpolationFlag = INTER_AREA; break;
            case "Cubic": interpolationFlag = INTER_CUBIC; break;
            case "Lanczos4": interpolationFlag = INTER_LANCZOS4; break;
        }
        Imgproc.resize(om, om, new Size(width,height), 0, 0, interpolationFlag);
        resized = true;
    }

    public Boolean resize()
    {
        window.showAndWait();
        return resized;
    }
}
