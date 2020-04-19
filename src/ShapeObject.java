import java.awt.*;
import java.io.Serializable;


class ShapeObject implements Serializable{
    //STATIC FIELDS
    public static final String PENCIL = "PENCIL", ERASER = "ERASER", FILL = "FILL";
    private static int currentStrokeSize = 1;
    private static Color currentColor = Color.black;
    private static String toolType = PENCIL;

    //NON STATIC FIELDS
    private int x1, y1, x2, y2, stroke;
    private Color col;
    public ShapeObject(int x1, int y1, int x2, int y2){
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.stroke = currentStrokeSize;
        this.col = currentColor;
    }

    //NON STATIC METHODS
    public int getX1() {
        return x1;
    }

    public int getY1() {
        return y1;
    }

    public int getX2() {
        return x2;
    }

    public int getY2() {
        return y2;
    }

    public int getStroke() {
        return stroke;
    }

    public Color getCol() {
        return col;
    }


    //STATIC METHODS
    public static String getToolType(){return toolType;}

    public static void setStroke(int stroke) {
        currentStrokeSize = stroke;
    }

    public static void setColor(Color col){
        currentColor = col;
        //System.out.println(col);
    }

    public static void setToolType(String tool){
        if (tool.equals("PENCIL") || tool.equals("ERASER") || tool.equals("FILL")){
            toolType = tool;
        }
    }
}
