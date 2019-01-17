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
    private static Mat m1, m2, m3;
    private static GraphicsContext gc;
    private static MatOfByte matOfByte;
    private static ArrayList<Mat> customMats;
    private static final int M2WIDTH = 100;
    private static double scale = 1.0;
    private static Canvas canvas;

    private static void draw()
    {
        Imgcodecs.imencode(".bmp", getDisplayMat(), matOfByte);
        gc.drawImage(new Image(new ByteArrayInputStream(matOfByte.toArray())),0,0);
    }

    public static Mat stitch (Mat mat1, Mat mat2)
    {
        m1 = mat1;
        m2 = mat2;
        m3 = new Mat();
        Core.hconcat(Arrays.asList(m1,m2),m3);
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
            customMats = new ArrayList<>();
            m2 = m2.submat(Range.all (), new Range((int)(p2.x-m1.width()), m2.width()) );     // kadrowanie dopasowywanego obrazu od zaznaczonego punktu w prawo
            double m2Scale = 1.0;
            if (m2.width() >= 2*M2WIDTH) {
                m2Scale = m2.width() / M2WIDTH;
                Imgproc.resize(m2, m2, new Size(M2WIDTH, m2.height()/m2Scale));
            }
            int cm = 1;
            for (double scale = 0.9; scale <= 1.10; scale += 0.01)
            {
                Mat sm = new Mat();
                Imgproc.resize(m2, sm, new Size(m2.width()*scale, m2.height()*scale), 0,0, INTER_NEAREST);      //tworzenie 21 różnych wersji wielkości dopasowywanego obrazu
                double yCenter = p2.y/m2Scale*scale;                                                                //określanie gdzie po przeskalowaniu znajduje się punkt zaznaczony punkt
                for (double angle = -4; angle<=4; angle += 1.0)
                {
                    Mat am = new Mat();
                    Imgproc.warpAffine(sm, am, Imgproc.getRotationMatrix2D(new Point(0.0, yCenter), angle, 1.0), sm.size(), INTER_LANCZOS4);        //tworzenie różnych wersji dopasowywanego obrazu poprzez obracanie go między -4 a 4 stopnie
                    customMats.add(am);
                    System.out.println("customMats: "+cm++);
                    Imgcodecs.imwrite("D:\\rozne\\wszelakie fociaste\\testowe\\custom mats\\scale "+scale+" angle "+angle+".png", am);
                }
            }
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
        if (scale < 16)
            scale *= 2;
        draw();
    }

    private static void scaleDown()
    {
        if (scale > 1.0/16.0)
            scale *= 0.5;
        draw();
    }

    //funkcja tworząca jedną macierz pikseli z dwóch
/*    public static Mat stitch(Mat m1, Mat m2)
    {
        int bestColumnNr = 0;
        int bestColumnScore = Integer.MAX_VALUE;
        int columnScore;
        double[] rgb1, rgb2;

        for (int column = 0; column < m1.width(); column++)
        {
            columnScore=0;
            for (int row = 0; row < m1.height(); row++)
            {
                rgb1 = m1.get(row, column);
                rgb2 = m2.get(row, 0);
                columnScore += getPixelScore(rgb1, rgb2);
            }

            if (columnScore<bestColumnScore)
            {
                bestColumnNr = column;
                bestColumnScore = columnScore;
            }
        }
        double maxdifference = m1.height()*3*255;
        double similarity = (maxdifference-bestColumnScore)/maxdifference*100;
        System.out.println("Podobieństwo kolumny: "+similarity+"%");

        Mat m = new Mat();
        Core.hconcat(Arrays.asList(m1.submat(0,m1.height(),0,bestColumnNr),m2),m);
        System.out.println(m.width());
        return m;
    }*/

    //zwraca różnicę między dwoma pikselami na podstawie ich wartości rgb
    private static int getPixelScore(double[] rgb1, double[] rgb2)
    {
        double score = 0.0;
        score += Math.abs(rgb1[0]-rgb2[0]);
        score += Math.abs(rgb1[1]-rgb2[1]);
        score += Math.abs(rgb1[2]-rgb2[2]);
        return (int)score;
    }
}
