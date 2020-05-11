import java.awt.*;
import java.io.Serializable;

//I WILL ADD COMMENTS LATER BECAUSE THERE IS A LOT MORE CODE TO ADD HERE
class DrawingComponent implements Serializable{
    //STATIC FIELDS
    public static final String PENCIL = "PENCIL", ERASER = "ERASER", FILL = "FILL";
    public static final int STROKE1 = 2, STROKE2 = 5, STROKE3 = 10, STROKE4 = 20;
    private static int currentStrokeSize = STROKE2;
    private static Color currentColor = Color.black;
    private static String toolType = PENCIL;

    //NON STATIC FIELDS
    private int cx, cy, stroke;
    private Color col;

    public DrawingComponent(int cx, int cy){
        this.cx = cx;
        this.cy = cy;
        this.stroke = currentStrokeSize;
        if (toolType.equals(ERASER)){
            this.col = Color.white;
        }else {
            this.col = currentColor;
        }
    }

    public int getCx(){
        return cx;
    }

    public int getCy(){
        return cy;
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

    public static int getCurrentStrokeSize(){
        return currentStrokeSize;
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
