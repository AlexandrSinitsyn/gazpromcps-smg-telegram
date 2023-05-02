package gazpromcps.smg.configuration;

import gazpromcps.smg.controller.BotController;
import gazpromcps.smg.exceptions.BotErrorType;
import gazpromcps.smg.exceptions.BotException;
import gazpromcps.smg.exceptions.BotLogicError;
import gazpromcps.smg.service.JobService;
import gazpromcps.smg.utils.ResourcesHandler;
import gazpromcps.smg.utils.Session;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Aspect
@Component
public class ExceptionHandler {
    @Autowired
    private JobService jobService;

    @SneakyThrows
    @Around("execution(* gazpromcps.smg.controller.*..*.*(..))")
    public Object exceptionHandling(final ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (final BotException e) {
            final BotController bot = e.getBot();

            bot.exceptionThrown();

            final Session session = bot.getSession();

            final ResourcesHandler resourcesHandler = session.getResourcesHandler();

            switch (e.getType()) {
                case INVALID_REQUEST_SEQUENCE -> session.reset(jobService.findAllActive());
                case UNKNOWN_ERROR -> log.error("UNKNOWN ERROR\n" + e.getCause().toString());
            }

            bot.send(resourcesHandler.variable(e.getType().getMessage()), null);

            log.warn(e.getBot().getSession().getUser().getName() + " -> " + e.getType());
        } catch (final BotLogicError e) {
            log.error("!!!FATAL!!!\n" + e.getMessage());

            e.getBot().error(BotErrorType.UNKNOWN_ERROR);
        } catch (final Exception e) {
            log.error("""
                    !!! FATAL !!!
                    Message:
                        %s
                    Cause:
                        %s
                    Stack trace:
                        %s
                    """.formatted(e.getMessage(), e.getCause() == null ? "null" : e.getCause().toString(),
                                    Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n\t"))));
        }

        return null;
    }

    // @AfterThrowing(pointcut = "execution(* gazpromcps.smg.controller.*..*.*(..))", throwing = "e")
    // public void exceptionHandling(final JoinPoint joinPoint, final BotException e) {
    //     final BotController bot = e.getBot();
    //
    //     bot.exceptionThrown();
    //
    //     final Session session = bot.getSession();
    //
    //     final ResourcesHandler resourcesHandler = session.getResourcesHandler();
    //
    //     //noinspection SwitchStatementWithTooFewBranches
    //     switch (e.getType()) {
    //         case INVALID_REQUEST_SEQUENCE -> session.reset(jobService.findAll());
    //         case UNKNOWN_ERROR -> log.error("UNKNOWN ERROR\n" + e.getCause().toString());
    //     }
    //
    //     bot.send(resourcesHandler.variable(e.getType().getMessage()), null);
    //
    //     log.warn(e.getBot().getSession().getUser().getName() + " -> " + e.getType());
    // }
    //
    // @AfterThrowing(pointcut = "execution(* gazpromcps.smg.controller.*..*.*(..))", throwing = "any")
    // public void fatalErrorsHandling(final JoinPoint joinPoint, final Throwable any) throws Throwable {
    //     if (any instanceof final BotLogicError logicError) {
    //         log.error("!!!FATAL!!!\n" + logicError.getMessage());
    //
    //         exceptionHandling(joinPoint, new BotException(logicError.getBot(), BotErrorType.UNKNOWN_ERROR));
    //     } else if (any instanceof final Exception e) {
    //         log.error("""
    //                 !!! FATAL !!!
    //                 Message:
    //                     %s
    //                 Cause:
    //                     %s
    //                 Stack trace:
    //                     %s
    //                 """.formatted(e.getMessage(), e.getCause() == null ? "null" : e.getCause().toString(),
    //                 Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).collect(Collectors.joining("\n\t"))));
    //     } else {
    //         throw any;
    //     }
    // }
}

// @ControllerAdvice
// class GlobalExceptionHandler {
//     @Autowired
//     private ExceptionHandler exceptionHandler;
//
//     @org.springframework.web.bind.annotation.ExceptionHandler(BotUncheckedException.class)
//     public void handleException(BotUncheckedException e) {
//         exceptionHandler.exceptionHandling(null, e.getBotException());
//     }
// }
