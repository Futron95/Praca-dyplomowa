package sample;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.Mat;

import java.util.Random;

public class Encryptor {
    static Random r = new Random();
    static TextField codeField;
    static boolean okPressed;

    public static void displayWindow()
    {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Szyfrowanie");
        window.setWidth(300);
        window.setHeight(180);
        window.setResizable(false);
        Label label = new Label("Zaszyfruj lub odszyfruj obraz:");
        codeField = new TextField();
        codeField.setPromptText("Kod od 8 do 24 znaków");
        Button okButton = new Button("Ok");
        okButton.setOnAction(e->{
            if (codeField.getText().length()<8 || codeField.getText().length()>24)
                return;
            okPressed = true;
            window.close();
        });
        Button generateButton = new Button("Generuj");
        generateButton.setOnAction(event -> codeField.setText(getNewCode()));
        VBox layout = new VBox(20);
        HBox hBox = new HBox(10);
        hBox.setAlignment(Pos.CENTER);
        hBox.getChildren().addAll(generateButton, codeField);
        layout.setAlignment(Pos.CENTER);
        layout.getChildren().addAll(label, hBox, okButton);
        layout.setPadding(new Insets(20,20,20,20));
        Scene scene = new Scene(layout);
        window.setScene(scene);
        window.showAndWait();
    }

    private static String getNewCode()
    {
        int charCode;
        String code = "";            //kod liter i znaków, który będzie używany do szyfrowania i odszyfrowywania obrazu
        for(int i=0;i<16;i++) {
                charCode = (r.nextInt(79) + 48);
                code += (char) charCode;
        }
        return code;
    }

    private static long getSeed(String code)
    {
        long seed = 0;
        long charactersBits;
        for (int i = 0; i<code.length(); i++)
        {
            charactersBits = (long)(code.charAt(i));
            seed = seed | (charactersBits<<(i*7));
        }
        return seed;
    }

    public static boolean encrypt(Mat m)
    {
        okPressed = false;
        displayWindow();
        if (okPressed == false)
            return false;
        String code = codeField.getText();
        int size = (int)m.total() * m.channels();
        byte[] matrix = new byte[size];
        m.get(0, 0, matrix);
        byte[] randomMatrix = new byte[size];
        int usedCharacters = 0;
        int subcodeLength;
        int charactersLeft = code.length();
        int maxSubcodeLength = 9;
        while (charactersLeft > 0)
        {
            subcodeLength = charactersLeft > maxSubcodeLength ? maxSubcodeLength : charactersLeft;
            maxSubcodeLength--;
            r = new Random(getSeed(code.substring(usedCharacters,usedCharacters+subcodeLength)));
            r.nextBytes(randomMatrix);
            charactersLeft -= subcodeLength;
            usedCharacters += subcodeLength;
            for (int i = 0; i < size; i++)            //pętle przechodzące przez wszystkie piksele obrazu
                matrix[i] = (byte) (matrix[i] ^ randomMatrix[i]);
        }
        m.put(0,0,matrix);
        return true;
    }
}
