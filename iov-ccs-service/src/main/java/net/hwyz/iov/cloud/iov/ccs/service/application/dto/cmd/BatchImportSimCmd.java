package net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量导入SIM卡命令
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportSimCmd {

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
     * 导入者
     */
    private String importBy;

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
