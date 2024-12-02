package io.jaspercloud.sdwan.server.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.jaspercloud.sdwan.server.repository.po.RouteRulePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface RouteRuleMapper extends BaseMapper<RouteRulePO> {

    List<RouteRulePO> selectByGroup(@Param("param") Map<String, Object> param);
}
