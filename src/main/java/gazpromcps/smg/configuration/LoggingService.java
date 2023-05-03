package gazpromcps.smg.configuration;

import gazpromcps.smg.annotations.QueryMapper;
import gazpromcps.smg.controller.BotController;
import gazpromcps.smg.entity.Role;
import gazpromcps.smg.entity.User;
import gazpromcps.smg.exceptions.BotErrorType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.Objects;

@Slf4j
@Aspect
@Component
public class LoggingService {

    @Around("execution(* gazpromcps.smg.controller.*..*.*(..)) && @annotation(queryMapper)")
    public Object onCommandLogging(final ProceedingJoinPoint joinPoint, final QueryMapper queryMapper) throws Throwable {
        final BotController bot = (BotController) joinPoint.getArgs()[0];

        final User user;
        if (Objects.equals(queryMapper.value(), "/start")) {
            user = new User(-1, "UNKNOWN", Role.ANONYMOUS, -1, new Timestamp(1));
        } else {
            user = bot.getSession().getUser();
        }

        log.info(">   INVOKE: {%10d, %9s, %20s}, %15s".formatted(
                user.getId(), user.getRole().name(), user.getName(), queryMapper.value()));
        try {
            final var res = joinPoint.proceed();

            log.info("< RESPONSE: {%10d, %9s, %20s}, %15s".formatted(
                    user.getId(), user.getRole().name(), user.getName(), queryMapper.value()));

            return res;
        } catch (final Throwable e) {
            log.info("!    ERROR: {%10d, %9s, %20s}, %15s, error_message: %s".formatted(
                    user.getId(), user.getRole().name(), user.getName(), queryMapper.value(), e.getMessage()));
            throw e;
        }
    }

    @Around(value = "execution(public void gazpromcps.smg.controller.handlers.QueryHandler.buttonClicked(..)) && args(bot))",
            argNames = "joinPoint,bot")
    public Object onButtonLogging(final ProceedingJoinPoint joinPoint, final BotController bot) throws Throwable {
        final User user = bot.getSession().getUser();
        final String data = bot.getUpdate().getCallbackQuery().getData();

        log.info(">   INVOKE: {%10d, %9s, %20s}, button_data: %s".formatted(
                user.getId(), user.getRole().name(), user.getName(), data));
        try {
            final var res = joinPoint.proceed();

            log.info(">   INVOKE: {%10d, %9s, %20s}, button_data: %s".formatted(
                    user.getId(), user.getRole().name(), user.getName(), data));

            return res;
        } catch (final Throwable e) {
            log.info(">   INVOKE: {%10d, %9s, %20s}, button_data: %s, error_message: %s".formatted(
                    user.getId(), user.getRole().name(), user.getName(), data, e.getMessage()));
            throw e;
        }
    }

    // fixme On bot.error(type), `bot` is not a bean -> no interception
    @Before(value = "execution(public void gazpromcps.smg.controller.BotController.error(..)) && args(type) && this(bot)",
            argNames = "joinPoint,bot,type")
    public void onErrorHandled(final JoinPoint joinPoint, final BotController bot, final BotErrorType type) {
        final User user = bot.getSession().getUser();

        log.info("!   ERROR: {%10d, %9s, %20s}, %s, error_type: %s".formatted(
                user.getId(), user.getRole().name(), user.getName(), bot.message().getText(), type.name()));
    }
}
