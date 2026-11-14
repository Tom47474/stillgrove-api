package com.stillgrove.stillgroveapi.chat;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface ChatMapper {
    @Select("SELECT id, title, status, last_message_at AS lastMessageAt, created_at AS createdAt FROM conversations WHERE user_id=#{userId} AND status='ACTIVE' ORDER BY last_message_at DESC, id DESC LIMIT 100")
    List<Map<String,Object>> list(@Param("userId") long userId);

    @Select("SELECT id, title, status, last_message_at AS lastMessageAt, created_at AS createdAt FROM conversations WHERE id=#{id} AND user_id=#{userId} AND status='ACTIVE'")
    Map<String,Object> find(@Param("id") long id, @Param("userId") long userId);

    @Select("SELECT next_sequence FROM conversations WHERE id=#{id} AND user_id=#{userId} AND status='ACTIVE' FOR UPDATE")
    Integer lockSequence(@Param("id") long id, @Param("userId") long userId);

    @Insert("INSERT INTO conversations(user_id,title) VALUES(#{userId},#{title})")
    int insertConversation(@Param("userId") long userId,@Param("title") String title);

    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();

    @Update("UPDATE conversations SET title=#{title} WHERE id=#{id} AND user_id=#{userId} AND status='ACTIVE'")
    int rename(@Param("id") long id,@Param("userId") long userId,@Param("title") String title);

    @Update("UPDATE conversations SET status='DELETED',deleted_at=UTC_TIMESTAMP(3) WHERE id=#{id} AND user_id=#{userId} AND status='ACTIVE'")
    int delete(@Param("id") long id,@Param("userId") long userId);

    @Update("UPDATE conversations SET next_sequence=next_sequence+#{count},last_message_at=UTC_TIMESTAMP(3) WHERE id=#{id}")
    void advance(@Param("id") long id,@Param("count") int count);

    @Select("SELECT id,sequence_no AS sequenceNo,role,status,content,created_at AS createdAt FROM messages WHERE conversation_id=#{conversationId} AND user_id=#{userId} AND deleted_at IS NULL AND sequence_no < #{before} ORDER BY sequence_no DESC LIMIT #{limit}")
    List<Map<String,Object>> messages(@Param("conversationId") long conversationId,@Param("userId") long userId,@Param("before") int before,@Param("limit") int limit);

    @Insert("INSERT INTO messages(user_id,conversation_id,sequence_no,role,status,content) VALUES(#{userId},#{conversationId},#{seq},#{role},#{status},#{content})")
    void insertMessage(@Param("userId") long userId,@Param("conversationId") long conversationId,@Param("seq") int seq,@Param("role") String role,@Param("status") String status,@Param("content") String content);

    @Update("UPDATE messages SET status=#{status},content=#{content},error_code=#{error} WHERE id=#{id} AND user_id=#{userId}")
    void finishMessage(@Param("id") long id,@Param("userId") long userId,@Param("status") String status,@Param("content") String content,@Param("error") String error);

    @Insert("INSERT INTO ai_generations(user_id,conversation_id,client_request_id,user_message_id,assistant_message_id,model_name,provider_name) VALUES(#{userId},#{conversationId},#{requestId},#{userMessageId},#{assistantMessageId},#{model},'BAILIAN')")
    void insertGeneration(@Param("userId") long userId,@Param("conversationId") long conversationId,@Param("requestId") String requestId,@Param("userMessageId") long userMessageId,@Param("assistantMessageId") long assistantMessageId,@Param("model") String model);

    @Select("SELECT assistant_message_id FROM ai_generations WHERE user_id=#{userId} AND client_request_id=#{requestId}")
    Long existingGeneration(@Param("userId") long userId,@Param("requestId") String requestId);

    @Update("UPDATE ai_generations SET status=#{status},error_code=#{error},finished_at=UTC_TIMESTAMP(3) WHERE assistant_message_id=#{messageId}")
    void finishGeneration(@Param("messageId") long messageId,@Param("status") String status,@Param("error") String error);

    @Insert("INSERT INTO outbox_events(aggregate_type,aggregate_id,aggregate_version,event_type,payload) VALUES('MESSAGE',#{messageId},1,'CONVERSATION_COMPLETED',JSON_OBJECT())")
    void enqueueCompleted(long messageId);

    @Select("SELECT role,content FROM messages WHERE conversation_id=#{conversationId} AND user_id=#{userId} AND status='COMPLETED' AND deleted_at IS NULL AND sequence_no > COALESCE((SELECT covered_sequence_no FROM conversation_summaries WHERE conversation_id=#{conversationId} AND user_id=#{userId}),0) ORDER BY sequence_no DESC LIMIT 20")
    List<Map<String,Object>> context(@Param("conversationId") long conversationId,@Param("userId") long userId);
}
