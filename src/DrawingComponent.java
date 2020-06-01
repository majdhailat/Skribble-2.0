//imports
import java.awt.*;
import java.io.Serializable;

/*
Stores static information abut what tool, stroke size and colour the user currently has selected
Creates drawing components: little pieces with specific attributes (x, y, col, size)that make up the larger picture
Drawing component created when the user draws
A bunch of drawing component objects are stored in the drawing components array in the client/ server
 */
class DrawingComponent implements Serializable{
    //STATIC VARIABLES
    public static final String PENCIL = "PENCIL", ERASER = "ERASER";
    public static final int STROKE1 = 2, STROKE2 = 5, STROKE3 = 10, STROKE4 = 20;
    private static int selectedStrokeSize = STROKE2;//the stroke size that the user has currently selected
    private static Color selectedColour = Color.black;//the colour that the user has currently selected
    private static String selectedToolType = PENCIL;//the tool type that the user has currently selected

    //NON STATIC VARIABLES
    private int cx, cy, stroke;//the x, y and stroke size of this individual drawing component
    private Color col;//the colour of this individual drawing component

    /*
    Creates a new drawing component with specified position and currently selected stroke size and color
     */
    public DrawingComponent(int cx, int cy){
        this.cx = cx;
        this.cy = cy;
        this.stroke = selectedStrokeSize;//setting stroke ot current stroke
        if (selectedToolType.equals(ERASER)){//checking for eraser
            this.col = Color.white;//setting colour to white
        }else {
            this.col = selectedColour;//not eraser -> setting color to current colour
        }
    }

    //NON STATIC METHODS
    /*
    returns x pos
     */
    public int getCx(){return cx;}

    /*
    returns y pos
     */
    public int getCy(){return cy;}

    /*
    returns stroke size
     */
    public int getStroke() {return stroke;}

    /*
    returns colour
     */
    public Color getCol() {return col;}

    //STATIC METHODS
    /*
    returns selected tool type
     */
    static String getSelectedToolType(){return selectedToolType;}

    /*
    sets tool type
     */
    public static void setSelectedToolType(String tool){selectedToolType = tool;}

    /*
    returns selected stroke size
     */
    public static int getSelectedStrokeSize(){return selectedStrokeSize;}

    /*
    sets selected stroke size
     */
    public static void setSelectedStrokeSize(int stroke) {selectedStrokeSize = stroke;}

    /*
    sets colour
     */
    public static void setSelectedColour(Color col){selectedColour = col;}

    /*
    returns selected colour
     */
    public static Color getSelectedColour(){return selectedColour;}
}
