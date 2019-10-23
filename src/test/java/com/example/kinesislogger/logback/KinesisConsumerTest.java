package com.example.kinesislogger.logback;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.*;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 키네시스 컨슈머 테스트
 *
 * @author 한주희
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource("classpath:logback.properties")
public class KinesisConsumerTest {

    @Value("${aws.kinesis.streamName}")
    private String streamName;

    @Value("${aws.kinesis.region}")
    private String regionName;

    @Value("${aws.kinesis.accessKey}")
    private String access_key_id;

    @Value("${aws.kinesis.secretKey}")
    private String secret_key_id;

    private AmazonKinesis client;

    @Before
    public void setUp() {

        this.client = AmazonKinesisClientBuilder.standard()
                .withRegion(regionName)
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(access_key_id, secret_key_id)))
                .build();
    }

    @Test
    public void SDK_API로_메세지_가져오기_테스트() {

        String shardIterator;
        GetShardIteratorRequest getShardIteratorRequest = new GetShardIteratorRequest();
        getShardIteratorRequest.setStreamName(streamName);
        getShardIteratorRequest.setShardId("0");
        getShardIteratorRequest.setShardIteratorType(ShardIteratorType.TRIM_HORIZON.name());

        GetShardIteratorResult getShardIteratorResult = client.getShardIterator(getShardIteratorRequest);
        shardIterator = getShardIteratorResult.getShardIterator();


        List<Record> records;
        while (shardIterator != null) {

            // Create a new getRecordsRequest with an existing shardIterator
            // Set the maximum records to return to 25
            GetRecordsRequest getRecordsRequest = new GetRecordsRequest();
            getRecordsRequest.setShardIterator(shardIterator);
            getRecordsRequest.setLimit(2);

            GetRecordsResult result = client.getRecords(getRecordsRequest);

            //result를 record 리스트에 넣음.
            records = result.getRecords();

            for (Record record :records) {

                String logMessage = new String( record.getData().array(), StandardCharsets.UTF_8 );
                System.out.println(record.getSequenceNumber() + " : " + logMessage);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException exception) {
                throw new RuntimeException(exception);
            }

            shardIterator = result.getNextShardIterator();
        }
    }


}
