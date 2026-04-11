package com.kzzz3.argus.cortex.conversation.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kzzz3.argus.cortex.conversation.infrastructure.entity.ConversationMessageEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageEntity> {
}
