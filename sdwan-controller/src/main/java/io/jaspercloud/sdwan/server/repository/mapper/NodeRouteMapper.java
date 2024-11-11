package io.jaspercloud.sdwan.server.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.jaspercloud.sdwan.server.repository.po.NodeRoutePO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NodeRouteMapper extends BaseMapper<NodeRoutePO> {
}
