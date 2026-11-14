package com.stillgrove.stillgroveapi.feedback;

import org.apache.ibatis.annotations.*;

@Mapper
public interface FeedbackMapper {
    @Insert("""
            INSERT INTO user_feedback(user_id,message_id,rating,reason_code)
            SELECT #{userId},m.id,#{rating},#{reason}
            FROM messages m JOIN conversations c ON c.id=m.conversation_id
            WHERE m.id=#{messageId} AND m.user_id=#{userId} AND c.user_id=#{userId}
              AND c.status='ACTIVE' AND m.role='ASSISTANT' AND m.status='COMPLETED'
            ON DUPLICATE KEY UPDATE rating=VALUES(rating),reason_code=VALUES(reason_code)
            """)
    int save(@Param("userId") long userId,@Param("messageId") long messageId,
             @Param("rating") String rating,@Param("reason") String reason);
}
