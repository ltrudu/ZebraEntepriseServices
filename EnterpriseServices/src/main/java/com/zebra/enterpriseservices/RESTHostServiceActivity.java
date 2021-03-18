package com.zebra.enterpriseservices;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

// The service can be launched using the graphical user interface, intent actions or adb.
//
// If the option "Start on boot" is enabled, the service will be automatically launched when the boot is complete.
//
//
// The service respond to two intent actions (both uses the category: android.intent.category.DEFAULT)
// - "com.zebra.enterpriseservices.startservice" sent on the component "com.zebra.enterpriseservices/com.zebra.enterpriseservices.StartServiceBroadcastReceiver":
//   Start the service.
// - "com.zebra.enterpriseservices.stopservice" sent on the component "com.zebra.enterpriseservices/com.zebra.enterpriseservices.StopServiceBroadcastReceiver":
//   Stop the service.
//
// The service can be started and stopped manually using the following adb commands:
//  - Start service:
//      adb shell am broadcast -a com.zebra.enterpriseservices.startservice -n com.zebra.enterpriseservices/com.zebra.enterpriseservices.StartServiceBroadcastReceiver
//  - Stop service:
//      adb shell am broadcast -a com.zebra.enterpriseservices.stopservice -n com.zebra.enterpriseservices/com.zebra.enterpriseservices.StopServiceBroadcastReceiver
//  - Setup service
//          The service can be configured using the following intent:
//          adb shell am broadcast -a com.zebra.enterpriseservices.setupservice -n com.zebra.enterpriseservices/com.zebra.enterpriseservices.SetupServiceBroadcastReceiver --es startonboot "true" --es allowexternalips "false"
//          The command must contain at least one of the extras:
//
//          - Configure autostart on boot:
//          --es startonboot "true"
//                  If the device get rebooted the service will start automatically once the reboot is completed.
//          --es startonboot "false"
//                  If the device is rebooted, the service will not be started (unless it has been setup/configured to boot on startup).
//
//          - Allow access from external IPs (false = )
//          --es allowexternalips "true"
//              Allows external devices to connect to the service.
//          --es allowexternalips "false"
//              Grant access to the localhost only (only the device running the service will be allowed to use it)


