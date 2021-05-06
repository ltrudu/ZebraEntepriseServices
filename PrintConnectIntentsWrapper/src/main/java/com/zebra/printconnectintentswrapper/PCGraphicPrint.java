package com.zebra.printconnectintentswrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.File;

public class PCGraphicPrint extends PCIntentsBase {

    /*
    An interface callback to be informed of the result
    of the print template intent
     */
    public interface onPrintGraphicResult
    {
        void success(PCGraphicPrintSettings settings);
        void error(String errorMessage, int resultCode, Bundle resultData, PCGraphicPrintSettings settings);
        void timeOut(PCGraphicPrintSettings settings);
    }

    private onPrintGraphicResult mGraphicPrintCallback = null;

    public PCGraphicPrint(Context aContext)
    {
        super(aContext);
    }

    public void execute(PCGraphicPrintSettings settings, onPrintGraphicResult callback)
    {
        if(callback == null)
        {
            Log.e(TAG, PCConstants.PCIntentsNoCallbackError);
            return;
        }

        mGraphicPrintCallback = callback;

        if(settings.mFileName.isEmpty())
        {
            if(mGraphicPrintCallback != null)
            {
                mGraphicPrintCallback.error(PCConstants.PCIntentsNoFileNameError, -1, null, settings);
            }
        }

        /*
        Launch timeout mechanism
         */

        super.execute(settings);

        PrintGraphicFileName(settings);
    }

    private void PrintGraphicFileName(final PCGraphicPrintSettings settings)
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PCConstants.PCComponentName,PCConstants.PCGraphicPrintService));
        intent.putExtra(PCConstants.PCGraphicPrintServiceFileName, settings.mFileName);

        // Image rotation in degrees (0, 90, 180, 270)
        intent.putExtra(PCConstants.PCGraphicPrintServiceRotation, settings.mRotation.toInt());
        // Image margin top in dots
        intent.putExtra(PCConstants.PCGraphicPrintServiceMarginTop, settings.mMarginTop);
        // Image margin left in dots
        intent.putExtra(PCConstants.PCGraphicPrintServiceMarginLeft, settings.mMarginLeft);
        // Image margin bottom in dots
        intent.putExtra(PCConstants.PCGraphicPrintServiceMarginBottom, settings.mMarginBottom);
        // Center image horizontally (default is false)
        intent.putExtra(PCConstants.PCGraphicPrintServiceCenter, settings.mCenter);

        // Image horizontal scaling amount in percentage points (5-300)
        int scaleX = Math.max(5, settings.mScaleX);
        scaleX = Math.min(scaleX, 300);
        intent.putExtra(PCConstants.PCGraphicPrintServiceScaleX, scaleX);

        // Image vertical scaling amount in percentage points (5-300)
        int scaleY = Math.max(5, settings.mScaleY);
        scaleY = Math.min(scaleY, 300);
        intent.putExtra(PCConstants.PCGraphicPrintServiceScaleY, scaleY);

        ResultReceiver receiver = buildIPCSafeReceiver(new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                // Stop timeout mechanism
                cleanAll();
                if (resultCode == 0) {
                    // Result code 0 indicates success
                    if(mGraphicPrintCallback != null)
                    {
                        mGraphicPrintCallback.success(settings);
                    }
                } else {
                    // Handle unsuccessful print
                    // Error message (null on successful print)
                    String errorMessage = resultData.getString(PCConstants.PCErrorMessage);
                    if(errorMessage == null)
                        errorMessage = PCConstants.getErrorMessage(resultCode);
                    if(mGraphicPrintCallback != null)
                    {
                        mGraphicPrintCallback.error(errorMessage, resultCode, resultData, settings);
                    }
                }
            }
        });
        intent.putExtra(PCConstants.PCTemplatePrintServiceResultReceiver, receiver);
        mContext.startService(intent);
    }

    @Override
    protected void onTimeOut(PCIntentsBaseSettings settings) {
        if(mGraphicPrintCallback != null)
        {
            mGraphicPrintCallback.timeOut((PCGraphicPrintSettings)settings);
        }
    }
}
