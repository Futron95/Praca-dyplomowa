package sample;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.io.File;

import static org.opencv.imgproc.Imgproc.*;

/*TODO
    pokazywanie przyblizenia procentowo
*/
public class Controller {
    @FXML
    private Canvas canvas;
    private GraphicsContext gc;
    private Mat m;
    private float scale;
    private Mat[] mats;             //tablica macierzy pixeli obrazu, umożliwia cofanie zmian
    private int currentM, backMs, forwardMs;
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
    @FXML
    private MenuItem stitchingMenuItem;
    @FXML
    private ImageView back;
    @FXML
    private ImageView forward;
    @FXML
    private ImageView zoomin;
    @FXML
    private ImageView zoomout;
    @FXML
    private ImageView rotate;

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
            filePath = selectedFile.getPath();
            System.out.println("Sciezka: "+filePath);
            resetMats();
            byteMat = new MatOfByte();
            canvas.setHeight(m.height());
            canvas.setWidth(m.width());
            gc = canvas.getGraphicsContext2D();
            scale = 1;
            drawImage(false);
            enableButtons();
        } else {
            System.out.println("Plik niepoprawny!");
        }
    }

    private void resetMats()
    {
        currentM = 0;
        backMs = 0;
        forwardMs=0;
        back.setDisable(true);
        forward.setDisable(true);
        m = Imgcodecs.imread(filePath);
        mats = new Mat[11];
        mats[currentM] = m.clone();
    }

    private Mat getScaleMat()
    {
        if (scale==1) {
            canvas.setHeight(m.height());
            canvas.setWidth(m.width());
            return m;
        }
        int width = (int) (scale*m.width());
        int height = (int) (scale*m.height());
        Mat sm = new Mat();
        Size scaledSize = new Size(width,height);
        Imgproc.resize(m, sm, scaledSize, 0,0,INTER_NEAREST);
        canvas.setHeight(sm.height());
        canvas.setWidth(sm.width());
        return sm;
    }

    private void drawImage(boolean change)
    {
        Imgcodecs.imencode(".bmp", getScaleMat(), byteMat);
        gc.drawImage(new Image(new ByteArrayInputStream(byteMat.toArray())),0,0);
        if (change==true) {
            currentM = (currentM + 1) % 11;
            mats[currentM] = m.clone();
            if (backMs < 10) {
                backMs++;
                if (backMs > 0) {
                    back.setDisable(false);
                    back.setImage(new Image(".\\icons\\back.png"));
                }
            }
        }
    }

    //cofanie zmian
    public void undo()
    {
        currentM = currentM-1;
        if (currentM<0)
            currentM=10;
        m = mats[currentM].clone();
        backMs -= 1;
        if (backMs==0)
        {
            back.setDisable(true);
            back.setImage(new Image(".\\icons\\back3.png"));
        }
        drawImage(false);
        forwardMs++;
        if (forward.isDisabled())
        {
            forward.setDisable(false);
            forward.setImage(new Image(".\\icons\\forward.png"));
        }
    }

    //ponowne wprowadzenie cofniętych zmian
    public void redo()
    {
        currentM = (currentM+1)%11;
        m = mats[currentM].clone();
        drawImage(false);
        forwardMs--;
        if (forwardMs==0)
        {
            forward.setDisable(true);
            forward.setImage(new Image(".\\icons\\forward3.png"));
        }
        backMs++;
        if (back.isDisabled())
        {
            back.setDisable(false);
            back.setImage(new Image(".\\icons\\back.png"));
        }
    }

    public void increaseBrightness()
    {
        m.convertTo(m,-1,1,10);
        drawImage(true);
    }

    public void decreaseBrightness()
    {
        m.convertTo(m,-1,1,-10);
        drawImage(true);
    }

    public void increaseContrast()
    {
        m.convertTo(m,-1,1.05);
        drawImage(true);
    }

    public void decreaseContrast()
    {
        m.convertTo(m,-1,0.95);
        drawImage(true);
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
        drawImage(true);
    }

    public void saveImage()
    {
        Imgcodecs.imwrite(filePath,m);
    }

    public void encodeImg()
    {
        encoder = new Encoder();
        encoder.encode(m);
        drawImage(true);
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
        drawImage(true);
    }

    public void scaleUp()
    {
        if (scale < 16)
            scale *= 2;
        drawImage(false);
    }

    public void scaleDown()
    {
        if (scale > 1.0/16.0)
            scale *= 0.5;
        drawImage(false);
    }

    public void rotateImg()
    {
        Imgproc.warpAffine(m,m, Imgproc.getRotationMatrix2D(new Point(m.width()/2, m.height()/2), -5.0, 1.0),m.size(), INTER_LANCZOS4);
        drawImage(true);
    }

    public void stitchImg()
    {
        Mat m2;
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter fileExtensions =
                new FileChooser.ExtensionFilter(
                        "Obrazy", "*.bmp", "*.jpg", "*.png");
        fc.getExtensionFilters().add(fileExtensions);
        File selectedFile = fc.showOpenDialog(null);
        if(selectedFile != null){
            m2 = Imgcodecs.imread(selectedFile.getAbsolutePath());
            m = Stitcher.stitch(m,m2);
            canvas.setWidth(m.width());
            drawImage(true);
        } else {
            System.out.println("Nie wczytano poprawnego pliku!");
        }
    }

    private void enableButtons()
    {
        brightnessUp.setDisable(false);
        brightnessUp.setImage(new Image(".\\icons\\brightness up.png"));
        brightnessDown.setDisable(false);
        brightnessDown.setImage(new Image(".\\icons\\brightness down.png"));
        contrastDown.setDisable(false);
        contrastDown.setImage(new Image(".\\icons\\contrast down.png"));
        contrastUp.setDisable(false);
        contrastUp.setImage(new Image(".\\icons\\contrast up.png"));
        saturationUp.setDisable(false);
        saturationUp.setImage(new Image(".\\icons\\saturation up.png"));
        saturationDown.setDisable(false);
        saturationDown.setImage(new Image(".\\icons\\saturation down.png"));
        zoomin.setDisable(false);
        zoomin.setImage(new Image(".\\icons\\zoomin.png"));
        zoomout.setDisable(false);
        zoomout.setImage(new Image(".\\icons\\zoomout.png"));
        rotate.setDisable(false);
        rotate.setImage(new Image(".\\icons\\rotate.png"));
        save.setDisable(false);
        if (!filePath.substring(filePath.length()-3).equals("jpg"))     //szyfrowanie i deszyfrowanie umożliwione jest tylko gdy rozszerzenie inne niz jpg poniewaz kompresja uniemożliwia poprawne odkodowanie
        {
            encode.setDisable(false);
            decode.setDisable(false);
        }
        stitchingMenuItem.setDisable(false);
    }

    public void brightnessDownPressed()
    {
        brightnessDown.setImage(new Image(".\\icons\\brightness down2.png"));
    }

    public void brightnessDownReleased()
    {
        brightnessDown.setImage(new Image(".\\icons\\brightness down.png"));
    }

    public void brightnessUpPressed()
    {
        brightnessUp.setImage(new Image(".\\icons\\brightness up2.png"));
    }

    public void brightnessUpReleased()
    {
        brightnessUp.setImage(new Image(".\\icons\\brightness up.png"));
    }

    public void contrastDownPressed()
    {
        contrastDown.setImage(new Image(".\\icons\\contrast down2.png"));
    }

    public void contrastDownReleased()
    {
        contrastDown.setImage(new Image(".\\icons\\contrast down.png"));
    }

    public void contrastUpPressed()
    {
        contrastUp.setImage(new Image(".\\icons\\contrast up2.png"));
    }

    public void contrastUpReleased()
    {
        contrastUp.setImage(new Image(".\\icons\\contrast up.png"));
    }
    public void saturationDownPressed()
    {
        saturationDown.setImage(new Image(".\\icons\\saturation down2.png"));
    }

    public void saturationDownReleased()
    {
        saturationDown.setImage(new Image(".\\icons\\saturation down.png"));
    }

    public void saturationUpPressed()
    {
        saturationUp.setImage(new Image(".\\icons\\saturation up2.png"));
    }

    public void saturationUpReleased()
    {
        saturationUp.setImage(new Image(".\\icons\\saturation up.png"));
    }

    public void backPressed()
    {
        back.setImage(new Image(".\\icons\\back2.png"));
    }

    public void backReleased()
    {
        back.setImage(new Image(".\\icons\\back.png"));
    }

    public void forwardPressed()
    {
        forward.setImage(new Image(".\\icons\\forward2.png"));
    }

    public void forwardReleased()
    {
        forward.setImage(new Image(".\\icons\\forward.png"));
    }

    public void zoominPressed()
    {
        zoomin.setImage(new Image(".\\icons\\zoomin2.png"));
    }

    public void zoominReleased()
    {
        zoomin.setImage(new Image(".\\icons\\zoomin.png"));
    }

    public void zoomoutPressed()
    {
        zoomout.setImage(new Image(".\\icons\\zoomout2.png"));
    }

    public void zoomoutReleased()
    {
        zoomout.setImage(new Image(".\\icons\\zoomout.png"));
    }

    public void rotatePressed()
    {
        rotate.setImage(new Image(".\\icons\\rotate2.png"));
    }

    public void rotateReleased()
    {
        rotate.setImage(new Image(".\\icons\\rotate.png"));
    }
}