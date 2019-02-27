package sample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
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
import sample.Converters.Rotater;
import sample.Converters.Stitcher;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

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
    public static HashMap<String, Image> buttonImages;
    @FXML
    private ImageView brightnessUp, brightnessDown, contrastUp, contrastDown, saturationUp, saturationDown, undoImageView, redoImageView, zoomin, zoomout, rotate;
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

    public static void loadButtonImages()
    {
        buttonImages = new HashMap<>();
        URL url = Controller.class.getResource("/icons");
        try {
            File iconsFolder = Paths.get(url.toURI()).toFile();
            Arrays.stream(iconsFolder.listFiles()).forEach(file -> {
                Image image = new Image(file.toURI().toString());
                buttonImages.put(file.getName(), image);
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void setScrollZooming()
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
        if (canvas.getOnScroll() == null)
            setScrollZooming();
        resetMats();
        byteMat = new MatOfByte();
        scale = 1;
        zoomLabel.setText((int) (scale * 100) + "%");
        canvas.setHeight(m.height());
        canvas.setWidth(m.width());
        gc = canvas.getGraphicsContext2D();
        Platform.runLater(drawer);
        enableButtons();
    }

    //przywraca macierze z obrazem i jego wczesniejszymi wersjami do domyślnych wartości
    private void resetMats()
    {
        currentM = 0;
        backMs = 0;
        forwardMs=0;
        undoImageView.setDisable(true);
        undoMenuItem.setDisable(true);
        undoImageView.setImage(buttonImages.get("undo3.png"));
        redoImageView.setDisable(true);
        redoMenuItem.setDisable(true);
        redoImageView.setImage(buttonImages.get("redo3.png"));
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
            redoImageView.setDisable(true);
            redoMenuItem.setDisable(true);
            redoImageView.setImage(buttonImages.get("redo3.png"));
        }
        currentM = (currentM + 1) % 11;
        mats[currentM] = m.clone();
        if (backMs < 10) {
            backMs++;
            if (undoImageView.isDisabled()) {
                undoImageView.setDisable(false);
                undoMenuItem.setDisable(false);
                undoImageView.setImage(buttonImages.get("undo.png"));
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
            undoImageView.setDisable(true);
            undoMenuItem.setDisable(true);
            undoImageView.setImage(buttonImages.get("undo3.png"));
        }
        Platform.runLater(drawer);
        forwardMs++;
        if (redoImageView.isDisabled())
        {
            redoImageView.setDisable(false);
            redoMenuItem.setDisable(false);
            redoImageView.setImage(buttonImages.get("redo.png"));
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
            redoImageView.setDisable(true);
            redoMenuItem.setDisable(true);
            redoImageView.setImage(buttonImages.get("redo3.png"));
        }
        backMs++;
        if (undoImageView.isDisabled())
        {
            undoImageView.setDisable(false);
            undoMenuItem.setDisable(false);
            undoImageView.setImage(buttonImages.get("undo.png"));
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
        zoomLabel.setText((int)(scale*100)+"%");
        unsavedChanges = false;
    }

    private void enableButtons()
    {
        brightnessUp.setDisable(false);
        brightnessUp.setImage(buttonImages.get("brightness up.png"));
        brightnessDown.setDisable(false);
        brightnessDown.setImage(buttonImages.get("brightness down.png"));
        contrastDown.setDisable(false);
        contrastDown.setImage(buttonImages.get("contrast down.png"));
        contrastUp.setDisable(false);
        contrastUp.setImage(buttonImages.get("contrast up.png"));
        saturationUp.setDisable(false);
        saturationUp.setImage(buttonImages.get("saturation up.png"));
        saturationDown.setDisable(false);
        saturationDown.setImage(buttonImages.get("saturation down.png"));
        zoomin.setDisable(false);
        zoomin.setImage(buttonImages.get("zoomin.png"));
        zoomout.setDisable(false);
        zoomout.setImage(buttonImages.get("zoomout.png"));
        rotate.setDisable(false);
        rotate.setImage(buttonImages.get("rotate.png"));
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
        brightnessUp.setDisable(true);
        brightnessUp.setImage(buttonImages.get("brightness up3.png"));
        brightnessDown.setDisable(true);
        brightnessDown.setImage(buttonImages.get("brightness down3.png"));
        contrastDown.setDisable(true);
        contrastDown.setImage(buttonImages.get("contrast down3.png"));
        contrastUp.setDisable(true);
        contrastUp.setImage(buttonImages.get("contrast up3.png"));
        saturationUp.setDisable(true);
        saturationUp.setImage(buttonImages.get("saturation up3.png"));
        saturationDown.setDisable(true);
        saturationDown.setImage(buttonImages.get("saturation down3.png"));
        zoomin.setDisable(true);
        zoomin.setImage(buttonImages.get("zoomin3.png"));
        zoomout.setDisable(true);
        zoomout.setImage(buttonImages.get("zoomout3.png"));
        rotate.setDisable(true);
        rotate.setImage(buttonImages.get("rotate3.png"));
        save.setDisable(true);
        saveAs.setDisable(true);
        zoomLabel.setVisible(true);
        undoMenuItem.setDisable(true);
        redoMenuItem.setDisable(true);
        redoImageView.setDisable(true);
        redoImageView.setImage(buttonImages.get("redo3.png"));
        undoImageView.setDisable(true);
        undoImageView.setImage(buttonImages.get("undo3.png"));
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

    public void brightnessDownPressed()
    {
        brightnessDown.setImage(buttonImages.get("brightness down2.png"));
    }

    public void brightnessDownReleased()
    {
        brightnessDown.setImage(buttonImages.get("brightness down.png"));
        Thread thread = new Thread(()->changeBrightness(-10));
        thread.start();
    }

    public void brightnessUpPressed()
    {
        brightnessUp.setImage(buttonImages.get("brightness up2.png"));
    }

    public void brightnessUpReleased()
    {
        brightnessUp.setImage(buttonImages.get("brightness up.png"));
        Thread thread = new Thread(()->changeBrightness(10));
        thread.start();
    }

    public void contrastDownPressed()
    {
        contrastDown.setImage(buttonImages.get("contrast down2.png"));
    }

    public void contrastDownReleased()
    {
        contrastDown.setImage(buttonImages.get("contrast down.png"));
        Thread thread = new Thread(()->changeContrast(0.95));
        thread.start();
    }

    public void contrastUpPressed()
    {
        contrastUp.setImage(buttonImages.get("contrast up2.png"));
    }

    public void contrastUpReleased()
    {
        contrastUp.setImage(buttonImages.get("contrast up.png"));
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

    public void saturationDownPressed()
    {
        saturationDown.setImage(buttonImages.get("saturation down2.png"));
    }

    public void saturationDownReleased()
    {
        saturationDown.setImage(buttonImages.get("saturation down.png"));
        Thread thread = new Thread(() -> changeSaturation(0.95));
        thread.start();
    }

    public void saturationUpPressed()
    {
        saturationUp.setImage(buttonImages.get("saturation up2.png"));
    }

    public void saturationUpReleased()
    {
        saturationUp.setImage(buttonImages.get("saturation up.png"));
        Thread thread = new Thread(() -> changeSaturation(1.05));
        thread.start();
    }

    public void undoPressed()
    {
        undoImageView.setImage(buttonImages.get("undo2.png"));
    }

    public void undoReleased()
    {
        undoImageView.setImage(buttonImages.get("undo.png"));
    }

    public void redoPressed()
    {
        redoImageView.setImage(buttonImages.get("redo2.png"));
    }

    public void redoReleased()
    {
        redoImageView.setImage(buttonImages.get("redo.png"));
    }

    public void zoominPressed()
    {
        zoomin.setImage(buttonImages.get("zoomin2.png"));
    }

    public void zoominReleased()
    {
        zoomin.setImage(buttonImages.get("zoomin.png"));
    }

    public void zoomoutPressed()
    {
        zoomout.setImage(buttonImages.get("zoomout2.png"));
    }

    public void zoomoutReleased()
    {
        zoomout.setImage(buttonImages.get("zoomout.png"));
    }

    public void rotatePressed()
    {
        rotate.setImage(buttonImages.get("rotate2.png"));
    }

    public void rotateReleased()
    {
        rotate.setImage(buttonImages.get("rotate.png"));
        rotateImg();
    }
}