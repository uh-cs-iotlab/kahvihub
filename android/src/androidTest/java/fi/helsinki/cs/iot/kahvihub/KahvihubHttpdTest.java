package fi.helsinki.cs.iot.kahvihub;

import android.app.Application;
import android.test.ApplicationTestCase;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class KahvihubHttpdTest extends ApplicationTestCase<Application> {

    private Application application;

    public KahvihubHttpdTest() {
        super(Application.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createApplication();
        application = getApplication();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}