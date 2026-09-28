package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 异步线程池配置测试
 * <p>
 * 验证 {@code eventThreadPool} Bean 可解析，且 {@code @Async("eventThreadPool")}
 * 事件监听真实地在独立线程池异步执行（回归：此前该 Bean 缺失，事件触发即抛
 * NoSuchBeanDefinitionException，SIM 批量入库事件被静默丢弃）。
 *
 * @author hwyz_leo
 */
@DisplayName("异步线程池配置测试")
class AsyncConfigTest {

    /**
     * 测试事件
     */
    record TestEvent(String payload) {
    }

    @Test
    @DisplayName("eventThreadPool Bean 可解析且 @Async 监听在 event- 线程异步执行")
    void eventThreadPool_resolvesAndExecutesAsync() throws Exception {
        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(
                AsyncConfig.class, AsyncTestApp.class)) {
            Executor executor = ctx.getBean("eventThreadPool", Executor.class);
            assertNotNull(executor);

            AsyncListener listener = ctx.getBean(AsyncListener.class);
            ctx.publishEvent(new TestEvent("sim-batch"));

            assertTrue(listener.latch().await(5, TimeUnit.SECONDS), "异步监听器未在超时内执行");
            assertTrue(listener.executedThread().get().startsWith("event-"),
                    "应在 event- 线程执行，实际线程: " + listener.executedThread().get());
        }
    }

    @Configuration
    @EnableAsync
    static class AsyncTestApp {

        @Bean
        AsyncListener asyncListener() {
            return new AsyncListener();
        }
    }

    static class AsyncListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<String> executedThread = new AtomicReference<>();

        /**
         * 通过 getter 访问状态：@Async 会生成 CGLIB 代理（Objenesis 创建，跳过字段初始化），
         * 直接读代理字段为 null；getter 会委托到真实目标实例。
         */
        CountDownLatch latch() {
            return latch;
        }

        AtomicReference<String> executedThread() {
            return executedThread;
        }

        @Async("eventThreadPool")
        @EventListener
        public void onTestEvent(TestEvent event) {
            executedThread.set(Thread.currentThread().getName());
            latch.countDown();
        }
    }
}
