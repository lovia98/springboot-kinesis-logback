package com.example.kinesislogger.logback;

import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.amazonaws.services.kinesis.model.StreamStatus;
import com.example.kinesislogger.logback.helpers.KinesisStatsReporter;

import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.ThreadPoolExecutor;

public class KinesisAppender<Event extends DeferredProcessingAware>
        extends BaseKinesisAppender<Event, AmazonKinesisAsyncClient> {

    private KinesisStatsReporter asyncCallHandler = new KinesisStatsReporter(this);

    @Override
    protected AmazonKinesisAsyncClient createClient(AWSCredentialsProvider credentials, ClientConfiguration configuration,
                                                    ThreadPoolExecutor executor) {

        AmazonKinesisAsyncClientBuilder.standard()
                .withRegion(getRegion())
                .withCredentials(new ProfileCredentialsProvider());

        return new AmazonKinesisAsyncClient(credentials, configuration, executor);
    }

    @Override
    protected void validateStreamName(AmazonKinesisAsyncClient client, String streamName) {

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

    @Override
    protected void putMessage(String message) throws Exception {
        ByteBuffer data = ByteBuffer.wrap(message.getBytes(getEncoding()));
        getClient().putRecordAsync(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
                .withStreamName(getStreamName()).withData(data), asyncCallHandler);
    }

}
