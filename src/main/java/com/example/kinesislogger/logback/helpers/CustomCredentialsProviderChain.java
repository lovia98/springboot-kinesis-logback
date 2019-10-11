package com.example.kinesislogger.logback.helpers;

import com.amazonaws.auth.AWSCredentialsProviderChain;

public final class CustomCredentialsProviderChain extends AWSCredentialsProviderChain {

    public CustomCredentialsProviderChain() {
//        super(new ClasspathPropertiesFileCredentialsProvider(), new EC2ContainerCredentialsProviderWrapper(),
//                new SystemPropertiesCredentialsProvider(), new EnvironmentVariableCredentialsProvider(),
//                new ProfileCredentialsProvider());

        super(new CustomClasspathPropertiesFileCredentialsProvider());
    }
}
