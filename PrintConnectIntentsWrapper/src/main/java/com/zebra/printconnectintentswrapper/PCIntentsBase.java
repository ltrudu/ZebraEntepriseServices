package com.zebra.printconnectintentswrapper;

import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.ResultReceiver;

/**
 * Created by laure on 06/07/2018.
 */

public abstract class PCIntentsBase {
    /*
    A TAG if we want to log something
     */
    protected static String TAG = "PCINTENTSAPI";

    /*
    A context to work with intents
     */
    protected Context mContext = null;

    protected PCIntentsBaseSettings mSettings = null;

    /*
    A handler that will be used by the derived
    class to prevent waiting to loong for DW in case
    of problem
     */
    protected Handler mTimeOutHandler;

    /*
    What will be done at the end of the TimeOut
     */
    protected Runnable mTimeOutRunnable = new Runnable() {
        @Override
        public void run() {
            onTimeOut(mSettings);
        }
    };


    public PCIntentsBase(Context aContext)
    {
        mContext = aContext;
        mTimeOutHandler = new Handler(mContext.getMainLooper());
    }


    protected void execute(PCIntentsBaseSettings settings)
    {
        mSettings = settings;
        /*
        Start time out mechanism
        Enabled by default in DWProfileBaseSettings
         */
        if(settings.mEnableTimeOutMechanism) {
            mTimeOutHandler.postDelayed(mTimeOutRunnable,
                    mSettings.mTimeOutMS);
        }
    }

    // This method makes your ResultReceiver safe for inter-process communication
    protected ResultReceiver buildIPCSafeReceiver(ResultReceiver actualReceiver) {
        Parcel parcel = Parcel.obtain();
        actualReceiver.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        ResultReceiver receiverForSending = ResultReceiver.CREATOR.createFromParcel(parcel);
        parcel.recycle();
        return receiverForSending;
    }

    protected abstract void onTimeOut(PCIntentsBaseSettings settings);

    protected void cleanAll()
    {
        if(mTimeOutHandler != null)
        {
            mTimeOutHandler.removeCallbacks(mTimeOutRunnable);
        }
    }
}
