package CheckersCommandLineClient;

import CheckersCommon.GameServer;
import CheckersCommon.Menu;
import CheckersCommon.PlayersGame;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.*;

/**
 * Created by Slid on 4/21/2016.
 */
public class GetServerAndLogin {
    public static void main(String[] args) throws Exception {
        GameServer gs;
        Menu menu;
        try {
            Registry registry = LocateRegistry.getRegistry("localhost",5387);
            gs = (GameServer)registry.lookup("ServerOfCheckers");

            menu = askToLogin(gs);

            boolean anotherSelection = true;
            do{
                int op = askMenuOptions(menu);
                switch(op){
                    case 0:
                        createAndRunGame(menu);
                        break;
                    case 1:
                        listCurrentGames(menu.listCurrentGames());
                        break;
                    case 2:
                        joinAndRunGame(menu);
                        break;
                    case 3:
                        showUserStatistics(menu.userStatistics().entrySet());
                        break;
                    case 4:
                        anotherSelection = !menu.logout();
                        break;
                }


            }while(anotherSelection);

        } catch (RemoteException ex) {
            System.err.println(ex.toString());
        }
    }

    private static Menu askToLogin(GameServer gs){
        Menu menu;
        do {
            String username = JOptionPane.showInputDialog("Please enter a Username(cancel will default to A):");
            String password = JOptionPane.showInputDialog("Please enter a password(cancel will default to A):");

            if(username == null)
                username = "A";
            if(password == null)
                password = "A";

            try{
            menu = gs.login(username, password);
            }catch(RemoteException ex){
                menu = null;
            }
            if(menu == null)
                JOptionPane.showMessageDialog(null, "Login failed, please try again.");
        }while(menu == null);
        return menu;
    }

    private static int askMenuOptions(Menu menu){
        Object[] menuOptions = {"Create Game", "List Current Games",
                "Join Games", "User Statistics", "Logout"};
        return JOptionPane.showOptionDialog(new JDialog(),
                "What would you like to do?",
                "Menu",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                menuOptions, menuOptions[0]);
    }

    private static void listCurrentGames(List<String> games){
        StringBuilder sb = new StringBuilder();
        for(String game : games){
            sb.append(game + "\n");
        }
        JOptionPane.showMessageDialog(new JDialog(), sb.toString());
    }

    private static void showUserStatistics(Set<Map.Entry<String,Integer>> userStats){
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,Integer> entry: userStats){
            sb.append(entry.getKey() + ": " + entry.getValue() + "\n");
        }
        JOptionPane.showMessageDialog(new JDialog(), sb.toString());
    }

    private static void createAndRunGame(Menu menu){
        String gameName = JOptionPane.showInputDialog("Please enter a game name:");
        PlayersGame pg;
        if(gameName != null)
        {
            try {
                pg = menu.createGame(gameName);
                if(pg != null){
                    new GameForm(pg);
                }
            }catch(RemoteException ex)
            {
                System.err.println(ex.toString());
            }
        }
    }

    private static void joinAndRunGame(Menu menu){
        String gameName = JOptionPane.showInputDialog("Please enter a game name:");
        PlayersGame pg;
        if(gameName != null)
        {
            try {
                pg = menu.joinGame(gameName);
                if(pg != null)
                    new GameForm(pg);
            }catch(RemoteException ex)
            {
                System.err.println(ex.toString());
            }
        }
    }

}
