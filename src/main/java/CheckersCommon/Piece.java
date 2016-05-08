package CheckersCommon;

/**
 * Created by Slid on 4/13/2016.
 */
public class Piece implements java.io.Serializable  {
    private final Colour colour;
    private volatile boolean isKing;

    public Piece(Colour colour)
    {
        isKing = false;
        this.colour = colour;
    }

    public Colour getColour() {
        return colour;
    }

    public Colour getOpposingColour(){
        if(this.colour == Colour.red)
            return Colour.black;
        else
            return Colour.red;
    }

    public boolean getIsKing()
    {
        return isKing;
    }

    public void makeKing()
    {
        if(isKing)
            throw new IllegalStateException("Cannot make a piece king twice");
        isKing = true;
    }
}
