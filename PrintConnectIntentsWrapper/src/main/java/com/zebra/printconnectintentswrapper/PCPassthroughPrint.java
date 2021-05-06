package com.zebra.printconnectintentswrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class PCPassthroughPrint extends PCIntentsBase {

    /*
    An interface callback to be informed of the result
    of the print template intent
     */
    public interface onPassthroughResult
    {
        void success(PCPassthroughPrintSettings settings);
        void error(String errorMessage, int resultCode, Bundle resultData, PCPassthroughPrintSettings settings);
        void timeOut(PCPassthroughPrintSettings settings);
    }

    private onPassthroughResult mPassthroughCallback = null;

    public PCPassthroughPrint(Context aContext)
    {
        super(aContext);
    }

    public void execute(PCPassthroughPrintSettings settings, onPassthroughResult callback)
    {
        if(callback == null)
        {
            Log.e(TAG, PCConstants.PCIntentsNoCallbackError);
            return;
        }

        mPassthroughCallback = callback;

        if(settings.mPassthroughData == null || settings.mPassthroughData.isEmpty())
        {
            if(mPassthroughCallback != null)
            {
                mPassthroughCallback.error(PCConstants.PCIntentsNoDataToPrintError, -1, null, settings);
                return;
            }
        }

        /*
        Launch timeout mechanism
         */
        super.execute(settings);

        PrintTemplateString(settings);
    }

    private void PrintTemplateString(final PCPassthroughPrintSettings settings)
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PCConstants.PCComponentName,PCConstants.PCPassthroughService));

        byte[] passthroughBytes = null;
        try {
            // Convert template ZPL string to a UTF-8 encoded byte array, which will be sent as an extra with the intent
            passthroughBytes = settings.mPassthroughData.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            if(mPassthroughCallback != null)
            {
                mPassthroughCallback.error(e.getMessage(), -1, null, settings);
            }
            cleanAll();
            return;
        }

        intent.putExtra(PCConstants.PCPassthroughData, passthroughBytes); // Template ZPL as UTF-8 encoded byte array

        ResultReceiver receiver = buildIPCSafeReceiver(new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                // Stop timeout mechanism
                cleanAll();
                if (resultCode == 0) {
                    // Result code 0 indicates success
                    if(mPassthroughCallback != null)
                    {
                        mPassthroughCallback.success(settings);
                    }
                } else {
                    // Handle unsuccessful print
                    // Error message (null on successful print)
                    String errorMessage = resultData.getString(PCConstants.PCErrorMessage);
                    if(errorMessage == null)
                        errorMessage = PCConstants.getErrorMessage(resultCode);
                    if(mPassthroughCallback != null)
                    {
                        mPassthroughCallback.error(errorMessage, resultCode, resultData, settings);
                    }
                }
            }
        });
        intent.putExtra(PCConstants.PCTemplatePrintServiceResultReceiver, receiver);
        mContext.startService(intent);
    }

    @Override
    protected void onTimeOut(PCIntentsBaseSettings settings) {
        if(mPassthroughCallback != null)
        {
            mPassthroughCallback.timeOut((PCPassthroughPrintSettings)settings);
        }
    }
}
