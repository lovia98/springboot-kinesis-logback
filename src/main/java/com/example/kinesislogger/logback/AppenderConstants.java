package com.example.kinesislogger.logback;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Regions;

public class AppenderConstants {

    public static final String USER_AGENT_STRING = "juhee-kinesis-logback-appender/1.0.0";
    // Default values
    public static final String DEFAULT_ENCODING = "UTF-8";
    public static final int DEFAULT_MAX_RETRY_COUNT = 3;
    public static final int DEFAULT_BUFFER_SIZE = 2000;
    public static final int DEFAULT_THREAD_COUNT = 20;
    public static final int DEFAULT_SHUTDOWN_TIMEOUT_SEC = 30;
    public static final int DEFAULT_THREAD_KEEP_ALIVE_SEC = 30;
    public static final String DEFAULT_REGION = Regions.AP_NORTHEAST_2.getName();
    public static final String DEFAULT_SERVICE_NAME = "lg-kinesis-logging";

}
