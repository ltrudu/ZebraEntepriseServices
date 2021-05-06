package com.zebra.printconnectintentswrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class PCTemplateFileNamePrint extends PCIntentsBase {

    /*
    An interface callback to be informed of the result
    of the print template intent
     */
    public interface onPrintFileNameResult
    {
        void success(PCTemplateFileNamePrintSettings settings);
        void error(String errorMessage, int resultCode, Bundle resultData, PCTemplateFileNamePrintSettings settings);
        void timeOut(PCTemplateFileNamePrintSettings settings);
    }

    private onPrintFileNameResult mPrintTemplateFileNameCallback = null;

    public PCTemplateFileNamePrint(Context aContext)
    {
        super(aContext);
    }

    public void execute(PCTemplateFileNamePrintSettings settings, onPrintFileNameResult callback)
    {
        if(callback == null)
        {
            Log.e(TAG, PCConstants.PCIntentsNoCallbackError);
            return;
        }

        mPrintTemplateFileNameCallback = callback;

        if(settings.mTemplateFileName.isEmpty())
        {
            if(mPrintTemplateFileNameCallback != null)
            {
                mPrintTemplateFileNameCallback.error(PCConstants.PCIntentsNoFileNameError, -1, null, settings);
            }
        }

        /*
        Launch timeout mechanism
         */
        super.execute(settings);

        /*
        Print file
         */
        switch (settings.mFileMode)
        {
            case PRINTCONNECTCONFIGFOLDER:
                Log.d(TAG, "Printing from printer memory:" + settings.mTemplateFileName);
                PrintTemplateFileNameFromPrintConnectAssetsFolder(settings);
                break;
            case FILE_SYSTEM:
                Log.d(TAG, "Printing from file system: " + settings.mTemplateFileName);
                PrintTemplateFileNameFromFileSystem(settings);
        }
    }

    private String getFileStringFromFileSystem(PCTemplateFileNamePrintSettings settings)
            throws IOException
    {
        String zplString = "";
        StringBuilder strb = new StringBuilder();
        BufferedReader bfr = new BufferedReader(new FileReader(settings.mTemplateFileName));
        String line = "";
        while ((line = bfr.readLine()) != null) {
            strb.append(line);
        }
        bfr.close();
        zplString = strb.toString();
        Log.d(TAG, "String read from file: \n" + zplString.toString());
        return zplString;
    }

    private void PrintTemplateFileNameFromPrintConnectAssetsFolder(final PCTemplateFileNamePrintSettings settings)
    {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PCConstants.PCComponentName,PCConstants.PCTemplatePrintService));
        intent.putExtra(PCConstants.PCTemplatePrintServiceTemplateFileName, settings.mTemplateFileName);
        if(settings.mVariableData != null && settings.mVariableData.size() > 0)
            intent.putExtra(PCConstants.PCTemplatePrintServiceVariableData, settings.mVariableData);
        ResultReceiver receiver = buildIPCSafeReceiver(new ResultReceiver(null) {
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                // Stop timeout mechanism
                cleanAll();
                if (resultCode == 0) {
                    // Result code 0 indicates success
                    if(mPrintTemplateFileNameCallback != null)
                    {
                        mPrintTemplateFileNameCallback.success(settings);
                    }
                } else {
                    // Handle unsuccessful print
                    // Error message (null on successful print)
                    String errorMessage = resultData.getString(PCConstants.PCErrorMessage);
                    if(errorMessage == null)
                        errorMessage = PCConstants.getErrorMessage(resultCode);
                    if(mPrintTemplateFileNameCallback != null)
                    {
                        mPrintTemplateFileNameCallback.error(errorMessage, resultCode, resultData, settings);
                    }
                }
            }
        });
        intent.putExtra(PCConstants.PCTemplatePrintServiceResultReceiver, receiver);
        mContext.startService(intent);
    }

    private void PrintTemplateFileNameFromFileSystem(final PCTemplateFileNamePrintSettings settings)
    {
        // Check if the file exists
        File myFile = new File(settings.mTemplateFileName);
        if (!myFile.exists())
        {
            if (this.mPrintTemplateFileNameCallback != null) {
                this.mPrintTemplateFileNameCallback.error("File not found", -1, null, settings);
            }
            cleanAll();
            return;
        }
        // Retrieve ZPL content as String
        String sZPLTemplateString = "";
        try
        {
            sZPLTemplateString = getFileStringFromFileSystem(settings);
        }
        catch (IOException e)
        {
            if (this.mPrintTemplateFileNameCallback != null) {
                this.mPrintTemplateFileNameCallback.error(e.getMessage(), -1, null, settings);
            }
            cleanAll();
            return;
        }
        if ((sZPLTemplateString == null) || (sZPLTemplateString.isEmpty()))
        {
            if (this.mPrintTemplateFileNameCallback != null) {
                this.mPrintTemplateFileNameCallback.error("Error: no zpl data.", -1, null, settings);
            }
            cleanAll();
            return;
        }
        // Convert the string to bytes array from the file content treated as an UTF-8 encoded String
        byte[] templateBytes = null;
        try
        {
            templateBytes = sZPLTemplateString.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            if (this.mPrintTemplateFileNameCallback != null) {
                this.mPrintTemplateFileNameCallback.error(e.getMessage(), -1, null, settings);
            }
            cleanAll();
            return;
        }

        // Create the PrintConnect Intent
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(PCConstants.PCComponentName, PCConstants.PCTemplatePrintWithContentService));
        // Set template data
        intent.putExtra(PCConstants.PCTemplatePrintWithContentServiceTemplateData, templateBytes);
        // Set variable data
        if ((settings.mVariableData != null) && (settings.mVariableData.size() > 0)) {
            intent.putExtra(PCConstants.PCTemplatePrintServiceVariableData, settings.mVariableData);
        }
        // Build result receiver to handle success and error
        ResultReceiver receiver = buildIPCSafeReceiver(new ResultReceiver(null)
        {
            protected void onReceiveResult(int resultCode, Bundle resultData)
            {
                PCTemplateFileNamePrint.this.cleanAll();
                if (resultCode == 0)
                {
                    if (PCTemplateFileNamePrint.this.mPrintTemplateFileNameCallback != null) {
                        PCTemplateFileNamePrint.this.mPrintTemplateFileNameCallback.success(settings);
                    }
                }
                else
                {
                    String errorMessage = resultData.getString(PCConstants.PCErrorMessage);
                    if(errorMessage == null)
                        errorMessage = PCConstants.getErrorMessage(resultCode);
                    if (PCTemplateFileNamePrint.this.mPrintTemplateFileNameCallback != null) {
                        PCTemplateFileNamePrint.this.mPrintTemplateFileNameCallback.error(errorMessage, resultCode, resultData, settings);
                    }
                }
            }
        });
        // Register receiver for success and error handling
        intent.putExtra(PCConstants.PCTemplatePrintServiceResultReceiver, receiver);
        // Send intent to PrintConnect
        this.mContext.startService(intent);
    }

    @Override
    protected void onTimeOut(PCIntentsBaseSettings settings) {
        if(mPrintTemplateFileNameCallback != null)
        {
            mPrintTemplateFileNameCallback.timeOut((PCTemplateFileNamePrintSettings)settings);
        }
    }
}
