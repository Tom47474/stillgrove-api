package com.stillgrove.stillgroveapi.memory;

import com.stillgrove.stillgroveapi.chat.AiClient;
import com.stillgrove.stillgroveapi.chat.ChatMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class MemoryService {
    private final MemoryMapper mapper;
    private final ChatMapper chat;
    private final AiClient ai;
    private final QdrantClient qdrant;
    private final TransactionTemplate transactions;
    public MemoryService(MemoryMapper mapper,ChatMapper chat,AiClient ai,QdrantClient qdrant,TransactionTemplate transactions) {
        this.mapper=mapper;this.chat=chat;this.ai=ai;this.qdrant=qdrant;this.transactions=transactions;
    }
    public String summary(long userId,long conversationId) { return mapper.summary(conversationId,userId); }
    public boolean canProcess(String eventType) {
        return switch(eventType) {
            case "CONVERSATION_COMPLETED" -> ai.available();
            case "MEMORY_UPSERT" -> ai.available() && qdrant.healthy();
            case "MEMORY_DELETE" -> qdrant.healthy();
            default -> true;
        };
    }
    @Transactional public void deleteForConversation(long userId,long conversationId) {
        for(long memoryId:mapper.activeForConversation(conversationId,userId)) {
            if(mapper.invalidate(memoryId,userId)==1) mapper.enqueueDelete(memoryId);
        }
    }
    public List<String> relevant(long userId,String query) {
        if(!ai.available()) return List.of();
        try {
            var found=new ArrayList<String>();
            for(var point:qdrant.search(userId,ai.embed(query))) {
                long id=((Number)point.get("id")).longValue();
                var row=mapper.validMemory(id,userId);
                if(row!=null && ((Number)row.get("version")).intValue()==((Number)point.get("version")).intValue())
                    found.add(String.valueOf(row.get("content")));
            }
            return found;
        } catch(Exception ex) { return List.of(); }
    }
    public void extract(long messageId) throws Exception {
        var source=mapper.completedMessage(messageId);
        if(source==null) return;
        long userId=((Number)source.get("userId")).longValue();
        long conversationId=((Number)source.get("conversationId")).longValue();
        var recent=new ArrayList<>(chat.context(conversationId,userId));
        if(recent.isEmpty()) return;
        Collections.reverse(recent);
        var transcript=new StringBuilder();
        for(var item:recent) transcript.append(item.get("role")).append(": ").append(item.get("content")).append('\n');
        String text=transcript.toString();
        String previousSummary=mapper.summary(conversationId,userId);
        if(Boolean.TRUE.equals(mapper.memoryEnabled(userId)) && mapper.countBySource(messageId)==0) {
            String fact=ai.complete(List.of(Map.of("role","system","content","从对话中只提取用户明确陈述、未来对话有用且相对稳定的一条个人偏好或背景。不得推测、不得记录第三人的隐私或暂时情绪。无合适记忆时只输出 NONE；有则只输出一句简短事实，不能附加解释。"),Map.of("role","user","content",text))).trim();
            if(!fact.equalsIgnoreCase("NONE") && !fact.isBlank() && fact.length()<=500) saveMemory(userId,conversationId,messageId,fact);
        }
        String summary=ai.complete(List.of(Map.of("role","system","content","用不超过300字将旧摘要与新增对话合并概括，只记录用户明确表达的内容，不添加推测。"),Map.of("role","user","content","旧摘要："+(previousSummary==null?"无":previousSummary)+"\n新增对话："+text))).trim();
        if(!summary.isBlank() && summary.length()<=1000) mapper.upsertSummary(userId,conversationId,summary,mapper.maxSequence(conversationId));
    }
    public void saveMemory(long userId,long conversationId,long messageId,String content) {
        transactions.executeWithoutResult(status -> {
            if(mapper.countBySource(messageId)>0 || mapper.countExactMemory(userId,content)>0) return;
            mapper.insertMemory(userId,content,conversationId,messageId,UUID.randomUUID().toString());
            mapper.enqueueMemory(mapper.lastInsertId());
        });
    }
    public void index(long memoryId) throws Exception {
        var memory=mapper.activeMemory(memoryId);
        if(memory==null) return;
        qdrant.upsert(String.valueOf(memory.get("pointId")),((Number)memory.get("userId")).longValue(),memoryId,
                ((Number)memory.get("version")).intValue(),ai.embed(String.valueOf(memory.get("content"))));
    }
    public void deleteVector(long memoryId) throws Exception {
        var memory=mapper.anyMemory(memoryId);
        if(memory!=null) qdrant.delete(String.valueOf(memory.get("pointId")));
    }
    @Transactional public void expire() {
        for(var item:mapper.expiredMemories()) {
            long id=((Number)item.get("id")).longValue();
            long userId=((Number)item.get("userId")).longValue();
            if(mapper.invalidate(id,userId)==1) mapper.enqueueDelete(id);
        }
    }
}
