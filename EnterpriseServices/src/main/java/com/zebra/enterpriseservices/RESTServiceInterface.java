package com.zebra.enterpriseservices;

import android.content.Context;
import android.util.Pair;

import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public interface RESTServiceInterface {
    Pair<RESTServiceWebServer.EJobStatus, String> processSession(NanoHTTPD.IHTTPSession session);
}
