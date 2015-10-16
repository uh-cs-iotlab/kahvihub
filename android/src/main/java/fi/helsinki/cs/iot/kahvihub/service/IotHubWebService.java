package fi.helsinki.cs.iot.kahvihub.service;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

import fi.helsinki.cs.iot.hub.utils.Log;
import fi.helsinki.cs.iot.hub.webserver.IotHubHTTPD;
import fi.helsinki.cs.iot.kahvihub.R;

public class IotHubWebService extends Service {

    public static final String START_FOREGROUND_ACTION =
            "fi.helsinki.cs.iot.kahvihub.service.IotHubWebService.action.startforeground";
    public static final String STOP_FOREGROUND_ACTION =
            "fi.helsinki.cs.iot.kahvihub.service.IotHubWebService.action.stopforeground";

    private static final String LOG_TAG = "IotHubWebService";
    private static final int IOT_HUB_PORT = 4563;
    private static final int FOREGROUND_ID = IOT_HUB_PORT;

    private int mStartMode;       // indicates how to behave if the service is killed
    private IBinder mBinder;      // interface for clients that bind
    private boolean mAllowRebind; // indicates whether onRebind should be used

    private IotHubHTTPD mServer;

    public IotHubWebService() {
        mStartMode = START_STICKY;
        mBinder = new LocalBinder();
        mAllowRebind = false; //TODO check which value should be set here
    }

    @Override
    public void onCreate() {
        // The service is being created
        super.onCreate();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // The service is starting, due to a call to startService()
        if (START_FOREGROUND_ACTION.equals(intent.getAction())) {
            Log.i(LOG_TAG, "Received Start Foreground Intent");

            startForeground(FOREGROUND_ID,
                    buildForegroundNotification("IoT Hub Web Server running"));
        }
        else if (STOP_FOREGROUND_ACTION.equals(intent.getAction())) {
            stopForeground(true);
            if (mServer != null) {
                mServer.stop();
                mServer = null;
            }
        }
        return mStartMode;
    }

    private Notification buildForegroundNotification(String msg) {

        NotificationCompat.Builder b = new NotificationCompat.Builder(this);
        b.setOngoing(true);

        b.setContentTitle(getString(R.string.app_name))
                .setContentText(msg)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(getString(R.string.app_name));
        return b.build();
    }


    @Override
    public IBinder onBind(Intent intent) {
        // A client is binding to the service with bindService()
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // All clients have unbound with unbindService()
        return mAllowRebind;
    }
    @Override
    public void onRebind(Intent intent) {
        // A client is binding to the service with bindService(),
        // after onUnbind() has already been called
    }
    @Override
    public void onDestroy() {
        // The service is no longer used and is being destroyed
        if (mServer != null) mServer.stop();
        super.onDestroy();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public IotHubWebService getService() {
            // Return this instance of IotHubWebService so clients can call public methods
            return IotHubWebService.this;
        }
    }

    public static int getPort() {
        return IOT_HUB_PORT;
    }
}
