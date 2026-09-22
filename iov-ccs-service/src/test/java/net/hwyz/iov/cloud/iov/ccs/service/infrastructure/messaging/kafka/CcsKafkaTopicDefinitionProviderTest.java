package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicDefinition;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config.CcsKafkaTopicProvisioningProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CCS Kafka Topic 定义提供者单元测试
 * <p>
 * 验证 CCS 作为生产者声明的全部 Topic：
 * sim-status / card-binding-status 全覆盖、无重复；分区数与副本数透传生效。
 *
 * @author hwyz_leo
 */
@DisplayName("CcsKafkaTopicDefinitionProvider 测试")
class CcsKafkaTopicDefinitionProviderTest {

    private CcsKafkaTopicProvisioningProperties properties;

    @BeforeEach
    void setUp() {
        properties = new CcsKafkaTopicProvisioningProperties();
        properties.setPartitions(3);
        properties.setReplicationFactor((short) 3);
    }

    private Set<String> declaredTopics() {
        Collection<KafkaTopicDefinition> definitions = provider().topicDefinitions();
        return definitions.stream().map(KafkaTopicDefinition::name).collect(Collectors.toSet());
    }

    private CcsKafkaTopicDefinitionProvider provider() {
        return new CcsKafkaTopicDefinitionProvider(
                properties, "ccs-sim-status-changed", "card-binding-status-changed");
    }

    @Nested
    @DisplayName("Topic 声明")
    class TopicDeclarationTests {

        @Test
        @DisplayName("声明 SIM 状态变更 topic")
        void declaresSimStatusTopic() {
            assertTrue(declaredTopics().contains("ccs-sim-status-changed"));
        }

        @Test
        @DisplayName("声明车卡绑定状态变更 topic")
        void declaresCardBindingStatusTopic() {
            assertTrue(declaredTopics().contains("card-binding-status-changed"));
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
}
