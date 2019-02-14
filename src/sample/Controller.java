package sample;

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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.file.Files;

import static org.opencv.imgproc.Imgproc.*;

public class Controller {
    @FXML
    private Canvas canvas;
    private GraphicsContext gc;
    private Mat m;
    private float scale;            //skala używana do pokazywania edytowanego obrazu w podglądzie
    private Mat[] mats;             //tablica macierzy pixeli obrazu, umożliwia cofanie zmian
    private int currentM, backMs, forwardMs;
    private MatOfByte byteMat;
    private File imageFile;
    private Image previewImage;
    public Boolean unsavedChanges;
    public static Controller currentController;
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
    private MenuItem saveAs;
    @FXML
    private MenuItem encrypt;
    @FXML
    private MenuItem autoStitchingMenuItem;
    @FXML
    private MenuItem manualStitchingMenuItem;
    @FXML
    private ImageView undoImageView;
    @FXML
    private ImageView redoImageView;
    @FXML
    private ImageView zoomin;
    @FXML
    private ImageView zoomout;
    @FXML
    private ImageView rotate;
    @FXML
    private Label zoomLabel;
    @FXML
    private MenuItem undoMenuItem;
    @FXML
    private MenuItem redoMenuItem;
    @FXML
    private MenuItem closeMenuItem;
    @FXML
    private ScrollPane scrollPane;


