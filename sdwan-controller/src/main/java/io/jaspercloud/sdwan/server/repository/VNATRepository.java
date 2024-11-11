package io.jaspercloud.sdwan.server.repository;

import io.jaspercloud.sdwan.server.entity.VNAT;
import io.jaspercloud.sdwan.server.repository.base.BaseRepository;
import io.jaspercloud.sdwan.server.repository.mapper.VNATMapper;
import io.jaspercloud.sdwan.server.repository.po.VNATPO;
import org.springframework.stereotype.Repository;

@Repository
public class VNATRepository extends BaseRepository<VNAT, VNATPO, VNATMapper> {

}
