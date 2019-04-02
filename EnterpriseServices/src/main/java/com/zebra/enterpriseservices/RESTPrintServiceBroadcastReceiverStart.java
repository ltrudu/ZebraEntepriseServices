package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RESTPrintServiceBroadcastReceiverStart extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(RESTPrintServiceConstants.TAG, "RESTPrintServiceBroadcastReceiverStart::onReceive");
        // Start service
        RESTPrintService.startService(context);
        RESTPrintServiceActivity.updateGUISwitchesIfNecessary();
    }
}
