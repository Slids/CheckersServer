package CheckersSocket;

import CheckersCommon.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Slid on 4/26/2016.
 */
public class ConnectionHandler implements Runnable {
    private final ServerSocket serv;
    private final Socket socket;
    private final GameServer gs;
    private static AtomicLong sId = new AtomicLong(0);
    private final ConcurrentHashMap<Long, Menu> userMenus;
    private final ConcurrentHashMap<Long, PlayersGame> userGames;
    volatile Long sessionId;
    private final String[] menuCalls = {"CreateGame", "ListCurrentGames", "JoinGame", "UserStatistics", "Logout"};
    private final String[] gameCalls =
            {"MovePiece", "GetBoard", "WaitMyTurn", "GetColour", "GetGameOver", "GetWon", "LeaveGame","RemoveFromMap"};


    ConnectionHandler(Socket socket, GameServer gs, ConcurrentHashMap<Long, Menu> userMenus,
                      ConcurrentHashMap<Long, PlayersGame> userGames, ServerSocket serv) {
        this.socket = socket;
        this.gs = gs;
        this.userMenus = userMenus;
        this.userGames = userGames;
        this.serv = serv;
    }

    @Override
    public void run() {
        try {
            while(!socket.isClosed()) {
                Scanner in = new Scanner(socket.getInputStream());
                String sent = in.nextLine();

                String[] calls = sent.split("\\|");


                if (calls.length < 1) {
                    continue;
                }

                else if(calls[0].equals("Session")) {
                    setSessionId(calls);
                    continue;
                }

                else if (calls[0].equals("ShutOffServer")) {
                    serv.close();
                    continue;
                }

                else if(sessionId == null){
                    writeToOutput("Not under a session");
                    continue;
                }

                else if(checkIfLogin(calls)){}

                else if(this.sessionId != null) {
                    if (Arrays.asList(menuCalls).stream().anyMatch(a -> a.equals(calls[0]))) {
                        Menu menu = userMenus.get(sessionId);
                        if (menu == null) {
                            writeToOutput("Could not get the menu for sessionId: " + sessionId);
                        } else {
                            doMenuCall(sessionId, menu, calls);
                        }
                    }

                    if (Arrays.asList(gameCalls).stream().anyMatch(a -> a.equals(calls[0]))) {
                        PlayersGame game = userGames.get(sessionId);
                        if (game == null) {
                            writeToOutput("Could not get the game for sessionId: " + sessionId);
                        } else {
                            doGameCalls(sessionId, game, calls);
                        }
                    }
                    continue;
                }

                //Made it here, no proper calls, return error
                else{writeToOutput("Not a valid call: " + calls[0]);}
            }
        } catch (Exception ex) {
            System.out.println("HEREHERESQUIRREL");
            //System.err.println(ex.toString());
        }finally {
            try {
                socket.close();
            }catch(Exception ex){
                System.out.println("Closing the socket in case");
            }
            System.out.println("Done");
        }
    }

    private void setSessionId(String[] calls){
        System.out.println("Session call");
        if(calls.length == 1){
            this.sessionId = sId.getAndIncrement();
            System.out.println("Sending session " + sessionId.toString());
            writeToOutput(sessionId.toString());
            return;
        }
        if(calls.length == 2){
            try{
                this.sessionId = Long.parseLong(calls[1]);
            }catch(Exception ex){
                writeToOutput("Invalid session id");
            }
            return;
        }
        writeToOutput("Invalid session call");
    }


    private boolean checkIfLogin(String[] call) {
        if (call[0].equals("Login")) {
            if(userMenus.containsKey(sessionId)) {
                writeToOutput("Already logged in");
            }
            else{
                if(call.length != 3)
                    writeToOutput("Not a valid log in call due to length");
                else {
                    try {
                        Menu menu = gs.login(call[1], call[2]);
                        if (menu == null)
                            writeToOutput("Failed to login");
                        else {
                            writeToOutput("Success");
                            userMenus.put(sessionId,menu);
                        }
                    } catch (RemoteException ex) {
                        writeToOutput("Login error");
                    }
                }
            }
            return true;
        } else
            return false;
    }

