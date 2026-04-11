package com.kzzz3.argus.cortex.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kzzz3.argus.cortex.auth.infrastructure.entity.AccountEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<AccountEntity> {
}
