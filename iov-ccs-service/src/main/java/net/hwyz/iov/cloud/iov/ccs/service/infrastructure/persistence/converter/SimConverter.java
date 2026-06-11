package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * SIM卡持久化对象与领域实体转换器
 *
 * @author hwyz_leo
 */
@Mapper(componentModel = "spring")
public interface SimConverter {

    SimConverter INSTANCE = Mappers.getMapper(SimConverter.class);

    /**
     * 持久化对象转换为领域实体
     *
     * @param po 持久化对象
     * @return 领域实体
     */
    Sim toEntity(SimPo po);

    /**
     * 领域实体转换为持久化对象
     *
     * @param entity 领域实体
     * @return 持久化对象
     */
    SimPo toPo(Sim entity);
}
