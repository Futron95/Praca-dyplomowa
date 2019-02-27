package sample.Converters;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import sample.CustomMat;
import sample.Interfaces.ProgressScene;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.getRotationMatrix2D;

public class Stitcher
{
    private static Point p1, p2;    //punkty kontrolne wskazujące odpowiadające sobie miejsca na dwóch obrazach, umożliwiając poprawne dopasowanie obrazów do siebie
    private static Mat om1, om2, m1, m2, m3, preparedMat, finalMat;
    private static GraphicsContext gc;
    private static MatOfByte matOfByte;
    private static ArrayList<CustomMat> customMats;
    private static final int M2TARGET_WIDTH = 500;
    private static double scale, m2Scale = 1.0;
    private static Canvas canvas;
    private static ScrollPane scrollPane;
    private static boolean automaticStitching;
    private static int m1CenterX, m1CenterY;
    private static Stage window;
    private static ProgressScene progressScene;
    private static final int controlPointsMatsSize = 400;

    private static void draw()
    {
        Imgcodecs.imencode(".bmp", getDisplayMat(), matOfByte);
        scrollPane.setMaxWidth(canvas.getWidth()+2);
        gc.drawImage(new Image(new ByteArrayInputStream(matOfByte.toArray())),0,0);
    }

    private static void checkOrder()
    {
        if (m1.width()-p1.x > m2.width()-p2.x)
        {
            Mat tempMat = m1;
            m1 = m2;
            m2 = tempMat;
            Point tempPoint = p1;
            p1 = p2;
            p2 = tempPoint;
        }
    }

    private static void createCustomMats()
    {
        Platform.runLater(() -> progressScene.processLabel.setText("Tworzenie obrazów do dopasowania"));
        om1 = m1.clone();
        om2 = m2.clone();
        customMats = new ArrayList<>();
        m2 = m2.submat(Range.all(), new Range((int)(p2.x), m2.width()-(int)(m1.width()-p1.x)) );     // kadrowanie dopasowywanego obrazu od zaznaczonego punktu do szerokości fragmentu pierwszego obrazu z którym będzie porównywany
        if (m2.width() > M2TARGET_WIDTH) {
            m2Scale = m2.width() / M2TARGET_WIDTH;
            Imgproc.resize(m2, m2, new Size(M2TARGET_WIDTH, m2.height()/m2Scale), INTER_CUBIC);
            Imgproc.resize(m1, m1, new Size(m1.width()/m2Scale, m1.height()/m2Scale), INTER_CUBIC);
        }
        for (double scale = 0.9; scale < 1.21; scale += 0.01)
            addScaledMats(scale);
    }

    private static void addScaledMats(double scale)
    {
        Mat sm = new Mat();     //nowy obiekt który przechowa przeskalowaną macierz na podstawie której będą tworzone jej wersje z dołożoną rotacją
        Imgproc.resize(m2, sm, new Size(m2.width()*scale, m2.height()*scale), 0,0, INTER_CUBIC);      //tworzenie 21 różnych wersji wielkości dopasowywanego obrazu
        double yControl = p2.y/m2Scale*scale;                                                              //określanie gdzie po przeskalowaniu znajduje się zaznaczony punkt
        for (double angle = -4; angle<=4; angle += 1.0)
        {
            CustomMat am = new CustomMat();
            Imgproc.warpAffine(sm, am, Imgproc.getRotationMatrix2D(new Point(0.0, yControl), angle, 1.0), sm.size(), INTER_CUBIC);        //tworzenie różnych wersji dopasowywanego obrazu poprzez obracanie go między -4 a 4 stopnie
            am.angle = angle;
            am.scale = scale;
            customMats.add(am);
            progressScene.increaseProgress();
            //Imgcodecs.imwrite("D:\\rozne\\wszelakie fociaste\\testowe\\custom mats\\scale "+scale+" angle "+angle+".png", am);
        }
    }

