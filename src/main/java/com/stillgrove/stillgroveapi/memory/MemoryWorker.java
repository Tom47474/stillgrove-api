package com.stillgrove.stillgroveapi.memory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@ConditionalOnProperty(prefix="stillgrove.rocketmq",name="enabled",havingValue="false",matchIfMissing=true)
public class MemoryWorker {
    private final MemoryMapper mapper;
    private final MemoryService service;
    public MemoryWorker(MemoryMapper mapper,MemoryService service) { this.mapper=mapper;this.service=service; }

    @Scheduled(fixedDelay=5000)
    public void process() {
        mapper.requeueStale();
        service.expire();
        for(var event:mapper.ready()) {
            if(!service.canProcess(String.valueOf(event.get("eventType")))) continue;
            long id=((Number)event.get("id")).longValue();
            if(mapper.claim(id)!=1) continue;
            try {
                long aggregateId=((Number)event.get("aggregateId")).longValue();
                switch(String.valueOf(event.get("eventType"))) {
                    case "CONVERSATION_COMPLETED" -> service.extract(aggregateId);
                    case "MEMORY_UPSERT" -> service.index(aggregateId);
                    case "MEMORY_DELETE" -> service.deleteVector(aggregateId);
                    default -> throw new IllegalArgumentException("未知事件类型");
                }
                mapper.complete(id);
            } catch(Exception ex) {
                mapper.fail(id,ex.getClass().getSimpleName());
            }
        }
    }
}
