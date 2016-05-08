package CheckersSocket;

import CheckersCommon.Colour;
import CheckersCommon.Piece;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Slid on 4/27/2016.
 */
public class CheckersGhostPlayer {
    final String gHost;
    final int gPort;
    final boolean tryToJoin;
    final Socket socket;

    CheckersGhostPlayer(boolean tryToJoin, String gHost, int gPort) throws IOException{
        this.gHost = gHost;
        this.gPort = gPort;
        this.tryToJoin = tryToJoin;
        this.socket = getSocket(gHost, gPort);
    }

    public void runGhostPlayer() throws IOException{
        try {
            Long sessionId = setSession();
            login();

            if (tryToJoin)
                tryToJoinGame();
            else {
                createGame();
            }
            playGame();
            logout();
        }finally{
            //Make sure the socket is closed
            socket.close();
        }
    }

    private long setSession() throws IOException{
        writeToSocket("Session");
        String ret = readFromSocket();
        return Long.parseLong(ret);
    }

    private boolean login() throws IOException{
        writeToSocket("Login|" + UUID.randomUUID().toString()
                + "|" + UUID.randomUUID().toString());
        String ret = readFromSocket();
        if(!ret.equals("Success"))
            throw new IllegalStateException("State bad");
        return true;
    }

    private void logout() throws IOException{
        writeToSocket( "Logout");
    }

    private void tryToJoinGame() throws IOException{
        writeToSocket("ListCurrentGames");
        String games = readFromSocket();
        String[] gameList = games.split(",");
        int i = 0;
        boolean joined = false;
        while(!joined && (i < (gameList.length))){
            writeToSocket("JoinGame" + "|" + gameList[i]);
            String ret = readFromSocket();
            if(ret.equals("Game joined"))
                joined = true;
            i = i + 1;
        }
        if(!joined){
            createGame();
        }
    }

    private void createGame () throws IOException{
        writeToSocket("CreateGame" + "|" + UUID.randomUUID().toString());
        String ret = readFromSocket();
        if(!ret.equals("Game created"))
            throw new IllegalStateException("Some failure");
    }

    private boolean isGameOver() throws IOException{
        writeToSocket("GetGameOver");
        String ret = readFromSocket();
        if(ret.equals("true"))
            return true;
        if(ret.equals("false"))
            return false;
        else
            throw new IllegalStateException("State is weird");
    }

    private void waitMyTurn() throws IOException{
        writeToSocket("WaitMyTurn");
        String ret = readFromSocket();
        if(!ret.equals("Opponents turn over"))
            throw new IllegalStateException("State is weird");
    }

    private Piece getPiece(String pieceString){
        String[] piecePieces = pieceString.split(",");
        if(piecePieces.length != 2)
            throw new IllegalArgumentException("Bad call: " + pieceString);
        Piece piece;
        if(piecePieces[0].equals("red"))
            piece = new Piece(Colour.red);
        else if(piecePieces[0].equals("black"))
            piece = new Piece(Colour.black);
        else if(piecePieces[0].equals("na"))
            return new Piece(Colour.na);
        else
            throw new IllegalArgumentException("Not a valid argument");
        if(piecePieces[1].equals("true"))
            piece.makeKing();
        else if(piecePieces[1].equals("false"))
            ;
        else
            throw new IllegalArgumentException("Not a valid argument");
        return piece;
    }

    private Piece[][] getBoard() throws IOException{
        writeToSocket("GetBoard");
        String ret = readFromSocket();
        Piece[][] board = new Piece[8][8];
        if(ret.contains(";")){
            String[] boardElements = ret.split(";");
            for(int i = 0; i < 8; i++){
                for(int j = 0; j < 8; j++){
                    board[i][j] = getPiece(boardElements[8*i + j]);
                }
            }
        }
        return board;
    }

    private Colour getColor() throws IOException{
        writeToSocket( "GetColour");
        String ret = readFromSocket();
        if(ret.equals("red"))
            return Colour.red;
        if(ret.equals("black"))
            return Colour.black;

        throw new IllegalStateException("What is this state?");
    }

