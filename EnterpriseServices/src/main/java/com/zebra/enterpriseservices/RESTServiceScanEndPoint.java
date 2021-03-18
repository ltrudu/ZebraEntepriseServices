package com.zebra.enterpriseservices;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import com.zebra.datawedgeprofileenums.BDF_E_OUTPUT_PLUGIN;
import com.zebra.datawedgeprofileenums.INT_E_DELIVERY;
import com.zebra.datawedgeprofileenums.MB_E_CONFIG_MODE;
import com.zebra.datawedgeprofileintents.DWProfileBaseSettings;
import com.zebra.datawedgeprofileintents.DWProfileCommandBase;
import com.zebra.datawedgeprofileintents.DWProfileDelete;
import com.zebra.datawedgeprofileintents.DWProfileDeleteSettings;
import com.zebra.datawedgeprofileintents.DWProfileSetConfig;
import com.zebra.datawedgeprofileintents.DWProfileSetConfigSettings;
import com.zebra.datawedgeprofileintents.DWScanReceiver;
import com.zebra.datawedgeprofileintents.DWScannerPluginDisable;
import com.zebra.datawedgeprofileintents.DWScannerPluginEnable;
import com.zebra.datawedgeprofileintents.DWScannerStartScan;
import com.zebra.datawedgeprofileintents.DWScannerStopScan;
import com.zebra.datawedgeprofileintents.DataWedgeConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import fi.iki.elonen.NanoHTTPD;

public class RESTServiceScanEndPoint implements RESTServiceInterface{

    private RESTServiceWebServer.EJobStatus mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
    private CountDownLatch mJobDoneLatch = null;
    private static CountDownLatch mScanJobLatch = null;
    private String mJobReturnMessage = "";
    private Context mContext = null;

    private String mScannedDataSource = null;
    private String mScannedData = null;
    private String mScannedDataSymbology = null;
    private Handler mScanTimeOutHandler = null;
    private Runnable mScanTimeOutRunnable = null;

    private static String RECEIVER_SUFFIX = ".RECVR";
    private static String INTENT_CATEGORY = "android.intent.category.DEFAULT";

    private static String STOP_SCANNER = "iodfergirg5udfhsidugise61r6g5sdf1g6sgze89zze+g4z8ter6g4ssdfsdgsgazdf65sdfg";

    /**
     * Scanner data receiver
     */
    DWScanReceiver mScanReceiver = null;

    public RESTServiceScanEndPoint(Context aContext)
    {
        mContext = aContext;
    }


    @Override
    public Pair<RESTServiceWebServer.EJobStatus, String> processSession(NanoHTTPD.IHTTPSession session)
    {
        Map<String, List<String>> paramsList = session.getParameters();
        List<String> commandList = paramsList.get("command");

        if(commandList == null || commandList.size() == 0)
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Datawedge error: Command not found in params : " + paramsList.toString());

        String command = commandList.get(0);
        switch(command)
        {
            case "setup":
                return setupprofile(session);
            case "enable":
                return enablePlugin();
            case "disable":
                return disablePlugin();
            case "start":
                return startScan();
            case "stop":
                return stopScan();
            case "waitscan":
                return waitscan(paramsList);
            case "stopwaitingscan":
                return stopwaitingscan();
        }
        return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Datawedge error: Unsupported command : " + command);
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> setupprofile(NanoHTTPD.IHTTPSession session) {
        // Retrieve the data that have been posted (i.e. json configuration)
        Map<String, String> files = null;
        NanoHTTPD.Method method = session.getMethod();
        if (NanoHTTPD.Method.PUT.equals(method) || NanoHTTPD.Method.POST.equals(method)) {
            try {
                files = new HashMap<String, String>();
                session.parseBody(files);
            } catch (IOException ioe) {
                return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (NanoHTTPD.ResponseException re) {
                return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "SERVER INTERNAL ERROR: IOException: " + re.getMessage());
            }
        }
        String postData = null;
        if(files != null && files.size() > 0)
        {
            postData = files.get("postData");
        }
        else
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Parse body failed for session:" + session.toString());
        }

        if(postData == null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Could not find POST data in:" + files.toString());
        }

        DWProfileSetConfigSettings settings = DWProfileSetConfigSettings.fromJson(postData);

        if(settings.MainBundle.APP_LIST == null)
        {
            // Create an empty list
            settings.MainBundle.APP_LIST = new HashMap<>();

            // Try to get package from parameters
            Map<String, List<String>> paramsList = session.getParameters();
            // We need to retrieve the list of associated packages
            List<String> packagesListParam = paramsList.get("package");
            if(packagesListParam != null && packagesListParam.size() > 0)
            {
                try
                {
                    String packageToAdd = packagesListParam.get(0);
                    settings.MainBundle.APP_LIST.put(packageToAdd, null);
                }
                catch(Exception e)
                {
                    return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "DataWedge Service: Error, can not get package list from parameters: " + packagesListParam.toString());
                }
            }
        }

