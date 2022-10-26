package com.zebra.enterpriseservices;

import android.util.Log;

public class LogHelper {
    protected static String TAG = "EnterpriseServices";
    private static boolean forceLogging = false;

    public static void enableLogging()
    {
        forceLogging = true;
    }

    public static void disableLogging()
    {
        forceLogging = false;
    }

    protected static void logV(String message)
    {
        if(BuildConfig.DEBUG || forceLogging)
        {
            Log.v(TAG, message);
        }
    }

    protected static void logD(String message)
    {
        if(BuildConfig.DEBUG || forceLogging)
        {
            Log.d(TAG, message);
        }
    }
    protected static void logE(String message)
    {
        if(BuildConfig.DEBUG || forceLogging)
        {
            Log.e(TAG, message);
        }
    }

}
