package com.example.kinesislogger;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.example.kinesislogger.logback.AppenderConstants;
import com.example.kinesislogger.logback.helpers.CustomClasspathPropertiesFileCredentialsProvider;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class KinesisPublishTest {

    private final String streamName = "log_stream_test";
    private final String regionName = "ap-northeast-2";

    @Test
    public void kinesis_test() throws Exception {

        //리전값 체크
        checkRegion(regionName);

        //클라이언트 생성
        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard();
        clientBuilder.setRegion(regionName); // 리전
        clientBuilder.setCredentials(new CustomClasspathPropertiesFileCredentialsProvider()); //인증정보
        clientBuilder.setClientConfiguration(getClientConfiguration()); //client환경 설정
        AmazonKinesis kinesisClient = clientBuilder.build();

        //Validate that the stream exists and is active
        validateStream(kinesisClient, streamName);

        putMessage("message", kinesisClient, streamName);
    }

    private ClientConfiguration getClientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setUserAgentPrefix(AppenderConstants.USER_AGENT_STRING);
        return clientConfiguration;
    }

    private void putMessage(String message, AmazonKinesis kinesisClient, String streamName) throws UnsupportedEncodingException {
        ByteBuffer data = ByteBuffer.wrap(message.getBytes("UTF-8"));
        kinesisClient.putRecord(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
                .withStreamName(streamName).withData(data));
    }

    private void checkRegion(String regionName) throws Exception {

        Region region = RegionUtils.getRegion(regionName);
        if (region == null) {
            System.err.println(regionName + " is not a valid AWS region.");
            throw new Exception(regionName + " is not a valid AWS region.");
        }
    }

    private static void validateStream(AmazonKinesis kinesisClient, String streamName) throws Exception {
        try {
            DescribeStreamResult result = kinesisClient.describeStream(streamName);
            if (!"ACTIVE".equals(result.getStreamDescription().getStreamStatus())) {
                System.err.println("Stream " + streamName + " is not active. Please wait a few moments and try again.");
                throw new Exception("Stream " + streamName + " is not active. Please wait a few moments and try again.");
            }
        } catch (ResourceNotFoundException e) {
            System.err.println("Stream " + streamName + " does not exist. Please create it in the console.");
            System.err.println(e);

            throw new Exception("Stream " + streamName + " does not exist. Please create it in the console.");
        } catch (Exception e) {
            System.err.println("Error found while describing the stream " + streamName);
            System.err.println(e);

            throw new Exception("Error found while describing the stream " + streamName);
        }
    }
}