    public Controller()
    {
        Controller.currentController = this;
        unsavedChanges = false;
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

    public void FileChooseAction(ActionEvent event)
    {
        FileChooser fc = new FileChooser();
        FileChooser.ExtensionFilter fileExtensions =
                new FileChooser.ExtensionFilter(
                        "Obraz", "*.bmp", "*.jpg", "*.png");
        fc.getExtensionFilters().add(fileExtensions);
        File openedFile = fc.showOpenDialog(null);
        if(openedFile != null){
            unsavedChanges = false;
            imageFile = openedFile;
            canvas.setDisable(false);
            canvas.setVisible(true);
            scrollPane.setVisible(true);
            if (canvas.getOnScroll() == null)
                setScrollZooming();
            System.out.println("Sciezka: "+imageFile.getAbsolutePath());
            resetMats();
            byteMat = new MatOfByte();
            scale = 1;
            zoomLabel.setText((int)(scale*100)+"%");
            canvas.setHeight(m.height());
            canvas.setWidth(m.width());
            gc = canvas.getGraphicsContext2D();
            drawImage(false);
            enableButtons();
        } else {
            System.out.println("Plik niepoprawny!");
        }
    }

    //przywraca macierze z obrazem i jego wczesniejszymi wersjami do domyślnych wartości
    private void resetMats()
    {
        currentM = 0;
        backMs = 0;
        forwardMs=0;
        undoImageView.setDisable(true);
        undoMenuItem.setDisable(true);
        undoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/undo3.png")));
        redoImageView.setDisable(true);
        redoMenuItem.setDisable(true);
        redoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/redo3.png")));
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
        BufferedImage imageCopy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        imageCopy.getGraphics().drawImage(image, 0, 0, null);

        byte[] data = ((DataBufferByte) imageCopy.getRaster().getDataBuffer()).getData();
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

    private void drawImage(boolean change)
    {
        canvas.setHeight(m.height()*scale);
        canvas.setWidth(m.width()*scale);
        Imgcodecs.imencode(".bmp", m, byteMat);
        previewImage = new Image(new ByteArrayInputStream(byteMat.toArray()));
        gc.drawImage(previewImage,0,0, canvas.getWidth(), canvas.getHeight());
        if (change==true) {
            unsavedChanges = true;
            if (forwardMs>0) {
                forwardMs = 0;
                redoImageView.setDisable(true);
                redoMenuItem.setDisable(true);
                redoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/redo3.png")));
            }
            currentM = (currentM + 1) % 11;
            mats[currentM] = m.clone();
            if (backMs < 10) {
                backMs++;
                if (undoImageView.isDisabled()) {
                    undoImageView.setDisable(false);
                    undoMenuItem.setDisable(false);
                    undoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/undo.png")));
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
            undoImageView.setDisable(true);
            undoMenuItem.setDisable(true);
            undoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/undo3.png")));
        }
        drawImage(false);
        forwardMs++;
        if (redoImageView.isDisabled())
        {
            redoImageView.setDisable(false);
            redoMenuItem.setDisable(false);
            redoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/redo.png")));
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
            redoImageView.setDisable(true);
            redoMenuItem.setDisable(true);
            redoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/redo3.png")));
        }
        backMs++;
        if (undoImageView.isDisabled())
        {
            undoImageView.setDisable(false);
            undoMenuItem.setDisable(false);
            undoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/undo.png")));
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

    public void saveFile(File file) {
        String extension = file.getPath().substring(file.getPath().lastIndexOf('.'));
        Imgcodecs.imencode(extension, m, byteMat);
        try {
            Files.write(file.toPath(), byteMat.toArray());
            unsavedChanges = false;
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        System.out.println("Sciezka zapisu: "+file.getAbsolutePath());
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
        drawImage(true);
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
           drawImage(true);
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
            m2 = Imgcodecs.imread(selectedFile.getAbsolutePath());
            Mat newM = Stitcher.stitch(m,m2, automaticStitching);
            if (newM!=m) {
                m=newM;
                canvas.setWidth(m.width());
                drawImage(true);
            }
        } else {
            System.out.println("Nie wczytano poprawnego pliku!");
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
        brightnessUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/brightness up.png")));
        brightnessDown.setDisable(false);
        brightnessDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/brightness down.png")));
        contrastDown.setDisable(false);
        contrastDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/contrast down.png")));
        contrastUp.setDisable(false);
        contrastUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/contrast up.png")));
        saturationUp.setDisable(false);
        saturationUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/saturation up.png")));
        saturationDown.setDisable(false);
        saturationDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/saturation down.png")));
        zoomin.setDisable(false);
        zoomin.setImage(new Image(Controller.class.getResourceAsStream("/icons/zoomin.png")));
        zoomout.setDisable(false);
        zoomout.setImage(new Image(Controller.class.getResourceAsStream("/icons/zoomout.png")));
        rotate.setDisable(false);
        rotate.setImage(new Image(Controller.class.getResourceAsStream("/icons/rotate.png")));
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
        brightnessUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/brightness up3.png")));
        brightnessDown.setDisable(true);
        brightnessDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/brightness down3.png")));
        contrastDown.setDisable(true);
        contrastDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/contrast down3.png")));
        contrastUp.setDisable(true);
        contrastUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/contrast up3.png")));
        saturationUp.setDisable(true);
        saturationUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/saturation up3.png")));
        saturationDown.setDisable(true);
        saturationDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/saturation down3.png")));
        zoomin.setDisable(true);
        zoomin.setImage(new Image(Controller.class.getResourceAsStream("/icons/zoomin3.png")));
        zoomout.setDisable(true);
        zoomout.setImage(new Image(Controller.class.getResourceAsStream("/icons/zoomout3.png")));
        rotate.setDisable(true);
        rotate.setImage(new Image(Controller.class.getResourceAsStream("/icons/rotate3.png")));
        save.setDisable(true);
        saveAs.setDisable(true);
        zoomLabel.setVisible(true);
        undoMenuItem.setDisable(true);
        redoMenuItem.setDisable(true);
        redoImageView.setDisable(true);
        redoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/redo3.png")));
        undoImageView.setDisable(true);
        undoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/undo3.png")));
        closeMenuItem.setDisable(true);
        encrypt.setDisable(true);
        autoStitchingMenuItem.setDisable(true);
        autoStitchingMenuItem.setDisable(true);
    }

    public void brightnessDownPressed()
    {
        brightnessDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/brightness down2.png")));
    }

    public void brightnessDownReleased()
    {
        brightnessDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/brightness down.png")));
    }

    public void brightnessUpPressed()
    {
        brightnessUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/brightness up2.png")));
    }

    public void brightnessUpReleased()
    {
        brightnessUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/brightness up.png")));
    }

    public void contrastDownPressed()
    {
        contrastDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/contrast down2.png")));
    }

    public void contrastDownReleased()
    {
        contrastDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/contrast down.png")));
    }

    public void contrastUpPressed()
    {
        contrastUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/contrast up2.png")));
    }

    public void contrastUpReleased()
    {
        contrastUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/contrast up.png")));
    }
    public void saturationDownPressed()
    {
        saturationDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/saturation down2.png")));
    }

    public void saturationDownReleased()
    {
        saturationDown.setImage(new Image(Controller.class.getResourceAsStream("/icons/saturation down.png")));
    }

    public void saturationUpPressed()
    {
        saturationUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/saturation up2.png")));
    }

    public void saturationUpReleased()
    {
        saturationUp.setImage(new Image(Controller.class.getResourceAsStream("/icons/saturation up.png")));
    }

    public void undoPressed()
    {
        undoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/undo2.png")));
    }

    public void undoReleased()
    {
        undoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/undo.png")));
    }

    public void redoPressed()
    {
        redoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/redo2.png")));
    }

    public void redoReleased()
    {
        redoImageView.setImage(new Image(Controller.class.getResourceAsStream("/icons/redo.png")));
    }

    public void zoominPressed()
    {
        zoomin.setImage(new Image(Controller.class.getResourceAsStream("/icons/zoomin2.png")));
    }

    public void zoominReleased()
    {
        zoomin.setImage(new Image(Controller.class.getResourceAsStream("/icons/zoomin.png")));
    }

    public void zoomoutPressed()
    {
        zoomout.setImage(new Image(Controller.class.getResourceAsStream("/icons/zoomout2.png")));
    }

    public void zoomoutReleased()
    {
        zoomout.setImage(new Image(Controller.class.getResourceAsStream("/icons/zoomout.png")));
    }

    public void rotatePressed()
    {
        rotate.setImage(new Image(Controller.class.getResourceAsStream("/icons/rotate2.png")));
    }

    public void rotateReleased()
    {
        rotate.setImage(new Image(Controller.class.getResourceAsStream("/icons/rotate.png")));
    }
}