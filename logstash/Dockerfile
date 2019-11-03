FROM docker.elastic.co/logstash/logstash:7.4.1
RUN mkdir -p ~/.aws/
RUN rm -f /usr/share/logstash/pipeline/logstash.conf
RUN logstash-plugin install logstash-input-kinesis
ADD config/ ~/.aws/
ADD pipeline/ /usr/share/logstash/pipeline/
