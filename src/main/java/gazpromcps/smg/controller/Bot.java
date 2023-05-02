package gazpromcps.smg.controller;

import gazpromcps.smg.configuration.EnvVars;
import gazpromcps.smg.controller.handlers.QueryHandler;
import gazpromcps.smg.exceptions.BotErrorType;
import gazpromcps.smg.exceptions.BotException;
import gazpromcps.smg.exceptions.BotLogicError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

@Slf4j
@Component
public class Bot extends AbstractBot {
    @Autowired
    private QueryHandler queryHandler;

    public Bot(final EnvVars env) {
        super(env);
    }

    @Override
    public void onUpdateReceived(final Update update) {
        final var botController = BotController.of(update, this, env);

        if (update.hasCallbackQuery()) {
            final String confirmation = botController.getSession().getConfirmation();
            if (confirmation != null) {
                runCommandHandler(botController, confirmation);
            } else {
                queryHandler.buttonClicked(botController);
            }
        } else if (update.hasInlineQuery()) {
            queryHandler.parseInline(botController, update.getInlineQuery().getQuery());
        } else if (update.hasMessage()) {
            if (update.getMessage().hasText()) {
                final String command = update.getMessage().getText().split("\\s+")[0];
                runCommandHandler(botController, command);
            } else if (update.getMessage().hasDocument()) {
                final Document document = update.getMessage().getDocument();
                final GetFile getFile = GetFile.builder().fileId(document.getFileId()).build();
                try {
                    queryHandler.fileProcessor(botController, execute(getFile), document.getFileName());
                } catch (final TelegramApiException e) {
                    throw new BotException(botController, BotErrorType.UNKNOWN_ERROR, e);
                }
            } else if (update.getMessage().hasPhoto()) {
                final PhotoSize photo = update.getMessage().getPhoto().stream()
                        .max(Comparator.comparing(PhotoSize::getFileSize))
                        .orElseThrow(() -> new BotException(botController, BotErrorType.UNKNOWN_ERROR));
                final GetFile getFile = GetFile.builder().fileId(photo.getFileId()).build();
                try {
                    final File downloadedAbstraction = execute(getFile);

                    queryHandler.fileProcessor(botController, downloadedAbstraction,
                            botController.getSession().getUser().getName() + "_" +
                                    DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(new Date()) + "_" +
                                    downloadedAbstraction.getFilePath().replace("/", "_").replace("\\", "_"));
                } catch (final TelegramApiException e) {
                    throw new BotException(botController, BotErrorType.UNKNOWN_ERROR, e);
                }
            }
        }
    }

    public void runCommandHandler(final BotController bot, final String command) {
        final String key = commands.keySet().stream()
                .filter(cmd -> command.matches("^" + cmd + "$")).findFirst().orElse("unknown-key");
        final Handler handler = commands.getOrDefault(key, null);

        if (handler != null) {
            try {
                final Object[] args = new Object[handler.method().getParameterTypes().length];
                args[0] = bot;
                handler.method().invoke(handler.abstractQueryHandler(), args);
            } catch (final IllegalAccessException e) {
                throw new BotException(bot, BotErrorType.INVALID_COMMAND_USAGE);
            } catch (final InvocationTargetException e) {
                if (e.getTargetException() instanceof final BotLogicError botLogicError) {
                    throw botLogicError;
                }

                throw new BotException(bot,
                        e.getTargetException() instanceof final BotException botException ?
                                botException.getType() : BotErrorType.UNKNOWN_ERROR, e);
            }
        } else {
            queryHandler.parseInline(bot, bot.message().getText());
        }
    }
}
