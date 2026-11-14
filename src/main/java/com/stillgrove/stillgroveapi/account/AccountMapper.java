package com.stillgrove.stillgroveapi.account;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AccountMapper {
    @Select("SELECT id FROM memories WHERE user_id=#{userId} AND status='ACTIVE'")
    List<Long> activeMemories(long userId);
    @Update("UPDATE memories SET status='DELETED',content='',deleted_at=UTC_TIMESTAMP(3),version=version+1 WHERE id=#{id} AND user_id=#{userId} AND status='ACTIVE'")
    int eraseMemory(@Param("id") long id,@Param("userId") long userId);
    @Insert("INSERT INTO outbox_events(aggregate_type,aggregate_id,aggregate_version,event_type,payload) VALUES('MEMORY',#{id},1,'MEMORY_DELETE',JSON_OBJECT())")
    void enqueueDelete(long id);
    @Insert("INSERT INTO data_deletion_requests(user_id,scope,status,completed_at) VALUES(#{userId},'ACCOUNT','COMPLETED',UTC_TIMESTAMP(3))")
    void recordDeletion(long userId);
    @Update("UPDATE users SET status='DELETED',deletion_requested_at=UTC_TIMESTAMP(3),deleted_at=UTC_TIMESTAMP(3) WHERE id=#{userId} AND status='ACTIVE'")
    int disableUser(long userId);
    @Update("UPDATE user_credentials SET email=CONCAT('deleted-',id,'@invalid.local'),password_hash='DELETED' WHERE user_id=#{userId}")
    void eraseCredential(long userId);
    @Update("UPDATE user_profiles SET display_name=NULL,avatar_url=NULL WHERE user_id=#{userId}")
    void eraseProfile(long userId);
    @Update("UPDATE conversations SET title='',status='DELETED',deleted_at=UTC_TIMESTAMP(3) WHERE user_id=#{userId}")
    void eraseConversations(long userId);
    @Update("UPDATE messages SET content='',status='FAILED',deleted_at=UTC_TIMESTAMP(3) WHERE user_id=#{userId}")
    void eraseMessages(long userId);
    @Update("UPDATE conversation_summaries SET content='' WHERE user_id=#{userId}")
    void eraseSummaries(long userId);
    @Delete("DELETE FROM verification_codes WHERE email=#{email}")
    void eraseCodes(String email);
    @Delete("DELETE FROM user_feedback WHERE user_id=#{userId}")
    void eraseFeedback(long userId);
    @Delete("DELETE FROM safety_events WHERE user_id=#{userId}")
    void eraseSafetyEvents(long userId);
}
