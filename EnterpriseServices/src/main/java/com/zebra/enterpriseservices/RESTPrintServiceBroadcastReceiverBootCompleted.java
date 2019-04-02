package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class RESTPrintServiceBroadcastReceiverBootCompleted extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverBootCompleted::onReceive");
        SharedPreferences sharedpreferences = context.getSharedPreferences(RESTPrintServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        boolean startService = sharedpreferences.getBoolean(RESTPrintServiceConstants.SHARED_PREFERENCES_START_SERVICE_ON_BOOT, false);
        Log.d(RESTPrintServiceConstants.TAG, startService ? "Auto start service" : "Do nothing on boot");
        if(startService)
        {
            RESTPrintService.startService(context);
        }
    }
}
