
import org.testng.TestNG;
import static org.testng.Assert.*;

import CheckersCommon.*;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by jgodbout on 5/1/2016.
 */
public class UserTests {

    public static void main(String[] args){
        TestNG test = new TestNG( );
        test.setTestClasses(new Class[]{UserTests.class});
        test.setVerbose(2);
        test.run();
    }


    @Test(description = "Basic log in/out")
    void basicLogInLogOut() throws Exception {
        String username = UUID.randomUUID().toString();
        String password = UUID.randomUUID().toString();

        UserDataImp user = new UserDataImp(username,password);
        assertNotNull(user);
        assertEquals(user.getName(),username);
        assertEquals(user.isInGame(),false);

        ExecutorService exec = Executors.newFixedThreadPool(100);
        AtomicInteger logCount = new AtomicInteger(0);
        CountDownLatch cdl = new CountDownLatch(100);
        CountDownLatch cdlStart = new CountDownLatch(100);
        for(int i = 0; i < 100; i++){
            exec.submit(() -> {
                try{
                    cdlStart.countDown();
                    cdlStart.await();
                    if(user.login(password))
                        logCount.incrementAndGet();
                }catch(Exception ex){}
                finally{
                    cdl.countDown();
                }
            });
        }

        cdl.await();
        assertEquals(logCount.get(), 1);

        CountDownLatch cdl2 = new CountDownLatch(100);
        CountDownLatch cdlStart2 = new CountDownLatch(100);
        for(int i = 0; i < 100; i++){
            exec.submit(() -> {
                try{
                    cdlStart2.countDown();
                    cdlStart2.await();
                    user.logout();
                    logCount.incrementAndGet();
                }catch(Exception ex){}
                finally{
                    cdl2.countDown();
                }
            });
        }


        cdl2.await();
        assertEquals(logCount.get(), 2);
    }

    @Test(description = "User can't leave after joining game")
    void joinGameLogoutTest() throws Exception {
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
            menus[1].joinGame(gameName);

            ExecutorService exec = Executors.newFixedThreadPool(100);
            CountDownLatch cdl = new CountDownLatch(100);
            AtomicInteger logCount = new AtomicInteger(0);
            IntStream.range(0, 100).forEach(
                    i -> exec.execute(
                            () -> {
                                try{
                                    if(menus[i%2].logout())
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

    @Test(description = "User can leave after leaving game")
    void leaveGameTest() throws Exception {
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
        menus[1].joinGame(gameName);

        gameServerImp.killGame(gameName);

        ExecutorService exec = Executors.newFixedThreadPool(100);
        CountDownLatch cdl = new CountDownLatch(2);
        AtomicInteger logCount = new AtomicInteger(0);
        IntStream.range(0, 2).forEach(
                i -> exec.execute(
                        () -> {
                            try{
                                if(menus[i%2].logout())
                                    logCount.incrementAndGet();
                            }catch(Exception ex){}
                            finally{
                                cdl.countDown();
                            }
                        }
                )
        );

        cdl.await();
        assertEquals(logCount.get(),2);
    }

    @Test(description = "User can't leave after joining game")
    void leaveGameScoring() throws Exception {
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


    @Test(description = "Basic log in/out/in")
    void basicLogInLogOutIn() throws Exception {
        String username = UUID.randomUUID().toString();
        String password = UUID.randomUUID().toString();

        GameServerImp gameServerImp = new GameServerImp("fish");

        Menu menu = gameServerImp.login(username,password);
        assertTrue(menu.logout());

        Menu menu2 = gameServerImp.login(username,password);
        assertNotNull(menu2);
        assertTrue(menu2.logout());
    }


    @Test(description = "log in/game/out/in")
    void logInGameLogOutLogIn() throws Exception {
        String username = UUID.randomUUID().toString();
        String password = UUID.randomUUID().toString();

        GameServerImp gameServerImp = new GameServerImp("fish");

        Menu menu = gameServerImp.login(username,password);
        PlayersGame pg = menu.createGame("A");
        pg.leaveGame();
        assertTrue(menu.logout());

        Menu menu2 = gameServerImp.login(username,password);
        assertNotNull(menu2);
        assertTrue(menu2.logout());
    }
}
