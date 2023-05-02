package gazpromcps.smg.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Data
@Configuration
@PropertySource("classpath:config.properties")
public class EnvVars {
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    @Value("${token.salt}")
    private String tokenSalt;

    @Value("${bot.superuser}")
    private long botSuperuserId;
}
