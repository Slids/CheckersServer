import CheckersCommon.GameServerImp;
import CheckersCommon.Menu;
import CheckersCommon.PlayersGame;
import org.testng.TestNG;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by Slid on 5/1/2016.
 */
public class GameServerTests {
    public static void main(String[] args){
        TestNG test = new TestNG( );
        test.setTestClasses(new Class[]{GameServerTests.class});
        test.setVerbose(2);
        test.run();
    }

//
//    @Test(description = "100 users try to make an account with the same username and password")
//    void loginTest() throws Exception {
//        GameServerImp gameServerImp = new GameServerImp("fish");
//
//        String username = UUID.randomUUID().toString();
//        String password = UUID.randomUUID().toString();
//
//        ExecutorService exec = Executors.newFixedThreadPool(100);
//        AtomicInteger logCount = new AtomicInteger(0);
//        CountDownLatch cdl = new CountDownLatch(100);
//        CountDownLatch cdlStart = new CountDownLatch(100);
//        for(int i = 0; i < 100; i++){
//            exec.submit(() -> {
//                try{
//                    cdlStart.countDown();
//                    cdlStart.await();
//                    Menu menu = gameServerImp.login(username,password);
//                    if(menu != null)
//                        logCount.incrementAndGet();
//                }catch(Exception ex){}
//                finally{
//                    cdl.countDown();
//                }
//            });
//        }
//
//        cdl.await();
//        assertEquals(logCount.get(),1);
//    }
//
//    @Test(description = "100 users try to make a game with the same name")
//    void gameCreationTest() throws Exception {
//        GameServerImp gameServerImp = new GameServerImp("fish");
//
//        String[] usernames = new String[100];
//        String[] passwords = new String[100];
//        Menu[] menus = new Menu[100];
//
//        for(int i = 0; i <100; i++) {
//            usernames[i] = UUID.randomUUID().toString();
//            passwords[i] = UUID.randomUUID().toString();
//            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
//        }
//
//        ExecutorService exec = Executors.newFixedThreadPool(100);
//        AtomicInteger logCount = new AtomicInteger(0);
//        CountDownLatch cdl = new CountDownLatch(100);
//        CountDownLatch cdlStart = new CountDownLatch(100);
//        IntStream.range(0, 100).forEach(
//                i -> exec.execute(
//                        () -> {
//                            try{
//                                cdlStart.countDown();
//                                cdlStart.await();
//                                PlayersGame pg = menus[i].createGame("fish");
//                                if(pg != null)
//                                    logCount.incrementAndGet();
//                            }catch(Exception ex){}
//                            finally{
//                                cdl.countDown();
//                            }
//                        }
//                )
//        );
//
//        cdl.await();
//        assertEquals(logCount.get(),1);
//        assertEquals(gameServerImp.listCurrentGames().size(),1);
//    }
//
//    @Test(description = "Verify GetName returns correct name")
//    void nameTest() throws Exception {
//        GameServerImp gameServerImp = new GameServerImp("fish");
//
//        assertEquals(gameServerImp.getName(),"fish");
//    }
//
//    @Test(description = "After shutdown, no users returned")
//    void shutdownNoUsers() throws Exception {
//        GameServerImp gameServerImp = new GameServerImp("fish");
//
//        String[] usernames = new String[100];
//        String[] passwords = new String[100];
//        Menu[] menus = new Menu[100];
//
//        for(int i = 0; i <100; i++) {
//            usernames[i] = UUID.randomUUID().toString();
//            passwords[i] = UUID.randomUUID().toString();
//        }
//
//        ExecutorService exec = Executors.newFixedThreadPool(100);
//        CountDownLatch cdl = new CountDownLatch(100);
//        CountDownLatch cdlStart = new CountDownLatch(100);
//        IntStream.range(0, 100).forEach(
//                i -> exec.execute(
//                        () -> {
//                            try{
//                                cdlStart.countDown();
//                                cdlStart.await();
//                                menus[i] = gameServerImp.login(usernames[i],passwords[i]);
//                            }catch(Exception ex){}
//                            finally{
//                                cdl.countDown();
//                            }
//                        }
//                )
//        );
//        cdlStart.await();
//        gameServerImp.shutDown();
//        cdl.await();
//        assertEquals(gameServerImp.getNumberOfUsers(),0);
//    }
//
//    @Test(description = "100 users try to make a game with the same name")
//    void shutdownNoGameTest() throws Exception {
//        GameServerImp gameServerImp = new GameServerImp("fish");
//
//        String[] usernames = new String[100];
//        String[] passwords = new String[100];
//        Menu[] menus = new Menu[100];
//
//        for(int i = 0; i <100; i++) {
//            usernames[i] = UUID.randomUUID().toString();
//            passwords[i] = UUID.randomUUID().toString();
//            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
//        }
//
//        ExecutorService exec = Executors.newFixedThreadPool(100);
//        CountDownLatch cdl = new CountDownLatch(100);
//        CountDownLatch cdlStart = new CountDownLatch(100);
//        IntStream.range(0, 100).forEach(
//                i -> exec.execute(
//                        () -> {
//                            try{
//                                cdlStart.countDown();
//                                cdlStart.await();
//                                PlayersGame pg = menus[i].createGame("fish" + i);
//                            }catch(Exception ex){}
//                            finally{
//                                cdl.countDown();
//                            }
//                        }
//                )
//        );
//
//        cdlStart.await();
//        gameServerImp.shutDown();
//        cdl.await();
//        assertEquals(gameServerImp.listCurrentGames().size(),0);
//    }
//
//    @Test(description = "Creating 100 have correct number of users")
//    void createUsers() throws Exception {
//        GameServerImp gameServerImp = new GameServerImp("fish");
//
//        String[] usernames = new String[100];
//        String[] passwords = new String[100];
//        Menu[] menus = new Menu[100];
//
//        for(int i = 0; i <100; i++) {
//            usernames[i] = UUID.randomUUID().toString();
//            passwords[i] = UUID.randomUUID().toString();
//        }
//
//        ExecutorService exec = Executors.newFixedThreadPool(100);
//        CountDownLatch cdl = new CountDownLatch(100);
//        CountDownLatch cdlStart = new CountDownLatch(100);
//        IntStream.range(0, 100).forEach(
//                i -> exec.execute(
//                        () -> {
//                            try{
//                                cdlStart.countDown();
//                                cdlStart.await();
//                                menus[i] = gameServerImp.login(usernames[i],passwords[i]);
//                            }catch(Exception ex){}
//                            finally{
//                                cdl.countDown();
//                            }
//                        }
//                )
//        );
//        cdlStart.await();
//        cdl.await();
//        assertEquals(gameServerImp.getNumberOfUsers(),100);
//    }
//
//    @Test(description = "100 games have correct number of games")
//    void createGamesTest() throws Exception {
//        GameServerImp gameServerImp = new GameServerImp("fish");
//
//        String[] usernames = new String[100];
//        String[] passwords = new String[100];
//        Menu[] menus = new Menu[100];
//
//        for(int i = 0; i <100; i++) {
//            usernames[i] = UUID.randomUUID().toString();
//            passwords[i] = UUID.randomUUID().toString();
//            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
//        }
//
//        ExecutorService exec = Executors.newFixedThreadPool(100);
//        CountDownLatch cdl = new CountDownLatch(100);
//        CountDownLatch cdlStart = new CountDownLatch(100);
//        IntStream.range(0, 100).forEach(
//                i -> exec.execute(
//                        () -> {
//                            try{
//                                cdlStart.countDown();
//                                cdlStart.await();
//                                PlayersGame pg = menus[i].createGame("fish" + i);
//                            }catch(Exception ex){}
//                            finally{
//                                cdl.countDown();
//                            }
//                        }
//                )
//        );
//
//        cdlStart.await();
//        cdl.await();
//        assertEquals(gameServerImp.listCurrentGames().size(),100);
//    }
//
//    @Test(description = "verify creation with previous data")
//    void gameServerPrevData() throws Exception {
//        GameServerImp gameServerImp = new GameServerImp("fish");
//
//        String[] usernames = new String[100];
//        String[] passwords = new String[100];
//        Menu[] menus = new Menu[100];
//
//        for(int i = 0; i <100; i++) {
//            usernames[i] = UUID.randomUUID().toString();
//            passwords[i] = UUID.randomUUID().toString();
//            gameServerImp.login(usernames[i],passwords[i]);
//        }
//
//
//        gameServerImp.shutDown();
//
//        GameServerImp gs3 = new GameServerImp("fish2", "fishCheckerData.txt");
//        assertEquals(gs3.getNumberOfUsers(),100);
//    }

