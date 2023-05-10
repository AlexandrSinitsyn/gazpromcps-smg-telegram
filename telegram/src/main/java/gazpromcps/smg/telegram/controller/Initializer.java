package gazpromcps.smg.telegram.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;


@Slf4j
@Component
public class Initializer {
    @Autowired
    private Bot bot;

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        try {
            final var telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(bot);
        } catch (final TelegramApiException e) {
            log.error("""
                    !!!FATAL!!!
                    Telegram has not been loaded!
                    %s
                    """.stripIndent(), e.getMessage());
        }
    }
}
