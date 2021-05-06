package com.zebra.printconnectintentswrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.UnsupportedEncodingException;

public class PCTemplateStringPrint extends PCIntentsBase {

    /*
    An interface callback to be informed of the result
    of the print template intent
     */
    public interface onPrintTemplateStringResult
    {
        void success(PCTemplateStringPrintSettings settings);
        void error(String errorMessage, int resultCode, Bundle resultData, PCTemplateStringPrintSettings settings);
        void timeOut(PCTemplateStringPrintSettings settings);
    }

    private onPrintTemplateStringResult mPrintTemplateStringCallback = null;

    public PCTemplateStringPrint(Context aContext)
    {
        super(aContext);
    }

    public void execute(PCTemplateStringPrintSettings settings, onPrintTemplateStringResult callback)
    {
        if(callback == null)
        {
            Log.e(TAG, PCConstants.PCIntentsNoCallbackError);
            return;
        }

        mPrintTemplateStringCallback = callback;

        if(settings.mZPLTemplateString.isEmpty())
        {
            if(mPrintTemplateStringCallback != null)
            {
                mPrintTemplateStringCallback.error(PCConstants.PCIntentsNoZPLDataError, -1, null, settings);
            }
        }

        /*
        Launch timeout mechanism
         */
        super.execute(settings);

        PrintTemplateString(settings);
    }

    private void PrintTemplateString(final PCTemplateStringPrintSettings settings)
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PCConstants.PCComponentName,PCConstants.PCTemplatePrintWithContentService));

        byte[] templateBytes = null;
        try {
            // Convert template ZPL string to a UTF-8 encoded byte array, which will be sent as an extra with the intent
            templateBytes = settings.mZPLTemplateString.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            if(mPrintTemplateStringCallback != null)
            {
                mPrintTemplateStringCallback.error(e.getMessage(), -1, null, settings);
            }
            cleanAll();
            return;
        }

        intent.putExtra(PCConstants.PCTemplatePrintWithContentServiceTemplateData, templateBytes); // Template ZPL as UTF-8 encoded byte array

        if(settings.mVariableData != null && settings.mVariableData.size() > 0)
            intent.putExtra(PCConstants.PCTemplatePrintServiceVariableData, settings.mVariableData);
        ResultReceiver receiver = buildIPCSafeReceiver(new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                // Stop timeout mechanism
                cleanAll();
                if (resultCode == 0) {
                    // Result code 0 indicates success
                    if(mPrintTemplateStringCallback != null)
                    {
                        mPrintTemplateStringCallback.success(settings);
                    }
                } else {
                    // Handle unsuccessful print
                    // Error message (null on successful print)
                    String errorMessage = resultData.getString(PCConstants.PCErrorMessage);
                    if(errorMessage == null)
                        errorMessage = PCConstants.getErrorMessage(resultCode);
                    if(mPrintTemplateStringCallback != null)
                    {
                        mPrintTemplateStringCallback.error(errorMessage, resultCode, resultData, settings);
                    }
                }
            }
        });
        intent.putExtra(PCConstants.PCTemplatePrintServiceResultReceiver, receiver);
        mContext.startService(intent);
    }

    @Override
    protected void onTimeOut(PCIntentsBaseSettings settings) {
        if(mPrintTemplateStringCallback != null)
        {
            mPrintTemplateStringCallback.timeOut((PCTemplateStringPrintSettings)settings);
        }
    }
}
