package CheckersCommon;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Slid on 4/20/2016.
 */
public class PlayersGame implements java.io.Serializable  {
    final Game game;
    final String user;
    final Colour colour;
    volatile boolean inGame;


    PlayersGame(Game game, String user, Colour colour) throws RemoteException {
        this.game = game;
        this.user = user;
        this.colour = colour;
        this.inGame = true;
    }

    public boolean movePiece(ArrayList<Integer[]> positions) throws RemoteException{
        if(isMyTurn()) {
            return game.movePiece(positions, user);
        }
        return false;
    }

    public synchronized Piece[][] getBoard() throws RemoteException{
        return game.getBoard();
    }

    public synchronized boolean isMyTurn() throws RemoteException{
        return game.getCurrentPlayer().equals(user);
    }

    public boolean getGameOver() throws RemoteException{
        return game.isGameOver();
    }

    public boolean getWon() throws RemoteException{
        return game.getWinner() == colour;
    }

    public Colour getColour(){
        return colour;
    }

    public void waitMyTurn(){
        try {
            game.waitForTurn(user);
        }catch(Exception ex)
        {
            System.err.println(ex.toString());
        }
    }

    public void leaveGame() throws RemoteException{
        game.leaveGame(user);
    }
}
