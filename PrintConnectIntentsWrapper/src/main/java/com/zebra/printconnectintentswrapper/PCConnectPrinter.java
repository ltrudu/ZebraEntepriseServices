package com.zebra.printconnectintentswrapper;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;

public class PCConnectPrinter extends PCIntentsBase {

    /*
    An interface callback to be informed of the result
    of the print template intent
     */
    public interface onConnectPrinterResult
    {
        void success(PCConnectPrinterSettings settings);
        void error(String errorMessage, PCConnectPrinterSettings settings);
        void timeOut(PCConnectPrinterSettings settings);
    }

    private onConnectPrinterResult mConnectPrinterCallback = null;

    public PCConnectPrinter(Context aContext)
    {
        super(aContext);
    }

    public void execute(PCConnectPrinterSettings settings, onConnectPrinterResult callback)
    {
        if(callback == null)
        {
            Log.e(TAG, PCConstants.PCIntentsNoCallbackError);
            return;
        }

        mConnectPrinterCallback = callback;

        /*
        Launch timeout mechanism
         */
        super.execute(settings);

        if(ConnectPrinter(settings))
        {
            cleanAll();
            if(mConnectPrinterCallback != null)
            {
                mConnectPrinterCallback.success(settings);
            }
        }
        else
        {
            cleanAll();
            if(mConnectPrinterCallback != null)
            {
                mConnectPrinterCallback.error("At least one mac address must be specified.", settings);
            }
        }


    }

    private boolean ConnectPrinter(final PCConnectPrinterSettings settings)
    {
        // Send intent here
        if((settings.mEthernetMacAddress != null && settings.mEthernetMacAddress.isEmpty() == false)
                || (settings.mWifiMacAddress != null && settings.mWifiMacAddress.isEmpty() == false)
                || (settings.mBluetoothMacAddress != null && settings.mBluetoothMacAddress.isEmpty() == false))
        {
            if(settings.mEthernetMacAddress == null || settings.mEthernetMacAddress.isEmpty())
            {
                settings.mEthernetMacAddress = "000000000000";
            }
            if(settings.mWifiMacAddress == null || settings.mWifiMacAddress.isEmpty())
            {
                settings.mWifiMacAddress = "000000000000";
            }
            if(settings.mBluetoothMacAddress == null || settings.mBluetoothMacAddress.isEmpty())
            {
                settings.mBluetoothMacAddress = "000000000000";
            }
            Intent simulateNFCIntent = new Intent();
            simulateNFCIntent.setAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
            Uri connectionURI = Uri.parse("http://www.zebra.com/apps/r/nfc?mE="+ settings.mEthernetMacAddress + "&mW=" + settings.mWifiMacAddress + "&mB=" + settings.mBluetoothMacAddress + "&c=XXXX-XXXXXXX-XX&s=XXXXXXXXXXXXXX&v=0");
            simulateNFCIntent.setData(connectionURI);
            Parcelable ndefMessages[] = new Parcelable[1];
            NdefRecord records[] = new NdefRecord[1];
            String payloadString = "\u0001zebra.com/apps/r/nfc?mE="+ settings.mEthernetMacAddress + "&mW=" + settings.mWifiMacAddress + "&mB=" + settings.mBluetoothMacAddress + "&c=XXXX-XXXXXXX-XX&s=XXXXXXXXXXXXXX&v=0";
            byte[] payload = payloadString.getBytes();
            byte[] type = NdefRecord.RTD_URI;
            byte[] id = new byte[1];
            records[0] = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, type, id, payload);
            ndefMessages[0] = new NdefMessage(records);
            simulateNFCIntent.putExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, ndefMessages);
            mContext.startActivity(simulateNFCIntent);
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    protected void onTimeOut(PCIntentsBaseSettings settings) {
        if(mConnectPrinterCallback != null)
        {
            if(mConnectPrinterCallback != null)
            {
                mConnectPrinterCallback.timeOut((PCConnectPrinterSettings)settings);
            }
        }
    }
}
