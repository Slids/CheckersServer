package CheckersCommon;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Slid on 4/16/2016.
 */
public class GameImp extends UnicastRemoteObject implements Game{
    private volatile String redPlayer;
    private volatile UserData redPlayerData;
    private volatile String blackPlayer;
    private volatile UserData blackPlayerData;
    private final Piece[][] board;
    private final Object boardLock;
    private static final Piece emptyPiece = new Piece(Colour.na);
    private volatile Colour currPlayer;
    private final AtomicBoolean gameStarted;
    private final String gameName;
    private volatile Colour winner;
    final AtomicBoolean gameOver;
    //Only her eto remove game at end
    final Map<String, Game> games;

    GameImp(String gameName, Map<String, Game> games) throws RemoteException{
        this.board = new Piece[8][8];
        this.currPlayer = Colour.black;
        this.gameStarted = new AtomicBoolean(false);
        this.boardLock = new Object();
        this.gameName = gameName;
        this.gameOver = new AtomicBoolean(false);
        this.games = games;
        //Set up the board
        for(int i = 0; i < 8;i++){
            for(int j = 0; j < 8; j++){
                if( i < 3 && (i+j)% 2 ==0 )
                    this.board[i][j] = new Piece(Colour.red);

                else if(i > 4 && (i+j)% 2 == 0 )
                    this.board[i][j] = new Piece(Colour.black);

                else
                    this.board[i][j] = emptyPiece;
            }
        }
        this.winner = Colour.na;
    }

    PlayersGame setFirstPlayer(UserData player) throws RemoteException{
        blackPlayer = "";
        String cashPlayerName = player.getName();
        PlayersGame pg = new PlayersGame(this, cashPlayerName, Colour.red);
        if(pg != null && player.setInGame()) {
            redPlayer = cashPlayerName;
            redPlayerData = player;
            return pg;
        }
        return null;
    }

    @Override
    public PlayersGame joinGame(UserData player) throws RemoteException{
        String cashPlayerName = player.getName();
            if (this.redPlayer == cashPlayerName)
                throw new IllegalStateException("Can't play against yourself");
            if (gameStarted.compareAndSet(false, true)) {
                PlayersGame pg = new PlayersGame(this, cashPlayerName,Colour.black );
                if(pg != null && player.setInGame()) {
                    this.blackPlayer = cashPlayerName;
                    this.blackPlayerData = player;
                    return pg;
                }
                gameStarted.compareAndSet(true,false);
                return null;
            } else
                return null;
    }

    @Override
    public String getCurrentPlayer() throws RemoteException {
        if(currPlayer == Colour.red)
            return redPlayer;
        else if(currPlayer == Colour.black)
            return blackPlayer;
        else
            return "No player (If you chose this as a username, your wierd..., May or may not be you";
    }

    @Override
    public Piece[][] getBoard() {
        synchronized (boardLock) {
            Piece[][] returnArray = new Piece[8][8];
            for (int i = 0; i < 8; i++) {
                for (int j = 0; j < 8; j++) {
                    if (board[i][j] == emptyPiece)
                        returnArray[i][j] = emptyPiece;
                    else{
                        returnArray[i][j] = new Piece(board[i][j].getColour());
                        if(board[i][j].getIsKing())
                            returnArray[i][j].makeKing();
                    }
                }
            }
            return returnArray;
        }
    }

    @Override
    public boolean movePiece(ArrayList<Integer[]> positions, String playersName) throws RemoteException {
        synchronized (boardLock) {
            if (!basicCheckMovesAreValid(positions)) {
                return false;
            }
            if (!checkColorBasedMoves(positions, currPlayer)){
                return false;
            }
            this.board[positions.get(positions.size() - 1)[0]][positions.get(positions.size() - 1)[1]]
                    = this.board[positions.get(0)[0]][positions.get(0)[1]];
            for (int i = 0; i < positions.size() - 1; i++) {
                this.board[positions.get(i)[0]][positions.get(i)[1]]
                        = emptyPiece;
            }
            setKingProperty(positions.get(positions.size()-1));
            if(this.currPlayer == Colour.red)
                this.currPlayer = Colour.black;
            else
                this.currPlayer = Colour.red;
            checkGameOver();
            boardLock.notifyAll();
            return true;
        }
    }

    public void waitForTurn(String playerName) throws RemoteException, InterruptedException{
        synchronized(boardLock){
            if(getCurrentPlayer() == playerName)
                return;
            while(!getCurrentPlayer().equals(playerName) && currPlayer != Colour.na) {
                boardLock.wait();
            }
        }
    }

    private boolean basicCheckMovesAreValid(ArrayList<Integer[]> moves){
        if(moves.size() < 2 || (moves.size() != 2 && moves.size()%2 != 1))
            return false;

        if((moves.get(0)[0] + moves.get(0)[1]) % 2 != 0)
            return false;

        if(board[moves.get(0)[0]][moves.get(0)[1]].getColour() != currPlayer)
            return false;

        for (int i = 0; i < moves.size(); i++) {
            //Checks the validity of the move space
            if(moves.get(i).length != 2)
                throw new IllegalArgumentException("One of the moves had invalid invalid size");
            if(moves.get(i)[0] >= 8 || moves.get(i)[1] >= 8)
                throw new IllegalArgumentException("One of the moves doesn't land on the board");
            if((moves.get(i)[0] + moves.get(i)[1]) % 2 != 0)
                throw new IllegalArgumentException("One of the moves doesn't land in a valid space");
            if(i > 0)
                if(Math.abs(moves.get(i)[0] - moves.get(i-1)[0]) + Math.abs(moves.get(i)[0] - moves.get(i-1)[0]) != 2)
                    throw new IllegalArgumentException("Moves arent valid");
        }
        return true;
    }

