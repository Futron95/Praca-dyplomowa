package sample;

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
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.opencv.core.Core.flip;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;
import static org.opencv.imgproc.Imgproc.getRotationMatrix2D;

public class Stitcher
{
    private static Point p1, p2;
    private static Mat om1, om2, m1, m2, m3, preparedMat, finalMat;
    private static GraphicsContext gc;
    private static MatOfByte matOfByte;
    private static ArrayList<CustomMat> customMats;
    private static final int M2TARGET_WIDTH = 100;
    private static double scale, m2Scale = 1.0;
    private static Canvas canvas;
    private static ScrollPane scrollPane;
    private static boolean flipped;
    private static int m1CenterX, m1CenterY;

    private static void draw()
    {
        Imgcodecs.imencode(".bmp", getDisplayMat(), matOfByte);
        scrollPane.setMaxWidth(canvas.getWidth()+2);
        gc.drawImage(new Image(new ByteArrayInputStream(matOfByte.toArray())),0,0);
    }

    private static void createCustomMats()
    {
        flipped = false;
        if (p1.x<m1.width()/2)
        {
            flipped = true;
            Mat tempMat = new Mat();
            flip(m1, tempMat, 1);
            m1 = tempMat.clone();
            flip(m2,tempMat, 1);
            m2 = tempMat.clone();
            p1.x = m1.width()-p1.x;
            p2.x = m2.width()-p2.x;
        }
        if (p1.x<p2.x)
        {
            Mat tempMat = m1;
            m1 = m2;
            m2 = tempMat;
            Point tempPoint = p1;
            p1 = p2;
            p2 = tempPoint;
        }
        om1 = m1.clone();
        om2 = m2.clone();
        customMats = new ArrayList<>();
        m2 = m2.submat(Range.all (), new Range((int)(p2.x), m2.width()-(int)(m1.width()-p1.x)) );     // kadrowanie dopasowywanego obrazu od zaznaczonego punktu do szerokości fragmentu pierwszego obrazu z którym będzie porównywany
        if (m2.width() >= 2* M2TARGET_WIDTH) {
            m2Scale = m2.width() / M2TARGET_WIDTH;
            Imgproc.resize(m2, m2, new Size(M2TARGET_WIDTH, m2.height()/m2Scale));
            Imgproc.resize(m1, m1, new Size(m1.width()/m2Scale, m1.height()/m2Scale));
        }
        for (double scale = 0.9; scale < 1.21; scale += 0.01)
            addScaledMats(scale);
    }

    private static void addScaledMats(double scale)
    {
        Mat sm = new Mat();     //nowy obiekt który przechowa przeskalowaną macierz na podstawie której będą tworzone jej wersje z dołożoną rotacją
        Imgproc.resize(m2, sm, new Size(m2.width()*scale, m2.height()*scale), 0,0, INTER_NEAREST);      //tworzenie 21 różnych wersji wielkości dopasowywanego obrazu
        double yCenter = p2.y/m2Scale*scale;                                                                //określanie gdzie po przeskalowaniu znajduje się punkt zaznaczony punkt
        for (double angle = -4; angle<=4; angle += 1.0)
        {
            CustomMat am = new CustomMat();
            Imgproc.warpAffine(sm, am, Imgproc.getRotationMatrix2D(new Point(0.0, yCenter), angle, 1.0), sm.size(), INTER_NEAREST);        //tworzenie różnych wersji dopasowywanego obrazu poprzez obracanie go między -4 a 4 stopnie
            am.angle = angle;
            am.scale = scale;
            customMats.add(am);
            //Imgcodecs.imwrite("D:\\rozne\\wszelakie fociaste\\testowe\\custom mats\\scale "+scale+" angle "+angle+".png", am);
        }
    }

