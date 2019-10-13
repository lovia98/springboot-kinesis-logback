package com.example.kinesislogger.logback;

import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.LayoutBase;
import ch.qos.logback.core.spi.DeferredProcessingAware;
import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.regions.Regions;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.example.kinesislogger.logback.helpers.BlockFastProducerPolicy;
import com.example.kinesislogger.logback.helpers.CustomClasspathPropertiesFileCredentialsProvider;
import com.example.kinesislogger.logback.helpers.NamedThreadFactory;
import com.example.kinesislogger.logback.helpers.Validator;

import java.util.concurrent.*;

public abstract class BaseKinesisAppender<Event extends DeferredProcessingAware, Client extends AmazonWebServiceClient>
        extends AppenderBase<Event> {

    private String encoding = AppenderConstants.DEFAULT_ENCODING;
    private int maxRetries = AppenderConstants.DEFAULT_MAX_RETRY_COUNT;
    private int bufferSize = AppenderConstants.DEFAULT_BUFFER_SIZE;
    private int threadCount = AppenderConstants.DEFAULT_THREAD_COUNT;
    private int shutdownTimeout = AppenderConstants.DEFAULT_SHUTDOWN_TIMEOUT_SEC;

    private String region;
    private String streamName;

    private boolean initializationFailed = false;
    private BlockingQueue<Runnable> taskBuffer;
    private ThreadPoolExecutor threadPoolExecutor;
    private LayoutBase<Event> layout;
    private Client client;
    private AWSCredentialsProvider credentials = new CustomClasspathPropertiesFileCredentialsProvider();


    @Override
    public void start() {
        if (layout == null) {
            initializationFailed = true;
            addError("Invalid configuration - No layout for appender: " + name);
            return;
        }

        if (streamName == null) {
            initializationFailed = true;
            addError("Invalid configuration - streamName cannot be null for appender: " + name);
            return;
        }

        checkRegion(region);

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setMaxErrorRetry(maxRetries);
        clientConfiguration
                .setRetryPolicy(new RetryPolicy(PredefinedRetryPolicies.DEFAULT_RETRY_CONDITION,
                        PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY, maxRetries, true));
        clientConfiguration.setUserAgentPrefix(AppenderConstants.USER_AGENT_STRING);

        BlockingQueue<Runnable> taskBuffer = new LinkedBlockingDeque<>(bufferSize);
        threadPoolExecutor = new ThreadPoolExecutor(threadCount, threadCount,
                AppenderConstants.DEFAULT_THREAD_KEEP_ALIVE_SEC, TimeUnit.SECONDS,
                taskBuffer, setupThreadFactory(), new BlockFastProducerPolicy());
        threadPoolExecutor.prestartAllCoreThreads();

        //kinesis client생성
        this.client = createClient(credentials, clientConfiguration, threadPoolExecutor);

        if (Validator.isBlank(region)) {
            addError("Region is Empty. required Amazon Kinesis Region.");
        }

        validateStreamName(client, streamName);

        super.start();
    }

    @Override
    public void stop() {
        threadPoolExecutor.shutdown();
        BlockingQueue<Runnable> taskQueue = threadPoolExecutor.getQueue();
        int bufferSizeBeforeShutdown = threadPoolExecutor.getQueue().size();
        boolean gracefulShutdown = true;
        try {
            gracefulShutdown = threadPoolExecutor.awaitTermination(shutdownTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // we are anyways cleaning up
        } finally {
            int bufferSizeAfterShutdown = taskQueue.size();
            if (!gracefulShutdown || bufferSizeAfterShutdown > 0) {
                String errorMsg = "Kinesis Log4J Appender (" + name + ") waited for " + shutdownTimeout
                        + " seconds before terminating but could send only "
                        + (bufferSizeAfterShutdown - bufferSizeBeforeShutdown) + " logevents, it failed to send "
                        + bufferSizeAfterShutdown + " pending log events from it's processing queue";
                addError(errorMsg);
            }
        }
        client.shutdown();
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
        this.region = region;
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
     * Returns count of tasks scheduled to send records to Kinesis. Since
     * currently each task maps to sending one record, it is equivalent to number
     * of records in the buffer scheduled to be sent to Kinesis.
     *
     * @return count of tasks scheduled to send records to Kinesis.
     */
    public int getTaskBufferSize() {
        return taskBuffer.size();
    }

    /**
     * stream name validation (추상 메소드)
     */
    protected abstract void validateStreamName(Client client, String streamName);

    /**
     * client생성 (추상 메소드)
     *
     * @param credentials
     * @param configuration
     * @param executor
     * @return
     */
    protected abstract Client createClient(AWSCredentialsProvider credentials, ClientConfiguration configuration,
                                           ThreadPoolExecutor executor);

    protected void setInitializationFailed(boolean initializationFailed) {
        this.initializationFailed = initializationFailed;
    }

    protected Client getClient() {
        return client;
    }

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

    /**
     * Send message to client
     *
     * @param message formatted message to send
     * @throws Exception if unable to add message
     */
    protected abstract void putMessage(String message) throws Exception;

    /**
     * {@link #threadPoolExecutor}에서 사용할 스레드 팩토리를 작성.
     */
    private ThreadFactory setupThreadFactory() {
        return new NamedThreadFactory(getClass().getSimpleName() + "[" + streamName + "]-");
    }

    private void checkRegion(String regionName) {
        Region region = RegionUtils.getRegion(regionName);
        if (region == null) {
            addError(regionName + " is not a valid AWS region.");
        }
    }
}
