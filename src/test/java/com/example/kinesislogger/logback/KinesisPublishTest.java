package com.example.kinesislogger.logback;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.example.kinesislogger.logback.helpers.BlockFastProducerPolicy;
import com.example.kinesislogger.logback.helpers.CustomClasspathPropertiesFileCredentialsProvider;
import com.example.kinesislogger.logback.helpers.NamedThreadFactory;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Java6Assertions.assertThat;


/**
 * 키네시스 전송 테스트
 *
 * @author 한주희
 */
public class KinesisPublishTest {

    private final String streamName = "log_stream_test";
    private final String regionName = "ap-northeast-2";
    private final String access_key_id = "access_key_id";
    private final String secret_key_id = "secret_key_id";
    private int threadCount = AppenderConstants.DEFAULT_THREAD_COUNT;
    private int bufferSize = AppenderConstants.DEFAULT_BUFFER_SIZE;


    /**
     * 동기 전송 테스트
     *
     * @throws Exception
     */
    @Test
    public void kinesis_test() throws Exception {

        validationRegion(regionName);

        //클라이언트 생성
        AmazonKinesis kinesisClient = getAmazonKinesis();

        validateStream(kinesisClient, streamName);

        PutRecordResult putRecordResult = putMessage("message", kinesisClient, streamName);

        assertThat(putRecordResult.getSequenceNumber()).isNotEmpty();
    }

    /**
     * 비동기 테스트
     *
     * @throws Exception
     */
    @Test
    public void kinesis_async_test() throws Exception {

        validationRegion(regionName);

        //비동기 클라이언트 생성
        AmazonKinesisAsync kinesisClient = getAmazonKinesisAsyncClient();

        validateStream(kinesisClient, streamName);

        PutRecordResult putRecordResult = putAsyncMessage("message", kinesisClient, streamName);

        assertThat(putRecordResult.getSequenceNumber()).isNotEmpty();
    }

    private AmazonKinesisAsync getAmazonKinesisAsyncClient() {

        BlockingQueue<Runnable> taskBuffer = new LinkedBlockingDeque<>(bufferSize);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(access_key_id, secret_key_id);

        AmazonKinesisAsyncClientBuilder clientBuilder = AmazonKinesisAsyncClientBuilder.standard();

        clientBuilder.setRegion(regionName); // 리전
        clientBuilder.setCredentials(new AWSStaticCredentialsProvider(awsCreds)); //인증정보
        clientBuilder.setClientConfiguration(getClientConfiguration().withConnectionTimeout(1000).withMaxErrorRetry(0)); //client환경 설정
        clientBuilder.setExecutorFactory(() -> new ThreadPoolExecutor(threadCount, threadCount,
                AppenderConstants.DEFAULT_THREAD_KEEP_ALIVE_SEC, TimeUnit.SECONDS,
                taskBuffer, setupThreadFactory(), new BlockFastProducerPolicy()));

        return clientBuilder.build();
    }

    private AmazonKinesis getAmazonKinesis() {

        BasicAWSCredentials awsCreds = new BasicAWSCredentials(access_key_id, secret_key_id);

        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard();
        clientBuilder.setRegion(regionName); // 리전
        clientBuilder.setCredentials(new AWSStaticCredentialsProvider(awsCreds)); //인증정보
        clientBuilder.setClientConfiguration(getClientConfiguration()); //client환경 설정
        return clientBuilder.build();
    }

    private ClientConfiguration getClientConfiguration() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setUserAgentPrefix(AppenderConstants.USER_AGENT_STRING);
        return clientConfiguration;
    }

    /**
     * 동기 메시지 전송
     *
     * @param message
     * @param kinesisClient
     * @param streamName
     * @throws UnsupportedEncodingException
     */
    private PutRecordResult putMessage(String message, AmazonKinesis kinesisClient, String streamName) throws UnsupportedEncodingException {

        ByteBuffer data = ByteBuffer.wrap(message.getBytes("UTF-8"));

        return kinesisClient.putRecord(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
                .withStreamName(streamName).withData(data));
    }

    /**
     * 비동기 메시지 전송
     *
     * @param message
     * @param kinesisClient
     * @param streamName
     * @return
     * @throws Exception
     */
    private PutRecordResult putAsyncMessage(String message, AmazonKinesisAsync kinesisClient, String streamName) throws Exception {

        ByteBuffer data = ByteBuffer.wrap(message.getBytes("UTF-8"));

        Future<PutRecordResult> putRecordResultFuture = kinesisClient.putRecordAsync(new PutRecordRequest().withPartitionKey(UUID.randomUUID().toString())
                .withStreamName(streamName).withData(data));

        return putRecordResultFuture.get();
    }

    private void validationRegion(String regionName) throws Exception {

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

    private ThreadFactory setupThreadFactory() {
        return new NamedThreadFactory(getClass().getSimpleName() + "[" + streamName + "]-");
    }

}
