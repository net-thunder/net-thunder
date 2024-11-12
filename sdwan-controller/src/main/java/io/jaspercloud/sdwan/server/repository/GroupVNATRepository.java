package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.GroupVNAT;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.GroupVNATMapper;
import io.jaspercloud.sdwan.server.repository.po.GroupVNATPO;
import org.springframework.stereotype.Repository;

@Repository
public class GroupVNATRepository extends BaseRepository<GroupVNAT, GroupVNATPO, GroupVNATMapper> {

}