        // Add Enterprise Service package to the list
        settings.MainBundle.APP_LIST.put(mContext.getPackageName(), null);


        // The profile is forced to work with intent profile only
        settings.mProfileName = mContext.getPackageName();
        settings.MainBundle.PROFILE_ENABLED = true;
        settings.MainBundle.CONFIG_MODE = MB_E_CONFIG_MODE.CREATE_IF_NOT_EXIST;
        settings.IntentPlugin.intent_action = mContext.getPackageName() + RECEIVER_SUFFIX;
        settings.IntentPlugin.intent_category = INTENT_CATEGORY;
        settings.IntentPlugin.intent_output_enabled = true;
        settings.IntentPlugin.intent_delivery = INT_E_DELIVERY.BROADCAST;
        settings.KeystrokePlugin.keystroke_output_enabled = false;
        // This profile will be associated with

        if(settings.BasicDataFormatting.bdf_enabled != null && settings.BasicDataFormatting.bdf_enabled)
        {
            settings.BasicDataFormatting.bdf_output_plugin = BDF_E_OUTPUT_PLUGIN.INTENT;
        }

        if(settings == null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Could not parse JSON structure:" + postData);
        }
        return setupDWProfile(settings);
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> waitscan(Map<String, List<String>> paramsList) {
        if(mScanJobLatch != null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "DataWedge Service: Error, a scanning job is already running in background. Please wait for it to finish or timeout. Or use the stopwaitingscan REST command to stop it.");
        }