    private static CustomMat findBestCustomMat()
    {
        m1CenterX = (int)(p1.x/m2Scale);
        m1CenterY = (int)(p1.y/m2Scale);
        Platform.runLater(() -> progressScene.processLabel.setText("Określanie najlepszego dopasowania"));
        customMats.parallelStream().forEach(m ->
        {
            int m2CenterY;
            int m1StartY, m2StartY;
            int commonRows, commonColumns;
            m2CenterY = (int)(p2.y/m2Scale*m.scale);
            commonColumns = m.width() < m1.width()-m1CenterX ? m.width() : m1.width()-m1CenterX;
            commonRows = m.height()-m2CenterY < m1.height()-m1CenterY ? m.height()-m2CenterY : m1.height()-m1CenterY;
            if (m1CenterY < m2CenterY)
            {
                m1StartY = 0;
                m2StartY = m2CenterY-m1CenterY;
                commonRows += m1CenterY;
            }
            else
            {
                m1StartY = m1CenterY-m2CenterY;
                m2StartY = 0;
                commonRows += m2CenterY;
            }
            int allComparedPixels = 0;
            for (int y = 0; y < commonRows; y++)
            {
                int lineComparedPixels = 0;
                for (int x = 0; x < commonColumns && lineComparedPixels<10; x++)
                {
                    double pixelDifference = comparePixelsIfNotBlack(m1.get(m1StartY+y, m1CenterX +x ), m.get(m2StartY+y, x));
                    if (pixelDifference != -1)
                    {
                        m.avgPixelDifference += pixelDifference;
                        lineComparedPixels++;
                    }
                }
                allComparedPixels+=lineComparedPixels;
            }
            m.avgPixelDifference /= allComparedPixels;
            progressScene.increaseProgress();
        });
        CustomMat bestCustomMat = new CustomMat();
        bestCustomMat.avgPixelDifference = Double.MAX_VALUE;
        for (CustomMat m : customMats) {
            if (m.avgPixelDifference < bestCustomMat.avgPixelDifference) {
                bestCustomMat = m;
            }
        }
        return bestCustomMat;
    }

    private static double comparePixels(double[] rgb1, double[] rgb2)
    {
        double difference =  Math.abs(rgb1[0]-rgb2[0]);
        difference += Math.abs(rgb1[1]-rgb2[1]);
        difference += Math.abs(rgb1[2]-rgb2[2]);
        return difference;
    }

    private static double comparePixelsIfNotBlack(double[] rgb1, double[] rgb2)
    {
        if (rgb2[0]+rgb2[1]+rgb2[2]==0)
            return -1;
        return comparePixels(rgb1, rgb2);
    }

    private static double getMatsDifference(byte[] mat1, byte[] mat2, int x1, int y1, int x2, int y2)
    {
        double difference = 0;
        int pixelsCount = 0;
        int i1, i2;                     //obrazy są zapisane w postaci tablic bajtów, więc dostanie się do danego subpixela wymaga obliczenia indeksu na podstawie współrzędnych x i y
        for (int x = -20; x<20; x++)
        {
            for (int y = -70; y<=70; y++)
            {
                i1 = ((y1+y)*controlPointsMatsSize+x1+x)*3;
                i2 = ((y2+y)*controlPointsMatsSize+x2+x)*3;
                if (i1<0 || i1>mat1.length || i2<0 || i2>mat2.length)       //jeżeli któryś ze sprawdzanych  pixeli jest poza tablicą, sprawdź następny pixel
                    continue;
                difference += Math.abs((mat1[i1]&255) - (mat2[i2]&255));
                difference += Math.abs((mat1[i1+1]&255) - (mat2[i2+1]&255));
                difference += Math.abs((mat1[i1+2]&255) - (mat2[i2+2]&255));
                pixelsCount++;
            }
        }
        return difference/pixelsCount;
    }

