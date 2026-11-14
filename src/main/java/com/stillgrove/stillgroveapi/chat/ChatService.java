package com.stillgrove.stillgroveapi.chat;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.stillgrove.stillgroveapi.memory.MemoryService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class ChatService {
    private final ChatMapper mapper;
    private final AiClient ai;
    private final MemoryService memory;
    public ChatService(ChatMapper mapper,AiClient ai,MemoryService memory) { this.mapper=mapper; this.ai=ai; this.memory=memory; }

    public List<Map<String,Object>> list(long userId) { return mapper.list(userId); }
    public Map<String,Object> get(long userId,long id) {
        var row=mapper.find(id,userId);
        if(row==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"会话不存在");
        return row;
    }
    @Transactional public Map<String,Object> create(long userId) {
        mapper.insertConversation(userId,"新的对话");
        return get(userId,mapper.lastInsertId());
    }
    @Transactional public Map<String,Object> rename(long userId,long id,String title) {
        if(mapper.rename(id,userId,title)!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"会话不存在");
        return get(userId,id);
    }
    @Transactional public void delete(long userId,long id) {
        if(mapper.delete(id,userId)!=1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"会话不存在");
        memory.deleteForConversation(userId,id);
    }
    public List<Map<String,Object>> messages(long userId,long id,int before) {
        get(userId,id);
        var rows=new ArrayList<>(mapper.messages(id,userId,before,50));
        Collections.reverse(rows);
        return rows;
    }
    public boolean aiAvailable() { return ai.available(); }

    public record Pending(long userId,long conversationId,long assistantMessageId,List<Map<String,String>> context) {}

    @Transactional public Pending prepare(long userId,long conversationId,String content,String requestId) {
        if(mapper.existingGeneration(userId,requestId)!=null)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"请求已处理，请刷新消息列表");
        Integer seq=mapper.lockSequence(conversationId,userId);
        if(seq==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"会话不存在");
        var previous=mapper.context(conversationId,userId);
        mapper.advance(conversationId,2);
        mapper.insertMessage(userId,conversationId,seq,"USER","COMPLETED",content);
        long userMessageId=mapper.lastInsertId();
        mapper.insertMessage(userId,conversationId,seq+1,"ASSISTANT","PENDING","");
        long assistantMessageId=mapper.lastInsertId();
        mapper.insertGeneration(userId,conversationId,requestId,userMessageId,assistantMessageId,ai.model());
        var context=new ArrayList<Map<String,String>>();
        context.add(Map.of("role","system","content","你是 Stillgrove，一个温和、尊重用户的倾听者。先共情，再给出简短而具体的回应。不要假装自己是医生或心理咨询师。遇到自伤风险时鼓励立即联系身边可信赖的人或当地紧急服务。"));
        Collections.reverse(previous);
        for(var row:previous) context.add(Map.of("role",String.valueOf(row.get("role")).toLowerCase(),"content",String.valueOf(row.get("content"))));
        context.add(Map.of("role","user","content",content));
        return new Pending(userId,conversationId,assistantMessageId,context);
    }
    public void stream(Pending pending,java.util.function.Consumer<String> onText) throws Exception {
        var context=new ArrayList<>(pending.context());
        String recentPrompt=context.getLast().get("content");
        var memories=memory.relevant(pending.userId(),recentPrompt);
        String summary=memory.summary(pending.userId(),pending.conversationId());
        if(summary!=null && !summary.isBlank()) context.add(1,Map.of("role","system","content","此前对话摘要："+summary));
        if(!memories.isEmpty()) context.add(1,Map.of("role","system","content","用户明确表达过的长期信息，仅在相关时使用："+String.join("；",memories)));
        ai.stream(context,onText);
    }
    @Transactional public void finish(Pending pending,String text,String error) {
        String status=error==null?"COMPLETED":"FAILED";
        mapper.finishMessage(pending.assistantMessageId(),pending.userId(),status,text,error);
        mapper.finishGeneration(pending.assistantMessageId(),status,error);
        if(error==null) mapper.enqueueCompleted(pending.assistantMessageId());
    }
}