        // We need to retrieve the time out parameter
        long timeout = 15000;
        List<String> timeoutList = paramsList.get("timeout");
        if(timeoutList != null && timeoutList.size() > 0)
        {
            try
            {
                timeout = Long.parseLong(timeoutList.get(0));
            }
            catch(Exception e)
            {
                return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "DataWedge Service: Error, can not interpret timeout parameter, should be a long value: "+timeoutList.toString());
            }
        }

        try
        {
            Looper.prepare();
        }
        catch(Exception e)
        {
        }

        mScanJobLatch = new CountDownLatch(1);

        if(mScanReceiver == null)
        {
            /**
             * Initialize the scan receiver
             */
            mScanReceiver = new DWScanReceiver(mContext,
                    mContext.getPackageName()+ RECEIVER_SUFFIX,
                    INTENT_CATEGORY,
                    false, // Displays special chars between brackets
                    // You can inline the code here (like in this exampleà, or make the current activity
                    // extends the interface DWScanReceiver.onScannedData then pass directly the
                    // "this" reference instead
                    new DWScanReceiver.onScannedData() {
                        @Override
                        public void scannedData(String source, String data, String typology) {
                            mScannedDataSource = source;
                            mScannedData = data;
                            mScannedDataSymbology = typology;
                            if(mScanJobLatch != null && mScanJobLatch.getCount() > 0)
                            {
                                mScanJobLatch.countDown();
                            }
                        }
                    }
            );
        }

        // Launch time out
        if(mScanTimeOutHandler != null)
        {
            mScanTimeOutHandler.removeCallbacks(mScanTimeOutRunnable);
        }
        else
        {
            mScanTimeOutHandler = new Handler();
        }

        if(mScanTimeOutRunnable == null)
        {
            mScanTimeOutRunnable = new Runnable() {
                @Override
                public void run() {
                    mScannedDataSource = "TIMEOUT";
                    mScanJobLatch.countDown();
                }
            };
        }

        mScanTimeOutHandler.postDelayed(mScanTimeOutRunnable, timeout);

        mScanReceiver.startReceive();

        try {
            mScanJobLatch.await();
            mScanJobLatch = null;
            if(mScannedDataSource.equalsIgnoreCase(STOP_SCANNER))
            {
                // Return an empty String
                return new Pair<>(RESTServiceWebServer.EJobStatus.CUSTOM, "");
            }
            // Remove Receiver
            if(mScanReceiver != null)
            {
                mScanReceiver.stopReceive();
                mScanReceiver = null;
            }
            String responseJSON = "{\n \"source\": \""+mScannedDataSource+"\",\n \"data\":\"" + mScannedData + "\",\n \"symbology\":\"" + mScannedDataSymbology + "\"\n}";
            return new Pair<>(RESTServiceWebServer.EJobStatus.CUSTOM, responseJSON);
        } catch (InterruptedException e) {
            if(mScanJobLatch != null)
            {
                while(mScanJobLatch.getCount() > 0)
                    mScanJobLatch.countDown();
                mScanJobLatch = null;
            }
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Waiting Scanner: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    protected Pair<RESTServiceWebServer.EJobStatus, String> stopwaitingscan() {
        if(mScanJobLatch == null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "DataWedge Service: Error, no scan task running in background.");
        }

        try
        {
        if(mScanReceiver != null)
        {
            mScanReceiver.stopReceive();
        }

        mScannedDataSource = STOP_SCANNER;

        while(mScanJobLatch.getCount() > 0)
                mScanJobLatch.countDown();

        return new Pair<>(RESTServiceWebServer.EJobStatus.SUCCEEDED, "Stop waiting scan succeeded.");
        } catch (Exception e) {
            if(mScanJobLatch != null)
            {
                while(mScanJobLatch.getCount() > 0)
                    mScanJobLatch.countDown();
            }
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Stop Waiting Scanner: Exception while terminating countdown latch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> setupDWProfile(final DWProfileSetConfigSettings settings) {
        if(mJobDoneLatch != null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "DataWedge Service: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        if(settings.mEnableTimeOutMechanism)
        {
            try
            {
                Looper.prepare();
            }
            catch(Exception e)
            {
            }
        }

        final DWProfileSetConfig setConfig = new DWProfileSetConfig(mContext);
        final DWProfileCommandBase.onProfileCommandResult onSetConfigCommandResult = new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    mJobReturnMessage = "Setup DataWedge: succeeded for profile:" + settings.mProfileName;
                    mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                    mJobDoneLatch.countDown();
                }
                else
                {
                    mJobReturnMessage = "Setup Scanner: error on profile(" +settings.mProfileName +"):"+ resultInfo;
                    mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                    mJobDoneLatch.countDown();
                }
            }

            @Override
            public void timeout(String profileName) {
                mJobReturnMessage = "Setup Scanner: timeout while trying to setup profile: " + settings.mProfileName;
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                mJobDoneLatch.countDown();
            }
        };

        DWProfileDeleteSettings deleteSettings = new DWProfileDeleteSettings()
        {{
            mProfileName = settings.mProfileName;
            mTimeOutMS = settings.mTimeOutMS;
        }};
        DWProfileDelete.onProfileCommandResult onDeleteCommandResult = new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                // We don't care about the result
                // if it is success, the profile has been deleted
                // if it is a failed, the profile does not exists
                // the only thing we need right now is to setup the configuration
                // using the CREATE_IF_NOT_EXISTS mode
                setConfig.execute(settings, onSetConfigCommandResult);
            }

            @Override
            public void timeout(String profileName) {
                mJobReturnMessage = "Setup Scanner: timeout while trying to delete profile: " + settings.mProfileName;
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                mJobDoneLatch.countDown();
            }
        };
        DWProfileDelete deleteProfile = new DWProfileDelete(mContext);
        deleteProfile.execute(deleteSettings, onDeleteCommandResult);

        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Setup Scanner: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> enablePlugin()
    {
        if(mJobDoneLatch != null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "DataWedge Service: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = mContext.getPackageName();
        }};

        try
        {
            Looper.prepare();
        }
        catch(Exception e)
        {
        }

        DWScannerPluginEnable dwpluginenable = new DWScannerPluginEnable(mContext);
        dwpluginenable.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    mJobReturnMessage = "Enable Scanner: succeeded for profile:" + profileName;
                    mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                    mJobDoneLatch.countDown();
                }
                else
                {
                    mJobReturnMessage = "Enable Scanner: error on profile(" +profileName +"):"+ resultInfo;
                    mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                    mJobDoneLatch.countDown();
                }
            }
            @Override
            public void timeout(String profileName) {
                mJobReturnMessage = "Enable Scanner: timeout while trying to enable scanner on profile: " + profileName;
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                mJobDoneLatch.countDown();
            }
        });

        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Enable Scanner: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> disablePlugin()
    {
        if(mJobDoneLatch != null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "DataWedge Service: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = mContext.getPackageName();
        }};

        try
        {
            Looper.prepare();
        }
        catch(Exception e)
        {
        }

        DWScannerPluginDisable dwplugindisable = new DWScannerPluginDisable(mContext);
        dwplugindisable.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    mJobReturnMessage = "Disable Scanner: succeeded for profile:" + profileName;
                    mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                    mJobDoneLatch.countDown();
                }
                else
                {
                    mJobReturnMessage = "Disable Scanner: error on profile(" +profileName +"):"+ resultInfo;
                    mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                    mJobDoneLatch.countDown();
                }
            }
            @Override
            public void timeout(String profileName) {
                mJobReturnMessage = "Disable Scanner: timeout while trying to enable scanner on profile: " + profileName;
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                mJobDoneLatch.countDown();
            }
        });

        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Disable Scanner: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> startScan()
    {
        if(mJobDoneLatch != null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Start Scan: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = mContext.getPackageName();
        }};

        try
        {
            Looper.prepare();
        }
        catch(Exception e)
        {
        }

        DWScannerStartScan dwstartscan = new DWScannerStartScan(mContext);
        dwstartscan.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    mJobReturnMessage = "Start Scanner: succeeded for profile:" + profileName;
                    mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                    mJobDoneLatch.countDown();
                }
                else
                {
                    mJobReturnMessage = "Start Scanner: error on profile(" +profileName +"):"+ resultInfo;
                    mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                    mJobDoneLatch.countDown();
                }
            }
            @Override
            public void timeout(String profileName) {
                mJobReturnMessage = "Start Scanner: timeout while trying to enable scanner on profile: " + profileName;
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                mJobDoneLatch.countDown();
            }
        });


        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Start Scan: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }

    private Pair<RESTServiceWebServer.EJobStatus, String> stopScan()
    {
        if(mJobDoneLatch != null)
        {
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Stop Scan: Error, a job is already running in background. Please wait for it to finish or timeout.");
        }

        mJobDoneLatch = new CountDownLatch(1);

        DWProfileBaseSettings settings = new DWProfileBaseSettings()
        {{
            mProfileName = mContext.getPackageName();
        }};

        try
        {
            Looper.prepare();
        }
        catch(Exception e)
        {
        }

        DWScannerStopScan dwstopscan = new DWScannerStopScan(mContext);
        dwstopscan.execute(settings, new DWProfileCommandBase.onProfileCommandResult() {
            @Override
            public void result(String profileName, String action, String command, String result, String resultInfo, String commandidentifier) {
                if(result.equalsIgnoreCase(DataWedgeConstants.COMMAND_RESULT_SUCCESS))
                {
                    mJobReturnMessage = "Stop Scanner: succeeded for profile:" + profileName;
                    mJobStatus = RESTServiceWebServer.EJobStatus.SUCCEEDED;
                    mJobDoneLatch.countDown();
                }
                else
                {
                    mJobReturnMessage = "Stop Scanner: error on profile(" +profileName +"):"+ resultInfo;
                    mJobStatus = RESTServiceWebServer.EJobStatus.FAILED;
                    mJobDoneLatch.countDown();
                }
            }
            @Override
            public void timeout(String profileName) {
                mJobReturnMessage = "Stop Scanner: timeout while trying to enable scanner on profile: " + profileName;
                mJobStatus = RESTServiceWebServer.EJobStatus.TIMEOUT;
                mJobDoneLatch.countDown();
            }
        });


        try {
            mJobDoneLatch.await();
            mJobDoneLatch = null;
            return new Pair<>(mJobStatus, mJobReturnMessage);
        } catch (InterruptedException e) {
            if(mJobDoneLatch != null)
            {
                while(mJobDoneLatch.getCount() > 0)
                    mJobDoneLatch.countDown();
                mJobDoneLatch = null;
            }
            return new Pair<>(RESTServiceWebServer.EJobStatus.FAILED, "Stop Scan: Exception while waiting for CountDownLatch : " + e.getMessage());
        }
    }


}
