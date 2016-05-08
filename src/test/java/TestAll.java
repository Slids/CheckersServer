import org.testng.TestNG;

/**
 * Created by Slid on 5/2/2016.
 */
public class TestAll {
    public static void main(String[] args){
        TestNG test = new TestNG( );
        test.setTestClasses(new Class[]{UserTests.class,GameTests.class,GameServerTests.class});
        test.setVerbose(2);
        test.run();
    }
}
