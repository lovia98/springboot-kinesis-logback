package com.example.kinesislogger.logback;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.ExecutorFactory;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.example.kinesislogger.logback.helpers.KinesisStatsReporter;

import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * Kinesis 비동기 Appender
 *
 * @param <Event>
 */
public class KinesisAsyncAppender<Event extends DeferredProcessingAware>
        extends BaseKinesisAppender<Event, AmazonKinesisAsync> {

    private KinesisStatsReporter asyncCallHandler = new KinesisStatsReporter(this);

    /**
     * kinesis 비동기 client 생성
     *
     * @param credential
     * @param configuration
     * @param threadFactory
     * @return
     */
    @Override
    protected AmazonKinesisAsync createClient(AWSCredentialsProvider credential, ClientConfiguration configuration,
                                              ExecutorFactory threadFactory) {

        return AmazonKinesisAsyncClientBuilder.standard()
                .withRegion(getRegion())
                .withCredentials(credential)
                .withClientConfiguration(configuration)
                .withExecutorFactory(threadFactory)
                .build();
    }

    /**
     * stream 체크
     *
     * @param client
     * @param streamName
     */
    @Override
    protected void validateStreamName(AmazonKinesis client, String streamName) {

        try {
            DescribeStreamResult result = client.describeStream(streamName);
            if (!"ACTIVE".equals(result.getStreamDescription().getStreamStatus())) {
                addError("Stream " + streamName + " is not active. Please wait a few moments and try again.");
            }
        } catch (ResourceNotFoundException e) {
            addError("Stream " + streamName + " does not exist. Please create it in the console.");
        } catch (Exception e) {
            addError("Error found while describing the stream " + streamName);
        }
    }

    /**
     * message 전송
     *
     * @param message
     * @throws Exception
     */
    @Override
    protected void putMessage(String message) throws Exception {
        ByteBuffer data = ByteBuffer.wrap(message.getBytes(getEncoding()));

        //callback 을 넣으면 비동기처리가 될지 확인 필요
//        ((AmazonKinesisAsync) getClient()).putRecordAsync(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
//                .withStreamName(getStreamName()).withData(data), asyncCallHandler);

        AmazonKinesisAsync client = (AmazonKinesisAsync) getClient();
        client.putRecordAsync(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
                .withStreamName(getStreamName()).withData(data));
    }

}
