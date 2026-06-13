package net.hwyz.iov.cloud.iov.ccs.service.application.cucc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CandidateParseStatus;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.CandidateStoreStatus;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.MnoType;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.SimStorageEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimImportCandidate;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimImportCandidateRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimNormalizationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * CUCC SIM服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CuccSimServiceImpl implements CuccSimService {

    private final SimNormalizationService simNormalizationService;
    private final SimImportCandidateRepository simImportCandidateRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public CuccProcessResult processSimInfo(String batchNo, List<SimInfo> simList) {
        log.info("处理CUCC SIM信息推送: batchNo={}, count={}", batchNo, simList != null ? simList.size() : 0);

        if (simList == null || simList.isEmpty()) {
            log.info("CUCC推送数据为空，直接返回成功");
            return CuccProcessResult.emptyData();
        }

        List<SimImportCandidate> candidates = new ArrayList<>();
        List<SimInfo> validSimList = new ArrayList<>();
        List<String> failedIccids = new ArrayList<>();

        // Step 1: 解析和规范化
        for (SimInfo simInfo : simList) {
            try {
                // 规范化
                SimInfo normalizedSim = simNormalizationService.normalize(simInfo);
                validSimList.add(normalizedSim);

                // 创建候选记录
                SimImportCandidate candidate = SimImportCandidate.builder()
                        .batchType(MnoType.CUCC.getCode())
                        .batchNo(batchNo)
                        .sourceMno(MnoType.CUCC.getCode())
                        .iccid(normalizedSim.getIccid())
                        .imsi(normalizedSim.getImsi())
                        .msisdn(normalizedSim.getMsisdn())
                        .parseStatus(CandidateParseStatus.OK.getCode())
                        .storeStatus(CandidateStoreStatus.PENDING.getCode())
                        .build();
                candidates.add(candidate);
            } catch (Exception e) {
                log.warn("CUCC SIM数据规范化失败: iccid={}, error={}", simInfo.getIccid(), e.getMessage());
                failedIccids.add(simInfo.getIccid());

                // 创建失败的候选记录
                SimImportCandidate candidate = SimImportCandidate.builder()
                        .batchType(MnoType.CUCC.getCode())
                        .batchNo(batchNo)
                        .sourceMno(MnoType.CUCC.getCode())
                        .iccid(simInfo.getIccid())
                        .imsi(simInfo.getImsi())
                        .msisdn(simInfo.getMsisdn())
                        .parseStatus(CandidateParseStatus.INVALID.getCode())
                        .storeStatus(CandidateStoreStatus.FAILED.getCode())
                        .failureReason(e.getMessage())
                        .build();
                candidates.add(candidate);
            }
        }

        // Step 2: 批量保存候选记录
        if (!candidates.isEmpty()) {
            simImportCandidateRepository.batchSave(candidates);
            log.info("CUCC候选记录保存完成: batchNo={}, total={}, ok={}, invalid={}",
                    batchNo, candidates.size(), validSimList.size(), failedIccids.size());
        }

        // Step 3: 发布存储事件，触发入库
        if (!validSimList.isEmpty()) {
            eventPublisher.publishEvent(SimStorageEvent.builder()
                    .batchType(MnoType.CUCC.getCode())
                    .batchNo(batchNo)
                    .sourceMno(MnoType.CUCC.getCode())
                    .simInfoList(validSimList)
                    .build());
        }

        // Step 4: 返回处理结果
        int successCount = validSimList.size();
        int failedCount = failedIccids.size();
        int totalCount = successCount + failedCount;

        if (failedIccids.isEmpty()) {
            return CuccProcessResult.allSuccess(totalCount);
        } else {
            return CuccProcessResult.partialFailed(totalCount, successCount, 0, failedCount, failedIccids);
        }
    }
}
