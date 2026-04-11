package com.kzzz3.argus.cortex.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kzzz3.argus.cortex.auth.infrastructure.entity.AuthSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuthSessionMapper extends BaseMapper<AuthSessionEntity> {
}
