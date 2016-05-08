package CheckersCommon;

import java.rmi.RemoteException;
import java.util.ArrayList;

/**
 * Created by Slid on 4/12/2016.
 */
public interface Game extends java.rmi.Remote {

    /**
     * Moves a peice
     * @param positions
     * This parameter must have all the moves the peice will do
     * Include the positions that the piece will jump over.
     * @return
     * @throws RemoteException
     */
    boolean movePiece(ArrayList<Integer[]> positions, String playersName) throws RemoteException;

    /**
     * Gets the current player
     * @return The name of the player
     * @throws RemoteException
     */
    String getCurrentPlayer() throws RemoteException;

    Piece[][] getBoard() throws RemoteException;

    PlayersGame joinGame(UserData player) throws RemoteException;

    void waitForTurn(String playerName) throws RemoteException, InterruptedException;

    boolean isGameOver() throws RemoteException;

    Colour getWinner() throws RemoteException;

    void leaveGame(String user) throws RemoteException;

    void killGame() throws RemoteException;
}
