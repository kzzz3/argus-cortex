package com.kzzz3.argus.cortex.payment.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.kzzz3.argus.cortex.payment.infrastructure.entity.PaymentScanSessionEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PaymentScanSessionMapper extends BaseMapper<PaymentScanSessionEntity> {
}
