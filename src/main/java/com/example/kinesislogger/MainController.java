package com.example.kinesislogger;

import com.example.kinesislogger.config.AsyncTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static net.logstash.logback.marker.Markers.append;

@Slf4j
@RestController
public class MainController {

    private final AsyncTestService service;

    public MainController(AsyncTestService service) {
        this.service = service;
    }

    @GetMapping("/")
    public void logging() {
        log.info("logging=userTest");
    }

    /**
     * 비동기 쓰레드 풀 로깅 처리 테스트
     * @return
     */
    @GetMapping("/a")
    public void asyncLoggingTest() {

        for (int i = 0; i < 10; i++) {
            service.asyncCall(i);
        }
    }

    /**
     * json로그에 key 추가
     */
    @GetMapping("/append")
    public void addKeyToJsonLog() {
        log.info(append("mycustomKey", "hello"), "test Message");
    }
}
