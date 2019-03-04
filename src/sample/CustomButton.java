package sample;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class CustomButton
{
    private ImageView imgView;
    public Image enabledIcon, pressedIcon, disabledIcon;
    public String name;

    public CustomButton(ImageView imgView, String name)
    {
        this.imgView = imgView;
        this.name = name;
        setIcons(name);
        imgView.setOnMousePressed(e -> imgView.setImage(pressedIcon));
    }

    public void setIcons(String iconName)
    {
        enabledIcon = new Image(Controller.class.getResourceAsStream("/icons/"+ iconName +".png"));
        pressedIcon = new Image(Controller.class.getResourceAsStream("/icons/"+ iconName +"2.png"));
        disabledIcon = new Image(Controller.class.getResourceAsStream("/icons/"+ iconName +"3.png"));
    }

    public void disable()
    {
        imgView.setDisable(true);
        imgView.setImage(disabledIcon);
    }

    public void enable()
    {
        imgView.setDisable(false);
        release();
    }

    public void release()
    {
        imgView.setImage(enabledIcon);
    }
}
