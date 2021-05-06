package com.zebra.printconnectintentswrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

public class PCUnselectPrinter extends PCIntentsBase {

    /*
    An interface callback to be informed of the result
    of the print template intent
     */
    public interface onUnselectPrinterResult
    {
        void success(PCIntentsBaseSettings settings);
        void error(String errorMessage, int resultCode, Bundle resultData, PCIntentsBaseSettings settings);
        void timeOut(PCIntentsBaseSettings settings);
    }

    private onUnselectPrinterResult mUnselectPrinterCallback = null;

    public PCUnselectPrinter(Context aContext)
    {
        super(aContext);
    }

    public void execute(PCIntentsBaseSettings settings, onUnselectPrinterResult callback)
    {
        if(callback == null)
        {
            Log.e(TAG, PCConstants.PCIntentsNoCallbackError);
            return;
        }

        mUnselectPrinterCallback = callback;

        /*
        Launch timeout mechanism
         */
        super.execute(settings);

        UnselectPrinter(settings);
    }

    private void UnselectPrinter(final PCIntentsBaseSettings settings)
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PCConstants.PCComponentName,PCConstants.PCUnselectPrinterService));

        ResultReceiver receiver = buildIPCSafeReceiver(new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                // Stop timeout mechanism
                cleanAll();
                if (resultCode == 0) {
                    // Result code 0 indicates success
                    if(mUnselectPrinterCallback != null)
                    {
                        mUnselectPrinterCallback.success(settings);
                    }
                } else {
                    // Handle unsuccessful print
                    // Error message (null on successful print)
                    String errorMessage = resultData.getString(PCConstants.PCErrorMessage);
                    if(errorMessage == null)
                        errorMessage = PCConstants.getErrorMessage(resultCode);
                    if(mUnselectPrinterCallback != null)
                    {
                        mUnselectPrinterCallback.error(errorMessage, resultCode, resultData, settings);
                    }
                }
            }
        });
        intent.putExtra(PCConstants.PCTemplatePrintServiceResultReceiver, receiver);
        mContext.startService(intent);
    }

    @Override
    protected void onTimeOut(PCIntentsBaseSettings settings) {
        if(mUnselectPrinterCallback != null)
        {
            mUnselectPrinterCallback.timeOut(settings);
        }
    }
}
