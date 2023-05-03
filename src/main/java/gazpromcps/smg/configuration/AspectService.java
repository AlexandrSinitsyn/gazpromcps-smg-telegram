package gazpromcps.smg.configuration;

import gazpromcps.smg.annotations.*;
import gazpromcps.smg.controller.Bot;
import gazpromcps.smg.controller.BotController;
import gazpromcps.smg.entity.Role;
import gazpromcps.smg.exceptions.BotErrorType;
import gazpromcps.smg.entity.User;
import gazpromcps.smg.service.UserService;
import gazpromcps.smg.utils.Session;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
@Aspect
@Component
public class AspectService {
    @Autowired
    private UserService userService;

    @Around("@annotation(restrictedAccess)")
    public Object restrictedAccessHandler(final ProceedingJoinPoint joinPoint, final RestrictedAccess restrictedAccess)
            throws Throwable {
        final BotController bot = (BotController) joinPoint.getArgs()[0];
        final long userId = bot.userId();

        final User user = userService.findById(userId);
        if (user == null || !user.getRole().isAllowed(restrictedAccess.minAllowed())) {
            bot.error(BotErrorType.NOT_ALLOWED);
            return null;
        }

        return joinPoint.proceed();
    }

    @Around("@annotation(queryArgs)")
    public Object queryArgsValidator(final ProceedingJoinPoint joinPoint, final QueryArgs queryArgs)
            throws Throwable {
        final BotController bot = (BotController) joinPoint.getArgs()[0];

        final Class<?>[] fullSignature = ((MethodSignature) joinPoint.getSignature()).getMethod().getParameterTypes();
        final Class<?>[] types = Arrays.copyOfRange(fullSignature, 1, fullSignature.length);
        final String[] args = awaitArgs(bot, bot.message().getText(), types.length, queryArgs.skipFirst());

        if (args == null) {
            return null;
        }

        try {
            final Object[] res = Stream.concat(Stream.of(bot), IntStream.range(0, types.length).mapToObj(i -> {
                final Class<?> type = types[i];
                final String arg = args[i];

                if (!arg.matches(queryArgs.regex()[i])) {
                    throw new RuntimeException();
                }

                if (type == String.class) {
                    return arg;
                } else if (type == Integer.class || type == int.class) {
                    return Integer.parseInt(arg);
                } else if (type == Long.class || type == long.class) {
                    return Long.parseLong(arg);
                } else if (type == Double.class || type == double.class) {
                    return Double.parseDouble(arg);
                } else {
                    return (Object) arg;
                }
            })).toArray();

            return joinPoint.proceed(res);
        } catch (final RuntimeException e) {
            bot.error(BotErrorType.INVALID_COMMAND_USAGE);
            return null;
        }
    }

    private String[] awaitArgs(final BotController bot, final String message, final int count, final boolean skipFirst) {
        final int shift = skipFirst ? 1 : 0;
        final String[] args = message.split("\\s+", count + shift);

        if (args.length == count + shift) {
            return Arrays.copyOfRange(args, shift, count + shift);
        } else {
            bot.error(BotErrorType.INVALID_COMMAND_USAGE);
            return null;
        }
    }

    @Around("@annotation(withResponseOf)")
    public Object withResponseOf(final ProceedingJoinPoint joinPoint, final WithResponseOf withResponseOf)
            throws Throwable {
        final var bot = (BotController) joinPoint.getArgs()[0];

        final var signature = (MethodSignature) joinPoint.getSignature();
        final String command = joinPoint.getTarget().getClass()
                .getMethod(signature.getMethod().getName(),
                        signature.getMethod().getParameterTypes()).getAnnotation(QueryMapper.class).value();

        final Session session = bot.getSession();
        if (session.getConfirmation() != null && session.getConfirmation().equals(command)) {
            session.setConfirmation(null);
            return joinPoint.proceed(new Object[]{bot, bot.getUpdate().getCallbackQuery().getData()});
        }

        session.setConfirmation(command);

        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                Arrays.stream(withResponseOf.buttons()).map(row -> Arrays.stream(row.value())
                        .map(name -> InlineKeyboardButton.builder()
                                .text(bot.i18n(name))
                                .callbackData(name).build()
                        ).toList()).toList());

        bot.send(bot.i18n(withResponseOf.question()), markup);

        return null;
    }

    @After("@annotation(askOnEnd)")
    public void askOnEnd(final JoinPoint joinPoint, final AskOnEnd askOnEnd) {
        final var bot = (BotController) joinPoint.getArgs()[0];

        if (bot.isExceptionOccurred()) {
            return;
        }

        final InlineKeyboardMarkup markup = new InlineKeyboardMarkup(
                Arrays.stream(askOnEnd.buttons()).map(row -> Arrays.stream(row.value())
                        .map(name -> InlineKeyboardButton.builder()
                                .text(bot.i18n(name))
                                .callbackData(name).build()
                        ).toList()).toList());

        if (askOnEnd.edit()) {
            bot.edit(bot.chatId(), bot.message().getMessageId(), bot.i18n(askOnEnd.question()), markup);
        } else {
            bot.send(bot.i18n(askOnEnd.question()), markup);
        }
    }

    @After("@annotation(finalAction)")
    public void finalAction(final JoinPoint joinPoint, final FinalAction finalAction) {
        final var bot = (BotController) joinPoint.getArgs()[0];

        ((Bot) bot.getBot()).runCommandHandler(bot, "/makereport");
    }

    @Around("@annotation(unwelcome)")
    public Object unwelcome(final ProceedingJoinPoint joinPoint, final Unwelcome unwelcome)
            throws Throwable {
        final BotController bot = (BotController) joinPoint.getArgs()[0];
        final long userId = bot.userId();

        final User user = userService.findById(userId);
        if (user != null && user.getRole() != Role.SUPERUSER &&
                user.getRole().isAllowed(unwelcome.maxWelcome()) && user.getRole() != unwelcome.maxWelcome()) {
            bot.error(BotErrorType.UNWELCOME);
            return null;
        }

        return joinPoint.proceed();
    }
}