    private boolean checkColorBasedMoves(ArrayList<Integer[]> moves, Colour colour){
        Colour opposingColour = colour == Colour.red ? Colour.black: Colour.red;
        if(board[moves.get(0)[0]][moves.get(0)[1]].getIsKing())
            return checkColorBasedMovesForKing(moves, opposingColour);
        int direction = colour == Colour.red ? 1:-1;

        //Not king, only move one position
        if(moves.size() == 2){
            if(board[moves.get(1)[0]][moves.get(1)[1]] != emptyPiece)
                return false;
            else if(moves.get(1)[0] - moves.get(0)[0] != direction)
                return false;
            else if(Math.abs(moves.get(1)[1] - moves.get(0)[1]) != 1)
                return false;
            else
                return true;
        }
        //This is for jumping
        for(int i = 1; i < moves.size(); i++){
            if(moves.get(i)[0] - moves.get(i-1)[0] != direction)
            return false;
            else if(Math.abs(moves.get(1)[1] - moves.get(0)[1]) != 1)
                return false;
            //All of the even indices should be empty spots
            else if( i%2 == 0 && board[moves.get(i)[0]][moves.get(i)[1]] != emptyPiece)
                return false;
            //All of the odd indices positions should be enemy spots
            else if(i%2 == 1 && board[moves.get(i)[0]][moves.get(i)[1]].getColour() != opposingColour)
                return false;
        }
        return true;
    }

    private boolean checkColorBasedMovesForKing(ArrayList<Integer[]> moves, Colour opposingColour){
        //Is king, only move one position
        if(moves.size() == 2){
            if(board[moves.get(1)[0]][moves.get(1)[1]] != emptyPiece)
                return false;
            else if(Math.abs(moves.get(1)[0] - moves.get(0)[0]) != 1)
                return false;
            else if(Math.abs(moves.get(1)[1] - moves.get(0)[1]) != 1)
                return false;
            else
                return true;
        }

        //Verify we never jump the same position twice;
        System.err.println("Failed check color based moves for king");
        for(int i = 1; i < moves.size(); i = i +2)
        {
            for(int j = i+2; j < moves.size(); j = j +2)
            {
                if(moves.get(i)[0] == moves.get(j)[0] &&
                        moves.get(i)[0] == moves.get(j)[0]){
                    return false; }
            }
        }
        //This is for jumping
        for(int i = 1; i < moves.size(); i++){
            if(Math.abs(moves.get(i)[0] - moves.get(i-1)[0]) != 1){
                return false; }
            else if(Math.abs(moves.get(i)[1] - moves.get(i-1)[1]) != 1){
                return false; }
                //All of the even moves should be empty spots
            else if(i%2 == 0 && board[moves.get(i)[0]][moves.get(i)[1]] != emptyPiece){
                return false; }
            //All of the odd indices positions should be enemy spots
            if(i%2 == 1 && board[moves.get(i)[0]][moves.get(i)[1]].getColour() != opposingColour){
                return false; }
        }
        return true;
    }

    public String getGameName(){
        return gameName;
    }

    private void setKingProperty(Integer[] piecePos){
        Piece piece = board[piecePos[0]][piecePos[1]];
        //if your king, done
        if(piece.getIsKing())
            return;

        if(piece.getColour() == Colour.red)
            if(piecePos[0] == 7)
                piece.makeKing();

        if(piece.getColour() == Colour.black)
            if(piecePos[0] == 0)
                piece.makeKing();
    }

    private boolean checkGameOver(){
        winner = isThereWinner();
        if(winner != Colour.na){
            currPlayer = Colour.na;
            tryToLeaveGame();
            return true;
        }
        return false;
    }

    private Colour isThereWinner(){
        int numRed = 0;
        int numBlack = 0;
        for(int i = 0; i < 7; i++){
            for(int j = 0; j < 7; j++){
                if(board[i][j].getColour() == Colour.red)
                    ++numRed;
                if(board[i][j].getColour() == Colour.black)
                    ++numBlack;
            }
        }
        if(numRed == 0)
            return Colour.black;
        if(numBlack == 0)
            return Colour.red;
        return Colour.na;
    }

    public void leaveGame(String user) throws RemoteException{
        synchronized(boardLock) {
            if(!gameOver.get()) {
                if(user.equals(redPlayer))
                    winner = Colour.black;
                else
                    winner = Colour.red;
            }
            currPlayer = Colour.na;
            tryToLeaveGame();
            boardLock.notifyAll();
        }
    }

    public boolean isGameOver() throws RemoteException{
        return currPlayer == Colour.na;
    }

    public Colour getWinner()  throws RemoteException{
        return winner;
    }

    private void tryToLeaveGame() {
        try {
            if (gameOver.compareAndSet(false, true)) {
                if (getWinner() == Colour.red) {
                    redPlayerData.setNotInGame(true, false);
                    if(blackPlayerData != null)
                    blackPlayerData.setNotInGame(false, false);
                } if (getWinner() == Colour.black) {
                    redPlayerData.setNotInGame(false, false);
                    if(blackPlayerData != null)
                        blackPlayerData.setNotInGame(true, false);
                }else{
                    redPlayerData.setNotInGame(false, true);
                    if(blackPlayerData != null)
                        blackPlayerData.setNotInGame(false, true);
                }
                games.remove(this.gameName);
            }
        }catch(Exception ex){
            System.err.println(ex.toString());
        }
    }

    public void killGame() throws RemoteException{
        synchronized(boardLock) {
            currPlayer = Colour.na;
            tryToLeaveGame();
            boardLock.notifyAll();
        }
    }
}
