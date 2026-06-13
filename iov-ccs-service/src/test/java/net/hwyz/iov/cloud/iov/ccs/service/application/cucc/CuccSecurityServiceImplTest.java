package net.hwyz.iov.cloud.iov.ccs.service.application.cucc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CUCC安全服务测试")
class CuccSecurityServiceImplTest {

    @InjectMocks
    private CuccSecurityServiceImpl securityService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(securityService, "appid", "test_app");
        ReflectionTestUtils.setField(securityService, "appSecret", "test_secret_key");
        ReflectionTestUtils.setField(securityService, "timeWindowMinutes", 5);
    }

    // ========== generateSignature / verifySignature ==========

    @Test
    @DisplayName("签名生成与验证 - 正常流程")
    void generateAndVerifySignature_success() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce_001";
        String body = "{\"test\":\"data\"}";

        String signature = securityService.generateSignature("test_app", timestamp, nonce, body);

        assertNotNull(signature);
        assertFalse(signature.isEmpty());

        boolean verified = securityService.verifySignature("test_app", timestamp, nonce, signature, body);
        assertTrue(verified);
    }

    @Test
    @DisplayName("签名验证 - 错误签名返回false")
    void verifySignature_wrongSignature() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce_002";
        String body = "{\"test\":\"data\"}";

        boolean verified = securityService.verifySignature("test_app", timestamp, nonce, "wrong_signature", body);
        assertFalse(verified);
    }

    @Test
    @DisplayName("签名验证 - 错误appid返回false")
    void verifySignature_wrongAppid() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce_003";
        String body = "{\"test\":\"data\"}";

        String signature = securityService.generateSignature("test_app", timestamp, nonce, body);

        boolean verified = securityService.verifySignature("wrong_app", timestamp, nonce, signature, body);
        assertFalse(verified);
    }

    @Test
    @DisplayName("签名验证 - 不同body返回false")
    void verifySignature_differentBody() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce_004";
        String body1 = "{\"test\":\"data1\"}";
        String body2 = "{\"test\":\"data2\"}";

        String signature = securityService.generateSignature("test_app", timestamp, nonce, body1);

        boolean verified = securityService.verifySignature("test_app", timestamp, nonce, signature, body2);
        assertFalse(verified);
    }

    // ========== isReplayAttack ==========

    @Test
    @DisplayName("防重放 - 正常请求不判定为重放")
    void isReplayAttack_normalRequest() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce_replay_001";

        boolean isReplay = securityService.isReplayAttack(timestamp, nonce);
        assertFalse(isReplay);
    }

    @Test
    @DisplayName("防重放 - 相同nonce+timestamp判定为重放")
    void isReplayAttack_duplicateNonce() {
        long timestamp = System.currentTimeMillis();
        String nonce = "nonce_replay_002";

        // 第一次请求
        assertFalse(securityService.isReplayAttack(timestamp, nonce));
        // 相同nonce+timestamp的第二次请求
        assertTrue(securityService.isReplayAttack(timestamp, nonce));
    }

    @Test
    @DisplayName("防重放 - 超时请求判定为重放")
    void isReplayAttack_expiredTimestamp() {
        long timestamp = System.currentTimeMillis() - 10 * 60 * 1000; // 10分钟前
        String nonce = "nonce_replay_003";

        boolean isReplay = securityService.isReplayAttack(timestamp, nonce);
        assertTrue(isReplay);
    }

    @Test
    @DisplayName("防重放 - 不同nonce不判定为重放")
    void isReplayAttack_differentNonce() {
        long timestamp = System.currentTimeMillis();

        assertFalse(securityService.isReplayAttack(timestamp, "nonce_a"));
        assertFalse(securityService.isReplayAttack(timestamp, "nonce_b"));
    }
}
