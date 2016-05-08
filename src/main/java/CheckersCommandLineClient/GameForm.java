/*
 * Copyright (C) 2016 Jonathan Godbout
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package CheckersCommandLineClient;

import CheckersCommon.Colour;
import CheckersCommon.Piece;
import CheckersCommon.PlayersGame;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Slid on 4/22/2016.
 */
public class GameForm {
    private CountDownLatch cdl = new CountDownLatch(1);
    final static int IMAGE_SIZE = 35;
    final JPanel mainPanel = new JPanel();
    final JPanel menuPanel = new JPanel();
    final JPanel factPanel = new JPanel();
    final JPanel boardPane = new JPanel();
    final int vDirection;
    final JButton enterButton = new JButton("Move");
    volatile JButton[][] boardButtons = new JButton[8][8];
    final JLabel currentColorPanel = new JLabel();
    final PlayersGame pg;
    volatile JFrame newFrame;
    volatile ArrayList<Integer[]> moves = new ArrayList<>();
    //board cash
    volatile Piece[][] board = new Piece[8][8];
    Executor es;

    public GameForm(PlayersGame pg){
        this.pg = pg;
        this.vDirection = pg.getColour() == Colour.red ? 1:-1;
        es = Executors.newSingleThreadExecutor();
        makeFrame();
        System.out.println("done");
    }

    public void startGame(){
        makeFrame();
    }

    private void makeFrame(){
        //main panel
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        //Board panel
        final GridLayout buttonLayout = new GridLayout(8,8);
        boardPane.setLayout(buttonLayout);

        createBoardPanel();

        es.execute(new waitMyTurn());
        //Choice panel
        enterButton.addActionListener(new moveAction());
        enterButton.setEnabled(false);
        enterButton.setSize(10,5);
        final JButton resetChoices = new JButton("Reset moves");
        resetChoices.addActionListener(new resetAction());
        resetChoices.setSize(10,5);


        final GridLayout factLayout = new GridLayout(0,2);
        factPanel.setLayout(factLayout);
        JLabel playersColor = new JLabel(pg.getColour().toString());
        factPanel.add(playersColor);
        updateCurrentColorPanel();
        factPanel.add(currentColorPanel);

        final GridLayout menuLayout = new GridLayout(0,2);
        menuPanel.setLayout(menuLayout);

        menuPanel.add(enterButton);
        menuPanel.add(resetChoices);

        mainPanel.add(boardPane);
        mainPanel.add(factPanel);
        mainPanel.add(menuPanel);

        testPanel(mainPanel);
    }

    private Icon getIcon(Piece p){
        String kingIfKing = p.getIsKing() ? "King" : "";
        if(p.getColour() == Colour.red) {
            try {
                Image img = ImageIO.read(this.getClass().getResource("redButton" + kingIfKing + ".png"));
                img = img.getScaledInstance(IMAGE_SIZE,IMAGE_SIZE,IMAGE_SIZE);
                return new ImageIcon(img);
            }catch(Exception ex){
                System.err.println(ex.toString());
            }
        }
        if(p.getColour() == Colour.black)
        try {
            Image img = ImageIO.read(this.getClass().getResource("blackButton" + kingIfKing + ".png"));
            img = img.getScaledInstance(IMAGE_SIZE,IMAGE_SIZE,IMAGE_SIZE);
            return new ImageIcon(img);
        }catch(Exception ex){
            System.err.println(ex.toString());
        }
        else
            return new ImageIcon();
        return null;
    }

