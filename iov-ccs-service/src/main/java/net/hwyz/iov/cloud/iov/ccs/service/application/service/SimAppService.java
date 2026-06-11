package net.hwyz.iov.cloud.iov.ccs.service.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.BatchImportSimCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimCreateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimUpdateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.query.SimQuery;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.result.SimResult;
import net.hwyz.iov.cloud.iov.ccs.service.application.assembler.SimAssembler;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimDomainService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * SIM卡应用服务
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimAppService {

    private final SimDomainService simDomainService;
    private final SimAssembler simAssembler;

    /**
     * 分页查询SIM卡
     *
     * @param query 查询条件
     * @return 分页结果
     */
    public IPage<SimResult> pageQuery(SimQuery query) {
        IPage<Sim> page = simDomainService.pageQuery(
                query.getIccid(),
                query.getBeginTime(),
                query.getEndTime(),
                query.getPageNum(),
                query.getPageSize()
        );
        return page.convert(simAssembler::toResult);
    }

    /**
     * 根据ID查询SIM卡
     *
     * @param id 主键
     * @return SIM卡结果
     */
    public SimResult getById(Long id) {
        Sim sim = simDomainService.getById(id);
        return simAssembler.toResult(sim);
    }

    /**
     * 新增SIM卡
     *
     * @param cmd 创建命令
     * @return 新增的SIM卡ID
     */
    public Long create(SimCreateCmd cmd) {
        Sim sim = simAssembler.toEntity(cmd);
        return simDomainService.create(sim);
    }

    /**
     * 修改SIM卡
     *
     * @param cmd 更新命令
     * @return 更新后的SIM卡
     */
    public SimResult update(SimUpdateCmd cmd) {
        Sim sim = simAssembler.toEntity(cmd);
        Sim updatedSim = simDomainService.update(sim);
        return simAssembler.toResult(updatedSim);
    }

    /**
     * 删除SIM卡
     *
     * @param id 主键
     */
    public void deleteById(Long id) {
        simDomainService.deleteById(id);
    }

    /**
     * 批量删除SIM卡
     *
     * @param ids ID列表
     */
    public void deleteByIds(List<Long> ids) {
        simDomainService.deleteByIds(ids);
    }

    /**
     * 状态变更：测试 -> 库存
     *
     * @param id       主键
     * @param operator 操作者
     */
    public void transitionToStock(Long id, String operator) {
        simDomainService.transitionToStock(id, operator);
    }

    /**
     * 状态变更：库存 -> 激活
     *
     * @param id       主键
     * @param operator 操作者
     */
    public void transitionToActive(Long id, String operator) {
        simDomainService.transitionToActive(id, operator);
    }

    /**
     * 更新SIM卡能力
     *
     * @param id           主键
     * @param smsAbility   短信能力
     * @param dataAbility  数据能力
     * @param voiceAbility 语音能力
     * @param operator     操作者
     */
    public void updateAbilities(Long id, Integer smsAbility, Integer dataAbility, Integer voiceAbility, String operator) {
        simDomainService.updateAbilities(id, smsAbility, dataAbility, voiceAbility, operator);
    }

    /**
     * 批量导入SIM卡
     *
     * @param cmd 批量导入命令
     * @return 导入数量
     */
    public int batchImport(BatchImportSimCmd cmd) {
        List<Sim> simList = simAssembler.toEntityList(cmd.getSimList());
        return simDomainService.batchImport(cmd.getMnoType(), simList, cmd.getBatchNum());
    }
}