    //calls
    private void doMenuCall(Long sessionId, Menu menu, String[] calls) throws RemoteException {
        PlayersGame pg;
        StringBuilder sb;
        switch (calls[0]) {
            case "CreateGame": {
                if (calls.length != 2) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                pg = menu.createGame(calls[1]);
                if (pg == null) {
                    writeToOutput("Could not make game");
                    return;
                }
                //User can only make one game, so this is safe
                userGames.put(sessionId, pg);
                writeToOutput("Game created");
                break;
            }
            case "ListCurrentGames": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                List<String> games = menu.listCurrentGames();
                sb = new StringBuilder();
                for (String game : games) {
                    sb.append(game);
                    sb.append(",");
                }
                writeToOutput(sb.toString());
                break;
            }
            case "JoinGame": {
                if (calls.length != 2) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                pg = menu.joinGame(calls[1]);
                if (pg == null) {
                    writeToOutput("Could not join game");
                    return;
                }
                //User can only make/join one game, so this is safe
                userGames.put(sessionId, pg);
                writeToOutput("Game joined");
                break;
            }
            case "UserStatistics": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                Map<String, Integer> userStats = menu.userStatistics();
                sb = new StringBuilder();
                for (Map.Entry<String, Integer> entry : userStats.entrySet()) {
                    sb.append(entry.getKey());
                    sb.append(": ");
                    sb.append(entry.getValue());
                    sb.append("\n");
                }
                writeToOutput(sb.toString());
                break;
            }
            case "Logout": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                if(menu.logout()){
                    userMenus.remove(sessionId);
                    writeToOutput("Logged out");
                }else {
                    writeToOutput("Failed to logout");
                }
                break;
            }
            default: {
                writeToOutput("No valid call");
            }
        }
    }

    private void doGameCalls(Long sessionId, PlayersGame game, String[] calls) {
        StringBuilder sb = new StringBuilder();
        switch (calls[0]) {
            case "MovePiece": {
                if (calls.length != 2) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                makeMoves(calls[1],game);
                break;
            }
            case "GetBoard": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                try {
                    Piece[][] board = game.getBoard();
                    for (Piece[] row : board) {
                        for (Piece piece : row) {
                            sb.append(piece.getColour());
                            sb.append(",");
                            sb.append(piece.getIsKing());
                            sb.append(";");
                        }
                    }
                    writeToOutput(sb.toString());
                } catch (RemoteException ex) {
                    System.err.println(ex.toString());
                    writeToOutput("Unable to get board.");
                }
                break;
            }
            case "WaitMyTurn": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                game.waitMyTurn();
                writeToOutput("Opponents turn over");
                break;
            }
            case "GetColour": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                writeToOutput(game.getColour().toString());
                break;
            }
            case "GetGameOver": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                try {
                    writeToOutput(game.getGameOver() ? "true" : "false");
                } catch (RemoteException ex) {
                    System.out.println(ex.toString());
                    writeToOutput("Unable to get game over");
                }
            }
            break;
            case "GetWon": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                try {
                    writeToOutput(game.getWon() ? "true" : "false");
                } catch (RemoteException ex) {
                    System.out.println(ex.toString());
                    writeToOutput("Failed to get won.");
                }
                break;
            }
            case "LeaveGame": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                try {
                    game.leaveGame();
                    writeToOutput("Left");
                } catch (RemoteException ex) {
                    System.out.println(ex.toString());
                    writeToOutput("Call returned rmiexception");
                }
                break;
            }case "RemoveFromMap": {
                if (calls.length != 1) {
                    writeToOutput("Not a valid game call");
                    return;
                }
                userGames.remove(sessionId);
                writeToOutput("Game removed from map");
                break;
            }
            default: {
                writeToOutput("No valid call");
            }
        }
    }

    private void makeMoves(String move, PlayersGame game){
        String[] intStringInMove = move.split(",");
        int[] intsInMove;
        try {
            intsInMove = Arrays.stream(intStringInMove).mapToInt(Integer::parseInt).toArray();
        }
        catch(Exception ex){
            writeToOutput("Could not parse move");
            return;
        }
        if(intsInMove.length % 2 != 0 || intsInMove.length < 2){
            writeToOutput("Invalid move");
        }
        else{
            ArrayList<Integer[]> moveList = new ArrayList<>();
            for(int i = 0; i < intsInMove.length; i = i+2){
                moveList.add(new Integer[]{intsInMove[i], intsInMove[i+1]});
            }
            try {
                if (game.movePiece(moveList)) {
                    writeToOutput("Done");
                } else {
                    writeToOutput("Move failed");
                }
            }catch (Exception ex){
                System.out.println(ex.toString());
                writeToOutput("Move failed");
            }
        }
    }

    private void writeToOutput(String toWrite) {
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            out.println(toWrite);
        } catch (Exception ex) {
            System.err.println(ex.toString());
        }
    }
}
