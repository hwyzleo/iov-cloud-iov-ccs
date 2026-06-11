package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimRepository;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.converter.SimConverter;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper.SimMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimPo;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SIM卡仓储实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SimRepositoryImpl implements SimRepository {

    private final SimMapper simMapper;
    private final SimConverter simConverter;

    @Override
    public Sim getById(Long id) {
        SimPo po = simMapper.selectById(id);
        return simConverter.toEntity(po);
    }

    @Override
    public Sim getByIccid(String iccid) {
        SimPo po = simMapper.selectByIccid(iccid);
        return simConverter.toEntity(po);
    }

    @Override
    public int countByIccid(String iccid) {
        return simMapper.countByIccid(iccid);
    }

    @Override
    public int save(Sim sim) {
        SimPo po = simConverter.toPo(sim);
        if (sim.getId() == null) {
            return simMapper.insert(po);
        } else {
            return simMapper.updateById(po);
        }
    }

    @Override
    public int deleteById(Long id) {
        return simMapper.deleteById(id);
    }

    @Override
    public int deleteByIccid(String iccid) {
        return simMapper.deleteByIccid(iccid);
    }

    @Override
    public IPage<Sim> pageQuery(String iccid, LocalDateTime beginTime, LocalDateTime endTime, int pageNum, int pageSize) {
        Page<SimPo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SimPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SimPo::getRowValid, 1);
        if (iccid != null && !iccid.isEmpty()) {
            wrapper.like(SimPo::getIccid, iccid);
        }
        if (beginTime != null) {
            wrapper.ge(SimPo::getCreateTime, beginTime);
        }
        if (endTime != null) {
            wrapper.le(SimPo::getCreateTime, endTime);
        }
        wrapper.orderByDesc(SimPo::getCreateTime);
        IPage<SimPo> poPage = simMapper.selectPage(page, wrapper);
        return poPage.convert(simConverter::toEntity);
    }

    @Override
    public List<Sim> listAll() {
        LambdaQueryWrapper<SimPo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SimPo::getRowValid, 1);
        List<SimPo> poList = simMapper.selectList(wrapper);
        return poList.stream().map(simConverter::toEntity).toList();
    }
}
