package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.AppVersion;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.AppVersionMapper;
import io.jaspercloud.sdwan.server.repository.po.AppVersionPO;
import org.springframework.stereotype.Repository;

@Repository
public class AppVersionRepository extends BaseRepositoryImpl<AppVersion, AppVersionPO, AppVersionMapper> {

}
