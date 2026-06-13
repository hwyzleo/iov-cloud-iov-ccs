package net.hwyz.iov.cloud.iov.ccs.service.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 集成测试基类
 * <p>
 * 启动完整Spring上下文，连接真实MySQL/Redis/Kafka
 * 每个测试方法自动回滚，不产生脏数据
 */
@Rollback
@Transactional
@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseTest {

    @BeforeAll
    protected static void beforeAll() {
        System.setProperty("nacos.logging.default.config.enabled", "false");
    }
}
