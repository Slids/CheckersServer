package CheckersCommon;

import java.rmi.RemoteException;
import java.util.Map;

/**
 * Created by Slid on 4/15/2016.
 */
public interface UserData extends java.rmi.Remote {
    boolean logout() throws RemoteException;
    Map<String,Integer> userStatistics() throws RemoteException;
    String getName() throws RemoteException;
    boolean isInGame() throws RemoteException;
    boolean setInGame() throws RemoteException;
    boolean setNotInGame(boolean won, boolean noWinner) throws RemoteException;
}
