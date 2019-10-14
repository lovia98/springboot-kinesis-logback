package com.example.kinesislogger.logback.helpers;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.example.kinesislogger.logback.KinesisAsyncAppender;

/**
 *  비동기 핸들러
 *  - 비동기 callback 처리를 위함.
 *  - 성공, 실패에 대한 응답 처리
 *
 * @author 한주희
 */
public class KinesisStatsReporter implements AsyncHandler<PutRecordRequest, PutRecordResult> {

    private final String appenderName;
    private final KinesisAsyncAppender<?> appender;
    private long successfulRequestCount;
    private long failedRequestCount;

    public KinesisStatsReporter(KinesisAsyncAppender<?> appender) {
        this.appenderName = appender.getStreamName();
        this.appender = appender;
    }

    /**
     * kinesis 전송 오류시 호출
     *
     */
    @Override
    public void onError(Exception exception) {
        this.failedRequestCount++;
        appender.addError("Failed to publish a log entry to kinesis using appender: " + appenderName, exception);
    }

    /**
     * kinesis 전송 성공 callback
     *
     *  prod환경에서는 유용하지 않음. 디버깅용
     */
    @Override
    public void onSuccess(PutRecordRequest request, PutRecordResult result) {

        this.successfulRequestCount++;
    }
}