    private void makeMove( Piece[][] board, Colour myColour) throws IOException{
        List<Integer[]> move = getMove(board,myColour);
        if (move != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < move.size(); i++) {
                sb.append(move.get(i)[0]);
                sb.append(",");
                sb.append(move.get(i)[1]);
                if (i < (move.size() - 1))
                    sb.append(",");
            }
            writeToSocket("MovePiece" + "|" + sb.toString());
        }
        else{
            writeToSocket("LeaveGame");
        }
        String ret = readFromSocket();
        if(!ret.equals("Done") && !ret.equals("Left") && !isGameOver()) {
            throw new IllegalStateException("Something failed");
        }
    }

    private void playGame() throws IOException{
        Colour myColour = getColor();
        while(!isGameOver()){
            waitMyTurn();
            Piece[][] board = getBoard();
            //Make this function
            makeMove(board, myColour);
        }
        writeToSocket("RemoveFromMap");
        readFromSocket();
    }

    //Functions for playing game
    private Piece getNextPosition(Piece[][] board, int row, int col, int vDirection, int hDirection){
        if(Math.abs(vDirection) + Math.abs(hDirection) != 2)
            throw new IllegalArgumentException("Not a valid move");
        if((col + hDirection) < 0 || (col + hDirection) > 7 ||
                (row + vDirection) < 0 || (row + vDirection) > 7)
            throw new IllegalArgumentException("Not a valid move");

        return board[row + vDirection][col+hDirection];
    }

    private List<Integer[]> getMove(Piece[][] board, Colour myColour){
        int vDirection = myColour == Colour.red ? 1:-1;
        int[] directions = {1,-1};
        LinkedList<Integer[]> moves = new LinkedList<>();
        for(int i = 0; i < 8; i ++){
            for(int j = 0; j < 8; j++){
                //we may be able to move
                if(board[i][j].getColour() == myColour){
                    for(int hDirection : directions){
                        if((j + hDirection) < 8 && (j + hDirection) >= 0
                                && (i + vDirection) < 8 && (i + vDirection) >= 0 ){
                            if(getNextPosition(board,i,j,vDirection,hDirection).getColour() == Colour.na){
                                moves.add(new Integer[]{i,j});
                                moves.add(new Integer[]{i+vDirection,j+hDirection});
                                return moves;
                            }
                            if(getNextPosition(board,i,j,vDirection,hDirection).getColour() == board[i][j].getOpposingColour() ){
                                if((j + 2*hDirection) < 8 && (j + 2*hDirection) >= 0 && (i + 2*vDirection) < 8
                                        && (i + 2*vDirection) >= 0 &&
                                getNextPosition(board,i+vDirection,j+hDirection,vDirection,hDirection).getColour() == Colour.na){
                                    moves.add(new Integer[]{i,j});
                                    moves.add(new Integer[]{i+vDirection,j+hDirection});
                                    moves.add(new Integer[]{i+2*vDirection,j+2*hDirection});
                                    return moves;
                                }
                            }

                        }
                        if((j + hDirection) < 8 && (j + hDirection) >= 0
                                && (i - vDirection) < 8 && (i - vDirection) >= 0 && board[i][j].getIsKing()){
                            if(getNextPosition(board,i,j,-vDirection,hDirection).getColour() == Colour.na){
                                moves.add(new Integer[]{i,j});
                                moves.add(new Integer[]{i-vDirection,j+hDirection});
                                return moves;
                            }
                            if(getNextPosition(board,i,j,-vDirection,hDirection).getColour() == board[i][j].getOpposingColour() ){
                                if((j + 2*hDirection) < 8 && (j + 2*hDirection) >= 0 && (i - 2*vDirection) < 8
                                        && (i - 2*vDirection) > 0 &&
                                        getNextPosition(board,i-vDirection,j+hDirection,-vDirection,hDirection).getColour() == Colour.na){
                                    moves.add(new Integer[]{i,j});
                                    moves.add(new Integer[]{i-vDirection,j+hDirection});
                                    moves.add(new Integer[]{i-2*vDirection,j+2*hDirection});
                                    return moves;
                                }
                            }
                        }
                    }
                }
            }
        }
        //printBoard(board); //Debug statement
        return null;
        //throw new IllegalStateException("Board is invalid");
    }

    //Helper socket functions
    private Socket getSocket(String gHost, int gPort) throws IOException {
        return new Socket(InetAddress.getByName(gHost), gPort);
    }

    private void writeToSocket(String toWrite) throws IOException{
        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
        out.println(toWrite);
    }

    private String readFromSocket() throws IOException{
        Scanner in = new Scanner(socket.getInputStream());
        //I only expect one line, more then that will be closed
        String ret =  in.nextLine();
        return ret;
    }

    public static void main(String[] args){
        ExecutorService e = Executors.newCachedThreadPool();
        for(int i = 0; i < 1; i++) {
            e.submit(() -> {
                try {
                    CheckersGhostPlayer cgp = new CheckersGhostPlayer(true, "localhost", 5386);
                    cgp.runGhostPlayer();
                } catch (IOException ex) {
                    System.err.println(ex.toString());
                }
            });
        }
    }

    private static String pieceToLetter(Piece piece){
        switch(piece.getColour().ordinal()){
            case 0:
                if(piece.getIsKing())
                    return "R";
                return "r";
            case 1:
                if(piece.getIsKing())
                    return "B";
                return "b";
            default:
                return " ";
        }
    }

    private static void printBoard(Piece[][] board){
        System.out.println("----------------");
        for (Piece[] row: board) {
            for (Piece el: row) {
                System.out.print("|" + pieceToLetter(el));
            }
            System.out.println();
        }
        System.out.println("----------------");
    }
}
