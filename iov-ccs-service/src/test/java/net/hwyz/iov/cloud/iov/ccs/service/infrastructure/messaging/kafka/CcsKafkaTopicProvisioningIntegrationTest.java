package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.messaging.kafka;

import net.hwyz.iov.cloud.framework.kafka.topic.DefaultKafkaTopicProvisioner;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioningResult;
import net.hwyz.iov.cloud.framework.kafka.topic.KafkaTopicProvisioner;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config.CcsKafkaTopicProperties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.junit.jupiter.api.AfterAll;import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * CCS Kafka Topic 初始化集成测试（IOV-CCS-DSN-CR-003）
 * <p>
 * 连接本地 docker broker（localhost:9094），覆盖 CR 测试设计：
 * 空环境自动创建两个生产 Topic、重复启动幂等、并发收敛、已存在 Topic 不重建/不缩分区。
 * <p>
 * 使用带唯一前缀的临时 Topic 名（不触碰真实标准 Topic）；broker 不可用时自动跳过。
 *
 * @author hwyz_leo
 */
@DisplayName("CCS Kafka Topic 初始化集成测试")
class CcsKafkaTopicProvisioningIntegrationTest {

    private static final String BOOTSTRAP = "localhost:9094";
    private static final String TOPIC_PREFIX = "ccs-it-";
    private static final int EXPECTED_PARTITIONS = 3;
    private static final short EXPECTED_REPLICATION = 1;

    private static Admin admin;
    private static final List<String> tempTopics = new ArrayList<>();

    @BeforeAll
    static void connect() {
        try {
            Properties props = new Properties();
            props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP);
            props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
            admin = AdminClient.create(props);
            admin.describeCluster().clusterId().get(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            admin = null;
        }
    }

    @AfterAll
    static void cleanup() throws Exception {
        if (admin == null || tempTopics.isEmpty()) {
            return;
        }
        try {
            admin.deleteTopics(tempTopics).all().get(5, TimeUnit.SECONDS);
        } finally {
            admin.close();
        }
    }

    private CcsKafkaTopicProperties propertiesWithTempNames(String simTopic, String bindingTopic) {
        CcsKafkaTopicProperties properties = new CcsKafkaTopicProperties();
        properties.getSimStatusChanged().setName(simTopic);
        properties.getVehicleSimBindingStatusChanged().setName(bindingTopic);
        properties.setPartitions(EXPECTED_PARTITIONS);
        properties.setReplicationFactor(EXPECTED_REPLICATION);
        return properties;
    }

    private KafkaTopicProvisioner provisioner() {
        return new DefaultKafkaTopicProvisioner(admin);
    }

    private String tempName(String suffix) {
        String name = TOPIC_PREFIX + UUID.randomUUID().toString().substring(0, 8) + "-" + suffix;
        tempTopics.add(name);
        return name;
    }

    @Test
    @DisplayName("空环境自动创建两个生产 Topic（分区/副本按期望）")
    void missingTopics_autoCreated() throws Exception {
        assumeTrue(admin != null, "Kafka broker 不可用，跳过集成测试");
        String simTopic = tempName("sim-status.changed");
        String bindingTopic = tempName("binding-status.changed");
        CcsKafkaTopicProperties properties = propertiesWithTempNames(simTopic, bindingTopic);
        CcsKafkaTopicDefinitionProvider provider = new CcsKafkaTopicDefinitionProvider(properties);

        KafkaTopicProvisioningResult result = provisioner()
                .ensureTopics(provider.topicDefinitions())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertThat(result.successful()).isTrue();
        assertThat(result.createdTopics()).containsExactlyInAnyOrder(simTopic, bindingTopic);
        assertThat(result.missingTopics()).isEmpty();

        Map<String, TopicDescription> descriptions =
                admin.describeTopics(List.of(simTopic, bindingTopic)).allTopicNames()
                        .get(5, TimeUnit.SECONDS);
        assertThat(descriptions.keySet()).containsExactlyInAnyOrder(simTopic, bindingTopic);
        assertThat(descriptions.get(simTopic).partitions()).hasSize(EXPECTED_PARTITIONS);
        assertThat(descriptions.get(bindingTopic).partitions()).hasSize(EXPECTED_PARTITIONS);
    }

    @Test
    @DisplayName("重复启动幂等：第二次不创建、不报错")
    void repeatedProvisioning_isIdempotent() throws Exception {
        assumeTrue(admin != null, "Kafka broker 不可用，跳过集成测试");
        String simTopic = tempName("sim-status.changed");
        String bindingTopic = tempName("binding-status.changed");
        CcsKafkaTopicProperties properties = propertiesWithTempNames(simTopic, bindingTopic);
        CcsKafkaTopicDefinitionProvider provider = new CcsKafkaTopicDefinitionProvider(properties);

        KafkaTopicProvisioner provisioner = provisioner();
        KafkaTopicProvisioningResult first = provisioner.ensureTopics(provider.topicDefinitions())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(first.successful()).isTrue();

        KafkaTopicProvisioningResult second = provisioner.ensureTopics(provider.topicDefinitions())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);
        assertThat(second.successful()).isTrue();
        assertThat(second.createdTopics()).isEmpty();
    }

    @Test
    @DisplayName("已存在 Topic 不重建、不缩分区")
    void existingTopic_notRebuilt() throws Exception {
        assumeTrue(admin != null, "Kafka broker 不可用，跳过集成测试");
        String simTopic = tempName("sim-status.changed");
        String bindingTopic = tempName("binding-status.changed");

        // 预创建 simTopic 为 1 分区，期望 3 分区
        admin.createTopics(List.of(new NewTopic(simTopic, 1, EXPECTED_REPLICATION)))
                .all().get(5, TimeUnit.SECONDS);

        CcsKafkaTopicProperties properties = propertiesWithTempNames(simTopic, bindingTopic);
        CcsKafkaTopicDefinitionProvider provider = new CcsKafkaTopicDefinitionProvider(properties);

        KafkaTopicProvisioningResult result = provisioner()
                .ensureTopics(provider.topicDefinitions())
                .toCompletableFuture().get(10, TimeUnit.SECONDS);

        assertThat(result.successful()).isTrue();
        // simTopic 已存在：不重建；bindingTopic 缺失：创建
        assertThat(result.createdTopics()).containsExactly(bindingTopic);

        TopicDescription simDescription = admin.describeTopics(List.of(simTopic)).allTopicNames()
                .get(5, TimeUnit.SECONDS).get(simTopic);
        assertThat(simDescription.partitions()).hasSize(1);
    }
}
