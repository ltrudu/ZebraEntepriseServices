package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import static android.content.Context.WIFI_SERVICE;

public class RESTPrintServiceWifiIPObserver {

    private BroadcastReceiver mNetworkStateBroadcastReceiver = null;
    private String mIpAddress = "";
    private Context mContext = null;

    public interface IIPChangeObserver
    {
        void onIPChanged(String newIP);
    }
    private IIPChangeObserver mIPChangeObserver = null;

    public RESTPrintServiceWifiIPObserver(Context aContext, IIPChangeObserver aIIPChangeObserver)
    {
        mContext = aContext;
        mIPChangeObserver = aIIPChangeObserver;
    }

    public void startObserver()
    {
        Log.d(RESTPrintServiceConstants.TAG, "Starting ip change observer.");
        registerNetworkChangeBroadcastReceiver();
    }

    public void stopObserver()
    {
        Log.d(RESTPrintServiceConstants.TAG, "Stopping ip change observer.");
        unregisterNetworkChangeBroadcastReceiver();
    }

    public  boolean isStarted()
    {
        return mNetworkStateBroadcastReceiver != null;
    }

    public  String getIPAddress()
    {
        if(mNetworkStateBroadcastReceiver == null || mIpAddress == null || mIpAddress.isEmpty())
        {
            mIpAddress = getDeviceIP();
        }
        return mIpAddress;
    }

    //////////////////////////////////////////////////////////////
    // Wifi network related methods
    //////////////////////////////////////////////////////////////
    private  void registerNetworkChangeBroadcastReceiver() {
        if(mNetworkStateBroadcastReceiver == null)
        {
            final IntentFilter filters = new IntentFilter();
            filters.addAction("android.net.wifi.WIFI_STATE_CHANGED");
            filters.addAction("android.net.wifi.STATE_CHANGE");
            mNetworkStateBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String currentIP = getDeviceIP();
                    if(mIpAddress.equalsIgnoreCase(currentIP) == false)
                    {
                        if(mIPChangeObserver != null)
                            mIPChangeObserver.onIPChanged(currentIP);
                        mIpAddress = currentIP;
                    }
                }
            };
            mContext.registerReceiver(mNetworkStateBroadcastReceiver, filters);
        }
        else
        {
            Log.d(RESTPrintServiceConstants.TAG, "StartObserver: Warning, WifiIPObserver already running.");
        }
        // get current ip if connected
        if(isConnectedToWifi())
            mIpAddress = getIPAddress();
    }

    private void unregisterNetworkChangeBroadcastReceiver()
    {
        if(mNetworkStateBroadcastReceiver != null)
        {
            mContext.unregisterReceiver(mNetworkStateBroadcastReceiver);
            mNetworkStateBroadcastReceiver = null;
        }
        else
        {
            Log.d(RESTPrintServiceConstants.TAG, "StartObserver: Warning, WifiIPObserver already stopped.");
        }
    }

    public boolean isConnectedToWifi() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (networkInfo != null && isNetworkAvailable() && networkInfo.isConnected()
                && wifiManager.isWifiEnabled() && networkInfo.getTypeName().equals("WIFI")) {
            return true;
        }
        return false;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
        } else {
            return false;
        }
    }

    private String getDeviceIP() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();
        final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        return formatedIpAddress;
    }
}
