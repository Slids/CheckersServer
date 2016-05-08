package CheckersCommon;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Slid on 4/15/2016.
 */
public class UserDataImp  extends UnicastRemoteObject implements UserData{
    private final String username;
    private final String password;
    private int wins;
    private int loss;
    private boolean loggedIn;
    private boolean inGame;

    public UserDataImp(String name, String password) throws RemoteException {
        this.username = name;
        this.wins = 0;
        this.loss = 0;
        this.loggedIn = false;
        this.inGame = false;
        this.password = password;
    }

    @Override
    public String getName() throws RemoteException {
        return this.username;
    }

    public synchronized boolean login(String password){
        if(!password.equals(this.password))
            return false;
        if(!loggedIn) {
            loggedIn = true;
            return true;
        }
        return false;
    }

    public synchronized boolean logout(){
        if(!inGame) {
            if (loggedIn) {
                loggedIn = false;
                return true;
            }
            throw new IllegalStateException("Not logged in");
        }
        return false;
    }

    public synchronized boolean isLoggedIn(){
        return loggedIn;
    }

    public synchronized boolean isInGame(){ return inGame;}

    public synchronized boolean setInGame(){
        if(!inGame && loggedIn) {
            inGame = true;
            return true;
        }
        if(inGame)
            return false;
        throw new IllegalStateException("Cannot play a game while not logged in");
    }

    public synchronized boolean setNotInGame(boolean won, boolean noWinner) throws RemoteException{
        if(inGame) {
            inGame = false;
            if(won && !noWinner)
                ++wins;
            else if(!noWinner)
                ++loss;
            return true;
        }
        else
            return false;
    }

    @Override
    public synchronized Map<String, Integer> userStatistics() throws RemoteException {
        Map<String, Integer> userStatistics = new HashMap<>();
        userStatistics.put("Wins",wins);
        userStatistics.put("Losses",loss);
        return Collections.unmodifiableMap(userStatistics);
    }

    synchronized void setWins(int wins) {
        this.wins = wins;
    }

    synchronized void setLoss(int loss) {
        this.loss = loss;
    }

    String getPassword() {
        return password;
    }
}