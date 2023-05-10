package gazpromcps.smg.telegram.controller;

import gazpromcps.smg.dto.MessageDTO;
import gazpromcps.smg.dto.MessageDTO.MessageType;
import gazpromcps.smg.dto.ServerResponse;
import gazpromcps.smg.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.Duration;

@Slf4j
@Component("Bot")
@PropertySource("classpath:config.properties")
public class Bot extends TelegramLongPollingBot {
    @Autowired
    private WebClient webClient;
    @Autowired
    private ApplicationContext context;

    private final String name, token;

    public Bot(@Value("${bot.name}") final String name,
               @Value("${bot.token}") final String token) {
        this.name = name;
        this.token = token;
    }

    @Override
    public String getBotUsername() {
        return name;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(final Update update) {
        final UserDTO user = UserDTO.builder()
                .id(update.getMessage().getFrom().getId())
                .name(update.getMessage().getFrom().getFirstName())
                .build();
        
        final MessageDTO message = MessageDTO.builder()
                .user(user)
                .type(MessageType.TEXT)
                .message("Hello!")
                .build();

        context.getBean("Bot", Bot.class).runUpdate(message);
    }

    @Async
    public void runUpdate(final MessageDTO message) {
        final ServerResponse response = message.get(webClient, "/user/" + message.getUser().getId(), Duration.ofSeconds(3));
        log.info(response.toString());
    }
}