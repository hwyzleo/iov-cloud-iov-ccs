package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimLog;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimLogRepository;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.converter.SimLogConverter;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper.SimLogMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimLogPo;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * SIM卡变更日志仓储实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SimLogRepositoryImpl implements SimLogRepository {

    private final SimLogMapper simLogMapper;
    private final SimLogConverter simLogConverter;

    @Override
    public int save(SimLog simLog) {
        SimLogPo po = simLogConverter.toPo(simLog);
        if (simLog.getId() == null) {
            return simLogMapper.insert(po);
        } else {
            return simLogMapper.updateById(po);
        }
    }

    @Override
    public int deleteByIccid(String iccid) {
        return simLogMapper.deleteByIccid(iccid);
    }

    @Override
    public List<SimLog> listByIccid(String iccid) {
        LambdaQueryWrapper<SimLogPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SimLogPo::getIccid, iccid);
        wrapper.eq(SimLogPo::getRowValid, 1);
        wrapper.orderByDesc(SimLogPo::getCreateTime);
        List<SimLogPo> poList = simLogMapper.selectList(wrapper);
        return poList.stream().map(simLogConverter::toEntity).toList();
    }
}
