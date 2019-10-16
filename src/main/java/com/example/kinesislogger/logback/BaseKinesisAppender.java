package com.example.kinesislogger.logback;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.ExecutorFactory;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.example.kinesislogger.logback.helpers.BlockFastProducerPolicy;
import com.example.kinesislogger.logback.helpers.NamedThreadFactory;
import com.example.kinesislogger.logback.helpers.Validator;

import java.util.concurrent.*;

public abstract class BaseKinesisAppender<Event extends DeferredProcessingAware, Client extends AmazonKinesis>
        extends AppenderBase<Event> {

    private String encoding = AppenderConstants.DEFAULT_ENCODING;
    private int maxRetries = AppenderConstants.DEFAULT_MAX_RETRY_COUNT;
    private int bufferSize = AppenderConstants.DEFAULT_BUFFER_SIZE;
    private int threadCount = AppenderConstants.DEFAULT_THREAD_COUNT;
    private int shutdownTimeout = AppenderConstants.DEFAULT_SHUTDOWN_TIMEOUT_SEC;

    private String accessKey;
    private String secretKey;

    private String region;
    private String streamName;

    private boolean initializationFailed = false;
    private LayoutBase<Event> layout;
    private Client client;
    private AWSCredentialsProvider credentials;


    /**
     * 로깅 start
     */
    @Override
    public void start() {

        if (isLayoutIsnull() || isStreamName()) {
            return;
        }

        validationRegion(region);

        //credentials
        credentials = new AWSStaticCredentialsProvider(new BasicAWSCredentials(getAccessKey(), getSecretKey()));

        //create kinesis client
        createConfigAndClient();

        //kinesis stream 체크
        validateStreamName(client, streamName);

        super.start();
    }

    /**
     * 로깅 종료
     */
    @Override
    public void stop() {
        client.shutdown();
    }

    /**
     * 로그 append
     *
     * @param logEvent
     */
    @Override
    protected void append(Event logEvent) {
        if (initializationFailed) {
            addError("Check the configuration and whether the configured stream " + streamName
                    + " exists and is active. Failed to initialize kinesis logback appender: " + name);
            return;
        }
        try {

            String message = this.layout.doLayout(logEvent);
            putMessage(message);

        } catch (Exception e) {
            addError("Failed to schedule log entry for publishing into Kinesis stream: " + streamName, e);
        }
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public LayoutBase<Event> getLayout() {
        return layout;
    }

    public void setLayout(LayoutBase<Event> layout) {
        this.layout = layout;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region == null ? AppenderConstants.DEFAULT_REGION : region;
    }

    /**
     * Returns configured stream name
     *
     * @return configured stream name
     */
    public String getStreamName() {
        return streamName;
    }

    /**
     * Configured encoding for the data to be published. If none specified,
     * default is UTF-8
     *
     * @return encoding for the data to be published. If none specified, default
     * is UTF-8
     */
    public String getEncoding() {
        return this.encoding;
    }

    /**
     * Sets encoding for the data to be published. If none specified, default is
     * UTF-8
     *
     * @param charset encoding for expected log messages
     */
    public void setEncoding(String charset) {
        Validator.validate(!Validator.isBlank(charset), "encoding cannot be blank");
        this.encoding = charset.trim();
    }

    /**
     * Returns configured maximum number of retries between API failures while
     * communicating with Kinesis. This is used in AWS SDK's default retries for
     * HTTP exceptions, throttling errors etc.
     *
     * @return configured maximum number of retries between API failures while
     * communicating with Kinesis
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Configures maximum number of retries between API failures while
     * communicating with Kinesis. This is used in AWS SDK's default retries for
     * HTTP exceptions, throttling errors etc.
     *
     * @param maxRetries the number of retries between API failures
     */
    public void setMaxRetries(int maxRetries) {
        Validator.validate(maxRetries > 0, "maxRetries must be > 0");
        this.maxRetries = maxRetries;
    }

    /**
     * Returns configured buffer size for this appender. This implementation would
     * buffer these many log events in memory while parallel threads are trying to
     * publish them to Kinesis.
     *
     * @return configured buffer size for this appender.
     */
    public int getBufferSize() {
        return bufferSize;
    }

    /**
     * Configures buffer size for this appender. This implementation would buffer
     * these many log events in memory while parallel threads are trying to
     * publish them to Kinesis.
     *
     * @param bufferSize buffer size for this appender
     */
    public void setBufferSize(int bufferSize) {
        Validator.validate(bufferSize > 0, "bufferSize must be >0");
        this.bufferSize = bufferSize;
    }

    /**
     * Returns configured number of parallel thread count that would work on
     * publishing buffered events to Kinesis
     *
     * @return configured number of parallel thread count that would work on
     * publishing buffered events to Kinesis
     */
    public int getThreadCount() {
        return threadCount;
    }

    /**
     * Configures number of parallel thread count that would work on publishing
     * buffered events to Kinesis
     *
     * @param parallelCount number of parallel thread count
     */
    public void setThreadCount(int parallelCount) {
        Validator.validate(parallelCount > 0, "threadCount must be >0");
        this.threadCount = parallelCount;
    }

    /**
     * Returns configured timeout between shutdown and clean up. When this
     * appender is asked to close/stop, it would wait for at most these many
     * seconds and try to send all buffered records to Kinesis. However if it
     * fails to publish them before timeout, it would drop those records and exit
     * immediately after timeout.
     *
     * @return configured timeout for shutdown and clean up.
     */
    public int getShutdownTimeout() {
        return shutdownTimeout;
    }

    /**
     * Configures timeout between shutdown and clean up. When this appender is
     * asked to close/stop, it would wait for at most these many seconds and try
     * to send all buffered records to Kinesis. However if it fails to publish
     * them before timeout, it would drop those records and exit immediately after
     * timeout.
     *
     * @param shutdownTimeout timeout between shutdown and clean up
     */
    public void setShutdownTimeout(int shutdownTimeout) {
        Validator.validate(shutdownTimeout > 0, "shutdownTimeout must be >0");
        this.shutdownTimeout = shutdownTimeout;
    }

    /**
     * stream이 존재 하는지, active되어 있는지 체크 (추상 메소드)
     */
    protected abstract void validateStreamName(AmazonKinesis client, String streamName);

    /**
     * client생성 (추상 메소드)
     *
     * @param credentials
     * @param configuration
     * @param threadFactory
     * @return
     */
    protected abstract Client createClient(AWSCredentialsProvider credentials, ClientConfiguration configuration,
                                           ExecutorFactory threadFactory);

    protected void setInitializationFailed(boolean initializationFailed) {
        this.initializationFailed = initializationFailed;
    }

    protected AmazonKinesis getClient() {
        return client;
    }


    /**
     * 메시지
     *
     * @param message
     * @throws Exception
     */
    protected abstract void putMessage(String message) throws Exception;

    /**
     * logback layout null 체크
     *
     * @return
     */
    private boolean isLayoutIsnull() {
        return checkNullTarget(layout, "Invalid configuration - No layout for appender: ");
    }

    /**
     * streamName null 체크
     *
     * @return
     */
    private boolean isStreamName() {
        return checkNullTarget(streamName, "Invalid configuration - streamName cannot be null for appender: ");
    }

    /**
     * Sets streamName for the kinesis stream to which data is to be published.
     *
     * @param streamName name of the kinesis stream to which data is to be
     *                   published.
     */
    public void setStreamName(String streamName) {
        Validator.validate(!Validator.isBlank(streamName), "streamName cannot be blank");
        this.streamName = streamName.trim();
    }

    /**
     * 체크할 대상이 null인지 여부
     *
     * @param target
     * @param errorMesg
     * @return
     */
    private boolean checkNullTarget(Object target, String errorMesg) {

        if (target == null) {
            this.initializationFailed = true;
            addError(errorMesg + this.name);

            return true;

        } else {
            return false;
        }
    }

    /**
     * 리전 체크
     *
     * @param regionName
     */
    private void validationRegion(String regionName) {

        if (Validator.isBlank(region)) {
            addError("Region is Empty. required Amazon Kinesis Region.");
        }

        Region region = RegionUtils.getRegion(regionName);
        if (region == null) {
            addError(regionName + " is not a valid AWS region.");
        }
    }

    /**
     * kinesisClient 생성
     * <p>
     * - clientConfiguration / threadFactory 을 설정하여 client에 set함
     */
    private void createConfigAndClient() {

        ClientConfiguration clientConfiguration = getClientConfigurationWithUserAgent();            //clientCofnig

        BlockingQueue<Runnable> taskBuffer = new LinkedBlockingDeque<>(bufferSize);                 //bufferSize

        ExecutorFactory threadFactory = () -> new ThreadPoolExecutor(threadCount, threadCount,      //threadFactory
                AppenderConstants.DEFAULT_THREAD_KEEP_ALIVE_SEC, TimeUnit.SECONDS,
                taskBuffer, setupThreadFactory(), new BlockFastProducerPolicy());

        this.client = createClient(credentials, clientConfiguration, threadFactory);          //awsKinesisClient
    }

    /**
     * clientConfig 생성
     *
     * @return
     */
    private ClientConfiguration getClientConfigurationWithUserAgent() {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(maxRetries);
        clientConfiguration
                .setRetryPolicy(new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
                        PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, maxRetries, true));
        clientConfiguration.setUserAgentPrefix(AppenderConstants.USER_AGENT_STRING);
        return clientConfiguration;
    }

    /**
     * 새로운 쓰레드를 만들때 마다 사용할 factory
     *
     * @return
     */
    private ThreadFactory setupThreadFactory() {
        return new NamedThreadFactory(getClass().getSimpleName() + "[" + streamName + "]-");
    }
}