    private void createBoardPanel(){
        try{
            board = pg.getBoard();
        }catch(RemoteException ex){
            System.err.println(ex.toString());
        }
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                boardButtons[i][j] = new JButton(getIcon(board[i][j]));
                boardButtons[i][j].setDisabledIcon(boardButtons[i][j].getIcon());
                if((i+j) % 2 == 0) {
                    boardButtons[i][j].setBackground(Color.white);
                    boardButtons[i][j].setBorderPainted( false );
                    boardButtons[i][j].setFocusPainted( false );
                    if(shouldBeEnabled(i,j))
                        boardButtons[i][j].addActionListener(new buttonAction(i,j));
                }
                if((i+j) % 2 == 1) {
                    boardButtons[i][j].setBackground(Color.darkGray);
                    boardButtons[i][j].setOpaque(true);
                    boardButtons[i][j].setEnabled(false);
                }
                if(pieceInPreviousMove(i,j))
                    boardButtons[i][j].setBackground(Color.yellow);
                //boardButtons[i][j].setSize(2,2);
                boardPane.add(boardButtons[i][j]);
            }
        }
    }

    private void updateCurrentColorPanel(){
        try {
            javax.swing.SwingUtilities.invokeLater(
                    () -> {
                        this.factPanel.invalidate();
                        this.factPanel.repaint();
                        this.mainPanel.revalidate();
                        this.mainPanel.repaint();
                        //this.newFrame.revalidate();
                        //this.newFrame.repaint();
                        //this.newFrame.pack();
                        //newFrame.setVisible(false);
                    });
        }catch(Exception ex){
            System.err.println(ex.toString());
        }
        try {
            currentColorPanel.setText("My Turn: " + pg.isMyTurn());
        }catch(Exception ex){}
        factPanel.add(currentColorPanel);
    }

    private void updateBoard(){
        try{
            board = pg.getBoard();
        }catch(RemoteException ex){
            System.err.println(ex.toString());
        }
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                this.boardButtons[i][j].setIcon(getIcon(board[i][j]));
                this.boardButtons[i][j].setDisabledIcon(boardButtons[i][j].getIcon());
                if((i+j) % 2 == 0) {
                    boardButtons[i][j].setBackground(Color.white);
                    if(boardButtons[i][j].getActionListeners().length != 0)
                        boardButtons[i][j].removeActionListener(
                                boardButtons[i][j].getActionListeners()[0]);
                    if(shouldBeEnabled(i,j)) {
                        this.boardButtons[i][j].addActionListener(new buttonAction(i, j));
                    }
                }
                if((i+j) % 2 == 1) {
                    this.boardButtons[i][j].setBackground(Color.darkGray);
                    this.boardButtons[i][j].setOpaque(true);
                    this.boardButtons[i][j].setEnabled(false);
                }
                if(pieceInPreviousMove(i,j))
                    this.boardButtons[i][j].setBackground(Color.yellow);
            }
        }

        try {
            javax.swing.SwingUtilities.invokeLater(
                    () -> {
                        this.boardPane.invalidate();
                        this.boardPane.repaint();
                        this.mainPanel.revalidate();
                        this.mainPanel.repaint();
                        //this.newFrame.revalidate();
                        //this.newFrame.repaint();
                        //this.newFrame.pack();
                        //newFrame.setVisible(false);
                    });
        }catch(Exception ex){
            System.err.println(ex.toString());
        }
    }


    private void updateBoardSimple(){
        for(int i = 0; i < 8; i++){
            for(int j = 0; j < 8; j++){
                if((i+j) % 2 == 0) {
                    boardButtons[i][j].setBackground(Color.white);
                    if(boardButtons[i][j].getActionListeners().length != 0)
                        boardButtons[i][j].removeActionListener(
                                boardButtons[i][j].getActionListeners()[0]);
                    if(shouldBeEnabled(i,j)) {
                        this.boardButtons[i][j].addActionListener(new buttonAction(i, j));
                    }
                }
                if(pieceInPreviousMove(i,j))
                    this.boardButtons[i][j].setBackground(Color.yellow);
            }
        }

        try {
            javax.swing.SwingUtilities.invokeLater(
                    () -> {
                        this.boardPane.invalidate();
                        this.boardPane.repaint();
                        this.mainPanel.revalidate();
                        this.mainPanel.repaint();
                        //this.newFrame.revalidate();
                        //this.newFrame.repaint();
                        //this.newFrame.pack();
                        //newFrame.setVisible(false);
                    });
        }catch(Exception ex){
            System.err.println(ex.toString());
        }
    }

    private boolean shouldBeEnabled(int i, int j){
        try{
            //if its not my turn, i cant do anything
            if(!pg.isMyTurn())
                return false;
            //First move needs to be my color
            if(moves.size() == 0)
                return board[i][j].getColour() == pg.getColour();

            //If we are not next to the last move, who cares
            Integer[] lastMove = moves.get(moves.size() - 1);
            Integer[] firstMove = moves.get(0);
            if(!nextToLast(lastMove, i,j, board[firstMove[0]][firstMove[1]].getIsKing())){
                return false;
            }

            //We're next to last, if only one move has been done and ere empty, return
            if(moves.size() == 1)
                if(board[i][j].getColour() == Colour.na)
                    return true;

            //If we've yet to return true, this better be an attack
            return validateAttack(i,j,i-lastMove[0],j-lastMove[1]);

        }catch(RemoteException ex){
            System.out.println(ex.toString());
        }
        return true;
    }

    private boolean nextToLast(Integer[] lastMove, int row, int col, boolean isKing){
        if((row - lastMove[0]) == vDirection && Math.abs(col - lastMove[1]) == 1)
            return true;
        if(isKing)
            if ((row - lastMove[0]) == -vDirection && Math.abs(col - lastMove[1]) == 1)
                return true;
        return false;
    }

    private boolean canMove(int i, int j, int vDirection, int hDirection){
        return validateAttack(i,j, vDirection, hDirection) ||
                validateEmpty(i,j);
    }

    private boolean validateEmpty(int i, int j){
        if(i < 0 || i > 7 || j < 0 || j > 7)
            return false;
        return board[i][j].getColour() == Colour.na;
    }

    private boolean validateAttack(int i, int j, int vDirection,int hDirection){
        Colour enemyColor = (pg.getColour() == Colour.black) ? Colour.red : Colour.black;
        if(Math.abs(vDirection) != 1 && Math.abs(hDirection) != 1)
            return false;
        if(!validateEmpty(i+vDirection,j+hDirection) ||
                board[i][j].getColour() != enemyColor ||
                pieceInPreviousMove(i,j))
            return false;
        return true;
    }

    private boolean pieceInPreviousMove(int i, int j){
        return moves.stream().anyMatch(a -> a[0] == i && a[1] == j);
    }

    private void testPanel(JPanel panel){
        javax.swing.SwingUtilities.invokeLater(
                () -> {
                    this.newFrame = new JFrame("Game");
                    newFrame.addWindowListener(new windowClose());
                    newFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                    newFrame.setSize(1,1);
                    newFrame.getContentPane().add(panel, BorderLayout.CENTER);
                    newFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                    //newFrame.setResizable(false);
                    newFrame.pack();
                    newFrame.setVisible(true);
                }
        );
        try {
            cdl.await();
        }catch(Exception ex){}

    }

    //ACTION BUTTONS
    private class buttonAction implements ActionListener {
        final int row, col;

        buttonAction(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Integer[] thisMove = {row,col};
            moves.add(thisMove);
            if(moves.size() == 2)
                enterButton.setEnabled(true);
            if(!(moves.size() <= 1) && !validateEmpty(row,col)){
                Integer[] lastMove = moves.get(moves.size()-2);
                int vDirection = row - lastMove[0];
                int hDirection = col - lastMove[1];
                Integer[] nextMove =  {row + vDirection, col + hDirection};
                moves.add(nextMove);
            }
            checkCloseGame();
            updateBoardSimple();
        }
    }

    private class resetAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            moves.clear();
            enterButton.setEnabled(false);
            updateBoardSimple();
        }
    }

    private class moveAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                pg.movePiece(moves);
            }catch(RemoteException ex){
                System.out.println(ex.toString());
            }
            moves.clear();
            enterButton.setEnabled(false);
            updateBoard();
            updateCurrentColorPanel();

            try {
                es.execute(new waitMyTurn());
            }catch(Exception ex){
                System.err.println(ex.toString());
            }
        }
    }

    //GAME CLOSE
    private void checkCloseGame(){
        try{
            if(pg.getGameOver()) {
                if (pg.getWon()){
                    JOptionPane.showMessageDialog(null, "You have won, congratulations:" + pg.getColour());
                }else{
                    JOptionPane.showMessageDialog(null, "You have lost, I'm sorry." + pg.getColour());
                }

                //We may have to close the frame
                javax.swing.SwingUtilities.invokeLater(
                        () -> {
                            try {
                                javax.swing.SwingUtilities.invokeLater(
                                        () -> newFrame.dispatchEvent(new WindowEvent(newFrame, WindowEvent.WINDOW_CLOSING))
                                );
                            }catch(Exception ex){System.out.println(ex);}
                        });
            }}
        catch(RemoteException ex){
            System.err.println("Cant get game over");
        }
    }

    private class windowClose implements WindowListener {
        @Override
        public void windowOpened(WindowEvent e) {
            //System.out.println("called opened");
        }

        @Override
        public void windowActivated(WindowEvent e) {
            //System.out.println("called activated");
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
            //System.out.println("called deactivated");

        }

        @Override
        public void windowIconified(WindowEvent e) {
            //System.out.println("called icon");

        }

        @Override
        public void windowDeiconified(WindowEvent e) {
            //System.out.println("called deicon");

        }

        @Override
        public void windowClosed(WindowEvent e) {
            //System.out.println("called closed");

        }

        @Override
        public void windowClosing(WindowEvent windowEvent) {
            try {
                if (!pg.getGameOver()) {
                    pg.leaveGame();
                }
                windowEvent.getWindow().dispose();
            } catch (RemoteException ex) {
                System.err.println("Failed to close");
            }finally {
                cdl.countDown();
            }
        }
    }

    //Game runnable
    private class waitMyTurn implements Runnable{
        public void run()
        {
            pg.waitMyTurn();
            updateBoard();
            updateCurrentColorPanel();
            checkCloseGame();
        }
    }

}

