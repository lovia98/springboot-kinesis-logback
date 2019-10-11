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
                .withRegion(Regions.AP_NORTHEAST_2)
                .withCredentials(new ProfileCredentialsProvider());

        return new AmazonKinesisAsyncClient(credentials, configuration, executor);
    }

    @Override
    protected void validateStreamName(AmazonKinesisAsyncClient client, String streamName) {
        DescribeStreamResult describeResult;
        try {
            describeResult = getClient().describeStream(streamName);
            String streamStatus = describeResult.getStreamDescription().getStreamStatus();
            if (!StreamStatus.ACTIVE.name().equals(streamStatus) && !StreamStatus.UPDATING.name().equals(streamStatus)) {
                setInitializationFailed(true);
                addError("Stream " + streamName + " is not ready (in active/updating status) for appender: " + name);
            }
        }
        catch(ResourceNotFoundException rnfe) {
            setInitializationFailed(true);
            addError("Stream " + streamName + " doesn't exist for appender: " + name, rnfe);
        }
        catch(AmazonServiceException ase) {
            setInitializationFailed(true);
            addError("Error connecting to AWS to verify stream " + streamName + " for appender: " + name, ase);
        }
    }

    @Override
    protected void putMessage(String message) throws Exception {
        ByteBuffer data = ByteBuffer.wrap(message.getBytes(getEncoding()));
        getClient().putRecordAsync(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
                .withStreamName(getStreamName()).withData(data), asyncCallHandler);
    }

}
