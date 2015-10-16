package fi.helsinki.cs.iot.kahvihub.activity;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.utils.Logger;
import fi.helsinki.cs.iot.kahvihub.R;
import fi.helsinki.cs.iot.kahvihub.database.KahvihubDataHandler;
import fi.helsinki.cs.iot.kahvihub.service.IotHubWebService;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "KahvihubMainActivity";

    private static void setLogger() {
        Logger logger = new Logger(){
            @Override
            public void d(String tag, String msg) {
                Log.d(TAG, msg);
            }
            @Override
            public void i(String tag, String msg) {
               Log.i(TAG, msg);
            }
            @Override
            public void w(String tag, String msg) {
                Log.w(TAG, msg);
            }
            @Override
            public void e(String tag, String msg) {
                Log.i(TAG, msg);
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Switch webServerSwitch = (Switch)findViewById(R.id.webServerSwitch);
        webServerSwitch.setChecked(isIotHubWebServiceRunning());
        webServerSwitch.setOnCheckedChangeListener(this);
        init(this.getApplicationContext());
    }

    private boolean isIotHubWebServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (IotHubWebService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.webServerSwitch:
                if (isChecked) {
                    Intent startIntent = new Intent(MainActivity.this, IotHubWebService.class);
                    startIntent.setAction(IotHubWebService.START_FOREGROUND_ACTION);
                    startService(startIntent);
                }
                else {
                    Intent stopIntent = new Intent(MainActivity.this, IotHubWebService.class);
                    stopIntent.setAction(IotHubWebService.STOP_FOREGROUND_ACTION);
                    startService(stopIntent);
                }
                break;
        }
    }
}
