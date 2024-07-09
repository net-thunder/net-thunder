package io.jaspercloud.sdwan.stun;

import io.jaspercloud.sdwan.core.proto.SDWanProtos;
import lombok.Data;

import java.net.InetSocketAddress;
import java.util.Objects;

@Data
public class MappingAddress {

    private SDWanProtos.MappingTypeCode mappingType;
    private InetSocketAddress mappingAddress;

    public MappingAddress(SDWanProtos.MappingTypeCode mappingType, InetSocketAddress mappingAddress) {
        this.mappingType = mappingType;
        this.mappingAddress = mappingAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappingAddress that = (MappingAddress) o;
        return getMappingType() == that.getMappingType() && Objects.equals(getMappingAddress(), that.getMappingAddress());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMappingType(), getMappingAddress());
    }
}