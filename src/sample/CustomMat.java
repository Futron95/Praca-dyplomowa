package sample;

import org.opencv.core.Mat;

public class CustomMat extends Mat {
    public double scale;
    public double angle;
    public double avgPixelDifference;

    public CustomMat ()
    {
        scale = 1.0;
        angle = 0.0;
        avgPixelDifference = 0.0;
    }
}
