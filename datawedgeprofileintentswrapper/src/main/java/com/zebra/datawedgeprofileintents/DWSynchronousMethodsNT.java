package com.zebra.datawedgeprofileintents;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Pair;

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

public class DWSynchronousMethodsNT {
    
    private String mLastMessage = "";
    private DWSynchronousMethods.EResults mLastResult = DWSynchronousMethods.EResults.NONE;
    private CountDownLatch mJobDoneLatch = null;
    private Context mContext = null;

    public DWSynchronousMethodsNT(Context context)
    {
        mContext = context;
    }

    private class SynchronousNTRunnable implements Runnable
    {
        private String mMethodName;
        private Object mParam;
        private Class<?> mParamClass;
        private Context mContext;
        public Pair<DWSynchronousMethods.EResults, String> mResults = null;
        public boolean mHasFinished = false;

        public SynchronousNTRunnable(Context context, String methodName, Object param, Class<?> paramClass)
        {
            mMethodName = methodName;
            mParam = param;
            mParamClass = paramClass;
            mContext = context;
        }

        @Override
        public void run() {
            try {
                mHasFinished = false;
                Method method;
                Object result = null;
                DWSynchronousMethods dwSynchronousMethods = new DWSynchronousMethods(mContext);
                method = DWSynchronousMethods.class.getMethod(mMethodName, mParamClass);
                result = method.invoke(dwSynchronousMethods, mParam);
                if(result != null)
                {
                    mResults = (Pair<DWSynchronousMethods.EResults, String>)result;
                }
                else
                    mResults = null;
                mHasFinished = true;
                
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * Return message left by the last executed method
     * it can be an error message if the method returned an Error result.
     * @return
     */
    public String getLastMessage()
    {
        return mLastMessage;
    }

    public Pair<DWSynchronousMethods.EResults,String> runInNewThread(String methodName, Object param, Class<?> paramClass)
    {
        SynchronousNTRunnable synchronousNTRunnable = new SynchronousNTRunnable(mContext, methodName, param, paramClass);
        Thread synchronizedThread = new Thread(synchronousNTRunnable);
        synchronizedThread.start();
        while (synchronousNTRunnable.mHasFinished == false) {
            try {
                Thread.sleep(100L);
            } catch (Throwable e) {
                // on android this may not be allowed, that's why we
                // catch throwable the wait should be very short because we are
                // just waiting for the bind of the socket
            }
        }
        return synchronousNTRunnable.mResults;
    }

    public Pair<DWSynchronousMethods.EResults,String> setupDWProfile(final DWProfileSetConfigSettings settings) 
    {
        return runInNewThread("setupDWProfile", settings, DWProfileSetConfigSettings.class);
    }
    
    public Pair<DWSynchronousMethods.EResults, String> enablePlugin()
    {
        return runInNewThread("enablePlugin", mContext.getPackageName(), String.class);
    }

    public Pair<DWSynchronousMethods.EResults, String> enablePlugin(String profileName)
    {
        return runInNewThread("enablePlugin", profileName, String.class);
    }

    public Pair<DWSynchronousMethods.EResults, String> disablePlugin()
    {
        return runInNewThread("disablePlugin", mContext.getPackageName(), String.class);
    }
    
    public Pair<DWSynchronousMethods.EResults, String> disablePlugin(String profileName)
    {
        return runInNewThread("disablePlugin", profileName, String.class);
    }

    public Pair<DWSynchronousMethods.EResults, String> startScan()
    {
        return runInNewThread("startScan", mContext.getPackageName(), String.class);
    }
    
    public Pair<DWSynchronousMethods.EResults, String> startScan(String profileName)
    {
        return runInNewThread("startScan", profileName, String.class);
    }

    public Pair<DWSynchronousMethods.EResults, String> stopScan()
    {
        return runInNewThread("stopScan", mContext.getPackageName(), String.class);
    }
    
    public Pair<DWSynchronousMethods.EResults, String> stopScan(String profileName)
    {
        return runInNewThread("stopScan", profileName, String.class);
    }


    public Pair<DWSynchronousMethods.EResults, String> profileExists()
    {
        return runInNewThread("profileExists", mContext.getPackageName(), String.class);
    }

    public Pair<DWSynchronousMethods.EResults, String> profileExists(String profileName)
    {
        return runInNewThread("profileExists", profileName, String.class);
    }

    public Pair<DWSynchronousMethods.EResults, String> deleteProfile()
    {
        return runInNewThread("deleteProfile", mContext.getPackageName(), String.class);
    }

    public Pair<DWSynchronousMethods.EResults, String> deleteProfile(String profileName)
    {
        return runInNewThread("deleteProfile", profileName, String.class);
    }

    public Pair<DWSynchronousMethods.EResults, String> switchBarcodeParams(DWProfileSwitchBarcodeParamsSettings settings)
    {
        return runInNewThread("switchBarcodeParams", settings, DWProfileSwitchBarcodeParamsSettings.class);
    }

}
