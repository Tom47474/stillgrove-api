package com.stillgrove.stillgroveapi.memory;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface MemoryMapper {
    @Select("SELECT id,event_type AS eventType,aggregate_id AS aggregateId,attempts FROM outbox_events WHERE status='NEW' AND next_retry_at<=UTC_TIMESTAMP(3) ORDER BY id LIMIT 20")
    List<Map<String,Object>> ready();
    @Update("UPDATE outbox_events SET status='PROCESSING' WHERE id=#{id} AND status='NEW'")
    int claim(long id);
    @Update("UPDATE outbox_events SET status='NEW',next_retry_at=UTC_TIMESTAMP(3) WHERE status IN ('PROCESSING','SENT') AND updated_at<DATE_SUB(UTC_TIMESTAMP(3), INTERVAL 5 MINUTE)")
    void requeueStale();
    @Update("UPDATE outbox_events SET status='DONE' WHERE id=#{id}")
    void complete(long id);
    @Update("UPDATE outbox_events SET status='SENT',sent_at=UTC_TIMESTAMP(3) WHERE id=#{id} AND status='PROCESSING'")
    void markSent(long id);
    @Select("SELECT id,event_type AS eventType,aggregate_id AS aggregateId,status FROM outbox_events WHERE id=#{id}")
    Map<String,Object> eventById(long id);
    @Update("UPDATE outbox_events SET attempts=attempts+1,status=IF(attempts+1>=max_attempts,'FAILED','NEW'),next_retry_at=DATE_ADD(UTC_TIMESTAMP(3), INTERVAL 30 SECOND),last_error=#{error} WHERE id=#{id}")
    void fail(@Param("id") long id,@Param("error") String error);
    @Select("SELECT user_id AS userId,conversation_id AS conversationId FROM messages WHERE id=#{id} AND status='COMPLETED'")
    Map<String,Object> completedMessage(long id);
    @Select("SELECT id,user_id AS userId,content,category,version,vector_point_id AS pointId FROM memories WHERE id=#{id} AND status='ACTIVE'")
    Map<String,Object> activeMemory(long id);
    @Select("SELECT id,user_id AS userId,content,category,version,vector_point_id AS pointId FROM memories WHERE id=#{id}")
    Map<String,Object> anyMemory(long id);
    @Select("SELECT id,content,version FROM memories WHERE id=#{id} AND user_id=#{userId} AND status='ACTIVE' AND (expires_at IS NULL OR expires_at>UTC_TIMESTAMP(3))")
    Map<String,Object> validMemory(@Param("id") long id,@Param("userId") long userId);
    @Select("SELECT COUNT(*) FROM memories WHERE source_message_id=#{sourceMessageId}")
    int countBySource(long sourceMessageId);
    @Select("SELECT memory_enabled FROM user_settings WHERE user_id=#{userId}")
    Boolean memoryEnabled(long userId);
    @Select("SELECT COUNT(*) FROM memories WHERE user_id=#{userId} AND content=#{content} AND status='ACTIVE'")
    int countExactMemory(@Param("userId") long userId,@Param("content") String content);
    @Select("SELECT id FROM memories WHERE source_conversation_id=#{conversationId} AND user_id=#{userId} AND status='ACTIVE'")
    List<Long> activeForConversation(@Param("conversationId") long conversationId,@Param("userId") long userId);
    @Update("UPDATE memories SET status='DELETED',deleted_at=UTC_TIMESTAMP(3),version=version+1 WHERE id=#{id} AND user_id=#{userId} AND status='ACTIVE'")
    int invalidate(@Param("id") long id,@Param("userId") long userId);
    @Insert("INSERT INTO outbox_events(aggregate_type,aggregate_id,aggregate_version,event_type,payload) VALUES('MEMORY',#{memoryId},1,'MEMORY_DELETE',JSON_OBJECT())")
    void enqueueDelete(long memoryId);
    @Select("SELECT id,user_id AS userId FROM memories WHERE status='ACTIVE' AND expires_at IS NOT NULL AND expires_at<=UTC_TIMESTAMP(3) LIMIT 100")
    List<Map<String,Object>> expiredMemories();
    @Insert("INSERT INTO memories(user_id,content,category,source_conversation_id,source_message_id,vector_point_id) VALUES(#{userId},#{content},'BACKGROUND',#{conversationId},#{messageId},#{pointId})")
    void insertMemory(@Param("userId") long userId,@Param("content") String content,@Param("conversationId") long conversationId,@Param("messageId") long messageId,@Param("pointId") String pointId);
    @Select("SELECT LAST_INSERT_ID()")
    long lastInsertId();
    @Insert("INSERT INTO outbox_events(aggregate_type,aggregate_id,aggregate_version,event_type,payload) VALUES('MEMORY',#{memoryId},1,'MEMORY_UPSERT',JSON_OBJECT())")
    void enqueueMemory(long memoryId);
    @Insert("INSERT INTO conversation_summaries(user_id,conversation_id,content,covered_sequence_no) VALUES(#{userId},#{conversationId},#{content},#{sequence}) ON DUPLICATE KEY UPDATE content=VALUES(content),covered_sequence_no=VALUES(covered_sequence_no),version=version+1")
    void upsertSummary(@Param("userId") long userId,@Param("conversationId") long conversationId,@Param("content") String content,@Param("sequence") int sequence);
    @Select("SELECT COALESCE(MAX(sequence_no),0) FROM messages WHERE conversation_id=#{conversationId} AND status='COMPLETED'")
    int maxSequence(long conversationId);
    @Select("SELECT content FROM conversation_summaries WHERE conversation_id=#{conversationId} AND user_id=#{userId} AND status='READY'")
    String summary(@Param("conversationId") long conversationId,@Param("userId") long userId);
}
