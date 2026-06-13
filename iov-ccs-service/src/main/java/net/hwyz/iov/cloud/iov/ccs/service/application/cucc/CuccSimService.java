package net.hwyz.iov.cloud.iov.ccs.service.application.cucc;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;

import java.util.List;

/**
 * CUCC SIM服务接口
 * <p>
 * 负责处理中国联通推送的SIM卡信息
 *
 * @author hwyz_leo
 */
public interface CuccSimService {

    /**
     * 处理SIM信息推送
     * <p>
     * 接收CUCC推送的SIM信息，解析并入库
     *
     * @param batchNo 批次号
     * @param simList SIM信息列表
     * @return 处理结果，包含失败的ICCID列表
     */
    CuccProcessResult processSimInfo(String batchNo, List<SimInfo> simList);

    /**
     * CUCC处理结果
     */
    record CuccProcessResult(
            boolean success,
            int totalCount,
            int successCount,
            int duplicateCount,
            int failedCount,
            List<String> failedIccids,
            String message
    ) {
        /**
         * 全部成功
         */
        public static CuccProcessResult allSuccess(int totalCount) {
            return new CuccProcessResult(true, totalCount, totalCount, 0, 0, List.of(), "全部成功");
        }

        /**
         * 部分失败
         */
        public static CuccProcessResult partialFailed(int totalCount, int successCount, int duplicateCount,
                                                       int failedCount, List<String> failedIccids) {
            return new CuccProcessResult(false, totalCount, successCount, duplicateCount, failedCount,
                    failedIccids, "部分失败");
        }

        /**
         * 数据为空
         */
        public static CuccProcessResult emptyData() {
            return new CuccProcessResult(true, 0, 0, 0, 0, List.of(), "数据为空");
        }
    }
}
