package com.example.kinesislogger;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.RegionUtils;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.DescribeStreamResult;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.ResourceNotFoundException;
import com.example.kinesislogger.logback.helpers.CustomClasspathPropertiesFileCredentialsProvider;
import com.example.kinesislogger.logback.utils.ConfigurationUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.UUID;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class MainControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void call_loging_controller() throws Exception {

        MvcResult mvcResult = this.mockMvc.perform(get("/"))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        assertThat(mvcResult.getResponse().getContentAsString()).isEqualTo("logging");

    }

    @Test
    public void kinesis_test() throws Exception {

        String streamName = "log_stream_test";
        String regionName = "ap-northeast-2";

        checkRegion(regionName);

        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard();

        clientBuilder.setRegion(regionName);
        clientBuilder.setCredentials(new CustomClasspathPropertiesFileCredentialsProvider());
        clientBuilder.setClientConfiguration(ConfigurationUtils.getClientConfigWithUserAgent());

        AmazonKinesis kinesisClient = clientBuilder.build();

        // Validate that the stream exists and is active
        validateStream(kinesisClient, streamName);

        putMessage("message", kinesisClient, streamName);
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
