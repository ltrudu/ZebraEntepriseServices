package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class RESTHostServiceBroadcastReceiverBootCompleted extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(RESTHostServiceConstants.TAG, "RESTHostServiceBroadcastReceiverBootCompleted::onReceive");
        SharedPreferences sharedpreferences = context.getSharedPreferences(RESTHostServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startService = sharedpreferences.getBoolean(RESTHostServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
        Log.d(RESTHostServiceConstants.TAG, startService ? "Auto start service" : "Do nothing on boot");
        if(startService)
        {
            RESTHostService.startService(context);
        }
    }
}
