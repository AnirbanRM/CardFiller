package sample;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class Element{

    static int DEFAULT_FONT_SIZE = 13;

    String entity;
    double X,Y;
    Font font;
    int size;
    Color color;
    boolean isBold,isItalic,isUnderline;

    Element(String entity, double X, double Y){
        this.entity = entity;
        this.X = X;
        this.Y = Y;
        this.size = DEFAULT_FONT_SIZE;
        this.color = Color.BLACK;
        this.font = new Font(this.size);
        this.isBold = false;
        this.isItalic = false;
        this.isUnderline = false;
    }
}
