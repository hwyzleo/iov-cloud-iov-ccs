package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CCS Kafka Topic 配置模型单元测试（IOV-CCS-DSN-CR-003）
 * <p>
 * 验证目录标准 Topic 名称默认值、环境化参数默认值与配置校验（空名称 / 非法分区数阻断）。
 *
 * @author hwyz_leo
 */
@DisplayName("CcsKafkaTopicProperties 测试")
class CcsKafkaTopicPropertiesTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Nested
    @DisplayName("默认值")
    class DefaultsTests {

        @Test
        @DisplayName("两个生产 Topic 使用目录标准名")
        void standardTopicNames() {
            CcsKafkaTopicProperties properties = new CcsKafkaTopicProperties();
            assertEquals("ccs.sim-status.changed", properties.getSimStatusChanged().getName());
            assertEquals("ccs.vehicle-sim-binding-status.changed",
                    properties.getVehicleSimBindingStatusChanged().getName());
        }

        @Test
        @DisplayName("分区数与副本数默认值环境化且非硬编码")
        void defaultCapacity() {
            CcsKafkaTopicProperties properties = new CcsKafkaTopicProperties();
            assertEquals(3, properties.getPartitions());
            assertEquals((short) 1, properties.getReplicationFactor());
            assertTrue(properties.getConfigs().isEmpty());
        }
    }

    @Nested
    @DisplayName("配置校验")
    class ValidationTests {

        @Test
        @DisplayName("合法配置通过校验")
        void validConfigPasses() {
            CcsKafkaTopicProperties properties = new CcsKafkaTopicProperties();
            properties.setPartitions(6);
            properties.setReplicationFactor((short) 3);
            properties.setConfigs(Map.of("retention.ms", "604800000"));
            Set<ConstraintViolation<CcsKafkaTopicProperties>> violations = validator.validate(properties);
            assertTrue(violations.isEmpty());
        }

        @Test
        @DisplayName("空白 Topic 名称阻断校验")
        void blankTopicNameFails() {
            CcsKafkaTopicProperties properties = new CcsKafkaTopicProperties();
            properties.getSimStatusChanged().setName(" ");
            Set<ConstraintViolation<CcsKafkaTopicProperties>> violations = validator.validate(properties);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("分区数小于 1 阻断校验")
        void invalidPartitionsFail() {
            CcsKafkaTopicProperties properties = new CcsKafkaTopicProperties();
            properties.setPartitions(0);
            Set<ConstraintViolation<CcsKafkaTopicProperties>> violations = validator.validate(properties);
            assertFalse(violations.isEmpty());
        }

        @Test
        @DisplayName("副本数小于 1 阻断校验")
        void invalidReplicationFactorFails() {
            CcsKafkaTopicProperties properties = new CcsKafkaTopicProperties();
            properties.setReplicationFactor((short) 0);
            Set<ConstraintViolation<CcsKafkaTopicProperties>> violations = validator.validate(properties);
            assertFalse(violations.isEmpty());
        }
    }
}
