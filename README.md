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
         <layout class="ch.qos.logback.classic.PatternLayout">
             <pattern>${log.pattern}</pattern>
         </layout>
     </appender>  
    ```

* 참고한 코드 
   - https://github.com/aws-samples/amazon-kinesis-learning
   - https://github.com/guardian/kinesis-logback-appender
