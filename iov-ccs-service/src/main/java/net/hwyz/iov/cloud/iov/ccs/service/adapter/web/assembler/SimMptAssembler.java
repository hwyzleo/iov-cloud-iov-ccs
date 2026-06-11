package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.SimCreateMpt;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.SimMpt;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.SimUpdateMpt;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimCreateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimUpdateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.query.SimQuery;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.result.SimResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * SIM卡管理后台装配器
 *
 * @author hwyz_leo
 */
@Mapper(componentModel = "spring")
public interface SimMptAssembler {

    SimMptAssembler INSTANCE = Mappers.getMapper(SimMptAssembler.class);

    /**
     * 结果转换为管理后台视图对象
     *
     * @param result 结果
     * @return 管理后台视图对象
     */
    SimMpt toMpt(SimResult result);

    /**
     * 创建视图对象转换为创建命令
     *
     * @param mpt 创建视图对象
     * @return 创建命令
     */
    SimCreateCmd toCreateCmd(SimCreateMpt mpt);

    /**
     * 更新视图对象转换为更新命令
     *
     * @param mpt 更新视图对象
     * @return 更新命令
     */
    SimUpdateCmd toUpdateCmd(SimUpdateMpt mpt);

    /**
     * 管理后台视图对象转换为查询条件
     *
     * @param mpt 管理后台视图对象
     * @return 查询条件
     */
    SimQuery toQuery(SimMpt mpt);
}
