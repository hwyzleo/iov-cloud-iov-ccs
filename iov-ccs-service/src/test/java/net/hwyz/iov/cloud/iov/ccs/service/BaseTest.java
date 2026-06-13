package net.hwyz.iov.cloud.iov.ccs.service;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

@Rollback
@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class BaseTest {

    @BeforeAll
    protected static void beforeAll() {
        System.setProperty("nacos.logging.default.config.enabled", "false");
    }
}
