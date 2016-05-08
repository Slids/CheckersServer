import CheckersCommon.GameServerImp;
import CheckersCommon.Menu;
import CheckersCommon.PlayersGame;
import org.testng.TestNG;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;

/**
 * Created by Slid on 5/2/2016.
 */
public class GameTests {

    public static void main(String[] args){
        TestNG test = new TestNG( );
        test.setTestClasses(new Class[]{GameTests.class});
        test.setVerbose(2);
        test.run();
    }


    @Test(description = "Only one user can join a game")
    void joinGameTest() throws Exception {
        GameServerImp gameServerImp = new GameServerImp("fish");

        String[] usernames = new String[101];
        String[] passwords = new String[101];
        Menu[] menus = new Menu[101];

        for(int i = 0; i <101; i++) {
            usernames[i] = UUID.randomUUID().toString();
            passwords[i] = UUID.randomUUID().toString();
            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
        }

        String gameName = UUID.randomUUID().toString();

        menus[100].createGame(gameName);

        ExecutorService exec = Executors.newFixedThreadPool(100);
        CountDownLatch cdl = new CountDownLatch(100);
        CountDownLatch cdlStart = new CountDownLatch(100);
        AtomicInteger logCount = new AtomicInteger(0);
        IntStream.range(0, 100).forEach(
                i -> exec.execute(
                        () -> {
                            try{
                                if(menus[i].joinGame(gameName) != null)
                                    logCount.incrementAndGet();
                            }catch(Exception ex){}
                            finally{
                                cdl.countDown();
                            }
                        }
                )
        );

        cdl.await();
        assertEquals(logCount.get(),1);
    }

    @Test(description = "Only one move can be made")
    void makeMoveTest() throws Exception {
        GameServerImp gameServerImp = new GameServerImp("fish");

        String[] usernames = new String[2];
        String[] passwords = new String[2];
        Menu[] menus = new Menu[2];

        for(int i = 0; i < 2; i++) {
            usernames[i] = UUID.randomUUID().toString();
            passwords[i] = UUID.randomUUID().toString();
            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
        }

        String gameName = UUID.randomUUID().toString();

        menus[0].createGame(gameName);

        PlayersGame pg = menus[1].joinGame(gameName);

        ArrayList<Integer[]> move = new ArrayList<Integer[]>();
        move.add(new Integer[]{5,1});
        move.add(new Integer[]{4,0});

        ExecutorService exec = Executors.newFixedThreadPool(100);
        CountDownLatch cdl = new CountDownLatch(100);
        CountDownLatch cdlStart = new CountDownLatch(100);
        AtomicInteger logCount = new AtomicInteger(0);
        IntStream.range(0, 100).forEach(
                i -> exec.execute(
                        () -> {
                            try{
                                cdlStart.countDown();
                                cdlStart.await();
                                if(pg.movePiece(move))
                                    logCount.incrementAndGet();
                            }catch(Exception ex){}
                            finally{
                                cdl.countDown();
                            }
                        }
                )
        );

        cdl.await();
        assertEquals(logCount.get(),1);
    }

    @Test(description = "Check correct user wins after leaving game")
    void checkLeaveGame() throws Exception {
        GameServerImp gameServerImp = new GameServerImp("fish");

        String[] usernames = new String[2];
        String[] passwords = new String[2];
        Menu[] menus = new Menu[2];

        for(int i = 0; i < 2; i++) {
            usernames[i] = UUID.randomUUID().toString();
            passwords[i] = UUID.randomUUID().toString();
            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
        }

        String gameName = UUID.randomUUID().toString();

        PlayersGame pg1 = menus[0].createGame(gameName);

        PlayersGame pg2 = menus[1].joinGame(gameName);

        ExecutorService exec = Executors.newFixedThreadPool(100);
        CountDownLatch cdl = new CountDownLatch(100);
        CountDownLatch cdlStart = new CountDownLatch(100);
        AtomicInteger logCount = new AtomicInteger(0);
        IntStream.range(0, 100).forEach(
                i -> exec.execute(
                        () -> {
                            try{
                                cdlStart.countDown();
                                cdlStart.await();
                                if(i%2 == 0)
                                    pg1.leaveGame();
                                else
                                    pg2.leaveGame();
                                if(pg1.getWon() && i%2 == 0)
                                    logCount.incrementAndGet();
                                if(pg2.getWon() && i%2 == 1)
                                    logCount.incrementAndGet();
                            }catch(Exception ex){}
                            finally{
                                cdl.countDown();
                            }
                        }
                )
        );

        cdl.await();
        assertEquals(logCount.get(),50);
    }

