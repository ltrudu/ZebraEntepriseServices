package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class RESTHostServiceBroadcastReceiverSetup extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LogHelper.logD( "RESTHostServiceBroadcastReceiverSetup::onReceive");
        String sStartOnBoot = intent.getExtras().getString(RESTHostServiceConstants.EXTRA_CONFIGURATION_START_ON_BOOT, null);
        if(sStartOnBoot != null)
        {
            LogHelper.logD( "RESTHostServiceBroadcastReceiverSetup::onReceive:Start on boot extra found with value:" + sStartOnBoot);
            boolean bStartOnBoot = sStartOnBoot.equalsIgnoreCase("true") || sStartOnBoot.equalsIgnoreCase("1");
            setSharedPreference(context, RESTHostServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, bStartOnBoot);
            // Update GUI if necessary
            RESTHostServiceActivity.updateGUISwitchesIfNecessary();
        }
        else
        {
            LogHelper.logD( "RESTHostServiceBroadcastReceiverSetup::onReceive:No start on boot extra found.");
        }

        String sAllowExternalIPs = intent.getExtras().getString(RESTHostServiceConstants.EXTRA_CONFIGURATION_ALLOW_EXTERNAL_IPs, null);
        if(sAllowExternalIPs != null)
        {
            LogHelper.logD( "RESTHostServiceBroadcastReceiverSetup::onReceive:Allow external IPs extra found with value:" + sAllowExternalIPs);
            boolean bAllowExternalIPs = sAllowExternalIPs.equalsIgnoreCase("true") || sAllowExternalIPs.equalsIgnoreCase("1");
            setSharedPreference(context, RESTHostServiceConstants.SHARED_PREFERENCES_ALLOW_EXTERNAL_IPs, bAllowExternalIPs);
            // Update rest server if launched
            RESTServiceWebServer.mAllowExternalIPs = bAllowExternalIPs;
            // Update GUI if necessary
            RESTHostServiceActivity.updateGUISwitchesIfNecessary();
        }
        else
        {
            LogHelper.logD( "RESTHostServiceBroadcastReceiverSetup::onReceive:No allow external IPs extra found.");
        }
        
    }

    private void setSharedPreference(Context context, String key, boolean value)
    {
        LogHelper.logD( "RESTHostServiceBroadcastReceiverSetup::setSharedPreference: Key=" + key + " | Value=" + value);
        // Setup shared preferences for next reboot
        SharedPreferences sharedpreferences = context.getSharedPreferences(RESTHostServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }
}
