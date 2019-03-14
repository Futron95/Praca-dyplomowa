package sample;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import sample.Converters.Encryptor;
import sample.Converters.Resizer;
import sample.Converters.Rotater;
import sample.Converters.Stitcher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;

import static org.opencv.imgproc.Imgproc.*;

public class Controller
{
    private GraphicsContext gc;
    private Mat m;
    private float scale;            //skala używana do pokazywania edytowanego obrazu w podglądzie
    private Mat[] mats;             //tablica macierzy pixeli obrazu, umożliwia cofanie zmian
    private int currentM, backMs, forwardMs;
    private MatOfByte byteMat;
    private File imageFile;
    private Image previewImage;
    public Boolean unsavedChanges;
    private Runnable drawer, navigator;
    public static Controller currentController;
    public ArrayList<CustomButton> customButtons;
    @FXML
    private ImageView brightnessUp, brightnessDown, contrastUp, contrastDown, saturationUp, saturationDown, undoImageView, redoImageView, zoomin, zoomout, rotate, sharpnessDown, sharpnessUp, resize;
    @FXML
    private MenuItem save, saveAs, encrypt, autoStitchingMenuItem, manualStitchingMenuItem, undoMenuItem, redoMenuItem, closeMenuItem;
    @FXML
    private Canvas canvas;
    @FXML
    private Label zoomLabel;
    @FXML
    private ScrollPane scrollPane;


    public Controller()
    {
        Controller.currentController = this;
        unsavedChanges = false;
        drawer = () -> drawImage();
        navigator = () -> navigationUpdate();
    }

    public void setUp()
    {
        canvas.setOnScroll(event ->
        {
            if (event.isControlDown())
            {
                if (event.getDeltaY()>0)
                    scaleUp();
                else
                    scaleDown();
            }
        });

        customButtons = new ArrayList<>();
        customButtons.add(new CustomButton(brightnessDown, "brightness down"));
        customButtons.add(new CustomButton(brightnessUp, "brightness up"));
        customButtons.add(new CustomButton(contrastDown, "contrast down"));
        customButtons.add(new CustomButton(contrastUp, "contrast up"));
        customButtons.add(new CustomButton(saturationDown, "saturation down"));
        customButtons.add(new CustomButton(saturationUp, "saturation up"));
        customButtons.add(new CustomButton(sharpnessDown, "sharpness down"));
        customButtons.add(new CustomButton(sharpnessUp, "sharpness up"));
        customButtons.add(new CustomButton(undoImageView, "undo"));
        customButtons.add(new CustomButton(redoImageView, "redo"));
        customButtons.add(new CustomButton(zoomin, "zoomin"));
        customButtons.add(new CustomButton(zoomout, "zoomout"));
        customButtons.add(new CustomButton(rotate, "rotate"));
        customButtons.add(new CustomButton(resize, "resize"));
    }

    private CustomButton getButton(String name)
    {
        for (CustomButton button: customButtons)
            if (button.name.equals(name))
                return button;
        return null;
    }

    public void FileChooseAction() {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter fileExtensions =
                new FileChooser.ExtensionFilter(
                        "Obraz", "*.bmp", "*.jpg", "*.png");
        fc.getExtensionFilters().add(fileExtensions);
        File openedFile = fc.showOpenDialog(null);
        if (openedFile == null)
            return;
        unsavedChanges = false;
        imageFile = openedFile;
        canvas.setDisable(false);
        canvas.setVisible(true);
        scrollPane.setVisible(true);
        enableButtons();
        resetMats();
        byteMat = new MatOfByte();
        scale = 1;
        zoomLabel.setText((int) (scale * 100) + "%");
        canvas.setHeight(m.height());
        canvas.setWidth(m.width());
        gc = canvas.getGraphicsContext2D();
        Platform.runLater(drawer);
    }

    //przywraca macierze z obrazem i jego wczesniejszymi wersjami do domyślnych wartości
    private void resetMats()
    {
        currentM = 0;
        backMs = 0;
        forwardMs=0;
        undoMenuItem.setDisable(true);
        getButton("undo").disable();
        redoMenuItem.setDisable(true);
        getButton("redo").disable();
        m = fileToMat(imageFile);
        mats = new Mat[11];
        mats[currentM] = m.clone();
    }

