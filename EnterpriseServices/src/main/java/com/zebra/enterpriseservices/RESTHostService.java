package com.zebra.enterpriseservices;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static androidx.core.app.NotificationCompat.PRIORITY_MIN;
import static com.zebra.enterpriseservices.RESTHostServiceConstants.PRINT_SERVER_PORT;

public class RESTHostService extends Service {
    private static final int SERVICE_ID = 2545;

    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private RESTServiceWebServer mRESTServer;

    public RESTHostService() {
    }

    public IBinder onBind(Intent paramIntent)
    {
        return null;
    }

    public void onCreate()
    {
        LogHelper.logD("RESTHostService::onCreate");
        this.mNotificationManager = ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE));
        startService();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogHelper.logD("RESTHostService::onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return Service.START_STICKY;
    }

    public void onDestroy()
    {
        LogHelper.logD("RESTHostService::onDestroy");
        stopService();
    }

    @SuppressLint({"Wakelock"})
    private void startService()
    {
        LogHelper.logD("RESTHostService::startService");
        try
        {
            Intent mainActivityIntent = new Intent(this, RESTHostServiceActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    getApplicationContext(),
                    0,
                    mainActivityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            // Create the Foreground Service
            String channelId = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? createNotificationChannel(mNotificationManager) : "";

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId);
            mNotification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.zebra_enterprise_services_notification_title))
                    .setContentText(getString(R.string.zebra_enterprise_services_notification_text))
                    .setTicker(getString(R.string.zebra_enterprise_services_notification_tickle))
                    .setPriority(PRIORITY_MIN)
                    .setCategory(NotificationCompat.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .build();

            TaskStackBuilder localTaskStackBuilder = TaskStackBuilder.create(this);
            localTaskStackBuilder.addParentStack(RESTHostServiceActivity.class);
            localTaskStackBuilder.addNextIntent(mainActivityIntent);
            notificationBuilder.setContentIntent(localTaskStackBuilder.getPendingIntent(0, FLAG_UPDATE_CURRENT));

            LogHelper.logD("RESTHostService::startService::startForeground");
            // Start foreground service
            startForeground(SERVICE_ID, mNotification);

            // Launch web server here
            if(mRESTServer != null)
            {
                mRESTServer.closeAllConnections();
                mRESTServer.stop();
                mRESTServer = null;
            }

            LogHelper.logD("RESTHostService::startService::start REST Server");
            mRESTServer = new RESTServiceWebServer(PRINT_SERVER_PORT, getBaseContext());
            mRESTServer.start();

            LogHelper.logD("RESTHostService::startService:Service started without error.");
        }
        catch(Exception e)
        {
            LogHelper.logD("RESTHostService::startService:Error while starting service.");
            LogHelper.logD(e.getMessage());
            e.printStackTrace();
        }


    }

    private void stopService()
    {
        try
        {
            LogHelper.logD("RESTHostService::stopService.");

            // Release web server here
            if(mRESTServer != null)
            {
                mRESTServer.closeAllConnections();
                mRESTServer.stop();
                mRESTServer = null;
            }
            
            stopForeground(true);
            LogHelper.logD("RESTHostService::stopService:Service stopped without error.");
        }
        catch(Exception e)
        {
            LogHelper.logD("RESTHostService::Error while stopping service.");
            LogHelper.logD(e.getMessage());
            e.printStackTrace();

        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private String createNotificationChannel(NotificationManager notificationManager){
        NotificationChannel channel = new NotificationChannel(getString(R.string.zebra_enterprise_services_channel_id), getString(R.string.zebra_enterprise_services_channel_name), NotificationManager.IMPORTANCE_HIGH);
        // omitted the LED color
        channel.setImportance(NotificationManager.IMPORTANCE_NONE);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        notificationManager.createNotificationChannel(channel);
        return getString(R.string.zebra_enterprise_services_channel_id);
    }

    public static void startService(Context context)
    {
        Intent myIntent = new Intent(context, RESTHostService.class);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            LogHelper.logD("RESTHostService::startService Starting for build >= Android O.");

            // Use start foreground service to prevent the runtime error:
            // "not allowed to start service intent app is in background"
            // to happen when running on OS >= Oreo
            context.startForegroundService(myIntent);
        }
        else
        {
            LogHelper.logD("RESTHostService::startService Starting for build < Android O.");
            context.startService(myIntent);
        }
    }

    public static void stopService(Context context)
    {
        LogHelper.logD("RESTHostService::stopService Stopping service");
        Intent myIntent = new Intent(context, RESTHostService.class);
        context.stopService(myIntent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RESTHostService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
