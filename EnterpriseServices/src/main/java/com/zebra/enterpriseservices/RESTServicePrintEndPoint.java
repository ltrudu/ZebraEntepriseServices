package com.zebra.enterpriseservices;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Base64;
import android.util.Pair;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import fi.iki.elonen.NanoHTTPD;

public class RESTServicePrintEndPoint implements RESTServiceInterface {
    private static RESTServiceWebServer.EJobStatus mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
    private static CountDownLatch mJobDoneLatch = null;
    private static String mJobReturnMessage = "";
    private Context mContext = null;

    public RESTServicePrintEndPoint(Context aContext)
    {
        mContext = aContext;
    }

    @Override
    public Pair<RESTServiceWebServer.EJobStatus, String> processSession(NanoHTTPD.IHTTPSession session)
    {
        Map<String, List<String>> paramsList = session.getParameters();
        List<String> commandList = paramsList.get("command");

        if(commandList == null || commandList.size() == 0) {
            LogHelper.logE("Print REST Endpoint error::processSession: Command not found in params : " + paramsList.toString());
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Print REST Endpoint error: Command not found in params : " + paramsList.toString());
        }
        String command = commandList.get(0);
        switch(command)
        {
            case "connect":
                LogHelper.logD("Print REST Endpoint::processSession: processing command: connect");
                return connect(paramsList);
            case "printzpl":
                LogHelper.logD("Print REST Endpoint::processSession: processing command: printzpl");
                return printZPLTemplate(paramsList);
            case "printtemplatefile":
                LogHelper.logD("Print REST Endpoint::processSession: processing command: printtemplatefile");
                return printTemplateFile(paramsList);
            case "printsingleline":
                LogHelper.logD("Print REST Endpoint::processSession: processing command: printsingleline");
                return printSingleLine(paramsList);
            case "passthrough":
                LogHelper.logD("Print REST Endpoint::processSession: processing command: passthrough");
                return passthroughPrint(paramsList);
            case "unselect":
                LogHelper.logD("Print REST Endpoint::processSession: processing command: unselect");
                return unselect();
        }
        LogHelper.logE("Print REST Endpoint::processSession: Unsupported command : " + command);
        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Print error::processSession: Unsupported command : " + command);
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> connect(Map<String, List<String>> paramsList) {

        String ethernetMacAddress = null;
        List<String> ethernetMacAddressList = paramsList.get("ethernetMAC");
        if(ethernetMacAddressList != null && ethernetMacAddressList.size() > 0)
            ethernetMacAddress = ethernetMacAddressList.get(0);

        String wifiMacAddress = null;
        List<String> wifiMacAddressList = paramsList.get("wifiMAC");
        if(wifiMacAddressList != null && wifiMacAddressList.size() > 0)
            wifiMacAddress = wifiMacAddressList.get(0);

        String bluetoothMacAddress = null;
        List<String> bluetoothMacAddressList = paramsList.get("bluetoothMAC");
        if(bluetoothMacAddressList != null && bluetoothMacAddressList.size() > 0)
            bluetoothMacAddress = bluetoothMacAddressList.get(0);

        LogHelper.logD("Print REST Endpoint::connect: Detected Mac Addresses: ");
        LogHelper.logD("Print REST Endpoint::connect: Ethernet Mac: " + ethernetMacAddress);
        LogHelper.logD("Print REST Endpoint::connect: Wifi Mac: " + wifiMacAddress);
        LogHelper.logD("Print REST Endpoint::connect: Bluetooth Mac: " + bluetoothMacAddress);

        if((ethernetMacAddress != null && ethernetMacAddress.isEmpty() == false)
                || (wifiMacAddress != null && wifiMacAddress.isEmpty() == false)
                || (bluetoothMacAddress != null && bluetoothMacAddress.isEmpty() == false))
        {
            if(ethernetMacAddress == null || ethernetMacAddress.isEmpty())
            {
                ethernetMacAddress = "000000000000";
            }
            if(wifiMacAddress == null || wifiMacAddress.isEmpty())
            {
                wifiMacAddress = "000000000000";
            }
            if(bluetoothMacAddress == null || bluetoothMacAddress.isEmpty())
            {
                bluetoothMacAddress = "000000000000";
            }
            Intent simulateNFCIntent = new Intent();
            simulateNFCIntent.setAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            Uri connectionURI = Uri.parse("http://www.zebra.com/apps/r/nfc?mE="+ ethernetMacAddress + "&mW=" + wifiMacAddress + "&mB=" + bluetoothMacAddress + "&c=XXXX-XXXXXXX-XX&s=XXXXXXXXXXXXXX&v=0");
            LogHelper.logD("Print REST Endpoint::connect: Simulating NFC Tag with URI: " + connectionURI.toString());
            simulateNFCIntent.setData(connectionURI);
            Parcelable ndefMessages[] = new Parcelable[1];
            NdefRecord records[] = new NdefRecord[1];
            String payloadString = "\u0001zebra.com/apps/r/nfc?mE="+ ethernetMacAddress + "&mW=" + wifiMacAddress + "&mB=" + bluetoothMacAddress + "&c=XXXX-XXXXXXX-XX&s=XXXXXXXXXXXXXX&v=0";
            LogHelper.logD("Print REST Endpoint::connect: Simulating NFC Tag with payload: " + payloadString);
            byte[] payload = payloadString.getBytes();
            byte[] type = NdefRecord.RTD_URI;
            byte[] id = new byte[1];
            records[0] = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, type, id, payload);
            ndefMessages[0] = new NdefMessage(records);
            simulateNFCIntent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, ndefMessages);
            mContext.startActivity(simulateNFCIntent);
        }
        else
        {
            LogHelper.logE("Print REST Endpoint::connect: Error, at least one MAC address should be provided with the parameters: ethernetMAC, wifiMAC or bluetoothMAC (case sensitive).");
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Connect to printer: at least one MAC address should be provided with the parameters: ethernetMAC, wifiMAC or bluetoothMAC (case sensitive).");
        }
        LogHelper.logD("Print REST Endpoint::connect: Sending cnnect printer intent: Succeeded.");
        mJobReturnMessage = "Sending cnnect printer intent: Succeeded.";
        mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
        return new Pair<>(mJobStatus, mJobReturnMessage);
    }
    private Pair<RESTServiceWebServer.EJobStatus, String> printZPLTemplate(Map<String, List<String>> paramsList) {
        LogHelper.logD("Print REST Endpoint::printZPLTemplate");
        String template = null;
        List<String> templateList = paramsList.get("template");
        if(templateList != null && templateList.size() > 0)
            template = templateList.get(0);
        LogHelper.logD("Print REST Endpoint::printZPLTemplate:template = " + template);

        String variables = null;
        List<String> variablesList = paramsList.get("variables");
        if(variablesList != null && variablesList.size() > 0)
            variables = variablesList.get(0);
        LogHelper.logD("Print REST Endpoint::printZPLTemplate:variables = " + variables);

        String encoding = null;
        List<String> encodingList = paramsList.get("encoding");
        if(encodingList != null && encodingList.size() > 0)
            encoding = encodingList.get(0);
        LogHelper.logD("Print REST Endpoint::printZPLTemplate:encoding = " + encoding);


        HashMap<String, String> variableData = null;
        if(template != null && template.isEmpty() == false)
        {
            String decodedTemplate = null;
            String decodedSemiColumnSeparatedVariables = null;

            if(encoding != null && encoding.equalsIgnoreCase("base64encoded")) {
                LogHelper.logD("Print REST Endpoint::printZPLTemplate: Template is base64encoded");
                // Check if the user is specifying a standard charset
                Charset stringCharSet = StandardCharsets.UTF_8;

                String standardCharsets = null;
                List<String> standardCharsetsList = paramsList.get("standardCharsets");
                if(standardCharsetsList.size() > 0) {
                    standardCharsets = standardCharsetsList.get(0);
                    LogHelper.logD("Print REST Endpoint::printZPLTemplate: Using custom charset: " + standardCharsets);
                }

                if(standardCharsets == null)
                {
                    LogHelper.logD("Print REST Endpoint::printZPLTemplate: No charset specified, using standard UTF_8");
                    // Default value
                    standardCharsets = "UTF_8";
                }

                if(standardCharsets != null)
                {
                    if(standardCharsets.equalsIgnoreCase("UTF_8"))
                    {
                        stringCharSet = StandardCharsets.UTF_8;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16"))
                    {
                        stringCharSet = StandardCharsets.UTF_16;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16BE"))
                    {
                        stringCharSet = StandardCharsets.UTF_16BE;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16LE"))
                    {
                        stringCharSet = StandardCharsets.UTF_16LE;
                    }
                    else if(standardCharsets.equalsIgnoreCase("ISO_8859_1"))
                    {
                        stringCharSet = StandardCharsets.ISO_8859_1;
                    }
                    else if(standardCharsets.equalsIgnoreCase("US_ASCII"))
                    {
                        stringCharSet = StandardCharsets.US_ASCII;
                    }
                }

                // We are using the base64 encoded version of the url scheme
                String base64EncodedData = template;
                String base64SemiColumnSeparatedVariableData = variables;

                // Decode template if found
                if (base64EncodedData != null) {
                    byte[] templateAsByteArray = null;
                    try
                    {
                        templateAsByteArray = Base64.decode(base64EncodedData, Base64.DEFAULT);
                    }
                    catch(Exception e)
                    {
                        LogHelper.logE("Print REST Endpoint::printZPLTemplate: Base64Decoding Error: " + e.getMessage());
                        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Base64Decoding Error: " + e.getMessage());
                    }
                    if (templateAsByteArray == null) {
                        LogHelper.logE("Print REST Endpoint::printZPLTemplate: Could not decode template data byte array.");
                        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: Could not decode template data byte array.");
                    }

                    decodedTemplate = new String(templateAsByteArray, stringCharSet);

                    if(decodedTemplate == null)
                    {
                        LogHelper.logE("Print REST Endpoint::printZPLTemplate: Could not interpret decoded template byte array to String.");
                        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: Could not interpret decoded template byte array to String.");
                    }
                    else
                    {
                        LogHelper.logD("Print REST Endpoint::printZPLTemplate: Base64Decoding Decoded template: " + decodedTemplate);
                    }
                }

                // Decode variable data if found
                if (base64SemiColumnSeparatedVariableData != null) {
                    LogHelper.logD("Print REST Endpoint::printZPLTemplate: Base64Decoding variable data.");
                    byte[] dataAsByteArray = Base64.decode(base64SemiColumnSeparatedVariableData, Base64.DEFAULT);
                    if (dataAsByteArray == null) {
                        LogHelper.logE("Print REST Endpoint::printZPLTemplate: Could not decode variable data byte array.");
                        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: Could not decode variable data byte array.");
                    }
                    decodedSemiColumnSeparatedVariables = new String(dataAsByteArray, stringCharSet);
                    if(decodedSemiColumnSeparatedVariables == null)
                    {
                        LogHelper.logE("Print REST Endpoint::printZPLTemplate: Could not interpret decoded variable byte array to String.");
                        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: Could not interpret decoded variable byte array to String.");
                    }
                    else
                    {
                        LogHelper.logD("Print REST Endpoint::printZPLTemplate: Base64Decoding Decoded semi column separated variables: " + decodedSemiColumnSeparatedVariables);
                    }
                }
            }
            else
            {
                LogHelper.logD("Print REST Endpoint::printZPLTemplate: Template and Variables are not base64Encoded.");
                LogHelper.logD("Print REST Endpoint::printZPLTemplate: Template : " + template);
                LogHelper.logD("Print REST Endpoint::printZPLTemplate: Variables : " + variables);
                // We are directly passing the string without base64 encoding
                decodedTemplate = template;
                decodedSemiColumnSeparatedVariables = variables;
            }

            if(decodedSemiColumnSeparatedVariables != null)
            {
                String[] splittedArray = decodedSemiColumnSeparatedVariables.split(":");
                if (splittedArray.length > 1) {
                    variableData = new HashMap<String, String>();
                    for (int i = 0; i < splittedArray.length; i = i + 2) {
                        variableData.put(splittedArray[i], splittedArray[i + 1]);
                    }
                    LogHelper.logD("Print REST Endpoint::printZPLTemplate: Found " + variableData.size() + " variable data.");
                }
                else
                {
                    LogHelper.logE("Print REST Endpoint::printZPLTemplate: A variable data extra was found but after decoding, no key-value pair was found. Length=" + splittedArray.length);
                    return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: A variable data extra was found but after decoding, no key-value pair was found. Length=" + splittedArray.length);
                }
            }

            if(decodedTemplate == null)
            {
                LogHelper.logE("Print REST Endpoint::printZPLTemplate: ZPL Template Print: No template data found.");
                return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: No template data found.");
            }
            else
            {
                return templatePrintWithContent(decodedTemplate, variableData);
            }
        }
        LogHelper.logE("Print REST Endpoint::printZPLTemplate: ZPL Template Print: No template data found.");
        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: Template data not found");
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> printTemplateFile(Map<String, List<String>> paramsList) {
        LogHelper.logD("Print REST Endpoint::printTemplateFile");
        String fileName = null;
        List<String> fileNameList = paramsList.get("filename");
        if(fileNameList != null && fileNameList.size() > 0)
            fileName = fileNameList.get(0);
        LogHelper.logD("Print REST Endpoint::printTemplateFile:fileName = " + fileName);

        String variables = null;
        List<String> variablesList = paramsList.get("variables");
        if(variablesList != null && variablesList.size() > 0)
            variables = variablesList.get(0);
        LogHelper.logD("Print REST Endpoint::printTemplateFile:variables = " + variables);

        String encoding = null;
        List<String> encodingList = paramsList.get("encoding");
        if(encodingList != null && encodingList.size() > 0)
            encoding = encodingList.get(0);
        LogHelper.logD("Print REST Endpoint::printTemplateFile:encoding = " + encoding);

        HashMap<String, String> variableData = null;
        if(fileName != null && fileName.isEmpty() == false)
        {
            String decodedFileName = null;
            String decodedSemiColumnSeparatedVariables = null;

            if(encoding != null && encoding.equalsIgnoreCase("base64encoded"))
            {
                LogHelper.logD("Print REST Endpoint::printTemplateFile: Template is base64encoded");
                // Check if the user is specifying a standard charset
                Charset stringCharSet = StandardCharsets.UTF_8;

                String standardCharsets = null;
                List<String> standardCharsetsList = paramsList.get("standardCharsets");
                if(standardCharsetsList.size() > 0) {
                    LogHelper.logD("Print REST Endpoint::printTemplateFile: Using custom charset: " + standardCharsets);
                    standardCharsets = standardCharsetsList.get(0);
                }

                if(standardCharsets == null || standardCharsets.isEmpty())
                {
                    LogHelper.logD("Print REST Endpoint::printTemplateFile: No charset specified, using standard UTF_8");
                    // Fall back to UTF_8
                    standardCharsets = "UTF_8";
                }

                if(standardCharsets != null)
                {
                    if(standardCharsets.equalsIgnoreCase("UTF_8"))
                    {
                        stringCharSet = StandardCharsets.UTF_8;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16"))
                    {
                        stringCharSet = StandardCharsets.UTF_16;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16BE"))
                    {
                        stringCharSet = StandardCharsets.UTF_16BE;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16LE"))
                    {
                        stringCharSet = StandardCharsets.UTF_16LE;
                    }
                    else if(standardCharsets.equalsIgnoreCase("ISO_8859_1"))
                    {
                        stringCharSet = StandardCharsets.ISO_8859_1;
                    }
                    else if(standardCharsets.equalsIgnoreCase("US_ASCII"))
                    {
                        stringCharSet = StandardCharsets.US_ASCII;
                    }
                }

                // We are using the base64 encoded version of the url scheme
                String base64EncodedFileName = fileName;

                String base64SemiColumnSeparatedVariableData = variables;

                // Decode fileName if found
                if (base64EncodedFileName != null) {
                    byte[] fileNameAsByteArray = null;
                    try {
                        fileNameAsByteArray = Base64.decode(base64EncodedFileName, Base64.DEFAULT);
                    } catch (Exception e) {
                        LogHelper.logE("Print REST Endpoint::printTemplateFile: Base64Decoding Error: " + e.getMessage());
                        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Base64Decoding Error: " + e.getMessage());
                    }
                    if (fileNameAsByteArray == null) {
                        LogHelper.logE("Print REST Endpoint::printTemplateFile: Base64Decoding Could not decode filename data byte array.");
                        new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Template FileName Print: Could not decode filename data byte array.");
                    }

                    decodedFileName = new String(fileNameAsByteArray, stringCharSet);

                    if (decodedFileName == null) {
                        LogHelper.logE("Print REST Endpoint::printTemplateFile: Base64Decoding Could not interpret decoded fileName byte array to String.");
                        new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Template FileName Print: Could not interpret decoded fileName byte array to String.");
                    }
                    else
                    {
                        LogHelper.logD("Print REST Endpoint::printTemplateFile: Base64Decoding Decoded filename: " + decodedFileName);
                    }
                }

                // Decode variable data if found
                if (base64SemiColumnSeparatedVariableData != null) {
                    LogHelper.logD("Print REST Endpoint::printTemplateFile: Base64Decoding variable data.");
                    byte[] dataAsByteArray = Base64.decode(base64SemiColumnSeparatedVariableData, Base64.DEFAULT);
                    if (dataAsByteArray == null) {
                        LogHelper.logE("Print REST Endpoint::printTemplateFile: Base64Decoding Could not decode variable data byte array.");
                        new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Template FileName Print: Could not decode variable data byte array.");
                    }
                    decodedSemiColumnSeparatedVariables = new String(dataAsByteArray, stringCharSet);
                    if(decodedSemiColumnSeparatedVariables == null)
                    {
                        LogHelper.logE("Print REST Endpoint::printTemplateFile: Base64Decoding Could not interpret decoded variable byte array to String.");
                        new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Template FileName Print: Could not interpret decoded variable byte array to String.");
                    }
                    else
                    {
                        LogHelper.logD("Print REST Endpoint::printTemplateFile: Base64Decoding Decoded semi column separated variables: " + decodedSemiColumnSeparatedVariables);
                    }
                }
            }
            else
            {
                LogHelper.logD("Print REST Endpoint::printTemplateFile: Template and Variables are not base64Encoded.");
                LogHelper.logD("Print REST Endpoint::printTemplateFile: Filename : " + fileName);
                LogHelper.logD("Print REST Endpoint::printTemplateFile: Variables : " + variables);
                // We are directly passing the string without base64 encoding
                decodedFileName = fileName;
                decodedSemiColumnSeparatedVariables = variables;

            }


            if(decodedFileName == null)
            {
                LogHelper.logE("Print REST Endpoint::printTemplateFile: No filename found.");
                new Pair<>(RESTServiceWebServer.EJobStatus.FAILED,  "Template FileName Print: " + "No filename found.");
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
                    LogHelper.logE("Print REST Endpoint::printTemplateFile: A variable data extra was found but after decoding, no key-value pair was found. Length=" + splittedArray.length);
                    new Pair<>(RESTServiceWebServer.EJobStatus.FAILED,  "Template FileName Print: A variable data extra was found but after decoding, no key-value pair was found. Length=" + splittedArray.length);
                }
            }
            return templatePrintWithFileName(decodedFileName, variableData);
        }
        LogHelper.logE("Print REST Endpoint::printTemplateFile: No filename found.");
        return  new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Template FileName Print: Template filename not found.");
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> printSingleLine(Map<String, List<String>> paramsList) {
        LogHelper.logD("Print REST Endpoint::printSingleLine");
        String text = null;
        List<String> textList = paramsList.get("text");
        if(textList != null && textList.size() > 0) {
            text = textList.get(0);
            LogHelper.logD("Print REST Endpoint::printSingleLine: Text to print = " + text);
        }

        String encoding = null;
        List<String> encodingList = paramsList.get("encoding");
        if(encodingList != null && encodingList.size() > 0) {
            encoding = encodingList.get(0);
            LogHelper.logD("Print REST Endpoint::printSingleLine: Encoding = " + encoding);
        }

        if(text != null && text.isEmpty() == false)
        {
            String decodedLineToPrint = null;
            if(encoding != null && encoding.equalsIgnoreCase("base64encoded")) {
                LogHelper.logD("Print REST Endpoint::printSingleLine: Text is base64encoded");
                // Check if the user is specifying a standard charset
                Charset stringCharSet = StandardCharsets.UTF_8;

                String standardCharsets = null;
                List<String> standardCharsetsList = paramsList.get("standardCharsets");
                if(standardCharsetsList.size() > 0) {
                    standardCharsets = standardCharsetsList.get(0);
                    LogHelper.logD("Print REST Endpoint::printSingleLine: Using custom charset: " + standardCharsets);
                }

                if(standardCharsets == null || standardCharsets.isEmpty())
                {
                    // Fall back to UTF_8
                    standardCharsets = "UTF_8";
                    LogHelper.logD("Print REST Endpoint::printSingleLine: No charset specified, using standard UTF_8");                }

                if(standardCharsets != null)
                {
                    if(standardCharsets.equalsIgnoreCase("UTF_8"))
                    {
                        stringCharSet = StandardCharsets.UTF_8;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16"))
                    {
                        stringCharSet = StandardCharsets.UTF_16;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16BE"))
                    {
                        stringCharSet = StandardCharsets.UTF_16BE;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16LE"))
                    {
                        stringCharSet = StandardCharsets.UTF_16LE;
                    }
                    else if(standardCharsets.equalsIgnoreCase("ISO_8859_1"))
                    {
                        stringCharSet = StandardCharsets.ISO_8859_1;
                    }
                    else if(standardCharsets.equalsIgnoreCase("US_ASCII"))
                    {
                        stringCharSet = StandardCharsets.US_ASCII;
                    }
                }

                // We are using the base64 encoded version of the url scheme
                String base64EncodedLineToPrint = text;

                // Decode line to print if found
                if (base64EncodedLineToPrint != null) {
                    try {
                        byte[] textNameAsByteArray = Base64.decode(base64EncodedLineToPrint, Base64.DEFAULT);
                        if (textNameAsByteArray == null) {
                            LogHelper.logE("Print REST Endpoint::printSingleLine: Base64Decoding Could not decode text data byte array.");
                            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Line Print: Could not decode text data byte array.");
                        }

                        decodedLineToPrint = new String(textNameAsByteArray, stringCharSet);

                        if (decodedLineToPrint == null) {
                            LogHelper.logE("Print REST Endpoint::printSingleLine: Base64Decoding Could not interpret decoded text byte array to String.");
                            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Line Print: Could not interpret decoded text byte array to String.");
                        }
                        else
                        {
                            LogHelper.logD("Print REST Endpoint::printSingleLine: Base64Decoding Decoded line to print: " + decodedLineToPrint);
                        }
                    }
                    catch(Exception e)
                    {
                        LogHelper.logE("Print REST Endpoint::printSingleLine: Base64Decoding Error: " + e.getMessage());
                        e.printStackTrace();
                        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Base64Decoding Error: " + e.getMessage());
                    }
                }
            }
            else
            {
                LogHelper.logD("Print REST Endpoint::printSingleLine: Text to print is not base64Encoded.");
                LogHelper.logD("Print REST Endpoint::printSingleLine: Text to print = " + text);
                // We are directly passing the string without base64 encoding
                decodedLineToPrint = text;
            }


            if(decodedLineToPrint == null)
            {
                LogHelper.logE("Print REST Endpoint::printSingleLine: No text to print.");
                return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Line Print: " + "No text to print found.");
            }
            else {
                return linePrintPassthrough(decodedLineToPrint);
            }

        }
        LogHelper.logE("Print REST Endpoint::printSingleLine: No text to print found.");
        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Line Print: Text to print not found.");
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> passthroughPrint(Map<String, List<String>> paramsList) {
        LogHelper.logD("Print REST Endpoint::passthroughPrint");
        String data = null;
        List<String> dataList = paramsList.get("data");
        if(dataList != null && dataList.size() > 0) {
            data = dataList.get(0);
            LogHelper.logD("Print REST Endpoint::passthroughPrint: Data to print = " + data);
        }

        String encoding = null;
        List<String> encodingList = paramsList.get("encoding");
        if(encodingList != null && encodingList.size() > 0) {
            encoding = encodingList.get(0);
            LogHelper.logD("Print REST Endpoint::passthroughPrint: Encoding = " + encoding);
        }

        if(data != null && data.isEmpty() == false)
        {
            String decodedPassthroughData = null;
            if(encoding != null && encoding.equalsIgnoreCase("base64encoded")) {
                LogHelper.logD("Print REST Endpoint::passthroughPrint: Data is base64encoded");
                // Check if the user is specifying a standard charset
                Charset stringCharSet = StandardCharsets.US_ASCII;

                String standardCharsets = null;
                List<String> standardCharsetsList = paramsList.get("standardCharsets");
                if (standardCharsetsList.size() > 0) {
                    standardCharsets = standardCharsetsList.get(0);
                    LogHelper.logD("Print REST Endpoint::passthroughPrint: Using custom charset: " + standardCharsets);
                }

                if (standardCharsets == null || standardCharsets.isEmpty()) {
                    // Fall back to UTF_8
                    standardCharsets = "UTF_8";
                    LogHelper.logD("Print REST Endpoint::passthroughPrint: No charset specified, using standard UTF_8");
                }

                if(standardCharsets != null)
                {
                    if(standardCharsets.equalsIgnoreCase("UTF_8"))
                    {
                        stringCharSet = StandardCharsets.UTF_8;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16"))
                    {
                        stringCharSet = StandardCharsets.UTF_16;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16BE"))
                    {
                        stringCharSet = StandardCharsets.UTF_16BE;
                    }
                    else if(standardCharsets.equalsIgnoreCase("UTF_16LE"))
                    {
                        stringCharSet = StandardCharsets.UTF_16LE;
                    }
                    else if(standardCharsets.equalsIgnoreCase("ISO_8859_1"))
                    {
                        stringCharSet = StandardCharsets.ISO_8859_1;
                    }
                    else if(standardCharsets.equalsIgnoreCase("US_ASCII"))
                    {
                        stringCharSet = StandardCharsets.US_ASCII;
                    }
                }

                // We are using the base64 encoded version of the url scheme
                String base64EncodedPassthroughData = data;

                // Decode line to print if found
                byte[] dataAsByteArray = Base64.decode(base64EncodedPassthroughData, Base64.DEFAULT);
                if (dataAsByteArray == null) {
                    LogHelper.logE("Print REST Endpoint::passthroughPrint: Base64Decoding Could not decode data byte array.");
                    return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Passthrough Print: Could not decode passthrough data byte array.");
                }

                decodedPassthroughData = new String(dataAsByteArray, stringCharSet);

                if(decodedPassthroughData == null)
                {
                    LogHelper.logE("Print REST Endpoint::passthroughPrint: Base64Decoding Could not interpret decoded passthrough byte array to String.");
                    return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Passthrough Print: Could not interpret decoded passthrough byte array to String.");
                }
                else
                {
                    LogHelper.logD("Print REST Endpoint::passthroughPrint: Base64Decoding Decoded passthrough data: " + decodedPassthroughData);
                }
            }
            else {
                LogHelper.logD("Print REST Endpoint::printSingleLine: Passthrough data to print is not base64Encoded.");
                LogHelper.logD("Print REST Endpoint::printSingleLine: Passthrough data to print = " + data);
                // We are directly passing the string without base64 encoding
                decodedPassthroughData = data;
            }

            if(decodedPassthroughData == null)
            {
                LogHelper.logE("Print REST Endpoint::printSingleLine: No passthrough data to print found.");
                return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Passthrough Print: No passthrough data to print found.");
            }
            else {

                return passthroughDataPrint(decodedPassthroughData);
            }
        }
        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Passthrough Print: Data to print not found.");
    }


    private Pair<RESTServiceWebServer.EJobStatus, String> unselect() {
        if(mJobDoneLatch != null)
        {
            LogHelper.logD("Print REST Endpoint::unselect: Error, a job is already running in background. Please wait for it to finish or timeout.");
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Unselect printer: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        PCUnselectPrinter linePrint = new PCUnselectPrinter(mContext);

        PCIntentsBaseSettings settings = new PCIntentsBaseSettings()
        {{
            mCommandId = "unselectPrinter";
            mTimeOutMS = 10000;
        }};

        linePrint.execute(settings, new PCUnselectPrinter.onUnselectPrinterResult() {
            @Override
            public void success(PCIntentsBaseSettings settings) {
                mJobReturnMessage = "Unselect printer: Succeeded.";
                mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                LogHelper.logD("Print REST Endpoint::unselect: Unselect printer: Succeeded.");
                mJobDoneLatch.countDown();
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCIntentsBaseSettings settings) {
                mJobReturnMessage = "Unselect printer: Error while trying to unselect printer" + "\n" + errorMessage;
                mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                LogHelper.logD("Print REST Endpoint::unselect: Error while trying to unselect printer" + "\n" + errorMessage);
                mJobDoneLatch.countDown();
            }

            @Override
            public void timeOut(PCIntentsBaseSettings settings) {
                mJobReturnMessage = "Unselect printer: Timeout while trying to unselect printer.";
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                LogHelper.logD("Unselect printer: Timeout while trying to unselect printer.");
                mJobDoneLatch.countDown();
            }
        });
        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            LogHelper.logD("Unselect printer: Job's done, returning status :" + mJobStatus.toString() +  "with message: " + mJobReturnMessage);
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            LogHelper.logE("Unselect printer: Exception while waiting for CountDownLatch : " + e.getMessage());
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Unselect printer: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> templatePrintWithContent(final String zplToPrint, final HashMap<String, String> variableData) {

        if(mJobDoneLatch != null)
        {
            LogHelper.logD("Print REST Endpoint::templatePrintWithContent: Error, a job is already running in background. Please wait for it to finish or timeout.");
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        PCTemplateStringPrint templateStringPrint = new PCTemplateStringPrint(mContext);

        PCTemplateStringPrintSettings settings = new PCTemplateStringPrintSettings()
        {{
            mZPLTemplateString = zplToPrint;
            mVariableData = variableData;
            mTimeOutMS = 10000;
        }};

        templateStringPrint.execute(settings, new PCTemplateStringPrint.onPrintTemplateStringResult() {
            @Override
            public void success(PCTemplateStringPrintSettings settings) {
                mJobReturnMessage = "ZPL Template Print: Template print string succeeded";
                mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                LogHelper.logD("Print REST Endpoint::templatePrintWithContent: Succeeded.");
                mJobDoneLatch.countDown();
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCTemplateStringPrintSettings settings) {
                mJobReturnMessage = "ZPL Template Print: Error while trying to template string print: " + errorMessage;
                mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                LogHelper.logD("Print REST Endpoint::templatePrintWithContent: Error: " + errorMessage);
                mJobDoneLatch.countDown();
            }

            @Override
            public void timeOut(PCTemplateStringPrintSettings settings) {
                mJobReturnMessage = "ZPL Template Print: Timeout while trying to print.";
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                LogHelper.logD("Print REST Endpoint::templatePrintWithContent: Timeout while trying to print.");
                mJobDoneLatch.countDown();
            }
        });

        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            LogHelper.logD("Print REST Endpoint::templatePrintWithContent: Job's done, returning status :" + mJobStatus.toString() +  "with message: " + mJobReturnMessage);
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            LogHelper.logE("Print REST Endpoint::templatePrintWithContent: Exception while waiting for CountDownLatch : " + e.getMessage());
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "ZPL Template Print: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> templatePrintWithFileName(final String fileName, final HashMap<String,String> variableData) {
        if(mJobDoneLatch != null)
        {
            LogHelper.logD("Print REST Endpoint::templatePrintWithFileName: Error, a job is already running in background. Please wait for it to finish or timeout.");
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Template FileName Print: A job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        PCTemplateFileNamePrint templateFileNamePrint = new PCTemplateFileNamePrint(mContext);

        PCTemplateFileNamePrintSettings settings = new PCTemplateFileNamePrintSettings()
        {{
            mTemplateFileName = fileName;
            mVariableData = variableData;
            mTimeOutMS = 10000;
        }};

        templateFileNamePrint.execute(settings, new PCTemplateFileNamePrint.onPrintFileNameResult() {
            @Override
            public void success(PCTemplateFileNamePrintSettings settings) {
                mJobReturnMessage = "Template FileName Print: Template print filename succeeded";
                mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                LogHelper.logD("Print REST Endpoint::templatePrintWithFileName: Succeeded.");
                mJobDoneLatch.countDown();
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCTemplateFileNamePrintSettings settings) {
                mJobReturnMessage = "Template FileName Print: Error while trying to print filename: " + settings.mTemplateFileName + " | " + errorMessage;
                mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                LogHelper.logD("Print REST Endpoint::templatePrintWithFileName: Error: " + errorMessage);
                mJobDoneLatch.countDown();
            }

            @Override
            public void timeOut(PCTemplateFileNamePrintSettings settings) {
                mJobReturnMessage = "Template FileName Print: Timeout while trying to print filename: " + settings.mTemplateFileName;
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                LogHelper.logD("Print REST Endpoint::templatePrintWithFileName: Timeout while trying to print filename: " + settings.mTemplateFileName);
                mJobDoneLatch.countDown();
            }
        });

        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            LogHelper.logD("Print REST Endpoint::templatePrintWithFileName: Job's done, returning status :" + mJobStatus.toString() +  "with message: " + mJobReturnMessage);
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            LogHelper.logE("Print REST Endpoint::templatePrintWithFileName: Exception while waiting for CountDownLatch : " + e.getMessage());
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Template FileName Print: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> linePrintPassthrough(final String lineToPrint) {
        if(mJobDoneLatch != null)
        {
            LogHelper.logD("Print REST Endpoint::linePrintPassthrough: Error, a job is already running in background. Please wait for it to finish or timeout.");
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Line Print: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        PCLinePrintPassthroughPrint linePrint = new PCLinePrintPassthroughPrint(mContext);

        PCLinePrintPassthroughPrintSettings settings = new PCLinePrintPassthroughPrintSettings()
        {{
            mLineToPrint = lineToPrint;
            mTimeOutMS = 10000;
        }};

        linePrint.execute(settings, new PCLinePrintPassthroughPrint.onLinePrintPassthroughResult() {
            @Override
            public void success(PCLinePrintPassthroughPrintSettings settings) {
                mJobReturnMessage = "Line Print: Succeeded";
                mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                LogHelper.logD("Print REST Endpoint::linePrintPassthrough: Succeeded.");
                mJobDoneLatch.countDown();
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCLinePrintPassthroughPrintSettings settings) {
                mJobReturnMessage = "Line Print: Error while trying to print line: " + errorMessage;
                mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                LogHelper.logD("Print REST Endpoint::linePrintPassthrough: Error: " + errorMessage);
                mJobDoneLatch.countDown();
            }

            @Override
            public void timeOut(PCLinePrintPassthroughPrintSettings settings) {
                mJobReturnMessage = "Line Print: Timeout while trying to print line";
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                LogHelper.logD("Print REST Endpoint::linePrintPassthrough: Timeout while trying to print line");
                mJobDoneLatch.countDown();
            }
        });

        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            LogHelper.logD("Print REST Endpoint::linePrintPassthrough: Job's done, returning status :" + mJobStatus.toString() +  "with message: " + mJobReturnMessage);
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            LogHelper.logE("Print REST Endpoint::linePrintPassthrough: Exception while waiting for CountDownLatch : " + e.getMessage());
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Line Print: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> passthroughDataPrint(final String passthroughData) {

        if(mJobDoneLatch != null)
        {
            LogHelper.logD("Print REST Endpoint::passthroughDataPrint: Error, a job is already running in background. Please wait for it to finish or timeout.");
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Passthrough Print: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        PCPassthroughPrint linePrint = new PCPassthroughPrint(mContext);

        PCPassthroughPrintSettings settings = new PCPassthroughPrintSettings()
        {{
            mPassthroughData = passthroughData;
            mTimeOutMS = 10000;
        }};

        linePrint.execute(settings, new PCPassthroughPrint.onPassthroughResult() {
            @Override
            public void success(PCPassthroughPrintSettings settings) {
                mJobReturnMessage = "Passthrough Print: Succeeded";
                mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                LogHelper.logD("Print REST Endpoint::passthroughDataPrint: Succeeded.");
                mJobDoneLatch.countDown();
            }

            @Override
            public void error(String errorMessage, int resultCode, Bundle resultData, PCPassthroughPrintSettings settings) {
                mJobReturnMessage = "Passthrough Print: Error while trying to print passthrough : " + errorMessage;
                mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                LogHelper.logD("Print REST Endpoint::passthroughDataPrint: Error: " + errorMessage);
                mJobDoneLatch.countDown();
            }

            @Override
            public void timeOut(PCPassthroughPrintSettings settings) {
                mJobReturnMessage = "Passthrough Print: Timeout while trying to print passthrough";
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                LogHelper.logD("Print REST Endpoint::passthroughDataPrint: Timeout while trying to print passthrough");
                mJobDoneLatch.countDown();
            }
        });

        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            LogHelper.logD("Print REST Endpoint::passthroughDataPrint: Job's done, returning status :" + mJobStatus.toString() +  "with message: " + mJobReturnMessage);
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            LogHelper.logE("Print REST Endpoint::passthroughDataPrint: Exception while waiting for CountDownLatch : " + e.getMessage());
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Passthrough Print: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

}