    private static void setControlPoints()
    {
        Platform.runLater(() -> progressScene.processLabel.setText("Ustalanie punktów kontrolnych"));
        Mat ma = m1.clone();
        Mat mb = m2.clone();
        int commonFieldX, commonFieldY, commOnFieldWidth, commonFieldHeight, commonFieldCenterX, commonFieldCenterY;
        int bestCenterX=0, bestCenterY=0, bestY=0, bestX=0;
        double difference, bestDifference = Double.MAX_VALUE;
        Imgproc.resize(ma, ma, new Size(controlPointsMatsSize, controlPointsMatsSize));
        Imgproc.resize(mb, mb, new Size(controlPointsMatsSize, controlPointsMatsSize));
        byte[] matrix1 = getSubpixelsByteArray(ma);
        byte[] matrix2 = getSubpixelsByteArray(mb);
        int shiftX = (int) (controlPointsMatsSize *0.8);
        for (int x = -shiftX; x<=shiftX; x++)
        {
            if (x == -10)
                x = 10;
            for (int y = -shiftX/5; y<=shiftX/5; y++)
            {
                commonFieldX = 0>x ? 0 : x;
                commonFieldY = 0>y ? 0 : y;
                commOnFieldWidth = controlPointsMatsSize - Math.abs(x);
                commonFieldHeight = controlPointsMatsSize - Math.abs(y);
                commonFieldCenterX = commonFieldX + commOnFieldWidth/2;
                commonFieldCenterY = commonFieldY + commonFieldHeight/2;
                difference = getMatsDifference(matrix1, matrix2, commonFieldCenterX, commonFieldCenterY, commonFieldCenterX-x, commonFieldCenterY-y);
                if (difference<bestDifference)
                {
                    bestDifference = difference;
                    bestCenterX = commonFieldCenterX;
                    bestCenterY = commonFieldCenterY;
                    bestX = x;
                    bestY = y;
                }

            }
            progressScene.increaseProgress();
        }
        p1 = new Point(m1.width()*bestCenterX/ controlPointsMatsSize, m1.height()*bestCenterY/ controlPointsMatsSize);
        p2 = new Point(m2.width()*(bestCenterX-bestX)/ controlPointsMatsSize, m2.height()*(bestCenterY-bestY)/ controlPointsMatsSize);
    }



    private static void createWindow()
    {
        window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Zszywanie dwóch obrazów");
    }

