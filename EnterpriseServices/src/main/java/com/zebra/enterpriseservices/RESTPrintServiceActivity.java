package com.zebra.enterpriseservices;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class RESTPrintServiceActivity extends AppCompatActivity {
    private Switch mStartStopServiceSwitch = null;
    private Switch mAutoStartServiceOnBootSwitch = null;
    private Switch mAllowExternalIPsSwitch = null;
    private TextView mDeviceIPTextView = null;
    protected static RESTPrintServiceActivity mMainActivity;
    private RESTPrintServiceWifiIPObserver mIPChangeObserver = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restprintservice);
        setTitle(R.string.restconfigactivitytitle);

        ((Button)findViewById(R.id.btLicense)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(RESTPrintServiceActivity.this, LicenceActivity.class);
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
                    if(!RESTPrintService.isRunning(RESTPrintServiceActivity.this))
                        RESTPrintService.startService(RESTPrintServiceActivity.this);
                    updateIP();
                }
                else
                {
                    mStartStopServiceSwitch.setText(getString(R.string.serviceStopped));
                    if(RESTPrintService.isRunning(RESTPrintServiceActivity.this))
                        RESTPrintService.stopService(RESTPrintServiceActivity.this);
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
                SharedPreferences sharedpreferences = getSharedPreferences(RESTPrintServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(RESTPrintServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, isChecked);
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
                SharedPreferences sharedpreferences = getSharedPreferences(RESTPrintServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedpreferences.edit();
                editor.putBoolean(RESTPrintServiceConstants.SHARED_PREFERENCES_ALLOW_EXTERNAL_IPs, isChecked);
                editor.commit();
                RESTPrintServiceWebServer.mAllowExternalIPs = isChecked;
            }
        });

        mDeviceIPTextView = (TextView)findViewById(R.id.tv_ip);

        SharedPreferences sharedpreferences = getSharedPreferences(RESTPrintServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startServiceOnBoot = sharedpreferences.getBoolean(RESTPrintServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
        if(startServiceOnBoot == true && RESTPrintService.isRunning(this.getApplicationContext()) == false)
        {
            // we automatically start the service if the option StartOnBoot is set to true, and the service is not started
            RESTPrintService.startService(this.getApplicationContext());
        }

    }

    @Override
    protected void onResume() {
        mMainActivity = this;
        if(mIPChangeObserver == null)
        {
            mIPChangeObserver = new RESTPrintServiceWifiIPObserver(getApplicationContext(), new RESTPrintServiceWifiIPObserver.IIPChangeObserver() {
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
                if(RESTPrintService.isRunning(RESTPrintServiceActivity.this))
                {
                    setServiceStartedSwitchValues(true, getString(R.string.serviceStarted));
                }
                else
                {
                    setServiceStartedSwitchValues(false, getString(R.string.serviceStopped));
                }

                SharedPreferences sharedpreferences = getSharedPreferences(RESTPrintServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                boolean startServiceOnBoot = sharedpreferences.getBoolean(RESTPrintServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
                setAutoStartServiceOnBootSwitch(startServiceOnBoot, startServiceOnBoot ? getString(R.string.startOnBoot) : getString(R.string.doNothingOnBoot));

                boolean allowExternalIPs = sharedpreferences.getBoolean(RESTPrintServiceConstants.SHARED_PREFERENCES_ALLOW_EXTERNAL_IPs, false);
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
                    if(RESTPrintService.isRunning(RESTPrintServiceActivity.this.getApplicationContext()) && mIPChangeObserver.isConnectedToWifi())
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
        if(RESTPrintServiceActivity.mMainActivity != null) // The application default activity has been opened
        {
            RESTPrintServiceActivity.mMainActivity.updateSwitches();
        }
    }
}
