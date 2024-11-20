package io.jaspercloud.sdwan.server.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.jaspercloud.sdwan.server.repository.po.AppVersionPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AppVersionMapper extends BaseMapper<AppVersionPO> {

    @Select("select * from biz_app_version\n" +
            "where id in (\n" +
            "    select max(id) from biz_app_version\n" +
            "    group by os\n" +
            ")")
    List<AppVersionPO> lastVersionList();
}
