package sample;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.util.Arrays;

public class Stitcher {
    public static Mat stitch(Mat m1, Mat m2)
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
    }

    private static int getPixelScore(double[] rgb1, double[] rgb2)
    {
        double score = 0.0;
        score += Math.abs(rgb1[0]-rgb2[0]);
        score += Math.abs(rgb1[1]-rgb2[1]);
        score += Math.abs(rgb1[2]-rgb2[2]);
        return (int)score;
    }
}
