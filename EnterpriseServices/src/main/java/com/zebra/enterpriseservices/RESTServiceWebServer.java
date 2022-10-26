package com.zebra.enterpriseservices;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class RESTServiceWebServer extends NanoHTTPD {

    private Context mContext = null;
    private RESTHostServiceWifiStateObserver mIPChangeObserver = null;
    private String mCurrentIP = "";
    private boolean mStopServing = false;

    protected enum EJobStatus
    {
        SUCCEEDED("SUCCEEDED"),
        FAILED("FAILED"),
        WORKING("WORKING"),
        TIMEOUT("TIMEOUT"),
        CUSTOM("CUSTOM");

        private String enumString;
        EJobStatus(String confName)
        {
            this.enumString = confName;
        }

        @Override
        public String toString()
        {
            return enumString;
        }

    }

    protected static boolean mAllowExternalIPs = false;

    private RESTServicePrintEndPoint mPrintEndPoint;
    private RESTServiceScanEndPoint mScanEndPoint;

    public RESTServiceWebServer(int port, Context context) {
        super(port);
        LogHelper.logD("RESTServiceWebServer::RESTServiceWebServer Creation on port: " + String.valueOf(port));
        mContext = context;
        SharedPreferences sharedpreferences = context.getSharedPreferences(RESTHostServiceConstants.SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mAllowExternalIPs = sharedpreferences.getBoolean(RESTHostServiceConstants.SHARED_PREFERENCES_ALLOW_EXTERNAL_IPs, false);
        mPrintEndPoint = new RESTServicePrintEndPoint(context);
        mScanEndPoint = new RESTServiceScanEndPoint(context);
    }

    @Override
    public void start() throws IOException {
        super.start();
        LogHelper.logD("RESTServiceWebServer::start");
        if(mIPChangeObserver == null)
        {
            LogHelper.logD("RESTServiceWebServer::start: Creating IP Change Observer");
            // We launch the observer but we do not need to be notified here if the IP change
            mIPChangeObserver = new RESTHostServiceWifiStateObserver(mContext, new RESTHostServiceWifiStateObserver.IIPChangeObserver() {
                @Override
                public void onIPChanged(String newIP) {
                    if(newIP.equalsIgnoreCase("0.0.0.0"))
                    {
                        // This means we are actually getting a new IP but the IP resolution is not finished
                        // We block any request from the webpage
                        mStopServing = true;
                        mCurrentIP = "0.0.0.0";
                    }
                    if(newIP.equalsIgnoreCase("0.0.0.0") == false && newIP.equalsIgnoreCase(mCurrentIP) == false) {
                        // If the IP has changed, we need to restart the server and unstop the service if necessary
                        mStopServing = true;
                        try {
                            mCurrentIP = newIP;
                            //Thread.sleep(2000);
                            mStopServing = false;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            LogHelper.logD("RESTServiceWebServer::start: Starting IP Change Observer");
            mIPChangeObserver.startObserver();
        }
        else if(mIPChangeObserver.isStarted() == false)
        {
            LogHelper.logD("RESTServiceWebServer::start: IP Change Observer already exists but not started. Starting it.");
            mIPChangeObserver.startObserver();
        }
        else
        {
            LogHelper.logD("RESTServiceWebServer::start: IP Change Observer already exists and is started.");
        }
        mStopServing = false;
    }

    @Override
    public void stop() {
        mStopServing = true;
        if(mIPChangeObserver != null && mIPChangeObserver.isStarted())
        {
            LogHelper.logD("RESTServiceWebServer::stop: Stopping IP Change Observer");
            mIPChangeObserver.stopObserver();
        }
        mIPChangeObserver = null;
        super.stop();
    }

    @Override
    public Response serve(IHTTPSession session) {
        LogHelper.logD("RESTServiceWebServer::serve: Serving session: \n"+ session.toString());
        String responseJSON = "";
        if(mStopServing)
        {
            LogHelper.logD("RESTServiceWebServer::serve: Stop serving is true, returning empty json string");
            // We are stopped, so we don't accept any requests from the webpage, and return an empty json string
            // Something happen and we did something, so the response will allways be OK from the serve method perspective
            // Other kind of responses (timeout, etc...) are managed by the REST server itself.
            Response resp =  newFixedLengthResponse(Response.Status.OK, "application/json", responseJSON);
            // We need to add CORS header to the response to allow Cross Origin Resource Sharing
            resp = addCORSHeaders(session.getHeaders(), resp, "*");
            return resp;
        }
        // Check if the remote IP is equal to the device IP
        if(session.getRemoteHostName().equalsIgnoreCase("localhost") == false && session.getRemoteIpAddress().equalsIgnoreCase("127.0.0.1") == false && mAllowExternalIPs == false && session.getRemoteIpAddress().equalsIgnoreCase(mIPChangeObserver.getIPAddress()) == false)
        {
            LogHelper.logE("RESTServiceWebServer::serve: Error, accessing the REST service from external IP has been blocked.");
            responseJSON= "{\n \"result\": \"error\",\n \"message\":\"Accessing REST service from external IP has been blocked. Only localhost call are permitted. Check configuration if you want to allow external IP addresses to call this web service.\"\n}";
        }
        else
        {
            Pair<EJobStatus, String> processParamsReturned = null;

            // Extract path from URI to retrieve command namespace or path
            // i.e. printconnect, or datawedge
            String path = "";
            try {
                URI uri = new URI(session.getUri());
                LogHelper.logV("RESTServiceWebServer::serve: URI: " + uri.toString());
                String splitted[] = uri.getPath().split("/");
                if(splitted.length > 1)
                {
                    // we only need the first argument in the path
                    path = splitted[1];
                }
                else
                {
                    path = "";
                }
            } catch (URISyntaxException e) {
                path = "";
                e.printStackTrace();
            }
            LogHelper.logV("RESTServiceWebServer::serve: Path: " + path);

            if(path.length() > 0)
            {
                switch(path)
                {
                    case "printconnect":
                        LogHelper.logD("RESTServiceWebServer::serve: Serving PrintConnect call.");
                        processParamsReturned = mPrintEndPoint.processSession(session);
                        break;
                    case "datawedge":
                        LogHelper.logD("RESTServiceWebServer::serve: Serving Datawedge call.");
                        processParamsReturned = mScanEndPoint.processSession(session);
                        break;
                    default:
                        processParamsReturned = null;
                }

                if(processParamsReturned != null)
                {
                    switch(processParamsReturned.first)
                    {
                        case SUCCEEDED:
                            LogHelper.logD("RESTServiceWebServer::serve: call returned Succeeded");
                            responseJSON= "{\n \"result\": \"succeeded\" \n}";
                            break;
                        case FAILED:
                            LogHelper.logD("RESTServiceWebServer::serve: call returned Error: " + processParamsReturned.second);
                            responseJSON= "{\n \"result\": \"error\",\n \"message\":\"" + processParamsReturned.second + "\"\n}";
                            break;
                        case TIMEOUT:
                            LogHelper.logD("RESTServiceWebServer::serve: call returned Timeout: " + processParamsReturned.second);
                            responseJSON= "{\n \"result\": \"timeout\",\n \"message\":\"" + processParamsReturned.second + "\"\n}";
                            break;
                        case CUSTOM:
                            LogHelper.logD("RESTServiceWebServer::serve: call returned Custom message: " + processParamsReturned.second);
                            responseJSON = processParamsReturned.second;
                            break;
                    }
                }
                else
                {
                    LogHelper.logE("RESTServiceWebServer::serve: Error message path not recognized: " + path);
                    responseJSON= "{\n \"result\": \"error\",\n \"message\":\"Path:" + path + " not recognized.\"\n}";
                }
            }
            else
            {
                LogHelper.logE("RESTServiceWebServer::serve: Error path malformed: " + path);
                responseJSON= "{\n \"result\": \"error\",\n \"message\":\"Path:" + path + " malformed.\"\n}";
            }
        }
        // Something happen and we did something, so the response will allways be OK from the serve method perspective
        // Other kind of responses (timeout, etc...) are managed by the REST server itself.
        Response resp =  newFixedLengthResponse(Response.Status.OK, "application/json", responseJSON);
        LogHelper.logD("RESTServiceWebServer::serve: JSON Response: " + responseJSON);

        // We need to add CORS header to the response to allow Cross Origin Resource Sharing
        resp = addCORSHeaders(session.getHeaders(), resp, "*");

        return resp;
    }


    protected Response addCORSHeaders(Map<String, String> queryHeaders, Response resp, String cors) {
        resp.addHeader("Access-Control-Allow-Origin", cors);
        resp.addHeader("Access-Control-Allow-Headers", calculateAllowHeaders(queryHeaders));
        resp.addHeader("Access-Control-Allow-Credentials", "true");
        resp.addHeader("Access-Control-Allow-Methods", RESTHostServiceConstants.PRINT_SERVER_CORS_ALLOWED_METHODS);
        resp.addHeader("Access-Control-Max-Age", "" + RESTHostServiceConstants.PRINT_SERVER_CORS_MAX_AGE);

        return resp;
    }

    private String calculateAllowHeaders(Map<String, String> queryHeaders) {
        // here we should use the given asked headers
        // but NanoHttpd uses a Map whereas it is possible for requester to send
        // several time the same header
        // let's just use default values for this version
        return System.getProperty(RESTHostServiceConstants.PRINT_SERVER_ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME, RESTHostServiceConstants.PRINT_SERVER_DEFAULT_ALLOWED_HEADERS);
    }
}
