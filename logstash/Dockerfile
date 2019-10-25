FROM docker.elastic.co/logstash/logstash:7.4.1
CMD mv config/credentials ~/.aws/credentials
RUN rm -f /usr/share/logstash/pipeline/logstash.conf
ADD pipeline/ /usr/share/logstash/pipeline/
ADD config/ /usr/share/logstash/config/
