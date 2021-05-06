package com.zebra.printconnectintentswrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.util.HashMap;

public class PCPrinterStatus extends PCIntentsBase {

    /*
    An interface callback to be informed of the result
    of the print template intent
     */
    public interface onPrinterStatusResult
    {
        void success(PCIntentsBaseSettings settings, HashMap<String, String> printerStatusMap);
        void error(String errorMessage, int resultCode, Bundle resultData, PCIntentsBaseSettings settings);
        void timeOut(PCIntentsBaseSettings settings);
    }

    private onPrinterStatusResult mPrinterStatusCallback = null;

    public PCPrinterStatus(Context aContext)
    {
        super(aContext);
    }

    public void execute(PCIntentsBaseSettings settings, onPrinterStatusResult callback)
    {
        if(callback == null)
        {
            Log.e(TAG, PCConstants.PCIntentsNoCallbackError);
            return;
        }

        mPrinterStatusCallback = callback;

        /*
        Launch timeout mechanism
         */
        super.execute(settings);

        GetPrinterStatus(settings);
    }

    private void GetPrinterStatus(final PCIntentsBaseSettings settings)
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PCConstants.PCComponentName,PCConstants.PCPrinterStatusService));

        ResultReceiver receiver = buildIPCSafeReceiver(new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                // Stop timeout mechanism
                cleanAll();
                if (resultCode == 0) {
                    // Result code 0 indicates success
                    if(mPrinterStatusCallback != null)
                    {
                        HashMap<String, String> printerStatusMap = (HashMap<String, String>)resultData.getSerializable(PCConstants.PCPrinterStatusMap);
                        mPrinterStatusCallback.success(settings, printerStatusMap);
                    }
                } else {
                    // Handle unsuccessful print
                    // Error message (null on successful print)
                    String errorMessage = resultData.getString(PCConstants.PCErrorMessage);
                    if(errorMessage == null)
                        errorMessage = PCConstants.getErrorMessage(resultCode);
                    if(mPrinterStatusCallback != null)
                    {
                        mPrinterStatusCallback.error(errorMessage, resultCode, resultData, settings);
                    }
                }
            }
        });
        intent.putExtra(PCConstants.PCTemplatePrintServiceResultReceiver, receiver);
        mContext.startService(intent);
    }

    @Override
    protected void onTimeOut(PCIntentsBaseSettings settings) {
        if(mPrinterStatusCallback != null)
        {
            mPrinterStatusCallback.timeOut(settings);
        }
    }
}
