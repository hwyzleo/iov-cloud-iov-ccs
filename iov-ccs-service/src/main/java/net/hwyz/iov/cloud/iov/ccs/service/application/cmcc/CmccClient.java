package net.hwyz.iov.cloud.iov.ccs.service.application.cmcc;

import java.time.LocalDateTime;

/**
 * CMCC接口客户端
 * <p>
 * 负责与中国移动API交互，包括Token获取/缓存、文件请求、文件下载
 *
 * @author hwyz_leo
 */
public interface CmccClient {

    /**
     * 获取访问Token
     * <p>
     * Token缓存策略：Redis key = MNO:CMCC:TOKEN，使用分布式锁 MNO:CMCC:TOKEN:LOCK
     *
     * @return Token值
     */
    String getToken();

    /**
     * 发起文件请求
     * <p>
     * 调用 CMCC POST {base_url}/SI_FileRequest 接口
     *
     * @param requestType 请求类型（SIM_EFFECTIVE_REQUEST）
     * @param startDate   开始日期
     * @param endDate     结束日期
     * @param encrypted   是否加密
     * @return 文件请求响应，包含fileId和timestamp
     */
    FileRequestResult requestFile(String requestType, LocalDateTime startDate, LocalDateTime endDate, boolean encrypted);

    /**
     * 下载文件
     * <p>
     * 调用 CMCC POST {base_url}/SI_FileDownload 接口
     *
     * @param fileId    文件ID
     * @param timestamp 时间戳
     * @return 文件内容（字节数组）
     */
    byte[] downloadFile(String fileId, String timestamp);

    /**
     * 文件请求结果
     */
    record FileRequestResult(
            String fileId,
            String timestamp,
            boolean success,
            String errorMessage
    ) {}
}
