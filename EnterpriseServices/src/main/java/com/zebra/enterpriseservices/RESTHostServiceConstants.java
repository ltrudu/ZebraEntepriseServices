package com.zebra.enterpriseservices;

public class RESTHostServiceConstants {
    protected static final String TAG  ="RESTHostService";

    // Shared preference keys
    protected static final String SHARED_PREFERENCES_NAME = "RESTHostService";
    protected static final String SHARED_PREFERENCES_START_SERVICE_ON_BOOT = "startonboot";
    protected static final String SHARED_PREFERENCES_ALLOW_EXTERNAL_IPs = "allowexternalips";

    // Intent extras keys
    protected static final String EXTRA_CONFIGURATION_START_ON_BOOT = "startonboot";
    protected static final String EXTRA_CONFIGURATION_ALLOW_EXTERNAL_IPs = "allowexternalips";

    // Server configuration
    protected static final int PRINT_SERVER_PORT = 8080;
    protected static final String PRINT_SERVER_CORS_ALLOWED_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";
    protected static final int PRINT_SERVER_CORS_MAX_AGE = 42 * 60 * 60;
    protected static final String PRINT_SERVER_DEFAULT_ALLOWED_HEADERS = "origin,accept,content-type";
    protected static final String PRINT_SERVER_ACCESS_CONTROL_ALLOW_HEADER_PROPERTY_NAME = "AccessControlAllowHeader";
}
