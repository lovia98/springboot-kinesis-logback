# kinesis-logback-appender sample

* 설정  
  1. /resource/logback.properties 에서 kinesis 연결에 필요한 값을 설정함.
  2. logback-spring.xml 설정  
   ```
     <appender name="KINESIS" class="com.example.kinesislogger.logback.KinesisAsyncAppender">
         <accessKey>${aws.kinesis.accessKey}</accessKey>
         <secretKey>${aws.kinesis.secretKey}</secretKey>
         <bufferSize>${aws.kinesis.bufferSize}</bufferSize>
         <threadCount>${aws.kinesis.threadCount}</threadCount>
         <maxRetries>${aws.kinesis.maxRetries}</maxRetries>
         <shutdownTimeout>${aws.kinesis.shutdownTimeout}</shutdownTimeout>
         <streamName>${aws.kinesis.streamName}</streamName>
         <region>${aws.kinesis.region}</region>
         <encoding>${aws.kinesis.encoding}</encoding>         
         <layout class="net.logstash.logback.layout.LogstashLayout"> <!-- json 포맷 -->
            <timestampPattern>yyyy-MM-dd' 'HH:mm:ss.SSS</timestampPattern>
            <customFields>{"port":"${server.port}"}</customFields>
        </layout>
     </appender>  
    ```

* 로직 내에서 로깅 json 객체에 key를 추가 하고 싶을 경우 lostash marker 이용
 example)
    ```
      import static net.logstash.logback.marker.Markers.append;

      /**
       * json로그에 key 추가
       */
      @GetMapping("/append")
      public void addKeyToJsonLog() {
          log.info(append("mycustomKey", "hello"), "test Message");
      }
    ```

* 참고한 코드 
   - https://github.com/aws-samples/amazon-kinesis-learning
   - https://github.com/guardian/kinesis-logback-appender
