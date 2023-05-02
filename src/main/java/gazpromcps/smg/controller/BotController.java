package gazpromcps.smg.controller;

import gazpromcps.smg.configuration.EnvVars;
import gazpromcps.smg.entity.User;
import gazpromcps.smg.exceptions.BotErrorType;
import gazpromcps.smg.utils.Session;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

import java.io.File;
import java.io.FileNotFoundException;

public interface BotController {
    Update getUpdate();
    Session getSession();
    AbstractBot getBot();
    EnvVars getEnvVars();
    boolean isExceptionOccurred();

    void exceptionThrown();

    default long userId() {
        final Update update = getUpdate();

        return update.hasCallbackQuery() ? update.getCallbackQuery().getFrom().getId()
                : update.hasInlineQuery() ? update.getInlineQuery().getFrom().getId()
                : update.hasMessage() ? update.getMessage().getFrom().getId()
                : -1;
    }
    default User getUserById(final long id) {
        return getBot().getSessions().values().stream().map(Session::getUser)
                .filter(u -> u.getId() == id).findFirst().orElse(null);
    }
    default long chatId() {
        final Update update = getUpdate();

        return update.hasCallbackQuery() ? update.getCallbackQuery().getMessage().getChatId()
                : update.hasInlineQuery() ? update.getMessage().getChatId()
                : update.hasMessage() ? update.getMessage().getChatId()
                : -1;
    }
    default Message message() {
        final Update update = getUpdate();

        if (update.hasCallbackQuery()) {
            final var res = update.getCallbackQuery().getMessage();
            res.setText(update.getCallbackQuery().getData());
            return res;
        } else if (update.hasInlineQuery()) {
            final var res = update.getMessage();
            res.setText(update.getInlineQuery().getQuery());
            return res;
        } else {
            return update.getMessage();
        }
    }
    default String i18n(final String key) {
        return getSession() == null ? null : getSession().getResourcesHandler().variable(key);
    }
    default void send(final long chatId, final String text, final ReplyKeyboard markup) {
        getBot().send(chatId, text, markup);
    }
    default void send(final String text, final ReplyKeyboard markup) {
        send(chatId(), text, markup);
    }
    default void error(final BotErrorType type) {
        send(i18n(type.getMessage()), null);
    }
    default void sendDocument(final long chatId, final File file) throws FileNotFoundException {
        getBot().sendDocument(chatId, file);
    }
    default void sendDocument(final File file) throws FileNotFoundException {
        sendDocument(chatId(), file);
    }
    default void edit(final long chatId, final int messageId, final String text, final InlineKeyboardMarkup markup) {
        getBot().edit(chatId, messageId, text, markup);
    }

    static BotControllerImpl of(final Update update, final AbstractBot bot, final EnvVars envVars) {
        return new BotControllerImpl(update, bot, envVars);
    }

    @Getter
    @RequiredArgsConstructor
    class BotControllerImpl implements BotController {
        private final Update update;
        private final AbstractBot bot;
        private final EnvVars envVars;
        private Session session;
        private boolean exceptionOccurred = false;

        @Override
        public void exceptionThrown() {
            exceptionOccurred = true;
        }

        @Override
        public Session getSession() {
            if (session == null) {
                session = bot.getSessions().get(userId());
            }

            return session;
        }
    }
}
