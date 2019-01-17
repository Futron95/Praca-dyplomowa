package sample;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;

import static org.opencv.imgproc.Imgproc.INTER_LANCZOS4;
import static org.opencv.imgproc.Imgproc.INTER_NEAREST;

public class Stitcher
{
    private static Point p1, p2;
    private static Mat om1, om2, m1, m2, m3;
    private static GraphicsContext gc;
    private static MatOfByte matOfByte;
    private static ArrayList<CustomMat> customMats;
    private static final int M2TARGET_WIDTH = 100;
    private static double scale, m2Scale = 1.0;
    private static Canvas canvas;

    private static void draw()
    {
        Imgcodecs.imencode(".bmp", getDisplayMat(), matOfByte);
        gc.drawImage(new Image(new ByteArrayInputStream(matOfByte.toArray())),0,0);
    }

    private static void createCustomMats()
    {
        customMats = new ArrayList<>();
        m2 = m2.submat(Range.all (), new Range((int)(p2.x-m1.width()), m2.width()-(int)(m1.width()-p1.x)) );     // kadrowanie dopasowywanego obrazu od zaznaczonego punktu do szerokości fragmentu pierwszego obrazu z którym będzie porównywany
        if (m2.width() >= 2* M2TARGET_WIDTH) {
            m2Scale = m2.width() / M2TARGET_WIDTH;
            Imgproc.resize(m2, m2, new Size(M2TARGET_WIDTH, m2.height()/m2Scale));
            Imgproc.resize(m1, m1, new Size(m1.width()/m2Scale, m1.height()/m2Scale));
        }
        int cm = 1;
        for (double scale = 1.0; scale < 1.21; scale += 0.01)
        {
            Mat sm = new Mat();     //nowy obiekt który przechowa przeskalowaną macierz na podstawie której będą tworzone jej wersje z dołożoną rotacją
            Imgproc.resize(m2, sm, new Size(m2.width()*scale, m2.height()*scale), 0,0, INTER_NEAREST);      //tworzenie 21 różnych wersji wielkości dopasowywanego obrazu
            double yCenter = p2.y/m2Scale*scale;                                                                //określanie gdzie po przeskalowaniu znajduje się punkt zaznaczony punkt
            for (double angle = -4; angle<=4; angle += 1.0)
            {
                CustomMat am = new CustomMat();
                Imgproc.warpAffine(sm, am, Imgproc.getRotationMatrix2D(new Point(0.0, yCenter), angle, 1.0), sm.size(), INTER_LANCZOS4);        //tworzenie różnych wersji dopasowywanego obrazu poprzez obracanie go między -4 a 4 stopnie
                am.angle = angle;
                am.scale = scale;
                customMats.add(am);
                System.out.println("customMats: "+cm++);
                Imgcodecs.imwrite("D:\\rozne\\wszelakie fociaste\\testowe\\custom mats\\scale "+scale+" angle "+angle+".png", am);
            }
        }
    }

    private static void findBestCustomMat()
    {
        int m1CenterX = (int)(p1.x/m2Scale);
        int m1CenterY = (int)(p1.y/m2Scale);
        int m2CenterY;
        int m1StartX = m1CenterX, m1StartY, m2StartX=0, m2StartY;
        int commonRows, commonColumns;
        CustomMat bestCustomMat = new CustomMat();
        bestCustomMat.avgPixelDifference = Double.MAX_VALUE;
        int counter=1;
        for (CustomMat m: customMats)
        {
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
                    double pixelDifference = comparePixels(m1.get(m1StartY+y, m1StartX+x ), m.get(m2StartY+y, m2StartX+x));
                    if (pixelDifference != -1)
                    {
                        m.avgPixelDifference += pixelDifference;
                        lineComparedPixels++;
                    }
                }
                allComparedPixels+=lineComparedPixels;
            }
            m.avgPixelDifference /= allComparedPixels;
            if (m.avgPixelDifference < bestCustomMat.avgPixelDifference)
                bestCustomMat = m;
            System.out.println("CustomMat: "+counter++ +" Scale: "+m.scale+" Angle: "+m.angle+" avgPixelDifference: "+m.avgPixelDifference);
        }
        System.out.println("Lowest custom mat avg pixel difference: "+bestCustomMat.avgPixelDifference+" Scale: "+bestCustomMat.scale+" Angle: "+bestCustomMat.angle);
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
        // m1 i m2 beda pomniejszane w celu przyspieszenia sprawdzania poprawnosci zszywania,
        // om1 i om2 beda wykorzystane na koniec by stworzyc zszyty obraz oryginalnej wielkości
        m1 = om1 = mat1.clone();
        m2 = om2 = mat2.clone();
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
            createCustomMats();
            findBestCustomMat();
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
                        p2 = new Point (x/scale,y/scale);
                    draw();
                    if (p1!=null && p2!=null)
                        confirmationButton.setDisable(false);
                }
        );
        canvas.setOnScroll( e ->
        {
            if (e.isControlDown())
            {
                if (e.getDeltaY()>0)
                    scaleUp();
                else
                    scaleDown();
            }
        });
        ScrollPane scrollPane = new ScrollPane(canvas);
        scrollPane.setMaxWidth(m1.width()+m2.width()+15);
        gc = canvas.getGraphicsContext2D();
        draw();
        VBox layout = new VBox(10);
        layout.getChildren().addAll(label, confirmationButton, scrollPane);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
        return m1;
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
            Imgproc.rectangle(sm, new Point(scale*p2.x-2, scale*p2.y-2), new Point(scale*p2.x+2, scale*p2.y+2), new Scalar(0,255,0));
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
}
