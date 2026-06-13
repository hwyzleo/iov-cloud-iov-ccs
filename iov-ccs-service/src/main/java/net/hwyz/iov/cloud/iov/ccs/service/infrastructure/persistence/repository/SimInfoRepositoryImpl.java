package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper.SimInfoMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimInfoPo;
import org.springframework.stereotype.Repository;

/**
 * SIM信息仓储实现
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class SimInfoRepositoryImpl implements SimInfoRepository {

    private final SimInfoMapper simInfoMapper;

    @Override
    public SimInfo getByIccid(String iccid) {
        SimInfoPo po = simInfoMapper.selectByIccid(iccid);
        return po != null ? convertToEntity(po) : null;
    }

    @Override
    public boolean existsByIccid(String iccid) {
        return simInfoMapper.existsByIccid(iccid);
    }

    @Override
    public int save(SimInfo simInfo) {
        SimInfoPo po = convertToPo(simInfo);
        return simInfoMapper.insertPo(po);
    }

    @Override
    public int update(SimInfo simInfo) {
        SimInfoPo po = convertToPo(simInfo);
        return simInfoMapper.updatePo(po);
    }

    @Override
    public void upsertSimInfo(SimInfo simInfo) {
        SimInfoPo existingPo = simInfoMapper.selectByIccid(simInfo.getIccid());

        if (existingPo != null) {
            // 更新：允许覆盖 IMSI/MSISDN/source_*
            existingPo.setImsi(simInfo.getImsi());
            existingPo.setMsisdn(simInfo.getMsisdn());
            existingPo.setSourceMno(simInfo.getSourceMno());
            existingPo.setSourceType(simInfo.getSourceType());
            existingPo.setSourceRef(simInfo.getSourceRef());
            simInfoMapper.updatePo(existingPo);
        } else {
            // 插入
            SimInfoPo po = convertToPo(simInfo);
            simInfoMapper.insertPo(po);
        }
    }

    /**
     * PO转实体
     *
     * @param po 持久化对象
     * @return 实体
     */
    private SimInfo convertToEntity(SimInfoPo po) {
        return SimInfo.builder()
                .id(po.getId())
                .iccid(po.getIccid())
                .imsi(po.getImsi())
                .msisdn(po.getMsisdn())
                .sourceMno(po.getSourceMno())
                .sourceType(po.getSourceType())
                .sourceRef(po.getSourceRef())
                .simStatus(po.getSimStatus())
                .bindingStatus(po.getBindingStatus())
                .realnameStatus(po.getRealnameStatus())
                .smsStatus(po.getSmsStatus())
                .dataStatus(po.getDataStatus())
                .voiceStatus(po.getVoiceStatus())
                .createdTime(po.getCreatedTime())
                .updatedTime(po.getUpdatedTime())
                .build();
    }

    /**
     * 实体转PO
     *
     * @param entity 实体
     * @return 持久化对象
     */
    private SimInfoPo convertToPo(SimInfo entity) {
        return SimInfoPo.builder()
                .id(entity.getId())
                .iccid(entity.getIccid())
                .imsi(entity.getImsi())
                .msisdn(entity.getMsisdn())
                .sourceMno(entity.getSourceMno())
                .sourceType(entity.getSourceType())
                .sourceRef(entity.getSourceRef())
                .simStatus(entity.getSimStatus())
                .bindingStatus(entity.getBindingStatus())
                .realnameStatus(entity.getRealnameStatus())
                .smsStatus(entity.getSmsStatus())
                .dataStatus(entity.getDataStatus())
                .voiceStatus(entity.getVoiceStatus())
                .build();
    }
}
