package com.example.kinesislogger.logback.helpers;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.PropertiesCredentials;

import java.io.IOException;
import java.io.InputStream;

public class CustomClasspathPropertiesFileCredentialsProvider implements AWSCredentialsProvider {

    /** The name of the properties file to check for credentials */
    private static String DEFAULT_PROPERTIES_FILE = "aws-kinesis-credentials.properties";

    private final String credentialsFilePath;

    /**
     * Creates a new ClasspathPropertiesFileCredentialsProvider that will
     * attempt to load the <code>AwsCredentials.properties</code> file from
     * the classpath to read AWS security credentials.
     */
    public CustomClasspathPropertiesFileCredentialsProvider() {
        this(DEFAULT_PROPERTIES_FILE);
    }

    /**
     * Creates a new ClasspathPropertiesFileCredentialsProvider that will
     * attempt to load a custom file from the classpath to read AWS security
     * credentials.
     *
     * @param credentialsFilePath
     *            The custom classpath resource path to a properties file
     *            from which the AWS security credentials should be loaded.
     *
     *            For example,
     *            <ul>
     *              <li>com/mycompany/credentials.properties</li>
     *              <li>beta-credentials.properties</li>
     *              <li>AwsCredentials.properties</li>
     *            </ul>
     */
    public CustomClasspathPropertiesFileCredentialsProvider(String credentialsFilePath) {
        if (credentialsFilePath == null)
            throw new IllegalArgumentException("Credentials file path cannot be null");

        // Make sure the path is absolute
        if (!credentialsFilePath.startsWith("/")) {
            this.credentialsFilePath = "/" + credentialsFilePath;
        } else {
            this.credentialsFilePath = credentialsFilePath;
        }
    }

    public AWSCredentials getCredentials() {
        InputStream inputStream = getClass().getResourceAsStream(credentialsFilePath);
        if (inputStream == null) {
            throw new SdkClientException("Unable to load AWS credentials from the " + credentialsFilePath + " file on the classpath");
        }

        try {
            return new PropertiesCredentials(inputStream);
        } catch (IOException e) {
            throw new SdkClientException("Unable to load AWS credentials from the " + credentialsFilePath + " file on the classpath", e);
        }
    }

    public void refresh() {}

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + credentialsFilePath + ")";
    }
}
