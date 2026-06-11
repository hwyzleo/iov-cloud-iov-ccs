package net.hwyz.iov.cloud.iov.ccs.service.domain.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.exception.SimDuplicateException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.exception.SimNotFoundException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.exception.SimStateTransitionException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimLog;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimLogRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SIM卡领域服务
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimDomainService {

    private final SimRepository simRepository;
    private final SimLogRepository simLogRepository;

    /**
     * 根据ID查询SIM卡
     *
     * @param id 主键
     * @return SIM卡实体
     */
    public Sim getById(Long id) {
        Sim sim = simRepository.getById(id);
        if (sim == null) {
            throw new SimNotFoundException(id);
        }
        return sim;
    }

    /**
     * 根据ICCID查询SIM卡
     *
     * @param iccid ICCID
     * @return SIM卡实体
     */
    public Sim getByIccid(String iccid) {
        Sim sim = simRepository.getByIccid(iccid);
        if (sim == null) {
            throw new SimNotFoundException(iccid, true);
        }
        return sim;
    }

    /**
     * 分页查询SIM卡
     *
     * @param iccid     ICCID
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    public IPage<Sim> pageQuery(String iccid, LocalDateTime beginTime, LocalDateTime endTime, int pageNum, int pageSize) {
        return simRepository.pageQuery(iccid, beginTime, endTime, pageNum, pageSize);
    }

    /**
     * 新增SIM卡
     *
     * @param sim SIM卡实体
     * @return 新增的SIM卡ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(Sim sim) {
        // ICCID唯一性校验
        int count = simRepository.countByIccid(sim.getIccid());
        if (count > 0) {
            throw new SimDuplicateException(sim.getIccid(), true);
        }

        // 设置默认状态和能力
        sim.setSimState(1);
        sim.setSmsAbility(1);
        sim.setDataAbility(1);
        sim.setVoiceAbility(1);
        sim.setRowVersion(1);
        sim.setRowValid(1);

        // 保存SIM卡
        simRepository.save(sim);

        // 记录变更日志
        recordLog(sim, "新增SIM卡");

        log.info("新增SIM卡成功，ICCID: {}", sim.getIccid());
        return sim.getId();
    }

    /**
     * 修改SIM卡信息
     *
     * @param sim SIM卡实体
     * @return 更新后的SIM卡
     */
    @Transactional(rollbackFor = Exception.class)
    public Sim update(Sim sim) {
        // 校验SIM卡是否存在
        Sim existingSim = getById(sim.getId());

        // 如果修改了ICCID，校验新ICCID的唯一性
        if (!existingSim.getIccid().equals(sim.getIccid())) {
            int count = simRepository.countByIccid(sim.getIccid());
            if (count > 0) {
                throw new SimDuplicateException(sim.getIccid(), true);
            }
        }

        // 更新SIM卡
        sim.setModifyTime(LocalDateTime.now());
        simRepository.save(sim);

        // 记录变更日志
        recordLog(sim, "修改SIM卡信息");

        log.info("修改SIM卡成功，ID: {}", sim.getId());
        return sim;
    }

    /**
     * 删除SIM卡
     *
     * @param id 主键
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        Sim sim = getById(id);
        deleteSim(sim);
    }

    /**
     * 根据ICCID删除SIM卡
     *
     * @param iccid ICCID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIccid(String iccid) {
        Sim sim = getByIccid(iccid);
        deleteSim(sim);
    }

    /**
     * 批量删除SIM卡
     *
     * @param ids ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByIds(List<Long> ids) {
        for (Long id : ids) {
            deleteById(id);
        }
    }

    /**
     * 删除SIM卡及其日志
     *
     * @param sim SIM卡实体
     */
    private void deleteSim(Sim sim) {
        // 删除变更日志
        simLogRepository.deleteByIccid(sim.getIccid());
        // 删除SIM卡
        simRepository.deleteByIccid(sim.getIccid());
        log.info("删除SIM卡成功，ICCID: {}", sim.getIccid());
    }

    /**
     * 状态变更：测试 -> 库存
     *
     * @param id       主键
     * @param operator 操作者
     */
    @Transactional(rollbackFor = Exception.class)
    public void transitionToStock(Long id, String operator) {
        Sim sim = getById(id);
        sim.transitionToStock();
        sim.setModifyBy(operator);
        simRepository.save(sim);
        recordLog(sim, "状态变更：测试 -> 库存");
        log.info("SIM卡状态变更成功，ID: {}, 状态: 库存", id);
    }

    /**
     * 状态变更：库存 -> 激活
     *
     * @param id       主键
     * @param operator 操作者
     */
    @Transactional(rollbackFor = Exception.class)
    public void transitionToActive(Long id, String operator) {
        Sim sim = getById(id);
        sim.transitionToActive();
        sim.setModifyBy(operator);
        simRepository.save(sim);
        recordLog(sim, "状态变更：库存 -> 激活");
        log.info("SIM卡状态变更成功，ID: {}, 状态: 激活", id);
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
    @Transactional(rollbackFor = Exception.class)
    public void updateAbilities(Long id, Integer smsAbility, Integer dataAbility, Integer voiceAbility, String operator) {
        Sim sim = getById(id);
        sim.updateAbilities(smsAbility, dataAbility, voiceAbility);
        sim.setModifyBy(operator);
        simRepository.save(sim);
        recordLog(sim, "更新SIM卡能力");
        log.info("更新SIM卡能力成功，ID: {}", id);
    }

    /**
     * 批量导入SIM卡
     *
     * @param mnoType  运营商类型
     * @param simList  SIM卡列表
     * @param batchNum 批次号
     * @return 导入数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int batchImport(String mnoType, List<Sim> simList, String batchNum) {
        int count = 0;
        for (Sim sim : simList) {
            // ICCID去重检查
            int existingCount = simRepository.countByIccid(sim.getIccid());
            if (existingCount > 0) {
                log.warn("ICCID已存在，跳过导入: {}", sim.getIccid());
                continue;
            }

            // 设置默认状态和能力
            sim.setMnoCode(mnoType);
            sim.setSimState(1);
            sim.setSmsAbility(1);
            sim.setDataAbility(1);
            sim.setVoiceAbility(1);
            sim.setRowVersion(1);
            sim.setRowValid(1);

            // 保存SIM卡
            simRepository.save(sim);

            // 记录变更日志
            recordLog(sim, "数据批次[" + batchNum + "]数据导入");

            count++;
        }
        log.info("批量导入SIM卡完成，批次: {}, 导入数量: {}", batchNum, count);
        return count;
    }

    /**
     * 记录变更日志
     *
     * @param sim    SIM卡实体
     * @param remark 备注
     */
    private void recordLog(Sim sim, String remark) {
        SimLog simLog = SimLog.create(sim, remark);
        simLogRepository.save(simLog);
    }
}
