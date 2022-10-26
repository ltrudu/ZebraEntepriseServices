package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RESTHostServiceBroadcastReceiverStop extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        LogHelper.logD( "RESTHostServiceBroadcastReceiverStop::onReceive");
        // Start service
        RESTHostService.stopService(context);
        RESTHostServiceActivity.updateGUISwitchesIfNecessary();
    }
}
