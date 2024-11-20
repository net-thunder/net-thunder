package io.jaspercloud.sdwan.server.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.jaspercloud.sdwan.server.repository.po.AppVersionPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppVersionMapper extends BaseMapper<AppVersionPO> {
}
