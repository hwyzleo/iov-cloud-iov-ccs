package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config.CcsKafkaTopicProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CCS Kafka Topic 定义提供者单元测试（IOV-CCS-DSN-CR-003）
 * <p>
 * 验证 CCS 作为生产者声明的全部 Topic：
 * 目录标准名（ccs.sim-status.changed / ccs.vehicle-sim-binding-status.changed）全覆盖、无重复；
 * 分区数、副本数与 Topic 参数透传生效；不声明消费 Topic。
 *
 * @author hwyz_leo
 */
@DisplayName("CcsKafkaTopicDefinitionProvider 测试")
class CcsKafkaTopicDefinitionProviderTest {

    private CcsKafkaTopicProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CcsKafkaTopicProperties();
        properties.setPartitions(3);
        properties.setReplicationFactor((short) 3);
        properties.setConfigs(Map.of("retention.ms", "604800000"));
    }

    private Set<String> declaredTopics() {
        Collection<KafkaTopicDefinition> definitions = provider().topicDefinitions();
        return definitions.stream().map(KafkaTopicDefinition::name).collect(Collectors.toSet());
    }

    private CcsKafkaTopicDefinitionProvider provider() {
        return new CcsKafkaTopicDefinitionProvider(properties);
    }

    @Nested
    @DisplayName("Topic 声明")
    class TopicDeclarationTests {

        @Test
        @DisplayName("声明 SIM 状态变更 topic（目录标准名）")
        void declaresSimStatusTopic() {
            assertTrue(declaredTopics().contains("ccs.sim-status.changed"));
        }

        @Test
        @DisplayName("声明车卡绑定状态变更 topic（目录标准名）")
        void declaresCardBindingStatusTopic() {
            assertTrue(declaredTopics().contains("ccs.vehicle-sim-binding-status.changed"));
        }

        @Test
        @DisplayName("无重复声明")
        void noDuplicateDeclarations() {
            Collection<KafkaTopicDefinition> definitions = provider().topicDefinitions();
            long distinct = definitions.stream().map(KafkaTopicDefinition::name).distinct().count();
            assertEquals(definitions.size(), distinct);
        }

        @Test
        @DisplayName("不声明 CCS 仅消费的 topic")
        void doesNotDeclareConsumerOnlyTopics() {
            Set<String> topics = declaredTopics();
            assertFalse(topics.contains("vehicle-part-binding-changed"));
        }

        @Test
        @DisplayName("不引用旧 Topic 名称")
        void doesNotReferenceLegacyTopicNames() {
            Set<String> topics = declaredTopics();
            assertFalse(topics.contains("ccs-sim-status-changed"));
            assertFalse(topics.contains("card-binding-status-changed"));
        }
    }

    @Nested
    @DisplayName("分区与副本数")
    class PartitionTests {

        @Test
        @DisplayName("分区数透传")
        void partitionsPassedThrough() {
            Collection<KafkaTopicDefinition> definitions = provider().topicDefinitions();
            assertTrue(definitions.stream().allMatch(d -> d.partitions() == 3));
        }

        @Test
        @DisplayName("副本数透传")
        void replicationFactorPassedThrough() {
            Collection<KafkaTopicDefinition> definitions = provider().topicDefinitions();
            assertTrue(definitions.stream().allMatch(d -> d.replicationFactor() == 3));
        }
    }

    @Nested
    @DisplayName("Topic 参数")
    class TopicConfigTests {

        @Test
        @DisplayName("Topic 级配置透传")
        void configsPassedThrough() {
            Collection<KafkaTopicDefinition> definitions = provider().topicDefinitions();
            assertTrue(definitions.stream().allMatch(d -> d.configs().equals(Map.of("retention.ms", "604800000"))));
        }
    }
}
