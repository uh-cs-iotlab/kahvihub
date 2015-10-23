package fi.helsinki.cs.iot.kahvihub.activity;

import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import fi.helsinki.cs.iot.hub.database.IotHubDataAccess;
import fi.helsinki.cs.iot.hub.jsengine.DuktapeJavascriptEngineWrapper;
import fi.helsinki.cs.iot.hub.utils.Logger;
import fi.helsinki.cs.iot.kahvihub.R;
import fi.helsinki.cs.iot.kahvihub.database.KahvihubDataHandler;
import fi.helsinki.cs.iot.kahvihub.service.IotHubWebService;

public class MainActivity extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {

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

        Button testButton = (Button)findViewById(R.id.testButton);
        testButton.setOnClickListener(this);
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

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.testButton) {
            ProgressDialog progressDialog = ProgressDialog.show(this,
                    "Testing", "Please wait...", true);
            doTesting(progressDialog);
        }
    }

    private void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                dialog.dismiss();
            }
        });
        // 3. Get the AlertDialog from create()
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void doTesting(ProgressDialog dialog) {
        int port = IotHubWebService.getPort();
        String res = DuktapeJavascriptEngineWrapper.performJavaHttpRequest(
                "GET", "http://127.0.0.1:" + port + "/plugins/", null);

        if(!"[]".equals(res.trim())) {
            dialog.dismiss();
            showMessage("Testing failed",
                    "I got the message " + res + " instead of [] for plugins");
        }

        //Dismiss the dialog
        dialog.dismiss();
        showMessage("Testing passed",
                "You got it right");
    }
}
