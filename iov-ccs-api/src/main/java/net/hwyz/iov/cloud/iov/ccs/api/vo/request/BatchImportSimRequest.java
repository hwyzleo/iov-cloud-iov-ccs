package net.hwyz.iov.cloud.iov.ccs.api.vo.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入SIM卡请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportSimRequest {

    /**
     * 运营商类型
     */
    private String mnoType;

    /**
     * SIM卡列表
     */
    private List<SimImportItem> simList;

    /**
     * 批次号
     */
    private String batchNum;

    /**
     * SIM卡导入项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimImportItem {

        /**
         * 集成电路卡识别码
         */
        private String iccid;

        /**
         * 国际移动用户识别号
         */
        private String imsi;

        /**
         * 手机号
         */
        private String msisdn;
    }
}
