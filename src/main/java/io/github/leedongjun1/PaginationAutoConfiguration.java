package io.github.leedongjun1;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableAspectJAutoProxy
@ConditionalOnMissingBean(PaginationAspect.class)
public class PaginationAutoConfiguration {
    @Bean
    public PaginationAspect paginationAspect() {
        return new PaginationAspect();
    }
}
