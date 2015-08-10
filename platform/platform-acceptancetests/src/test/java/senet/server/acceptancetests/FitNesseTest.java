package senet.server.acceptancetests;

import com.neuri.trinidad.JUnitHelper;
import com.neuri.trinidad.TestEngine;
import com.neuri.trinidad.fitnesserunner.FitNesseRepository;
import com.neuri.trinidad.fitnesserunner.FitTestEngine;
import com.neuri.trinidad.transactionalrunner.TransactionalTestEngineDecorator;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;

public class FitNesseTest {
    JUnitHelper helper;

    public FitNesseTest() {
        TestEngine engine = new TransactionalTestEngineDecorator("classpath:spring.xml", new FitTestEngine());

        String path = ClassLoader.getSystemResource("spring.xml").getPath().replace("target/test-classes/spring.xml", "FitNesse");
        try {
            helper = new JUnitHelper(new FitNesseRepository(URLDecoder.decode(path, "UTF-8")), engine,
                    new File(System.getProperty("java.io.tmpdir"), "senet-fitnesse-tests").getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            throw new Error(e);
        }
    }

    @Test
    public void runRegressionTests() throws Exception {
        helper.assertSuitePasses("RegressionTests");
    }

    @Ignore("Use for development only, i.e. testing a single test")
    @Test
    public void runRegressionTest() throws Exception {
        helper.assertTestPasses("RegressionTests.StrataTournaments.TrophyLeaderboards.CalculatePoints");
    }
}
