package com.stillgrove.stillgroveapi.memory;

import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix="stillgrove.rocketmq",name="enabled",havingValue="true")
public class RocketMqBridge {
    private final MemoryMapper mapper;
    private final MemoryService service;
    private final ClientServiceProvider provider=ClientServiceProvider.loadService();
    private final Producer producer;
    private final SimpleConsumer consumer;
    private final String topic;

    public RocketMqBridge(MemoryMapper mapper,MemoryService service,
                          @Value("${stillgrove.rocketmq.endpoints}") String endpoints,
                          @Value("${stillgrove.rocketmq.topic}") String topic,
                          @Value("${stillgrove.rocketmq.consumer-group}") String group) throws Exception {
        this.mapper=mapper;this.service=service;this.topic=topic;
        var configuration=ClientConfiguration.newBuilder().setEndpoints(endpoints).build();
        producer=provider.newProducerBuilder().setClientConfiguration(configuration).setTopics(topic).build();
        consumer=provider.newSimpleConsumerBuilder().setClientConfiguration(configuration)
                .setConsumerGroup(group).setAwaitDuration(Duration.ofSeconds(1))
                .setSubscriptionExpressions(Map.of(topic,new FilterExpression("*",FilterExpressionType.TAG))).build();
    }

    @Scheduled(fixedDelay=5000)
    public void publish() {
        mapper.requeueStale();
        service.expire();
        for(var event:mapper.ready()) {
            if(!service.canProcess(String.valueOf(event.get("eventType")))) continue;
            long id=((Number)event.get("id")).longValue();
            if(mapper.claim(id)!=1) continue;
            try {
                var message=provider.newMessageBuilder().setTopic(topic)
                        .setBody(Long.toString(id).getBytes(StandardCharsets.UTF_8)).build();
                producer.send(message);
                mapper.markSent(id);
            } catch(Exception ex) { mapper.fail(id,ex.getClass().getSimpleName()); }
        }
    }

    @Scheduled(fixedDelay=1000)
    public void consume() throws Exception {
        for(var message:consumer.receive(10,Duration.ofMinutes(3))) {
            long id=Long.parseLong(StandardCharsets.UTF_8.decode(message.getBody()).toString());
            var event=mapper.eventById(id);
            if(event==null || "DONE".equals(event.get("status"))) { consumer.ack(message); continue; }
            try {
                long aggregateId=((Number)event.get("aggregateId")).longValue();
                switch(String.valueOf(event.get("eventType"))) {
                    case "CONVERSATION_COMPLETED" -> service.extract(aggregateId);
                    case "MEMORY_UPSERT" -> service.index(aggregateId);
                    case "MEMORY_DELETE" -> service.deleteVector(aggregateId);
                    default -> throw new IllegalArgumentException("未知事件类型");
                }
                mapper.complete(id);
                consumer.ack(message);
            } catch(Exception ex) {
                mapper.fail(id,ex.getClass().getSimpleName());
                consumer.ack(message);
            }
        }
    }

    @PreDestroy public void close() throws Exception { consumer.close();producer.close(); }
}
