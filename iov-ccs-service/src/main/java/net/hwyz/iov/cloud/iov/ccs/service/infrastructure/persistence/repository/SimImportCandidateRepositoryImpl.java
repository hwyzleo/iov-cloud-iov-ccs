package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimImportCandidate;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimImportCandidateRepository;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper.SimImportCandidateMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.SimImportCandidatePo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SIM导入候选仓储实现
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class SimImportCandidateRepositoryImpl implements SimImportCandidateRepository {

    private final SimImportCandidateMapper simImportCandidateMapper;

    @Override
    public SimImportCandidate getByBatchAndIccid(String batchType, String batchNo, String iccid) {
        SimImportCandidatePo po = simImportCandidateMapper.selectByBatchAndIccid(batchType, batchNo, iccid);
        return po != null ? convertToEntity(po) : null;
    }

    @Override
    public boolean existsByBatchAndIccid(String batchType, String batchNo, String iccid) {
        return simImportCandidateMapper.existsByBatchAndIccid(batchType, batchNo, iccid);
    }

    @Override
    public List<SimImportCandidate> listPendingByBatchNo(String batchNo) {
        List<SimImportCandidatePo> poList = simImportCandidateMapper.selectPendingByBatchNo(batchNo);
        return poList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
    }

    @Override
    public int save(SimImportCandidate candidate) {
        SimImportCandidatePo po = convertToPo(candidate);
        return simImportCandidateMapper.insertPo(po);
    }

    @Override
    public int batchSave(List<SimImportCandidate> candidates) {
        List<SimImportCandidatePo> poList = candidates.stream()
                .map(this::convertToPo)
                .collect(Collectors.toList());
        return simImportCandidateMapper.batchInsertPo(poList);
    }

    @Override
    public int updateStoreStatus(Long id, String storeStatus, String failureReason) {
        return simImportCandidateMapper.updateStoreStatus(id, storeStatus, failureReason);
    }

    /**
     * PO转实体
     *
     * @param po 持久化对象
     * @return 实体
     */
    private SimImportCandidate convertToEntity(SimImportCandidatePo po) {
        return SimImportCandidate.builder()
                .id(po.getId())
                .batchType(po.getBatchType())
                .batchNo(po.getBatchNo())
                .sourceMno(po.getSourceMno())
                .iccid(po.getIccid())
                .imsi(po.getImsi())
                .msisdn(po.getMsisdn())
                .parseStatus(po.getParseStatus())
                .storeStatus(po.getStoreStatus())
                .failureReason(po.getFailureReason())
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
    private SimImportCandidatePo convertToPo(SimImportCandidate entity) {
        return SimImportCandidatePo.builder()
                .id(entity.getId())
                .batchType(entity.getBatchType())
                .batchNo(entity.getBatchNo())
                .sourceMno(entity.getSourceMno())
                .iccid(entity.getIccid())
                .imsi(entity.getImsi())
                .msisdn(entity.getMsisdn())
                .parseStatus(entity.getParseStatus())
                .storeStatus(entity.getStoreStatus())
                .failureReason(entity.getFailureReason())
                .build();
    }
}