    private Mat fileToMat(File input)
    {
        BufferedImage image = null;
        try {
            image = ImageIO.read(input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat img = new Mat(image.getHeight(),image.getWidth(), CvType.CV_8UC3);
        img.put(0, 0, data);
        return img;
    }

    private void redrawScaledImage()
    {
        canvas.setHeight(m.height()*scale);
        canvas.setWidth(m.width()*scale);
        gc.drawImage(previewImage,0,0, canvas.getWidth(), canvas.getHeight());
    }

    private void drawImage()
    {
        canvas.setHeight(m.height()*scale);
        canvas.setWidth(m.width()*scale);
        Imgcodecs.imencode(".bmp", m, byteMat);
        previewImage = new Image(new ByteArrayInputStream(byteMat.toArray()));
        gc.drawImage(previewImage,0,0, canvas.getWidth(), canvas.getHeight());
    }
    
    private void navigationUpdate()
    {
        unsavedChanges = true;
        save.setDisable(false);
        if (forwardMs>0) {
            forwardMs = 0;
            redoMenuItem.setDisable(true);
            getButton("redo").disable();
        }
        currentM = (currentM + 1) % 11;
        mats[currentM] = m.clone();
        if (backMs < 10) {
            backMs++;
            if (undoImageView.isDisabled()) {
                undoMenuItem.setDisable(false);
                getButton("undo").enable();
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
            undoMenuItem.setDisable(true);
            getButton("undo").disable();
        }
        Platform.runLater(drawer);
        forwardMs++;
        if (redoImageView.isDisabled())
        {
            redoMenuItem.setDisable(false);
            getButton("redo").enable();
        }
    }

    //ponowne wprowadzenie cofniętych zmian
    public void redo()
    {
        currentM = (currentM+1)%11;
        m = mats[currentM].clone();
        Platform.runLater(drawer);
        forwardMs--;
        if (forwardMs==0)
        {
            redoMenuItem.setDisable(true);
            getButton("redo").disable();
        }
        backMs++;
        if (undoImageView.isDisabled())
        {
            undoMenuItem.setDisable(false);
            getButton("undo").enable();
        }
    }



    public void saveFile(File file) {
        String extension = file.getPath().substring(file.getPath().lastIndexOf('.'));
        Imgcodecs.imencode(extension, m, byteMat);
        try {
            Files.write(file.toPath(), byteMat.toArray());
            unsavedChanges = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        save.setDisable(true);
    }

    public void saveImage() {
        saveFile(imageFile);
    }


    public void saveImageAs()
    {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter eFjpg = new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg");
        FileChooser.ExtensionFilter eFpng = new FileChooser.ExtensionFilter("PNG", "*.png");
        FileChooser.ExtensionFilter eFbmp = new FileChooser.ExtensionFilter("BMP", "*.bmp");
        fileChooser.getExtensionFilters().addAll(eFbmp, eFjpg, eFpng);
        String fileName = imageFile.toPath().getFileName().toString();
        fileChooser.setInitialFileName(fileName);
        String extension = fileName.substring(fileName.lastIndexOf('.'));
        switch (extension)
        {
            case ".jpg" :
            case ".jpeg":fileChooser.setSelectedExtensionFilter(eFjpg); break;
            case ".bmp" :fileChooser.setSelectedExtensionFilter(eFbmp); break;
            case ".png" :fileChooser.setSelectedExtensionFilter(eFpng); break;
        }
        fileChooser.setInitialDirectory(imageFile.getParentFile());
        File file = fileChooser.showSaveDialog(scrollPane.getScene().getWindow());
        if (file == null)
            return;
        imageFile = file;
        saveFile(imageFile);
    }

    private String getImageExtension()
    {
        String path = imageFile.getPath();
        return path.substring((path.lastIndexOf('.')+1));
    }

    public void encryptImg()
    {
        boolean encrypted = Encryptor.encrypt(m);
        if (encrypted == false)
            return;
        String imgExtension = getImageExtension();
        if (imgExtension.equals("jpg") || imgExtension.equals("jpeg"))
        {
            String path = imageFile.getAbsolutePath();
            path = path.substring(0,path.length()-imgExtension.length())+"png";
            imageFile = new File(path);
            saveFile(imageFile);
        }
        Platform.runLater(drawer);
        Platform.runLater(navigator);
    }

    public void scaleUp()
    {
        if (scale < 16 && canvas.getWidth()<5000 && canvas.getHeight()<5000) {
            scale *= 2;
            zoomLabel.setText((int)(scale*100)+"%");
        }
        redrawScaledImage();
    }

    public void scaleDown()
    {
        if (scale > 1.0/16.0) {
            scale *= 0.5;
            zoomLabel.setText((int)(scale*100)+"%");
        }
        redrawScaledImage();
    }

    public void rotateImg()
    {
       Rotater rotater = new Rotater(m);
       m = rotater.rotate();
       if (rotater.rotated) {
           Platform.runLater(drawer);
           Platform.runLater(navigator);
       }
    }

    private void resizeImg()
    {
        Resizer resizer = new Resizer(m);
        if (resizer.resize() == true){
            Platform.runLater(drawer);
            Platform.runLater(navigator);
        }
    }

    public void autoStitching()
    {
        stitchImg(true);
    }

    public void manualStitching()
    {
        stitchImg(false);
    }

    private void stitchImg(boolean automaticStitching)
    {
        Mat m2;
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter fileExtensions =
                new FileChooser.ExtensionFilter(
                        "Obrazy", "*.bmp", "*.jpg", "*.png");
        fc.getExtensionFilters().add(fileExtensions);
        File selectedFile = fc.showOpenDialog(null);
        if(selectedFile != null){
            m2 = fileToMat(selectedFile);
            Mat newM = Stitcher.stitch(m,m2, automaticStitching);
            if (newM!=m) {
                m=newM;
                canvas.setWidth(m.width());
                Platform.runLater(drawer);
                Platform.runLater(navigator);
            }
        }
    }

    public void closeImage()
    {
        disableButtons();
        canvas.setDisable(true);
        canvas.setVisible(false);
        scrollPane.setVisible(false);
        scale = 1;
        unsavedChanges = false;
    }

    private void enableButtons()
    {
        customButtons.forEach(button -> button.enable());
        save.setDisable(false);
        saveAs.setDisable(false);
        zoomLabel.setVisible(true);
        undoMenuItem.setDisable(false);
        redoMenuItem.setDisable(false);
        closeMenuItem.setDisable(false);
        encrypt.setDisable(false);
        autoStitchingMenuItem.setDisable(false);
        manualStitchingMenuItem.setDisable(false);
    }

    private void disableButtons()
    {
        customButtons.forEach(button -> button.disable());
        save.setDisable(true);
        saveAs.setDisable(true);
        zoomLabel.setVisible(false);
        undoMenuItem.setDisable(true);
        redoMenuItem.setDisable(true);
        closeMenuItem.setDisable(true);
        encrypt.setDisable(true);
        autoStitchingMenuItem.setDisable(true);
        manualStitchingMenuItem.setDisable(true);
    }

    private void changeBrightness(int change)
    {
        m.convertTo(m,-1,1, change);
        Platform.runLater(drawer);
        Platform.runLater(navigator);
    }

    private void changeContrast(double change)
    {
        m.convertTo(m,-1, change);
        Platform.runLater(drawer);
        Platform.runLater(navigator);
    }

    private void changeSharpness(Boolean increase)
    {
        Mat dest = new Mat(m.rows(),m.cols(),m.type());
        Imgproc.GaussianBlur(m, dest, new Size(0,0), 1);
        if (increase == false)
            m = dest;
        else
            Core.addWeighted(m, 1.5, dest, -0.5, 0, m);
        Platform.runLater(drawer);
        Platform.runLater(navigator);
    }

    public void brightnessDownReleased()
    {
        getButton("brightness down").release();
        Thread thread = new Thread(()->changeBrightness(-10));
        thread.start();
    }


    public void brightnessUpReleased()
    {
        getButton("brightness up").release();
        Thread thread = new Thread(()->changeBrightness(10));
        thread.start();
    }

    public void sharpnessDownReleased()
    {
        getButton("sharpness down").release();
        Thread thread = new Thread(()->changeSharpness(false));
        thread.start();
    }

    public void sharpnessUpReleased()
    {
        getButton("sharpness up").release();
        Thread thread = new Thread(()->changeSharpness(true));
        thread.start();
    }

    public void contrastDownReleased()
    {
        getButton("contrast down").release();
        Thread thread = new Thread(()->changeContrast(0.95));
        thread.start();
    }

    public void contrastUpReleased()
    {
        getButton("contrast up").release();
        Thread thread = new Thread(()->changeContrast(1.05));
        thread.start();
    }

    private void changeSaturation(double change)
    {
        Mat img = new Mat();
        Imgproc.cvtColor(m, img, COLOR_RGB2HSV);
        byte[] bytes = new byte[(int) (3 * img.total())];
        img.get(0, 0, bytes);
        int value;
        for (int i = 1; i < img.total() * 3; i += 3) {
            value = bytes[i] & 255;
            value *= change;
            if (value > 255)
                continue;
            bytes[i] = (byte) value;
        }
        img.put(0, 0, bytes);
        Imgproc.cvtColor(img, m, COLOR_HSV2RGB);
        Platform.runLater(drawer);
        Platform.runLater(navigator);
    }

    public void saturationDownReleased()
    {
        getButton("saturation down").release();
        Thread thread = new Thread(() -> changeSaturation(0.90));
        thread.start();
    }

    public void saturationUpReleased()
    {
        getButton("saturation up").release();
        Thread thread = new Thread(() -> changeSaturation(1.1));
        thread.start();
    }

    public void undoReleased()
    {
        getButton("undo").release();
    }

    public void redoReleased()
    {
        getButton("redo").release();
    }

    public void zoominReleased()
    {
        getButton("zoomin").release();
    }

    public void zoomoutReleased()
    {
        getButton("zoomout").release();
    }

    public void rotateReleased()
    {
        getButton("rotate").release();
        rotateImg();
    }

    public void resizeReleased()
    {
        getButton("resize").release();
        resizeImg();
    }
}