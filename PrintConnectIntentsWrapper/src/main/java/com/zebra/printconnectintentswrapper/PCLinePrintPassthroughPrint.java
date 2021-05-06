package com.zebra.printconnectintentswrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class PCLinePrintPassthroughPrint extends PCIntentsBase {

    /*
    An interface callback to be informed of the result
    of the print template intent
     */
    public interface onLinePrintPassthroughResult
    {
        void success(PCLinePrintPassthroughPrintSettings settings);
        void error(String errorMessage, int resultCode, Bundle resultData, PCLinePrintPassthroughPrintSettings settings);
        void timeOut(PCLinePrintPassthroughPrintSettings settings);
    }

    private onLinePrintPassthroughResult mLinePrintPassthroughCallback = null;

    public PCLinePrintPassthroughPrint(Context aContext)
    {
        super(aContext);
    }

    public void execute(PCLinePrintPassthroughPrintSettings settings, onLinePrintPassthroughResult callback)
    {
        if(callback == null)
        {
            Log.e(TAG, PCConstants.PCIntentsNoCallbackError);
            return;
        }

        mLinePrintPassthroughCallback = callback;

        /*
        if(settings.mLineToPrint.isEmpty())
        {
            if(mLinePrintPassthroughCallback != null)
            {
                mLinePrintPassthroughCallback.error(PCConstants.PCIntentsNoDataToPrintError, -1, null, settings);
                return;
            }
        }
        */


        /*
        Launch timeout mechanism
         */
        super.execute(settings);

        LinePrintPassthroughPrint(settings);
    }

    private void LinePrintPassthroughPrint(final PCLinePrintPassthroughPrintSettings settings)
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PCConstants.PCComponentName,PCConstants.PCLinePrintPassthroughService));

        byte[] linePrintPassthroughBytes = null;
        try {
            // Convert template ZPL string to a UTF-8 encoded byte array, which will be sent as an extra with the intent
            linePrintPassthroughBytes = settings.mLineToPrint.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            if(mLinePrintPassthroughCallback != null)
            {
                mLinePrintPassthroughCallback.error(e.getMessage(), -1, null, settings);
            }
            cleanAll();
            return;
        }

        intent.putExtra(PCConstants.PCLinePrintPassthroughData, linePrintPassthroughBytes); // Template ZPL as UTF-8 encoded byte array

        ResultReceiver receiver = buildIPCSafeReceiver(new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                // Stop timeout mechanism
                cleanAll();
                if (resultCode == 0) {
                    // Result code 0 indicates success
                    if(mLinePrintPassthroughCallback != null)
                    {
                        mLinePrintPassthroughCallback.success(settings);
                    }
                } else {
                    // Handle unsuccessful print
                    // Error message (null on successful print)
                    String errorMessage = resultData.getString(PCConstants.PCErrorMessage);
                    if(errorMessage == null)
                        errorMessage = PCConstants.getErrorMessage(resultCode);
                    if(mLinePrintPassthroughCallback != null)
                    {
                        mLinePrintPassthroughCallback.error(errorMessage, resultCode, resultData, settings);
                    }
                }
            }
        });
        intent.putExtra(PCConstants.PCTemplatePrintServiceResultReceiver, receiver);
        mContext.startService(intent);
    }

    @Override
    protected void onTimeOut(PCIntentsBaseSettings settings) {
        if(mLinePrintPassthroughCallback != null)
        {
            mLinePrintPassthroughCallback.timeOut((PCLinePrintPassthroughPrintSettings)settings);
        }
    }
}
