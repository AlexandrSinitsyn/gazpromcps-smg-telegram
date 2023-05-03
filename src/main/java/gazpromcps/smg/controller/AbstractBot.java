package gazpromcps.smg.controller;

import gazpromcps.smg.annotations.QueryMapper;
import gazpromcps.smg.configuration.EnvVars;
import gazpromcps.smg.controller.handlers.AbstractQueryHandler;
import gazpromcps.smg.controller.handlers.CommandHandler;
import gazpromcps.smg.controller.handlers.FileHandler;
import gazpromcps.smg.controller.handlers.QueryHandler;
import gazpromcps.smg.service.UserService;
import gazpromcps.smg.utils.Session;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public abstract class AbstractBot extends TelegramLongPollingBot {
    protected record Handler(AbstractQueryHandler abstractQueryHandler, Method method) {}

    @Autowired
    protected UserService userService;
    @Autowired
    private ApplicationContext context;
    protected final Map<String, Handler> commands = new HashMap<>();
    @Getter
    protected final Map<Long, Session> sessions = new HashMap<>();

    protected final EnvVars env;

    @PostConstruct
    private void sessionLoader() {
        sessions.putAll(userService.findAll().stream()
                .collect(HashMap::new, (map, u) -> map.put(u.getId(), new Session(u)), Map::putAll));

        // commands.putAll(new Reflections("gazpromcps.smg.controller.handlers").getSubTypesOf(AbstractQueryHandler.class)
        //         .stream().flatMap(clazz ->
        //                 Arrays.stream(clazz.getDeclaredMethods())
        //                         .filter(m -> m.isAnnotationPresent(QueryMapper.class))
        //                         .map(method -> new Handler(context.getBean(clazz), method)))
        //         .collect(HashMap::new, (cmd, h) -> cmd.put(h.method().getAnnotation(QueryMapper.class).value(), h), Map::putAll));

        commands.putAll(Stream.of(AbstractQueryHandler.class, CommandHandler.class, FileHandler.class, QueryHandler.class).flatMap(clazz ->
                        Arrays.stream(clazz.getDeclaredMethods())
                                .filter(m -> m.isAnnotationPresent(QueryMapper.class))
                                .map(method -> new Handler(context.getBean(clazz), method)))
                .collect(HashMap::new, (cmd, h) -> cmd.put(h.method().getAnnotation(QueryMapper.class).value(), h), Map::putAll));

        log.info("\n" + commands.keySet().stream().map(Object::toString).collect(Collectors.joining("\n")));
    }

    @Override
    public String getBotUsername() {
        return env.getBotName();
    }

    @Override
    public String getBotToken() {
        return env.getBotToken();
    }

    private String escape(final String message) {
        final AtomicBoolean found = new AtomicBoolean(false);

        return message.chars().mapToObj(c -> {
            if (c == '`') {
                return found.getAndSet(!found.get()) ? "</code>" : "<code>";
            } else {
                return String.valueOf((char) c);
            }
        }).collect(Collectors.joining(""));
    }

    public void send(final long chatId, final String text, final ReplyKeyboard markup) {
        final SendMessage message = SendMessage.builder()
                .chatId(chatId).text(escape(text)).replyMarkup(markup).build();
        message.enableHtml(true);
        // message.enableMarkdown(true);

        try {
            executeAsync(message);
        } catch (final TelegramApiException e) {
            log.error("!!! TelegramApiException[send]: (%10d, %s) >> %s".formatted(chatId, text, e.getMessage()));
        }
    }

    public void sendDocument(final long chatId, final File file) throws FileNotFoundException {
        final SendDocument message = SendDocument.builder()
                .chatId(chatId).document(new InputFile(new FileInputStream(file), file.getName())).build();

        executeAsync(message);
    }

    public void edit(final long chatId, final int messageId, final String text,
                                                    final InlineKeyboardMarkup markup) {
        final EditMessageText message = EditMessageText.builder()
                .chatId(chatId).messageId(messageId).text(escape(text)).replyMarkup(markup).build();
        message.enableHtml(true);
        // message.enableMarkdown(true);

        try {
            executeAsync(message);
        } catch (final TelegramApiException e) {
            log.error("!!! TelegramApiException[edit]: (%10d, msg: %10d, %s) >> %s".formatted(chatId, messageId, text, e.getMessage()));
        }
    }
}
