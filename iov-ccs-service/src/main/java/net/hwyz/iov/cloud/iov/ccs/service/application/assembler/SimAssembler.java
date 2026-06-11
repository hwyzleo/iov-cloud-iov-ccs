package net.hwyz.iov.cloud.iov.ccs.service.application.assembler;

import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.BatchImportSimCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimCreateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimUpdateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.result.SimResult;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.Sim;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * SIM卡装配器
 *
 * @author hwyz_leo
 */
@Mapper(componentModel = "spring")
public interface SimAssembler {

    SimAssembler INSTANCE = Mappers.getMapper(SimAssembler.class);

    /**
     * 创建命令转换为实体
     *
     * @param cmd 创建命令
     * @return 实体
     */
    Sim toEntity(SimCreateCmd cmd);

    /**
     * 更新命令转换为实体
     *
     * @param cmd 更新命令
     * @return 实体
     */
    Sim toEntity(SimUpdateCmd cmd);

    /**
     * 实体转换为结果
     *
     * @param entity 实体
     * @return 结果
     */
    SimResult toResult(Sim entity);

    /**
     * 实体列表转换为结果列表
     *
     * @param entities 实体列表
     * @return 结果列表
     */
    List<SimResult> toResultList(List<Sim> entities);

    /**
     * 导入项转换为实体
     *
     * @param item 导入项
     * @return 实体
     */
    Sim toEntity(BatchImportSimCmd.SimImportItem item);

    /**
     * 导入项列表转换为实体列表
     *
     * @param items 导入项列表
     * @return 实体列表
     */
    List<Sim> toEntityList(List<BatchImportSimCmd.SimImportItem> items);
}
