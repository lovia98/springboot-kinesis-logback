package com.example.kinesislogger.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AsyncTestService {

    @Async
    public void asyncCall(int i) {
        log.info("async Method Test - {}", i);
    }
}