    private static void manualStitching()
    {
        if (m1.height()!=m2.height())
            return;
        m3 = new Mat();
        Core.hconcat(Arrays.asList(m1,m2),m3);
        double m3width = m3.width();
        scale = 1.0;
        while (m3width*scale>3000)
        {
            scale *= 0.5;
        }
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        window.setMinWidth(512);
        window.setMaxWidth(screenSize.width);
        window.setMaxHeight(screenSize.height);
        Label label = new Label("Zaznacz punkt wspólny obu obrazów.");
        Button confirmationButton = new Button("ok");
        confirmationButton.setDisable(true);
        confirmationButton.setOnAction(event -> stitchingAlgorithm());
        matOfByte = new MatOfByte();
        canvas = new Canvas(m1.width()+m2.width(), m1.height());
        canvas.setOnMouseClicked( e ->
                {
                    double x = e.getX();
                    double y = e.getY();
                    if (x <= m1.width()*scale)
                        p1 = new Point(x/scale,y/scale);
                    else
                        p2 = new Point (x/scale-m1.width(),y/scale);
                    draw();
                    if (p1!=null && p2!=null)
                        confirmationButton.setDisable(false);
                }
        );
        canvas.setOnScroll(Stitcher::handle);
        scrollPane = new ScrollPane(canvas);
        gc = canvas.getGraphicsContext2D();
        draw();
        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, confirmationButton, scrollPane);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }

    private static void initProgressWindow()
    {
        progressScene = new ProgressScene();
        window.setScene(progressScene.scene);
        window.setWidth(400);
        window.setHeight(200);
        window.setResizable(false);
    }

    private static void stitchingAlgorithm()
    {
        initProgressWindow();
        Thread thread = new Thread(() ->
        {
            progressScene.progressPointsLimit = 859;
            if (automaticStitching == true) {
                progressScene.progressPointsLimit += controlPointsMatsSize*1.6-20;
                setControlPoints();
            }
            checkOrder();
            createCustomMats();
            CustomMat bcm = findBestCustomMat();
            createPreparedMat(bcm);
            createFinalMat();
            Platform.runLater(() -> window.close());
        });
        thread.start();
        if (!window.isShowing())
            window.showAndWait();
    }

    public static Mat stitch(Mat mat1, Mat mat2, Boolean automaticStitching)
    {
        // m1 i m2 to mniejsze wersje obrazów tworzone w celu przyspieszenia sprawdzania poprawnosci zszywania,
        m1 = mat1.clone();
        m2 = mat2.clone();
        finalMat = mat1;
        Stitcher.automaticStitching = automaticStitching;
        createWindow();
        if (automaticStitching != true)
            manualStitching();
        else
            stitchingAlgorithm();
        return finalMat;
    }

    public static byte[] getSubpixelsByteArray(Mat m)
    {
        int channels = m.channels();
        int size = (int) (m.total() * channels);
        byte[] array = new byte[size];
        m.get(0,0, array);
        return array;
    }

    private static void createFinalMat()
    {
        Platform.runLater(() -> progressScene.processLabel.setText("Łączenie pełnowymiarowych obrazów"));
        int xBorder = (int)(p1.x-p2.x);
        int yBorder = (int)(p1.y-p2.y);
        int i2, width2 = preparedMat.width();
        byte[] preparedMatrix = getSubpixelsByteArray(preparedMat);
        double gradientCounter;
        finalMat = new Mat(om1.height(), xBorder+preparedMat.width(), om1.type(), new Scalar(0));
        om1.copyTo(new Mat(finalMat, new Rect(0,0,om1.width(),om1.height())));
        int rows = finalMat.height();
        int i1, width1 = finalMat.width();
        byte[] finalMatrix = getSubpixelsByteArray(finalMat);
        double startingProgressPoint = progressScene.progressPoints;
        for (int y = 0; y < rows; y++)
        {
            gradientCounter = 0.1;
            for (int x = xBorder; x < finalMat.width(); x++)
            {
                int x2 = x - xBorder;
                int y2 = y - yBorder;
                i2 = (x2+y2*width2)*preparedMat.channels();
                if (i2<0 || i2>=preparedMatrix.length || (preparedMatrix[i2] | preparedMatrix[i2+1] | preparedMatrix[i2+2]) == 0)
                    continue;
                i1 = (x+y*width1)*finalMat.channels();
                if (gradientCounter < 1.0 && x<om1.width()) {
                    finalMatrix[i1]   = (byte)((finalMatrix[i1]&255)*(1.0-gradientCounter) + (preparedMatrix[i2]&255)*gradientCounter);
                    finalMatrix[i1+1] = (byte)((finalMatrix[i1+1]&255)*(1.0-gradientCounter) + (preparedMatrix[i2+1]&255)*gradientCounter);
                    finalMatrix[i1+2] = (byte)((finalMatrix[i1+2]&255)*(1.0-gradientCounter) + (preparedMatrix[i2+2]&255)*gradientCounter);
                    gradientCounter += 0.1;
                } else {
                    finalMatrix[i1] = preparedMatrix[i2];
                    finalMatrix[i1+1] = preparedMatrix[i2+1];
                    finalMatrix[i1+2] = preparedMatrix[i2+2];
                }
            }
            progressScene.progressPoints = startingProgressPoint+(y*1.0/rows*300);
            progressScene.updateProgressBar();
        }
        finalMat.put(0,0, finalMatrix);
        //Imgproc.rectangle(finalMat, new Point(p1.x-2, p1.y-2), new Point(p1.x+2, p1.y+2), new Scalar(255,0,0));
        p1 = null;
        p2 = null;
    }

    public static Point getRotatedPointCoordinates(Point p, double angle)
    {
        double cos = Math.cos(Math.toRadians(angle));
        double sin = Math.sin(Math.toRadians(angle));
        double x = p.x*cos - p.y*sin;
        double y = p.x*sin + p.y*cos;
        return new Point(x,y);
    }

    public static Point getRotatedPointCoordinates(Point p1, Point p2, double angle)
    {
        p2.x -= p1.x;
        p2.y -= p1.y;
        getRotatedPointCoordinates(p2, angle);
        p2.x += p1.x;
        p2.y += p1.y;
        return p2;
    }

    public static Size getBoundingBoxSize(Mat m, double angle)
    {
        Point p0 = new Point(0,0);
        Point p1 = new Point( 0, m.height());
        Point p2 = new Point (m.width(), m.height());
        Point p3 = new Point( m.width(), 0);
        p1 = getRotatedPointCoordinates(p1, angle);
        p2 = getRotatedPointCoordinates(p2, angle);
        p3 = getRotatedPointCoordinates(p3, angle);
        double minX = p0.x < p1.x ? p0.x : p1.x;
        if (p2.x<minX) minX = p2.x;
        if (p3.x<minX) minX = p3.x;
        double minY = p0.y < p1.y ? p0.y : p1.y;
        if (p2.y<minY) minY = p2.y;
        if (p3.y<minY) minY = p3.y;
        double maxX = p0.x > p1.x ? p0.x : p1.x;
        if (p2.x>maxX) maxX = p2.x;
        if (p3.x>maxX) maxX = p3.x;
        double maxY = p0.y > p1.y ? p0.y : p1.y;
        if (p2.y>maxY) maxY = p2.y;
        if (p3.y>maxY) maxY = p3.y;
        double width = Math.abs(minX-maxX);
        double height = Math.abs(minY-maxY);
        return new Size(width, height);
    }

    private static void createPreparedMat(CustomMat bcm)
    {
        Platform.runLater(() -> progressScene.processLabel.setText("Przygotowywanie pełnowymiarowego obrazu do nałożenia"));
        om2 = om2.submat(Range.all(), new Range((int) (p2.x), om2.width()-1));
        Imgproc.resize(om2, om2, new Size(om2.width()*bcm.scale, om2.height()*bcm.scale));
        p2 = new Point(0, Stitcher.p2.y*bcm.scale);
        if (bcm.angle == 0) {
            preparedMat = om2;
            return;
        }
        Size newSize = getBoundingBoxSize(om2, bcm.angle);
        Point translateCoordinates = new Point((int)((newSize.width-om2.width())/2),(int)((newSize.height-om2.height())/2));
        Point centerPoint = new Point(newSize.width/2, newSize.height/2);
        p2.x += translateCoordinates.x;
        p2.y += translateCoordinates.y;
        p2 = getRotatedPointCoordinates(centerPoint, p2, -bcm.angle);
        preparedMat = new Mat(newSize, om2.type(), new Scalar(0));
        Rect roi = new Rect(translateCoordinates, om2.size());
        om2.copyTo(new Mat(preparedMat, roi));
        Imgproc.warpAffine(preparedMat, preparedMat, getRotationMatrix2D(centerPoint, bcm.angle, 1.0), preparedMat.size(), INTER_NEAREST);
        progressScene.increaseProgress();
        //Imgproc.rectangle(preparedMat, new Point(p2.x-2, p2.y-2), new Point(p2.x+2, p2.y+2), new Scalar(255,0,0));
        //Imgcodecs.imwrite("D:\\rozne\\wszelakie fociaste\\testowe\\finalMatCommonPoint.jpg", preparedMat);
    }

    private static Mat getDisplayMat()
    {
        Mat sm = new Mat();
        if (scale != 1)
            Imgproc.resize(m3, sm, new Size(m3.width()*scale, m3.height()*scale),0,0, INTER_NEAREST);
        else
            sm = m3.clone();
        canvas.setHeight(sm.height());
        canvas.setWidth(sm.width());
        if (p1!=null)
            Imgproc.rectangle(sm, new Point(scale*p1.x-2, scale*p1.y-2), new Point(scale*p1.x+2, scale*p1.y+2), new Scalar(0,255,0));
        if (p2!=null)
            Imgproc.rectangle(sm, new Point(scale*(p2.x+m1.width())-2, scale*p2.y-2), new Point(scale*(p2.x+m1.width())+2, scale*p2.y+2), new Scalar(0,255,0));
        return sm;
    }

    private static void scaleUp()
    {
        if (m3.width()*scale < 10000 || scale<1.0) {
            scale *= 2;
            draw();
        }
    }

    private static void scaleDown()
    {
        if (m3.width()*scale > 500) {
            scale *= 0.5;
            draw();
        }
    }

    private static void handle(ScrollEvent e) {
        if (e.isControlDown()) {
            if (e.getDeltaY() > 0)
                scaleUp();
            else
                scaleDown();
        }
    }
}