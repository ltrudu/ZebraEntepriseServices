package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class RESTPrintServiceBroadcastReceiverSetup extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverSetup::onReceive");
        String sStartOnBoot = intent.getExtras().getString(RESTPrintServiceConstants.EXTRA_CONFIGURATION_START_ON_BOOT, null);
        if(sStartOnBoot != null)
        {
            Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverSetup::onReceive:Start on boot extra found with value:" + sStartOnBoot);
            boolean bStartOnBoot = sStartOnBoot.equalsIgnoreCase("true") || sStartOnBoot.equalsIgnoreCase("1");
            setSharedPreference(context, RESTPrintServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, bStartOnBoot);
            // Update GUI if necessary
            RESTPrintServiceActivity.updateGUISwitchesIfNecessary();
        }
        else
        {
            Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverSetup::onReceive:No start on boot extra found.");
        }

        String sAllowExternalIPs = intent.getExtras().getString(RESTPrintServiceConstants.EXTRA_CONFIGURATION_ALLOW_EXTERNAL_IPs, null);
        if(sAllowExternalIPs != null)
        {
            Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverSetup::onReceive:Allow external IPs extra found with value:" + sAllowExternalIPs);
            boolean bAllowExternalIPs = sAllowExternalIPs.equalsIgnoreCase("true") || sAllowExternalIPs.equalsIgnoreCase("1");
            setSharedPreference(context, RESTPrintServiceConstants.SHARED_PREFERENCES_ALLOW_EXTERNAL_IPs, bAllowExternalIPs);
            // Update rest server if launched
            RESTPrintServiceWebServer.mAllowExternalIPs = bAllowExternalIPs;
            // Update GUI if necessary
            RESTPrintServiceActivity.updateGUISwitchesIfNecessary();
        }
        else
        {
            Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverSetup::onReceive:No allow external IPs extra found.");
        }
        
    }

    private void setSharedPreference(Context context, String key, boolean value)
    {
        Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverSetup::setSharedPreference: Key=" + key + " | Value=" + value);
        // Setup shared preferences for next reboot
        SharedPreferences sharedpreferences = context.getSharedPreferences(RESTPrintServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }
}
