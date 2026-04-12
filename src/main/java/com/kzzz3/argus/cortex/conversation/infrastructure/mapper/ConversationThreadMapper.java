package com.kzzz3.argus.cortex.conversation.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kzzz3.argus.cortex.conversation.infrastructure.entity.ConversationThreadEntity;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ConversationThreadMapper extends BaseMapper<ConversationThreadEntity> {

    @Select("SELECT DISTINCT owner_account_id FROM conversation_thread WHERE conversation_id = #{conversationId}")
    List<String> selectOwnerAccountIdsByConversationId(String conversationId);
}
