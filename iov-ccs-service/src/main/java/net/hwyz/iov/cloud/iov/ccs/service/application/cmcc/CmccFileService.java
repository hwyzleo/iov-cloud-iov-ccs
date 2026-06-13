package net.hwyz.iov.cloud.iov.ccs.service.application.cmcc;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.CmccFileRequestRecord;

import java.time.LocalDateTime;

/**
 * CMCC文件处理服务
 * <p>
 * 负责CMCC文件的完整处理流程：请求→回调→下载→解密→解析→候选→入库
 *
 * @author hwyz_leo
 */
public interface CmccFileService {

    /**
     * 发起文件请求（定时任务调用）
     * <p>
     * 计算日期范围，调用CMCC接口请求文件，保存请求记录
     *
     * @return 文件请求记录
     */
    CmccFileRequestRecord requestFile();

    /**
     * 处理文件回调
     * <p>
     * 接收CMCC回调通知，触发后续处理流程
     *
     * @param fileId      文件ID
     * @param successful  是否成功
     * @param message     错误信息（successful=false时）
     */
    void handleCallback(String fileId, boolean successful, String message);

    /**
     * 处理文件（下载→解密→解压→解析→候选→入库）
     * <p>
     * 完整的文件处理流水线
     *
     * @param fileId 文件ID
     */
    void processFile(String fileId);

    /**
     * 计算同步日期范围
     *
     * @param lastRecord 上次同步记录（可为null）
     * @return 日期范围 [startDate, endDate]
     */
    LocalDateTime[] calculateDateRange(CmccFileRequestRecord lastRecord);
}
