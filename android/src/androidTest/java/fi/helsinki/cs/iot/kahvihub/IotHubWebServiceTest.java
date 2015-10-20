package fi.helsinki.cs.iot.kahvihub;

import android.content.Context;
import android.content.Intent;
import android.test.ServiceTestCase;
import android.test.suitebuilder.annotation.SmallTest;


import org.junit.After;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.utils.Logger;
import fi.helsinki.cs.iot.kahvihub.database.KahvihubDataHandler;
import fi.helsinki.cs.iot.kahvihub.service.IotHubWebService;

/**
 * Created by mineraud on 20.10.2015.
 */
public class IotHubWebServiceTest extends ServiceTestCase<IotHubWebService> {

    private static final String TAG = "IotHubWebServiceTest";

    public IotHubWebServiceTest() {
        super(IotHubWebService.class);
    }

    private static void setLogger() {
        Logger logger = new Logger(){
            @Override
            public void d(String tag, String msg) {
                android.util.Log.d(TAG, msg);
            }
            @Override
            public void i(String tag, String msg) {
                android.util.Log.i(TAG, msg);
            }
            @Override
            public void w(String tag, String msg) {
                android.util.Log.w(TAG, msg);
            }
            @Override
            public void e(String tag, String msg) {
                android.util.Log.i(TAG, msg);
            }
        };
        fi.helsinki.cs.iot.hub.utils.Log.setLogger(logger);
    }

    private void setIotHubDataHandler(Context context) {
        IotHubDataAccess.setInstance(new KahvihubDataHandler(context));
    }

    private void init(Context context) {
        setLogger();
        setIotHubDataHandler(context);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        init(getContext());
        Log.d(TAG, "Setup complete");
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        Log.d(TAG, "Teardown complete");
    }

    /**
     * Test basic startup/shutdown of Service
     */
    @SmallTest
    public void testStartable() {
        Intent startIntent = new Intent();
        startIntent.setClass(getContext(), IotHubWebServiceTest.class);
        startService(startIntent);
        assertNotNull(getService());
    }
}