public class RESTHostServiceActivity extends AppCompatActivity {
    private Switch mStartStopServiceSwitch = null;
    private Switch mAutoStartServiceOnBootSwitch = null;
    private Switch mAllowExternalIPsSwitch = null;
    private TextView mDeviceIPTextView = null;
    protected static RESTHostServiceActivity mMainActivity;
    private RESTHostServiceWifiStateObserver mIPChangeObserver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restprintservice);

        ((Button)findViewById(R.id.btLicense)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(RESTHostServiceActivity.this, LicenceActivity.class);
                startActivity(myIntent);
            }
        });

        mStartStopServiceSwitch = (Switch)findViewById(R.id.startStopServiceSwitch);
        mStartStopServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStarted));
                    if(!RESTHostService.isRunning(RESTHostServiceActivity.this))
                        RESTHostService.startService(RESTHostServiceActivity.this);
                    updateIP();
                }
                else
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStopped));
                    if(RESTHostService.isRunning(RESTHostServiceActivity.this))
                        RESTHostService.stopService(RESTHostServiceActivity.this);
                    updateIP();
                }
            }
        });

        mAutoStartServiceOnBootSwitch = (Switch)findViewById(R.id.startOnBootSwitch);
        mAutoStartServiceOnBootSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mAutoStartServiceOnBootSwitch.setText(getString(R.string.startOnBoot));
                }
                else
                {
                    mAutoStartServiceOnBootSwitch.setText(getString(R.string.doNothingOnBoot));
                }
                SharedPreferences sharedpreferences = getSharedPreferences(RESTHostServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(RESTHostServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, isChecked);
                editor.commit();
            }
        });

        mAllowExternalIPsSwitch = (Switch)findViewById(R.id.allowExternalIPsSwitch);
        mAllowExternalIPsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    mAllowExternalIPsSwitch.setText(getString(R.string.allowexteralip));
                }
                else
                {
                    mAllowExternalIPsSwitch.setText(getString(R.string.blockexternalip));
                }
                SharedPreferences sharedpreferences = getSharedPreferences(RESTHostServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(RESTHostServiceConstants.SHARED_PREFERENCES_ALLOW_EXTERNAL_IPs, isChecked);
                editor.commit();
                RESTServiceWebServer.mAllowExternalIPs = isChecked;
            }
        });

        mDeviceIPTextView = (TextView)findViewById(R.id.tv_ip);

        SharedPreferences sharedpreferences = getSharedPreferences(RESTHostServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startServiceOnBoot = sharedpreferences.getBoolean(RESTHostServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
        if(startServiceOnBoot == true && RESTHostService.isRunning(this.getApplicationContext()) == false)
        {
            // we automatically start the service if the option StartOnBoot is set to true, and the service is not started
            RESTHostService.startService(this.getApplicationContext());
        }

    }

    @Override
    protected void onResume() {
        mMainActivity = this;
        if(mIPChangeObserver == null)
        {
            mIPChangeObserver = new RESTHostServiceWifiStateObserver(getApplicationContext(), new RESTHostServiceWifiStateObserver.IIPChangeObserver() {
                @Override
                public void onIPChanged(String newIP) {
                    updateIP();
                }
            });

            mIPChangeObserver.startObserver();
        }
        else if(mIPChangeObserver.isStarted() == false)
        {
            mIPChangeObserver.startObserver();
        }
        super.onResume();
        updateSwitches();
        updateIP();
    }


    @Override
    protected void onPause() {
        mMainActivity = null;
        super.onPause();
        // Stop observing IP Changes
        if(mIPChangeObserver != null && mIPChangeObserver.isStarted())
        {
            mIPChangeObserver.stopObserver();
        }
        mIPChangeObserver = null;
    }

    public void updateSwitches()
    {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(RESTHostService.isRunning(RESTHostServiceActivity.this))
                {
                    setServiceStartedSwitchValues(true, getString(R.string.serviceStarted));
                }
                else
                {
                    setServiceStartedSwitchValues(false, getString(R.string.serviceStopped));
                }

                SharedPreferences sharedpreferences = getSharedPreferences(RESTHostServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean startServiceOnBoot = sharedpreferences.getBoolean(RESTHostServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
                setAutoStartServiceOnBootSwitch(startServiceOnBoot, startServiceOnBoot ? getString(R.string.startOnBoot) : getString(R.string.doNothingOnBoot));

                boolean allowExternalIPs = sharedpreferences.getBoolean(RESTHostServiceConstants.SHARED_PREFERENCES_ALLOW_EXTERNAL_IPs, false);
                setAllowExternalIPsSwitch(allowExternalIPs, allowExternalIPs ? getString(R.string.allowexteralip) : getString(R.string.blockexternalip));

            }
        });

    }

    private void setServiceStartedSwitchValues(final boolean checked, final String text)
    {
        mStartStopServiceSwitch.setChecked(checked);
        mStartStopServiceSwitch.setText(text);
    }

    private void setAutoStartServiceOnBootSwitch(final boolean checked, final String text)
    {
        mAutoStartServiceOnBootSwitch.setChecked(checked);
        mAutoStartServiceOnBootSwitch.setText(text);
    }

    private void setAllowExternalIPsSwitch(final boolean checked, final String text)
    {
        mAllowExternalIPsSwitch.setChecked(checked);
        mAllowExternalIPsSwitch.setText(text);
    }

    private void updateIP()
    {
        if(mIPChangeObserver != null)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(RESTHostService.isRunning(RESTHostServiceActivity.this.getApplicationContext()) && mIPChangeObserver.isConnected())
                    {
                        String deviceIP = mIPChangeObserver.getIPAddress();
                        mDeviceIPTextView.setVisibility(View.VISIBLE);
                        mDeviceIPTextView.setText(getString(R.string.iptextviewprefix) + deviceIP);
                    }
                    else
                    {
                        mDeviceIPTextView.setVisibility(View.GONE);
                    }
                }
            });
        }
    }

    public static void updateGUISwitchesIfNecessary()
    {
        // Update GUI if necessary
        if(RESTHostServiceActivity.mMainActivity != null) // The application default activity has been opened
        {
            RESTHostServiceActivity.mMainActivity.updateSwitches();
        }
    }
}
