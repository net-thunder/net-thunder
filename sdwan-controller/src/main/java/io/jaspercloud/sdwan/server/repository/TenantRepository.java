package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.Tenant;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.TenantMapper;
import io.jaspercloud.sdwan.server.repository.po.TenantPO;
import org.springframework.stereotype.Repository;

@Repository
public class TenantRepository extends BaseRepository<Tenant, TenantPO, TenantMapper> {

}
