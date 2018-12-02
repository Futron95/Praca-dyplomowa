package sample;

import org.opencv.core.Mat;

import java.util.Random;

public class Encoder {
    Random r;
    String codeString;
    double[] rgb;

    public Encoder()
    {
        r = new Random();
        int charcode;
        codeString = "";            //kod liter i znaków, który będzie używany do szyfrowania i odszyfrowywania obrazu
        for(int i=0;i<20;i++) {
                charcode = (r.nextInt(43) + 48);
                codeString += (char) charcode;
        }
        setSeed(codeString);
    }

    public void setSeed(String code)
    {
        long seed = 1;
        for (int i = 0; i<code.length(); i++)
            seed *= code.charAt(i);
        r = new Random(seed);
    }

    public void decode(Mat m, String code)
    {
        setSeed(code);
        encode(m);
    }

    public void encode(Mat m)
    {
        for (int y = 0; y < m.height(); y++)            //pętle przechodzące przez wszystkie piksele obrazu
            for (int x = 0; x < m.width(); x++) {
                rgb = m.get(y, x);                      //pobieranie wartości rgb danego pixela
                flipBits();
                m.put(y,x,rgb);
            }
    }

    private void flipBits()
    {
        for (int i = 0; i < 3; i++)     //pętla przechodząca przez subpixele
        {
            int flippingByte = r.nextInt()&0xFF;
            int subpixel = (int)rgb[i];
            int flippedSubpixel = subpixel^flippingByte;    //XOR między subpixelem a bajtem zmieniającym daje zaszyfrowany subpixel
            rgb[i] = (double)flippedSubpixel;
        }
    }
}
