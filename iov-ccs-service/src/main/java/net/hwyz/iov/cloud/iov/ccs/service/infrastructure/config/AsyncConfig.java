package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务线程池配置
 * <p>
 * 提供 {@code @Async("eventThreadPool")} 引用的线程池 Bean：
 * <ul>
 *   <li>SIM 批量入库事件（{@code SimStorageEvent}）处理为数据关键路径，拒绝策略用 CallerRunsPolicy
 *       兜底，避免事件丢失（宁可阻塞发布线程也不丢弃）</li>
 *   <li>关闭时等待在途任务完成，避免批量入库被中断</li>
 * </ul>
 * 注意：不要在 @Bean 方法内手动调用 {@code executor.initialize()}——
 * {@code ThreadPoolTaskExecutor} 实现了 {@code InitializingBean}，Spring 会在 Bean 初始化时
 * 自动调用 {@code afterPropertiesSet() → initialize()}；手动再调一次会创建第二个线程池并泄漏第一个。
 *
 * @author hwyz_leo
 */
@Configuration
public class AsyncConfig {

    /**
     * SIM 存储事件处理线程池（@Async("eventThreadPool")）
     */
    @Bean(name = "eventThreadPool")
    public Executor eventThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("event-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
