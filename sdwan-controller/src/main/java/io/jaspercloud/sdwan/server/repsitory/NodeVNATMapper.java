package io.jaspercloud.sdwan.server.repsitory;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.jaspercloud.sdwan.server.entity.NodeVNAT;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NodeVNATMapper extends BaseMapper<NodeVNAT> {
}
