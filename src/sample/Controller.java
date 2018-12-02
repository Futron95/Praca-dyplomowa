package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.opencv.imgproc.Imgproc.COLOR_HSV2RGB;
import static org.opencv.imgproc.Imgproc.COLOR_RGB2HSV;

public class Controller {
    @FXML
    private Canvas canvas;
    private GraphicsContext gc;
    private Mat m;              //macierz pixeli obrazu
    private MatOfByte byteMat;
    private String filePath;
    @FXML
    private ImageView brightnessUp;
    @FXML
    private ImageView brightnessDown;
    @FXML
    private ImageView contrastUp;
    @FXML
    private ImageView contrastDown;
    @FXML
    private ImageView saturationUp;
    @FXML
    private ImageView saturationDown;
    @FXML
    private MenuItem save;
    @FXML
    private MenuItem encode;
    @FXML
    private MenuItem decode;
    private Encoder encoder;
    static{
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public void FileChooseAction(ActionEvent event)
    {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter fileExtensions =
                new FileChooser.ExtensionFilter(
                        "Obrazy", "*.bmp", "*.jpg", "*.png");
        fc.getExtensionFilters().add(fileExtensions);
        File selectedFile = fc.showOpenDialog(null);
        if(selectedFile != null){
            filePath = selectedFile.getAbsolutePath();
            System.out.println("Sciezka: "+filePath);
            m = Imgcodecs.imread(filePath);
            byteMat = new MatOfByte();
            canvas.setHeight(m.height());
            canvas.setWidth(m.width());
            gc = canvas.getGraphicsContext2D();
            drawImage();
            enableButtons();
        } else {
            System.out.println("Plik niepoprawny!");
        }
    }

    private void drawImage()
    {
        Imgcodecs.imencode(".bmp", m, byteMat);
        gc.drawImage(new Image(new ByteArrayInputStream(byteMat.toArray())),0,0);
    }

    public void increaseBrightness()
    {
        m.convertTo(m,-1,1,10);
        drawImage();
    }

    public void decreaseBrightness()
    {
        m.convertTo(m,-1,1,-10);
        drawImage();
    }

    public void increaseContrast()
    {
        m.convertTo(m,-1,1.05);
        drawImage();
    }

    public void decreaseContrast()
    {
        m.convertTo(m,-1,0.95);
        drawImage();
    }

    public void increaseSaturation()
    {
        changeSaturation(10.0);
    }

    public void decreaseSaturation()
    {
        changeSaturation(-10.0);
    }

    private void changeSaturation(double change)
    {
        Mat img = new Mat();
        double pixelData[];
        Imgproc.cvtColor(m,img,COLOR_RGB2HSV);
        for(int y=0; y<img.height(); y++)
        {
            for (int x = 0; x < img.width(); x++)
            {
                pixelData = img.get(y,x);
                pixelData[1]+=change;
                if (pixelData[1]>180)
                    pixelData[1] = 180;
                if (pixelData[1]<0)
                    pixelData[1] = 0;
                img.put(y,x,pixelData);
            }
        }
        Imgproc.cvtColor(img,m,COLOR_HSV2RGB);
        drawImage();
    }

    public void saveImage()
    {
        Imgcodecs.imwrite(filePath,m);
    }

    public void encodeImg()
    {
        encoder = new Encoder();
        encoder.encode(m);
        drawImage();
        CodeOutputWindow.display(encoder.codeString);
    }

    public void openDecodeWindow()
    {
        CodeInputWindow.display(this);
    }

    public void decodeImg(String code)
    {
        encoder = new Encoder();
        encoder.decode(m,code);
        drawImage();
    }

    private void enableButtons()
    {
        brightnessUp.setDisable(false);
        brightnessDown.setDisable(false);
        contrastDown.setDisable(false);
        contrastUp.setDisable(false);
        saturationUp.setDisable(false);
        saturationDown.setDisable(false);
        save.setDisable(false);
        encode.setDisable(false);
        decode.setDisable(false);
    }
}