    @Test(description = "Check correct user wins after leaving game")
    void checKillGame() throws Exception {
        GameServerImp gameServerImp = new GameServerImp("fish");

        String[] usernames = new String[2];
        String[] passwords = new String[2];
        Menu[] menus = new Menu[2];

        for(int i = 0; i < 2; i++) {
            usernames[i] = UUID.randomUUID().toString();
            passwords[i] = UUID.randomUUID().toString();
            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
        }

        String gameName = UUID.randomUUID().toString();

        PlayersGame pg1 = menus[0].createGame(gameName);

        PlayersGame pg2 = menus[1].joinGame(gameName);


        ExecutorService exec = Executors.newFixedThreadPool(100);
        CountDownLatch cdl = new CountDownLatch(100);
        CountDownLatch cdlStart = new CountDownLatch(100);
        AtomicInteger logCount = new AtomicInteger(0);

        gameServerImp.killGame(gameName);

        IntStream.range(0, 100).forEach(
                i -> exec.execute(
                        () -> {
                            try{
                                cdlStart.countDown();
                                cdlStart.await();
                                if(i%2 == 0)
                                    pg1.leaveGame();
                                else
                                    pg2.leaveGame();
                                if(pg1.getWon() && i%2 == 0)
                                    logCount.incrementAndGet();
                                if(pg2.getWon() && i%2 == 1)
                                    logCount.incrementAndGet();
                            }catch(Exception ex){}
                            finally{
                                cdl.countDown();
                            }
                        }
                )
        );

        cdl.await();
        assertEquals(logCount.get(),0);
    }


    @Test(description = "Wait my turn signals user correctly")
    void waitMyTurnTest() throws Exception {
        GameServerImp gameServerImp = new GameServerImp("fish");

        String[] usernames = new String[2];
        String[] passwords = new String[2];
        Menu[] menus = new Menu[2];
        PlayersGame[] pgs = new PlayersGame[2];

        for(int i = 0; i < 2; i++) {
            usernames[i] = UUID.randomUUID().toString();
            passwords[i] = UUID.randomUUID().toString();
            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
        }

        String gameName = UUID.randomUUID().toString();

        pgs[0] = menus[0].createGame(gameName);

        pgs[1] = menus[1].joinGame(gameName);

        ArrayList<Integer[]> move = new ArrayList<Integer[]>();
        move.add(new Integer[]{5,1});
        move.add(new Integer[]{4,0});
        AtomicInteger logCount = new AtomicInteger(0);

        ExecutorService exec = Executors.newFixedThreadPool(2);


        CountDownLatch cdl = new CountDownLatch(1);
        CountDownLatch cdl2 = new CountDownLatch(2);

        IntStream.range(0, 2).forEach(
                i -> exec.execute(
                        () -> {
                            try{
                                pgs[i].waitMyTurn();
                                logCount.incrementAndGet();
                            }catch(Exception ex){}
                            finally{
                                cdl.countDown();
                                cdl2.countDown();
                            }
                        }
                )
        );

        cdl.await();
        assertEquals(logCount.get(),1);

        pgs[1].movePiece(move);

        cdl2.await();
        assertEquals(logCount.get(),2);
    }
}
