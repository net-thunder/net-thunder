package io.jaspercloud.sdwan.server.repsitory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.jaspercloud.sdwan.server.entity.RouteRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RouteRuleMapper extends BaseMapper<RouteRule> {
}
