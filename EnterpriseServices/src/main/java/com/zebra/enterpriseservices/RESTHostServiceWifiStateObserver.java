package com.zebra.enterpriseservices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;

import static android.content.Context.WIFI_SERVICE;

public class RESTHostServiceWifiStateObserver {

    private BroadcastReceiver mNetworkStateBroadcastReceiver = null;
    private String mIpAddress = "";
    private Context mContext = null;
    private CountDownLatch mJobDoneLatch = null;

    public interface IIPChangeObserver
    {
        void onIPChanged(String newIP);
    }
    private IIPChangeObserver mIPChangeObserver = null;

    public RESTHostServiceWifiStateObserver(Context aContext, IIPChangeObserver aIIPChangeObserver)
    {
        mContext = aContext;
        mIPChangeObserver = aIIPChangeObserver;
    }

    public void startObserver()
    {
        LogHelper.logD( "Starting ip change observer.");
        registerNetworkChangeBroadcastReceiver();
    }

    public void stopObserver()
    {
        LogHelper.logD( "Stopping ip change observer.");
        unregisterNetworkChangeBroadcastReceiver();
    }

    public  boolean isStarted()
    {
        return mNetworkStateBroadcastReceiver != null;
    }

    public  String getIPAddress()
    {
        getDeviceIP();
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
                        getIPAddress();
                        if(mIPChangeObserver != null)
                            mIPChangeObserver.onIPChanged(mIpAddress);
                }
            };
            mContext.registerReceiver(mNetworkStateBroadcastReceiver, filters);
        }
        else
        {
            LogHelper.logD( "StartObserver: Warning, WifiIPObserver already running.");
        }
        // get current ip if connected
        if(isConnected())
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
            LogHelper.logD( "StartObserver: Warning, WifiIPObserver already stopped.");
        }
    }

    public boolean isConnected() {
        /*
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        NetworkInfo networkInfo = ((ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        String networkType = networkInfo.getTypeName();
        if (networkInfo != null && isNetworkAvailable() && networkInfo.isConnected())
        {
            if(networkType.equalsIgnoreCase("WIFI")  && wifiManager.isWifiEnabled()) {
                return true;
            }
            else if(networkType.equalsIgnoreCase("MOBILE"))
            {
                return true;
            }
        }
        */
        ConnectivityManager connectivityManager =  (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager != null)
        {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
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

    private void getDeviceIP() {
        WifiManager wifiManager = (WifiManager) mContext.getSystemService(WIFI_SERVICE);
        // If wifi is enabled we just get the WIFI address
        if(wifiManager != null && wifiManager.isWifiEnabled()) {
            getWifiIPAddress(wifiManager);
        }
        else {
            // We try to get the WAN address (or any other address...)
            // TODO: test on ethernet craddle to see if it gets the right IP address
            try {
                getWanIPAddress();
            } catch (Exception e) {
                e.printStackTrace();
                mIpAddress = "127.0.0.1";
            }
        }
    }

    private void getWifiIPAddress(WifiManager wifiManager)
    {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null) {
            int ipAddress = wifiInfo.getIpAddress();
            final String formatedIpAddress = String.format("%d.%d.%d.%d", (ipAddress & 0xff), (ipAddress >> 8 & 0xff), (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
            mIpAddress = formatedIpAddress;
        }

    }

    private void getWanIPAddress() throws Exception {
        if(mJobDoneLatch != null)
        {
            // we are already reading the IP address, this case shouldn't appears in the workflow, but who knows
            throw new Exception("Already looking for an IP address.");
        }
        mJobDoneLatch = new CountDownLatch(1);

        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try  {
                    try
                    {
                        InetAddress primera = InetAddress.getLocalHost();
                        String hostname = InetAddress.getLocalHost().getHostName ();

                        if (!primera.isLoopbackAddress () &&
                                !hostname.equalsIgnoreCase ("localhost") &&
                                primera.getHostAddress ().indexOf (':') == -1)
                        {
                            // Got it without delay!!
                            mIpAddress = primera.getHostAddress ();
                            //System.out.println ("First try! " + HOST_NAME + " IP " + HOST_IPADDRESS);
                            mJobDoneLatch.countDown();
                            return;
                        }
                        for (Enumeration<NetworkInterface> netArr = NetworkInterface.getNetworkInterfaces(); netArr.hasMoreElements();)
                        {
                            NetworkInterface netInte = netArr.nextElement ();
                            for (Enumeration<InetAddress> addArr = netInte.getInetAddresses (); addArr.hasMoreElements ();)
                            {
                                InetAddress laAdd = addArr.nextElement ();
                                String ipstring = laAdd.getHostAddress ();
                                String hostName = laAdd.getHostName ();

                                if (laAdd.isLoopbackAddress()) continue;
                                if (hostName.equalsIgnoreCase ("localhost")) continue;
                                if (ipstring.indexOf (':') >= 0) continue;

                                mIpAddress = ipstring;
                                mJobDoneLatch.countDown();
                                return;
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
        } catch (InterruptedException e) {
            if (mJobDoneLatch != null) {
                while (mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
        }
    }
}
