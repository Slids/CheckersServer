
import CheckersCommandLineClient.GameForm;
import CheckersCommon.*;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Created by Slid on 4/19/2016.
 */
public class test {

   public static void main(String[] args) throws Exception {
        GameServer gameServer = (GameServer) GameServerImp.toStub(new GameServerImp("testServer"));
        Menu p1Menu = gameServer.login("newUser","randPassword");
        PlayersGame P1Game = p1Menu.createGame("myGame");
        System.out.println(p1Menu.listCurrentGames());

       Menu p2Menu = gameServer.login("newUser2","randPassword");
       PlayersGame P2game = p2Menu.joinGame("myGame");

       class g2 extends Thread{
           int total;
           @Override
           public void run(){
               GameForm gf2 = new GameForm(P2game);
           }
       }

       class g1 extends Thread{
           int total;
           @Override
           public void run(){
               GameForm gf1 = new GameForm(P1Game);
           }
       }

       Thread gt1 = new g1();
       Thread gt2 = new g2();
       gt1.start();
       gt2.start();
       gt1.join();
       gt2.join();

       ArrayList<Integer[]> move = new ArrayList<Integer[]>();
       move.add(new Integer[]{5,1});
       move.add(new Integer[]{4,0});
       assert( P2game.movePiece(move));
       printBoard(P2game.getBoard());
       printBoard(P1Game.getBoard());
       p1Menu.logout();
       p2Menu.logout();
        System.exit(0);
    }

    private static String pieceToLetter(Piece piece){
        switch(piece.getColour().ordinal()){
            case 0:
                return "r";
            case 1:
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
