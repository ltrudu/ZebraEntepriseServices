package com.zebra.printconnectintentswrapper;

public class PCIntentsBaseSettings
{
    /*
    Use this to track the source of the intent
     */
    public String mCommandId = "";

    /*
    Some method return only errors (StartScan, StopScan)
    We do not need a time out for them
     */
    public boolean mEnableTimeOutMechanism = true;

    /*
    A time out, in case we don't receive an answer
    from PrintConnect
     */
    public long mTimeOutMS = 5000;
}
