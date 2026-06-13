package net.hwyz.iov.cloud.iov.ccs.service.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.MnoType;
import net.hwyz.iov.cloud.iov.ccs.api.vo.enums.SourceType;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimNormalizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/**
 * 手动录入SIM服务实现
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ManualSimServiceImpl implements ManualSimService {

    private final SimInfoRepository simInfoRepository;
    private final SimNormalizationService simNormalizationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void saveSimInfo(SimInfo simInfo) {
        log.info("手动保存SIM信息: iccid={}", simInfo.getIccid());

        // 规范化
        SimInfo normalizedSim = simNormalizationService.normalize(simInfo);

        // 设置默认状态
        normalizedSim.setSimStatus(1);  // TEST
        normalizedSim.setBindingStatus(0);  // UNBOUNDED
        normalizedSim.setRealnameStatus(1);  // NO_REAL_NAME
        normalizedSim.setSmsStatus(true);
        normalizedSim.setDataStatus(true);
        normalizedSim.setVoiceStatus(true);

        // 设置来源
        normalizedSim.setSourceMno(MnoType.UNKNOWN.getCode());
        if (normalizedSim.getSourceType() == null) {
            normalizedSim.setSourceType("manual_save");
        }

        // 检查ICCID是否已存在
        if (simInfoRepository.existsByIccid(normalizedSim.getIccid())) {
            throw new ServiceException("ICCID已存在: " + normalizedSim.getIccid());
        }

        // 保存
        simInfoRepository.save(normalizedSim);
        log.info("SIM信息保存成功: iccid={}", normalizedSim.getIccid());
    }

    @Override
    @Transactional
    public void batchSaveSimInfo(List<SimInfo> simInfoList) {
        log.info("批量保存SIM信息: count={}", simInfoList != null ? simInfoList.size() : 0);

        if (simInfoList == null || simInfoList.isEmpty()) {
            return;
        }

        List<String> failedIccids = new ArrayList<>();

        for (SimInfo simInfo : simInfoList) {
            try {
                saveSimInfo(simInfo);
            } catch (Exception e) {
                log.warn("SIM信息保存失败: iccid={}, error={}", simInfo.getIccid(), e.getMessage());
                failedIccids.add(simInfo.getIccid());
            }
        }

        if (!failedIccids.isEmpty()) {
            throw new BatchSaveException("批量保存部分失败: " + failedIccids.size() + "条", failedIccids);
        }
    }

    @Override
    @Transactional
    public void syncData(String hexData) {
        log.info("同步SIM数据: hexLength={}", hexData != null ? hexData.length() : 0);

        if (hexData == null || hexData.isEmpty()) {
            throw new ServiceException("Hex数据不能为空");
        }

        try {
            // Step 1: Hex解码
            byte[] bytes = HexFormat.of().parseHex(hexData);

            // Step 2: JSON解析
            String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            List<Map<String, String>> dataList = objectMapper.readValue(json, new TypeReference<>() {});

            if (dataList == null || dataList.isEmpty()) {
                log.info("同步数据为空");
                return;
            }

            // Step 3: 处理每条记录
            for (Map<String, String> data : dataList) {
                String iccid = data.get("iccid");
                String imsi = data.get("imsi");
                String msisdn = data.get("msisdn");

                SimInfo simInfo = SimInfo.builder()
                        .iccid(iccid)
                        .imsi(imsi)
                        .msisdn(msisdn)
                        .sourceMno(MnoType.UNKNOWN.getCode())
                        .sourceType("sync_data")
                        .build();

                // 规范化
                SimInfo normalizedSim = simNormalizationService.normalize(simInfo);

                // 强制状态字段
                normalizedSim.setSimStatus(1);  // TEST
                normalizedSim.setBindingStatus(0);  // UNBOUNDED
                normalizedSim.setRealnameStatus(1);  // NO_REAL_NAME
                normalizedSim.setSmsStatus(true);
                normalizedSim.setDataStatus(true);
                normalizedSim.setVoiceStatus(true);

                // Upsert语义
                simInfoRepository.upsertSimInfo(normalizedSim);
            }

            log.info("同步SIM数据完成: count={}", dataList.size());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("Hex解码或JSON解析失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimInfo> listSimInfo(Map<String, Object> params) {
        log.debug("条件查询SIM信息: params={}", params);
        return simInfoRepository.listByCondition(params);
    }

    @Override
    @Transactional(readOnly = true)
    public SimInfo getSimInfo(String iccid) {
        log.debug("查询SIM信息详情: iccid={}", iccid);
        SimInfo simInfo = simInfoRepository.getByIccid(iccid);
        if (simInfo == null) {
            throw new ServiceException("SIM信息不存在: " + iccid);
        }
        return simInfo;
    }

    @Override
    @Transactional
    public void updateSimInfo(String iccid, SimInfo simInfo) {
        log.info("更新SIM信息: iccid={}", iccid);

        SimInfo existing = simInfoRepository.getByIccid(iccid);
        if (existing == null) {
            throw new ServiceException("SIM信息不存在: " + iccid);
        }

        // 仅手动/同步来源可更新
        String sourceType = existing.getSourceType();
        if (sourceType == null || (!sourceType.equals(SourceType.MANUAL_SAVE.getCode())
                && !sourceType.equals(SourceType.MANUAL_BATCH.getCode())
                && !sourceType.equals(SourceType.SYNC_DATA.getCode()))) {
            throw new ServiceException("仅手动/同步来源的SIM信息可更新");
        }

        // 更新允许的字段
        if (simInfo.getImsi() != null) {
            existing.setImsi(simInfo.getImsi());
        }
        if (simInfo.getMsisdn() != null) {
            existing.setMsisdn(simInfo.getMsisdn());
        }
        if (simInfo.getSimStatus() != null) {
            existing.setSimStatus(simInfo.getSimStatus());
        }
        if (simInfo.getBindingStatus() != null) {
            existing.setBindingStatus(simInfo.getBindingStatus());
        }
        if (simInfo.getRealnameStatus() != null) {
            existing.setRealnameStatus(simInfo.getRealnameStatus());
        }
        if (simInfo.getSmsStatus() != null) {
            existing.setSmsStatus(simInfo.getSmsStatus());
        }
        if (simInfo.getDataStatus() != null) {
            existing.setDataStatus(simInfo.getDataStatus());
        }
        if (simInfo.getVoiceStatus() != null) {
            existing.setVoiceStatus(simInfo.getVoiceStatus());
        }

        simInfoRepository.update(existing);
        log.info("SIM信息更新成功: iccid={}", iccid);
    }

    @Override
    @Transactional
    public void deleteSimInfo(String iccid) {
        log.info("删除SIM信息: iccid={}", iccid);

        SimInfo existing = simInfoRepository.getByIccid(iccid);
        if (existing == null) {
            throw new ServiceException("SIM信息不存在: " + iccid);
        }

        simInfoRepository.deleteById(existing.getId());
        log.info("SIM信息删除成功: iccid={}, sourceMno={}", iccid, existing.getSourceMno());
    }

    @Override
    @Transactional
    public void batchDeleteSimInfo(List<String> iccids) {
        log.info("批量删除SIM信息: count={}", iccids != null ? iccids.size() : 0);

        if (iccids == null || iccids.isEmpty()) {
            return;
        }

        List<String> failedIccids = new ArrayList<>();

        for (String iccid : iccids) {
            try {
                deleteSimInfo(iccid);
            } catch (Exception e) {
                log.warn("SIM信息删除失败: iccid={}, error={}", iccid, e.getMessage());
                failedIccids.add(iccid);
            }
        }

        if (!failedIccids.isEmpty()) {
            throw new BatchSaveException("批量删除部分失败: " + failedIccids.size() + "条", failedIccids);
        }
    }
}
