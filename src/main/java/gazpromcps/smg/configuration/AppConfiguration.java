package gazpromcps.smg.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAspectJAutoProxy
// @EnableJpaRepositories
@EnableScheduling
public class AppConfiguration {
    // @Bean
    // public AspectService aspectService() {
    //     return new AspectService();
    // }
}
