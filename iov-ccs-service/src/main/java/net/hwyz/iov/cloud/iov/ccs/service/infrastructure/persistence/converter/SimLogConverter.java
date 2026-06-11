package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimLog;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimLogPo;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * SIM卡变更日志持久化对象与领域实体转换器
 *
 * @author hwyz_leo
 */
@Mapper(componentModel = "spring")
public interface SimLogConverter {

    SimLogConverter INSTANCE = Mappers.getMapper(SimLogConverter.class);

    /**
     * 持久化对象转换为领域实体
     *
     * @param po 持久化对象
     * @return 领域实体
     */
    SimLog toEntity(SimLogPo po);

    /**
     * 领域实体转换为持久化对象
     *
     * @param entity 领域实体
     * @return 持久化对象
     */
    SimLogPo toPo(SimLog entity);
}
