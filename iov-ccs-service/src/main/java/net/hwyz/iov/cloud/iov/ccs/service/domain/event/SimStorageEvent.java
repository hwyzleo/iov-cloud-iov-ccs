package net.hwyz.iov.cloud.iov.ccs.service.domain.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;

import java.util.List;

/**
 * SIM存储事件
 * <p>
 * 当SIM数据完成解析后，发布此事件触发入库操作
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimStorageEvent {

    /**
     * 批次类型：CMCC/CUCC
     */
    private String batchType;

    /**
     * 批次号：fileId 或 batchNo
     */
    private String batchNo;

    /**
     * 来源运营商：CMCC/CUCC
     */
    private String sourceMno;

    /**
     * SIM信息列表
     */
    private List<SimInfo> simInfoList;
}
