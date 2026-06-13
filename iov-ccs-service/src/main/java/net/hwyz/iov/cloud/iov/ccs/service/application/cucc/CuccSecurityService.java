package net.hwyz.iov.cloud.iov.ccs.service.application.cucc;

/**
 * CUCC安全服务接口
 * <p>
 * 负责HMAC-SHA256验签和防重放攻击
 *
 * @author hwyz_leo
 */
public interface CuccSecurityService {

    /**
     * 验证签名
     * <p>
     * 使用HMAC-SHA256算法验证请求签名
     *
     * @param appid     应用ID
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param signature 签名
     * @param body      请求体
     * @return 是否验证通过
     */
    boolean verifySignature(String appid, long timestamp, String nonce, String signature, String body);

    /**
     * 检查重放攻击
     * <p>
     * 验证timestamp + nonce是否在有效时间窗内（默认5分钟）
     *
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @return 是否为重放攻击（true表示重放）
     */
    boolean isReplayAttack(long timestamp, String nonce);

    /**
     * 生成签名
     * <p>
     * 使用HMAC-SHA256算法生成签名（用于测试）
     *
     * @param appid     应用ID
     * @param timestamp 时间戳
     * @param nonce     随机数
     * @param body      请求体
     * @return 签名
     */
    String generateSignature(String appid, long timestamp, String nonce, String body);
}