    private static CustomMat findBestCustomMat()
    {
        m1CenterX = (int)(p1.x/m2Scale);
        m1CenterY = (int)(p1.y/m2Scale);
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
                    double pixelDifference = comparePixels(m1.get(m1StartY+y, m1CenterX +x ), m.get(m2StartY+y, x));
                    if (pixelDifference != -1)
                    {
                        m.avgPixelDifference += pixelDifference;
                        lineComparedPixels++;
                    }
                }
                allComparedPixels+=lineComparedPixels;
            }
            m.avgPixelDifference /= allComparedPixels;
        });
        CustomMat bestCustomMat = new CustomMat();
        bestCustomMat.avgPixelDifference = Double.MAX_VALUE;
        for (CustomMat m : customMats)
            if (m.avgPixelDifference < bestCustomMat.avgPixelDifference)
                bestCustomMat = m;
        System.out.println("Lowest custom mat avg pixel difference: "+bestCustomMat.avgPixelDifference+" Scale: "+bestCustomMat.scale+" Angle: "+bestCustomMat.angle);
        return bestCustomMat;
    }


    private static double comparePixels(double[] rgb1, double[] rgb2)
    {
        if (rgb2[0]+rgb2[1]+rgb2[2]==0)
            return -1;
        double difference = 0.0;
        difference += Math.abs(rgb1[0]-rgb2[0]);
        difference += Math.abs(rgb1[1]-rgb2[1]);
        difference += Math.abs(rgb1[2]-rgb2[2]);
        return difference;
    }

    public static Mat stitch (Mat mat1, Mat mat2)
    {
        // m1 i m2 to mniejsze wersje obrazów tworzone w celu przyspieszenia sprawdzania poprawnosci zszywania,
        m1 = mat1.clone();
        m2 = mat2.clone();
        finalMat = mat1;
        m3 = new Mat();
        Core.hconcat(Arrays.asList(m1,m2),m3);
        double m3width = m3.width();
        scale = 1.0;
        while (m3width*scale>3000)
        {
            scale *= 0.5;
        }
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Zszywanie dwóch obrazów");
        window.setMinWidth(512);
        window.setMaxWidth(1920);
        window.setMaxHeight(1080);
        Label label = new Label("Zaznacz punkt wspólny obu obrazów.");
        Button confirmationButton = new Button("ok");
        confirmationButton.setDisable(true);
        confirmationButton.setOnAction(event -> {
            confirmationButton.setDisable(true);
            canvas.setDisable(true);
            createCustomMats();
            CustomMat bcm = findBestCustomMat();
            createPreparedMat(bcm);
            createFinalMat();
            window.close();
        });
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
        return finalMat;
    }

    private static double[] getGradientPixel (double[] rgb1, double[] rgb2, double gradientCounter)
    {
        if (rgb1==null)
            return rgb2;
        double[] returnRgb = new double[3];
        returnRgb[0] = rgb1[0]*(1.0-gradientCounter)+ rgb2[0]*gradientCounter;
        returnRgb[1] = rgb1[1]*(1.0-gradientCounter)+ rgb2[1]*gradientCounter;
        returnRgb[2] = rgb1[2]*(1.0-gradientCounter)+ rgb2[2]*gradientCounter;
        return returnRgb;
    }

    private static void createFinalMat()
    {
        int xBorder = (int)p1.x-(int)p2.x;
        int yBorder = (int)p1.y-(int)p2.y;
        double[] rgb1, rgb2;
        double gradientCounter;
        finalMat = new Mat(om1.height(), xBorder+preparedMat.width(), om1.type());
        for (int y = 0; y < finalMat.height(); y++)
        {
            gradientCounter = 0.1;
            for (int x = 0; x < finalMat.width(); x++)
            {
                if (x<om1.width())
                    rgb1 = om1.get(y, x);
                else
                    rgb1 = null;
                if (x < xBorder)
                    finalMat.put(y, x, rgb1);
                else
                {
                    int x2 = x - xBorder;
                    int y2 = y - yBorder;
                    if (y2<0 || x2<0)
                    {
                        if (rgb1!=null)
                            finalMat.put(y, x, rgb1);
                        else
                            finalMat.put(y, x, new double[]{0, 0, 0});
                    }
                    else
                    {
                        rgb2 = preparedMat.get(y2, x2);
                        if (rgb2[0] + rgb2[1] + rgb2[2] == 0) {
                            if (rgb1 != null)
                                finalMat.put(y, x, rgb1);
                            else
                                finalMat.put(y, x, new double[]{0, 0, 0});
                        }
                        else {
                            if (gradientCounter<1.0) {
                                finalMat.put(y, x, getGradientPixel(rgb1, rgb2, gradientCounter));
                                gradientCounter += 0.1;
                            }
                            else
                                finalMat.put(y, x, rgb2);
                        }
                    }

                }
            }
        }
        if (flipped)
        {
            Mat tempMat = new Mat();
            flip(finalMat, tempMat, 1);
            finalMat = tempMat;
        }
        p1 = null;
        p2 = null;
    }

    private static Point getRotatedPointCoordinates(Point p, double angle)
    {
        double cos = Math.cos(Math.toRadians(angle));
        double sin = Math.sin(Math.toRadians(angle));
        double x = p.x*cos - p.y*sin;
        double y = p.x*sin + p.y*cos;
        return new Point(x,y);
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

    private static void createPreparedMat(CustomMat bcm) {
        om2 = om2.submat(Range.all(), new Range((int) (p2.x), om2.width()-1));
        Imgproc.resize(om2, om2, new Size(om2.width()*bcm.scale, om2.height()*bcm.scale));
        Size newSize = getBoundingBoxSize(om2, bcm.angle);
        Point translateCoordinates = new Point((int)((newSize.width-om2.width())/2),(int)((newSize.height-om2.height())/2));
        p2 = new Point(0, Stitcher.p2.y*bcm.scale);
        Point centerPoint = new Point(newSize.width/2, newSize.height/2);
        if (bcm.angle == 0) {
            preparedMat = om2;
            return;
        }
        p2.x += translateCoordinates.x-centerPoint.x;
        p2.y += translateCoordinates.y-centerPoint.y;
        p2 = getRotatedPointCoordinates(p2, -bcm.angle);
        p2.x += centerPoint.x;
        p2.y += centerPoint.y;
        preparedMat = new Mat(newSize, om2.type());
        Rect roi = new Rect(translateCoordinates, om2.size());
        om2.copyTo(new Mat(preparedMat, roi));
        Imgproc.warpAffine(preparedMat, preparedMat, getRotationMatrix2D(centerPoint, bcm.angle, 1.0), preparedMat.size(), INTER_NEAREST);
        //Imgproc.rectangle(preparedMat, new Point(p2.x-2, p2.y-2), new Point(p2.x+2, p2.y+2), new Scalar(255,0,0));
        Imgcodecs.imwrite("D:\\rozne\\wszelakie fociaste\\testowe\\finalMatCommonPoint.jpg", preparedMat);
    }

    private static Mat getDisplayMat() {
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