    @Test(description = "verify creation with previous data restores data")
    void gameServerPrevDataGameScoring() throws Exception {
        GameServerImp gameServerImp = new GameServerImp("fish");

        String[] usernames = new String[100];
        String[] passwords = new String[100];
        Menu[] menus = new Menu[100];
        String[] gameName = new String[100];
        PlayersGame[] pgs = new PlayersGame[100];

        for(int i = 0; i < 100; i++) {
            usernames[i] = UUID.randomUUID().toString();
            passwords[i] = UUID.randomUUID().toString();
            menus[i] = gameServerImp.login(usernames[i],passwords[i]);
            gameName[i] = UUID.randomUUID().toString();
            if(i%2 == 0)
                pgs[i] = menus[i].createGame(gameName[i]);
            else
                pgs[i] = menus[i].joinGame(gameName[i-1]);
        }

        ExecutorService exec = Executors.newFixedThreadPool(100);
        CountDownLatch cdl = new CountDownLatch(100);
        CountDownLatch cdlStart = new CountDownLatch(100);
        IntStream.range(0, 100).forEach(
                i -> exec.execute(
                        () -> {
                            try{
                                cdlStart.countDown();
                                cdlStart.await();
                                pgs[i].leaveGame();

                            }catch(Exception ex){}
                            finally{
                                cdl.countDown();
                            }
                        }
                )
        );


        cdl.await();
        gameServerImp.shutDown();

        GameServerImp gs2 = new GameServerImp("fish2", "fishCheckerData.txt");

        for(int i = 0; i < 100; i++) {
            menus[i] = gs2.login(usernames[i],passwords[i]);
        }

        for(int i = 0; i < 100; i++){
            assert(menus[i].userStatistics().get("Wins") +
                    menus[i].userStatistics().get("Losses") == 1);
            if(i% 2 == 0) {
                assert (menus[i].userStatistics().get("Wins") +
                        menus[i + 1].userStatistics().get("Wins") == 1);
                assert (menus[i].userStatistics().get("Losses") +
                        menus[i + 1].userStatistics().get("Losses") == 1);
            }
        }
    }
}
