package com.example.kinesislogger.logback;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class BaseKinesisAppenderTest {

    private KinesisAsyncAppender appender;

    @Before
    public void setUp() {
        appender = new KinesisAsyncAppender();
    }

    @Test
    public void test() {


    }
}