package server;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class Config implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/api/events/websocket");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS();
    }

    /**
     * Necessary for spring version 3.1.x to 3.2.2,
     * because of <a href="https://github.com/spring-projects/spring-framework/issues/32171">...</a>
     */
    @Bean(name = "customClientInboundChannelExecutor")
    public ThreadPoolTaskExecutor customClientInboundChannelExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("clientInboundChannel-");

        // This is to prevent the exception
        executor.setAcceptTasksAfterContextClose(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();
        return executor;
    }

    /**
     * Necessary for spring version 3.1.x to 3.2.2,
     * because of <a href="https://github.com/spring-projects/spring-framework/issues/32171">...</a>
     */
    @Bean(name = "customClientOutboundChannelExecutor")
    public ThreadPoolTaskExecutor customClientOutboundChannelExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("clientOutboundChannel-");

        // This is to prevent the exception
        executor.setAcceptTasksAfterContextClose(true);
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();
        return executor;
    }

    /**
     * Necessary for spring version 3.1.x to 3.2.2,
     * because of <a href="https://github.com/spring-projects/spring-framework/issues/32171">...</a>
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor(customClientInboundChannelExecutor());
    }

    /**
     * Necessary for spring version 3.1.x to 3.2.2,
     * because of <a href="https://github.com/spring-projects/spring-framework/issues/32171">...</a>
     */
    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor(customClientOutboundChannelExecutor());
    }
}