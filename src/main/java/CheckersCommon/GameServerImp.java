package CheckersCommon;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by jgodbout on 4/15/2016.
 */
public class GameServerImp extends UnicastRemoteObject implements GameServer {
    private final Map<String,UserDataImp> users;
    private final Map<String, Game> games;
    private final String name;
    private volatile boolean shutDownServer;
    private final Semaphore s = new Semaphore(0);
    private Integer releaseSem;

    public GameServerImp(String name) throws RemoteException{
        this.name = name;
        this.games = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
        this.shutDownServer = false;
        this.releaseSem = 0;
    }

    //Have previous data
    public GameServerImp(String name, String uri) throws IOException{
        this.name = name;
        this.games = new ConcurrentHashMap<>();
        this.users = new ConcurrentHashMap<>();
        this.shutDownServer = false;
        this.releaseSem = new AtomicInteger(0);
        Files.lines(Paths.get(uri)).forEach(a -> {
            String[] userData = a.split(",");
                UserDataImp user;
            try {
                user = new UserDataImp(userData[0], userData[1]);
                users.put(user.getName(),user);
            }catch(RemoteException ex){
                throw new ExceptionInInitializerError(ex.toString());
            }
                user.setWins(Integer.parseInt(userData[2]));
                user.setLoss(Integer.parseInt(userData[3]));
            });
    }

    @Override
    public PlayersGame createGame(UserData user, String gameName) throws RemoteException {
        synchronized (releaseSem) {
            ++releaseSem;
            s.drainPermits();
        }
        if(!shutDownServer) {
            GameImp newGame = new GameImp(gameName, games);
            Game arrayGame = games.putIfAbsent(gameName, newGame);
            if (arrayGame == null){
                PlayersGame pg =  newGame.setFirstPlayer(user);
                if(pg == null)
                    games.remove(gameName);
                synchronized (s) {
                    --releaseSem;
                    if(releaseSem == 0)
                        s.release();
                }
                return pg;
            }
        }
        synchronized (releaseSem) {
            --releaseSem;
            if(releaseSem == 0)
                s.release();
        }
        return null;
    }

    @Override
    public  List<String> listCurrentGames() {
        if(!shutDownServer) {
            List<String> gameList = games.keySet().stream().collect(Collectors.toList());
            return Collections.unmodifiableList(gameList);
        }
        return Collections.unmodifiableList( new ArrayList<String>());
    }

    @Override
    public Menu login(String username, String password) throws RemoteException {
        if(username.equals(""))
            return null;
        synchronized (s) {
            ++releaseSem;
            s.drainPermits();
        }
        if(!shutDownServer) {
            UserDataImp newUser = new UserDataImp(username, password);
            UserDataImp oldUser = users.putIfAbsent(username, newUser);
            //This means that the user previously existed, so try to log in
            if ( oldUser != null ) {
                synchronized (s) {
                    --releaseSem;
                    if(releaseSem == 0)
                        s.release();
                }
                if (oldUser.login(password))
                    return new Menu(oldUser, this);
                else
                    return null;
            }

            //User is new, try to log in
            else if (oldUser == null) {
                synchronized (s) {
                    --releaseSem;
                    if(releaseSem == 0)
                        s.release();
                }
                if (newUser.login(password))
                    return new Menu(newUser, this);
            }
        }
        synchronized (s) {
            --releaseSem;
            if(releaseSem == 0)
                s.release();
        }
        return null;
    }

    @Override
    public Game retrieveGame(String gameName) throws RemoteException {
        if(!shutDownServer) {
            return games.get(gameName);
        }
        return null;
    }

    public String getName() {
        return name;
    }

    //Admin functions
    public void shutDown() throws InterruptedException {
        shutDownServer = true;

        s.acquire();
        Set<Map.Entry<String,Game>> es = games.entrySet();
        for(Map.Entry<String,Game> entry : es){
            try {
                entry.getValue().killGame();
            }catch(Exception ex){
                System.err.println(ex.toString());
            }
        }
        games.clear();

        //Games are finished, no user will have statistics change
        Collection<UserDataImp> userSet = users.values();
        try(  PrintWriter out = new PrintWriter( name + "CheckerData.txt" )  ){
            for (UserDataImp user: userSet) {
                Map<String, Integer> userStats =  user.userStatistics();
                String output = user.getName() + "," + user.getPassword() + "," + userStats.get("Wins") + "," + userStats.get("Losses");
                out.println(output);
            }
        }catch(IOException ex){
            System.err.println(ex.toString());
        }
        userSet.clear();
    }

    public int getTotalGames() {
        return games.size();
    }

    public int getNumberOfUsers() {
        return users.size();
    }

    public void killGame(String gameName) {
        Game game = games.remove(gameName);
        if(game != null){
            try{
                game.killGame();
            }catch(Exception ex){
                System.err.println(ex.toString());
            }
        }
    }
}
