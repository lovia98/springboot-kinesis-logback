package com.example.kinesislogger.logback.helpers;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.example.kinesislogger.logback.KinesisAppender;

public class KinesisStatsReporter implements AsyncHandler<PutRecordRequest, PutRecordResult> {

    private final String appenderName;
    private final KinesisAppender<?> appender;
    private long successfulRequestCount;
    private long failedRequestCount;

    public KinesisStatsReporter(KinesisAppender<?> appender) {
        this.appenderName = appender.getStreamName();
        this.appender = appender;
    }

    /**
     * This method is invoked when there is an exception in sending a log record
     * to Kinesis. These logs would end up in the application log if configured
     * properly.
     */
    @Override
    public void onError(Exception exception) {
        failedRequestCount++;
        appender.addError("Failed to publish a log entry to kinesis using appender: " + appenderName, exception);
    }

    /**
     * This method is invoked when a log record is successfully sent to Kinesis.
     * Though this is not too useful for production use cases, it provides a good
     * debugging tool while tweaking parameters for the appender.
     */
    @Override
    public void onSuccess(PutRecordRequest request, PutRecordResult result) {
        successfulRequestCount++;
    }
}
