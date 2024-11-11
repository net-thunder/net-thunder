package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.GroupMember;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.GroupMemberMapper;
import io.jaspercloud.sdwan.server.repository.po.GroupMemberPO;
import org.springframework.stereotype.Repository;

@Repository
public class GroupMemberRepository extends BaseRepository<GroupMember, GroupMemberPO, GroupMemberMapper> {

}
