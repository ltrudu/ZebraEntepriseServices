package com.zebra.browserintenturl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.zebra.enterpriseservices.R;
import com.zebra.printconnectintentswrapper.PCIntentsBaseSettings;
import com.zebra.printconnectintentswrapper.PCLinePrintPassthroughPrint;
import com.zebra.printconnectintentswrapper.PCLinePrintPassthroughPrintSettings;
import com.zebra.printconnectintentswrapper.PCPassthroughPrint;
import com.zebra.printconnectintentswrapper.PCPassthroughPrintSettings;
import com.zebra.printconnectintentswrapper.PCTemplateFileNamePrint;
import com.zebra.printconnectintentswrapper.PCTemplateFileNamePrintSettings;
import com.zebra.printconnectintentswrapper.PCTemplateStringPrint;
import com.zebra.printconnectintentswrapper.PCTemplateStringPrintSettings;
import com.zebra.printconnectintentswrapper.PCUnselectPrinter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class UrlPrintActivity extends Activity {

    private static String TAG = "PCUriPrint";

    private boolean verbose = false;
    private QUIT_MODE quit_mode = QUIT_MODE.FINISH_AFFINITY;
    private String componentName  ="";

    private enum QUIT_MODE
    {
        FINISH_AFFINITY,
        FINISH_AND_REMOVE_TASK,
        MOVE_TASK_TO_BACK,
        KILL_PROCESS,
        SYSTEM_EXIT,
        LAUNCH_INTENT;

        public static QUIT_MODE getFromString(String quit_mode)
        {
            switch(quit_mode)
            {
                case "FINISH_AFFINITY":
                    return FINISH_AFFINITY;
                case "FINISH_AND_REMOVE_TASK":
                    return FINISH_AND_REMOVE_TASK;
                case "MOVE_TASK_TO_BACK":
                    return MOVE_TASK_TO_BACK;
                case "KILL_PROCESS":
                    return KILL_PROCESS;
                case "SYSTEM_EXIT":
                    return SYSTEM_EXIT;
                case "LAUNCH_INTENT":
                    return LAUNCH_INTENT;
            }
            return FINISH_AFFINITY;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_url_print);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = getIntent();
        if(Intent.ACTION_VIEW.equals(intent.getAction())){
            Uri uri = intent.getData();

            String needVerboseMessages = uri.getQueryParameter("verbose");
            if(needVerboseMessages == null)
            {
                needVerboseMessages = intent.getExtras().getString("verbose", "false");
            }

            verbose = needVerboseMessages.equalsIgnoreCase("true");

            String s_quit_mode = uri.getQueryParameter("quitmode");
            if(s_quit_mode == null)
            {
                s_quit_mode = intent.getExtras().getString("quitmode", "FINISH_AFFINITY");
            }

            quit_mode = QUIT_MODE.getFromString(s_quit_mode);

            if(quit_mode == QUIT_MODE.LAUNCH_INTENT)
            {
                componentName = uri.getQueryParameter("component");
                if(componentName == null)
                {
                    componentName = intent.getExtras().getString("component");
                }
                if(componentName == null || componentName.isEmpty())
                {
                    showMessage("Error !!\nComponent name is empty.\nSwitching to default mode: FINISH_AFFINITY.", false, 0);
                    quit_mode = QUIT_MODE.FINISH_AFFINITY;
                }
            }

            String command = uri.getHost();

            if(command == null)
            {
                uri.getQueryParameter("command");
            }
            if(command == null)
            {
                command = intent.getExtras().getString("command");
            }

            if(command.equalsIgnoreCase("printzpl"))
            {
                printZPLTemplate(intent);
            }
            else if(command.equalsIgnoreCase("printtemplatefile"))
            {
                printTemplateFile(intent);
            }
            else if(command.equalsIgnoreCase("printsingleline"))
            {
                printSingleLine(intent);
            }
            else if(command.equalsIgnoreCase("passthrough"))
            {
                passtroughPrint(intent);
            }
            else if(command.equalsIgnoreCase("unselect"))
            {
                unselectPrinter(intent);
            }
            else
            {
                verbose = true;
                // Display an error message if nothing was found in the url-intent
                showMessage("Unable to find the requested command.\nCommand=" + command, true, 5000);
            }
        }
    }

    private void unselectPrinter(Intent intent) {
        PCUnselectPrinter linePrint = new PCUnselectPrinter(this);

        PCIntentsBaseSettings settings = new PCIntentsBaseSettings()
        {{
            mCommandId = "unselectPrinter";
        }};

        linePrint.execute(settings, new PCUnselectPrinter.onUnselectPrinterResult() {
            @Override
            public void success(PCIntentsBaseSettings settings) {
                showMesageAndQuit("Unselect printer successfull.");
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCIntentsBaseSettings settings) {
                showMesageAndQuit("Error while trying to unselect printer" + "\n" + errorMessage);
            }

            @Override
            public void timeOut(PCIntentsBaseSettings settings) {
                showMesageAndQuit("Print error: Timeout while trying to unselect printer.");
            }
        });
    }

    private void passtroughPrint(Intent intent) {
        Uri uri = intent.getData();
        String decodedPassthroughData = null;

        if(uri.getScheme().equalsIgnoreCase("base64encoded")) {
            // Check if the user is specifying a standard charset
            Charset stringCharSet = StandardCharsets.US_ASCII;

            String isUTFStringParameter = uri.getQueryParameter("standardCharsets");
            if(isUTFStringParameter == null)
            {
                isUTFStringParameter = intent.getExtras().getString("standardCharsets");
            }

            if(isUTFStringParameter != null)
            {
                if(isUTFStringParameter.equalsIgnoreCase("UTF_8"))
                {
                    stringCharSet = StandardCharsets.UTF_8;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16"))
                {
                    stringCharSet = StandardCharsets.UTF_16;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16BE"))
                {
                    stringCharSet = StandardCharsets.UTF_16BE;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16LE"))
                {
                    stringCharSet = StandardCharsets.UTF_16LE;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("ISO_8859_1"))
                {
                    stringCharSet = StandardCharsets.ISO_8859_1;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("US_ASCII"))
                {
                    stringCharSet = StandardCharsets.US_ASCII;
                }
            }

            // We are using the base64 encoded version of the url scheme
            String base64EncodedPassthroughData = uri.getQueryParameter("data");
            if (base64EncodedPassthroughData == null) {
                base64EncodedPassthroughData = intent.getExtras().getString("data");
            }

            // Decode line to print if found
            if (base64EncodedPassthroughData != null) {
                byte[] fileNameAsByteArray = Base64.decode(base64EncodedPassthroughData, Base64.DEFAULT);
                if (fileNameAsByteArray == null) {
                    showMesageAndQuit("Print error: Could not decode passthrough data byte array.");
                }

                decodedPassthroughData = new String(fileNameAsByteArray, stringCharSet);

                if(decodedPassthroughData == null)
                {
                    showMesageAndQuit("Print error: Could not interpret decoded passthrough byte array to String.");
                }
            }
            else
            {
                showMesageAndQuit("Print error: No passthrough data to print found.");
            }
        }
        else {
            // We are directly passing the string without base64 encoding
            decodedPassthroughData = uri.getQueryParameter("data");
            if (decodedPassthroughData == null) {
                decodedPassthroughData = intent.getExtras().getString("data");
            }
        }

        if(decodedPassthroughData == null)
        {
            showMesageAndQuit( "Print error: No passthrough data to print found.");
        }
        else {

            showMessage("Printing passthrough data", false, 0);
            passthroughDataPrint(decodedPassthroughData);
        }
    }

    private void passthroughDataPrint(final String passthroughData) {
        PCPassthroughPrint linePrint = new PCPassthroughPrint(this);

        PCPassthroughPrintSettings settings = new PCPassthroughPrintSettings()
        {{
            mPassthroughData = passthroughData;
        }};

        linePrint.execute(settings, new PCPassthroughPrint.onPassthroughResult() {
            @Override
            public void success(PCPassthroughPrintSettings settings) {
                showMesageAndQuit("Print passthrough succeeded");
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCPassthroughPrintSettings settings) {
                showMesageAndQuit("Error while trying to print passthrough\n" + errorMessage);
            }

            @Override
            public void timeOut(PCPassthroughPrintSettings settings) {
                showMesageAndQuit("Print error: Timeout while trying to print passthrough");
            }
        });
    }

    private void printSingleLine(Intent intent) {
        Uri uri = intent.getData();
        String decodedLineToPrint = null;

        if(uri.getScheme().equalsIgnoreCase("base64encoded")) {
            // Check if the user is specifying a standard charset
            Charset stringCharSet = StandardCharsets.US_ASCII;

            String isUTFStringParameter = uri.getQueryParameter("standardCharsets");
            if(isUTFStringParameter == null)
            {
                isUTFStringParameter = intent.getExtras().getString("standardCharsets");
            }

            if(isUTFStringParameter != null)
            {
                if(isUTFStringParameter.equalsIgnoreCase("UTF_8"))
                {
                    stringCharSet = StandardCharsets.UTF_8;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16"))
                {
                    stringCharSet = StandardCharsets.UTF_16;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16BE"))
                {
                    stringCharSet = StandardCharsets.UTF_16BE;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16LE"))
                {
                    stringCharSet = StandardCharsets.UTF_16LE;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("ISO_8859_1"))
                {
                    stringCharSet = StandardCharsets.ISO_8859_1;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("US_ASCII"))
                {
                    stringCharSet = StandardCharsets.US_ASCII;
                }
            }

            // We are using the base64 encoded version of the url scheme
            String base64EncodedLineToPrint = uri.getQueryParameter("text");
            if (base64EncodedLineToPrint == null) {
                base64EncodedLineToPrint = intent.getExtras().getString("text");
            }

            // Decode line to print if found
            if (base64EncodedLineToPrint != null) {
                byte[] fileNameAsByteArray = Base64.decode(base64EncodedLineToPrint, Base64.DEFAULT);
                if (fileNameAsByteArray == null) {
                    showMesageAndQuit("Print error: Could not decode text data byte array.");
                }

                decodedLineToPrint = new String(fileNameAsByteArray, stringCharSet);

                if(decodedLineToPrint == null)
                {
                    showMesageAndQuit("Print error: Could not interpret decoded text byte array to String.");
                }
            }
        }
        else
        {
            // We are directly passing the string without base64 encoding
            decodedLineToPrint = uri.getQueryParameter("text");
            if(decodedLineToPrint == null)
            {
                decodedLineToPrint = intent.getExtras().getString("text");
            }
        }


        if(decodedLineToPrint == null)
        {
            showMesageAndQuit( "Print error: " + "No text to print found.");
        }
        else {

            showMessage("Printing line", false, 0);
            linePrintPassthrough(decodedLineToPrint);
        }

    }

    private void linePrintPassthrough(final String lineToPrint) {
        PCLinePrintPassthroughPrint linePrint = new PCLinePrintPassthroughPrint(this);

        PCLinePrintPassthroughPrintSettings settings = new PCLinePrintPassthroughPrintSettings()
        {{
            mLineToPrint = lineToPrint;
        }};

        linePrint.execute(settings, new PCLinePrintPassthroughPrint.onLinePrintPassthroughResult() {
            @Override
            public void success(PCLinePrintPassthroughPrintSettings settings) {
                showMesageAndQuit("Line print succeeded");
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCLinePrintPassthroughPrintSettings settings) {
                showMesageAndQuit("Error while trying to print line\n" + errorMessage);
            }

            @Override
            public void timeOut(PCLinePrintPassthroughPrintSettings settings) {
                showMesageAndQuit("Print error: Timeout while trying to print line");
            }
        });
    }

    private void printTemplateFile(Intent intent) {
        Uri uri = intent.getData();
        String decodedFileName = null;
        String decodedSemiColumnSeparatedVariables = null;
        HashMap<String, String> variableData = null;

        if(uri.getScheme().equalsIgnoreCase("base64encoded")) {
            // Check if the user is specifying a standard charset
            Charset stringCharSet = StandardCharsets.US_ASCII;

            String isUTFStringParameter = uri.getQueryParameter("standardCharsets");
            if(isUTFStringParameter == null)
            {
                isUTFStringParameter = intent.getExtras().getString("standardCharsets");
            }

            if(isUTFStringParameter != null)
            {
                if(isUTFStringParameter.equalsIgnoreCase("UTF_8"))
                {
                    stringCharSet = StandardCharsets.UTF_8;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16"))
                {
                    stringCharSet = StandardCharsets.UTF_16;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16BE"))
                {
                    stringCharSet = StandardCharsets.UTF_16BE;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16LE"))
                {
                    stringCharSet = StandardCharsets.UTF_16LE;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("ISO_8859_1"))
                {
                    stringCharSet = StandardCharsets.ISO_8859_1;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("US_ASCII"))
                {
                    stringCharSet = StandardCharsets.US_ASCII;
                }
            }

            // We are using the base64 encoded version of the url scheme
            String base64EncodedFileName = uri.getQueryParameter("filename");
            if (base64EncodedFileName == null) {
                base64EncodedFileName = intent.getExtras().getString("filename");
            }

            String base64SemiColumnSeparatedVariableData = uri.getQueryParameter("variables");
            if (base64SemiColumnSeparatedVariableData == null) {
                base64SemiColumnSeparatedVariableData = intent.getExtras().getString("variables");
            }

            // Decode fileName if found
            if (base64EncodedFileName != null) {
                byte[] fileNameAsByteArray = Base64.decode(base64EncodedFileName, Base64.DEFAULT);
                if (fileNameAsByteArray == null) {
                    showMesageAndQuit("Print error: Could not decode filename data byte array.");
                }

                decodedFileName = new String(fileNameAsByteArray, stringCharSet);

                if(decodedFileName == null)
                {
                    showMesageAndQuit("Print error: Could not interpret decoded fileName byte array to String.");
                }
            }

            // Decode variable data if found
            if (base64SemiColumnSeparatedVariableData != null) {
                byte[] dataAsByteArray = Base64.decode(base64SemiColumnSeparatedVariableData, Base64.DEFAULT);
                if (dataAsByteArray == null) {
                    showMesageAndQuit("Print error: Could not decode variable data byte array.");
                }
                decodedSemiColumnSeparatedVariables = new String(dataAsByteArray, stringCharSet);
                if(decodedSemiColumnSeparatedVariables == null)
                {
                    showMesageAndQuit("Print error: Could not interpret decoded variable byte array to String.");
                }
            }
        }
        else
        {
            // We are directly passing the string without base64 encoding
            decodedFileName = uri.getQueryParameter("filename");
            if(decodedFileName == null)
            {
                decodedFileName = intent.getExtras().getString("filename");
            }

            decodedSemiColumnSeparatedVariables = uri.getQueryParameter("variables");
            if(decodedSemiColumnSeparatedVariables == null)
            {
                decodedSemiColumnSeparatedVariables = intent.getExtras().getString("variables");
            }
        }


        if(decodedFileName == null)
        {
            showMesageAndQuit( "Print error: " + "No filename found.");
        }

        if(decodedSemiColumnSeparatedVariables != null)
        {
            String[] splittedArray = decodedSemiColumnSeparatedVariables.split(":");
            if (splittedArray.length > 1) {
                variableData = new HashMap<String, String>();
                variableData.put("%BLA%", "bar");
                for (int i = 0; i < splittedArray.length; i = i + 2) {
                    variableData.put(splittedArray[i], splittedArray[i + 1]);
                }
            }
            else
            {
                showMesageAndQuit( "Print error: A variable data extra was found but after decoding, no key-value pair was found. Length=" + splittedArray.length);
            }
        }

        if(decodedFileName == null)
        {
            showMesageAndQuit( "Print error: no template filename provided");
        }
        else {
            showMessage("Printing", false, 0);
            templatePrintWithFileName(decodedFileName, variableData);
        }
    }

    private void templatePrintWithFileName(final String fileName, final HashMap<String,String> variableData) {
        PCTemplateFileNamePrint templateFileNamePrint = new PCTemplateFileNamePrint(this);

        PCTemplateFileNamePrintSettings settings = new PCTemplateFileNamePrintSettings()
        {{
            mTemplateFileName = fileName;
            mVariableData = variableData;
        }};

        templateFileNamePrint.execute(settings, new PCTemplateFileNamePrint.onPrintFileNameResult() {
            @Override
            public void success(PCTemplateFileNamePrintSettings settings) {
                showMesageAndQuit("Template print file name succeeded : " + settings.mTemplateFileName);
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCTemplateFileNamePrintSettings settings) {
                showMesageAndQuit("Error while trying to print filename :" + settings.mTemplateFileName + "\n" + errorMessage);
            }

            @Override
            public void timeOut(PCTemplateFileNamePrintSettings settings) {
                showMesageAndQuit("Print error: Timeout while trying to print filename: " + settings.mTemplateFileName);
            }
        });
    }

    private void printZPLTemplate(Intent intent) {
        Uri uri = intent.getData();
        String decodedTemplate = null;
        String decodedSemiColumnSeparatedVariables = null;
        HashMap<String, String> variableData = null;

        if(uri.getScheme().equalsIgnoreCase("base64encoded")) {
            // Check if the user is specifying a standard charset
            Charset stringCharSet = StandardCharsets.US_ASCII;

            String isUTFStringParameter = uri.getQueryParameter("standardCharsets");
            if(isUTFStringParameter == null)
            {
                isUTFStringParameter = intent.getExtras().getString("standardCharsets");
            }

            if(isUTFStringParameter != null)
            {
                if(isUTFStringParameter.equalsIgnoreCase("UTF_8"))
                {
                    stringCharSet = StandardCharsets.UTF_8;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16"))
                {
                    stringCharSet = StandardCharsets.UTF_16;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16BE"))
                {
                    stringCharSet = StandardCharsets.UTF_16BE;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("UTF_16LE"))
                {
                    stringCharSet = StandardCharsets.UTF_16LE;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("ISO_8859_1"))
                {
                    stringCharSet = StandardCharsets.ISO_8859_1;
                }
                else if(isUTFStringParameter.equalsIgnoreCase("US_ASCII"))
                {
                    stringCharSet = StandardCharsets.US_ASCII;
                }
            }

            // We are using the base64 encoded version of the url scheme
            String base64EncodedData = uri.getQueryParameter("template");
            if (base64EncodedData == null) {
                base64EncodedData = intent.getExtras().getString("template");
            }

            String base64SemiColumnSeparatedVariableData = uri.getQueryParameter("variables");
            if (base64SemiColumnSeparatedVariableData == null) {
                base64SemiColumnSeparatedVariableData = intent.getExtras().getString("variables");
            }

            // Decode template if found
            if (base64EncodedData != null) {
                byte[] templateAsByteArray = Base64.decode(base64EncodedData, Base64.DEFAULT);
                if (templateAsByteArray == null) {
                    showMesageAndQuit("Print error: Could not decode template data byte array.");
                }

                decodedTemplate = new String(templateAsByteArray, stringCharSet);

                if(decodedTemplate == null)
                {
                    showMesageAndQuit("Print error: Could not interpret decoded template byte array to String.");
                }
            }

            // Decode variable data if found
            if (base64SemiColumnSeparatedVariableData != null) {
                byte[] dataAsByteArray = Base64.decode(base64SemiColumnSeparatedVariableData, Base64.DEFAULT);
                if (dataAsByteArray == null) {
                    showMesageAndQuit("Print error: Could not decode variable data byte array.");
                }
                decodedSemiColumnSeparatedVariables = new String(dataAsByteArray, stringCharSet);
                if(decodedSemiColumnSeparatedVariables == null)
                {
                    showMesageAndQuit("Print error: Could not interpret decoded variable byte array to String.");
                }
            }
        }
        else
        {
            // We are directly passing the string without base64 encoding
            decodedTemplate = uri.getQueryParameter("template");
            if(decodedTemplate == null)
            {
                decodedTemplate = intent.getExtras().getString("template");
            }

            decodedSemiColumnSeparatedVariables = uri.getQueryParameter("variables");
            if(decodedSemiColumnSeparatedVariables == null)
            {
                decodedSemiColumnSeparatedVariables = intent.getExtras().getString("variables");
            }
        }

        if(decodedSemiColumnSeparatedVariables != null)
        {
            String[] splittedArray = decodedSemiColumnSeparatedVariables.split(":");
            if (splittedArray.length > 1) {
                variableData = new HashMap<String, String>();
                for (int i = 0; i < splittedArray.length; i = i + 2) {
                    variableData.put(splittedArray[i], splittedArray[i + 1]);
                }
            }
            else
            {
                showMesageAndQuit( "Print error: A variable data extra was found but after decoding, no key-value pair was found. Length=" + splittedArray.length);
            }
        }

        if(decodedTemplate == null)
        {
            showMesageAndQuit( "Print error: " + "No template data found.");
        }
        else
        {
            showMessage("Printing", false, 0);
            templatePrintWithContent(decodedTemplate, variableData);
        }
    }

    private void showMesageAndQuit(String message)
    {
        showMessage(message, true, 100);
    }

    private void showMessage(final String message, boolean quit, int sleepTime)
    {
        Log.d(TAG, message);
        if(verbose)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(UrlPrintActivity.this, message, Toast.LENGTH_LONG).show();
                }
            });
        }

        if(sleepTime > 0)
        {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if(quit)
        {
            switch(quit_mode)
            {
                case FINISH_AND_REMOVE_TASK:
                    finishAndRemoveTask();
                    break;
                case KILL_PROCESS:
                    android.os.Process.killProcess(android.os.Process.myPid());
                    break;
                case MOVE_TASK_TO_BACK:
                    moveTaskToBack(true);
                    break;
                case SYSTEM_EXIT:
                    System.exit(0);
                    break;
                case LAUNCH_INTENT:
                    Intent intent = getPackageManager().getLaunchIntentForPackage(componentName);
                    if(intent == null) {
                        try {
                            // if play store installed, open play store, else open browser
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + componentName));
                        } catch (Exception e) {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + componentName));
                        }
                    }
                    startActivity(intent);
                    break;
                case FINISH_AFFINITY:
                default:
                    finishAffinity();
            }
        }
    }

    private void templatePrintWithContent(final String zplToPrint, final HashMap<String, String> variableData) {

        PCTemplateStringPrint templateStringPrint = new PCTemplateStringPrint(this);

        PCTemplateStringPrintSettings settings = new PCTemplateStringPrintSettings()
        {{
            mZPLTemplateString = zplToPrint;
            mVariableData = variableData;
        }};

        templateStringPrint.execute(settings, new PCTemplateStringPrint.onPrintTemplateStringResult() {
            @Override
            public void success(PCTemplateStringPrintSettings settings) {
                showMesageAndQuit("Template print string succeeded");
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCTemplateStringPrintSettings settings) {
                showMesageAndQuit("Error while trying to template string print: \n" + errorMessage);
            }

            @Override
            public void timeOut(PCTemplateStringPrintSettings settings) {
                showMesageAndQuit("Print error: Timeout while trying to print.");
            }
        });
    }

}
