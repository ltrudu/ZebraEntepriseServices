package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RESTPrintServiceBroadcastReceiverStop extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverStop::onReceive");
        // Start service
        RESTPrintService.stopService(context);
        RESTPrintServiceActivity.updateGUISwitchesIfNecessary();
    }
}
