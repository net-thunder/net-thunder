package io.jaspercloud.sdwan.server.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.jaspercloud.sdwan.server.repository.po.AccountPO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<AccountPO> {
}
