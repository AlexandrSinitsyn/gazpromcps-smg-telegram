package gazpromcps.smg.telegram.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableScheduling
public class AppConfiguration {

    @Bean
    public WebClient apiClient() {
        return WebClient.create("http://smg-server:8080/api/v1/");
    }
}
