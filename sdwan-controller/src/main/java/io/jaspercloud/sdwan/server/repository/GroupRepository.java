package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.Group;
import io.jaspercloud.sdwan.server.repository.base.BaseRepositoryImpl;
import io.jaspercloud.sdwan.server.repository.mapper.GroupMapper;
import io.jaspercloud.sdwan.server.repository.po.GroupPO;
import org.springframework.stereotype.Repository;

@Repository
public class GroupRepository extends BaseRepositoryImpl<Group, GroupPO, GroupMapper> {

}
