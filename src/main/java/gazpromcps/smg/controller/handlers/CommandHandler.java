package gazpromcps.smg.controller.handlers;

import gazpromcps.smg.annotations.*;
import gazpromcps.smg.controller.BotController;
import gazpromcps.smg.entity.CompletedJob;
import gazpromcps.smg.entity.Role;
import gazpromcps.smg.entity.User;
import gazpromcps.smg.exceptions.BotErrorType;
import gazpromcps.smg.exceptions.BotException;
import gazpromcps.smg.utils.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Slf4j
@Component
public class CommandHandler extends AbstractQueryHandler {
    @Async
    @QueryMapper("/start")
    @AskOnEnd(question = "description" /*todo name-your-company*/, buttons = {})
    @Unwelcome(maxWelcome = Role.USER)
    public void start(final BotController bot) {
        final Update update = bot.getUpdate();
        final long userId = bot.userId();

        final User user;
        if (userService.findById(userId) == null) {
            final String lastName = update.getMessage().getFrom().getLastName();

            String username = update.getMessage().getFrom().getFirstName();
            if (lastName != null) {
                username += " " + lastName;
            }

            final User form = User.builder().chatId(bot.chatId()).id(userId)
                    .role(bot.getEnvVars().getBotSuperuserId() == userId ? Role.SUPERUSER : Role.ANONYMOUS)
                    .name(username).build();
            user = userService.save(form);
        } else {
            user = userService.findById(userId);
        }

        bot.getBot().getSessions().computeIfAbsent(userId, ignored -> new Session(userService.save(user)));

        bot.send(bot.i18n("start"), keyboardButtons(bot));
    }

    @Async
    @QueryMapper("/register")
    @QueryArgs(regex = ".*")
    @Unwelcome(maxWelcome = Role.USER)
    @AskOnEnd(question = "await-register", buttons = {})
    public void register(final BotController bot, final String token) {
        final List<User> canRegister = userService.findAllByMinRole(Role.SERVICE);

        final User target = canRegister.stream().filter(u -> Objects.equals(u.generateToken(bot.getEnvVars().getTokenSalt()), token))
                .findFirst().orElse(null);

        if (target == null) {
            throw new BotException(bot, BotErrorType.INVALID_TOKEN);
        }

        final var targetLocale = bot.getBot().getSessions().get(target.getId()).getResourcesHandler();

        final var from = bot.getUpdate().getMessage().getFrom();
        bot.send(target.getChatId(), targetLocale.variable("decide-accept-user").formatted(
                from.getFirstName(), from.getLastName(), from.getUserName()),
                new InlineKeyboardMarkup(List.of(List.of(
                        button(targetLocale.variable("accept-user"), "accept %d %d".formatted(bot.userId(), bot.chatId())),
                        button(targetLocale.variable("decline-user"), "decline %d %d".formatted(bot.userId(), bot.chatId()))))));
    }

    @Async
    @QueryMapper("/token")
    @RestrictedAccess(minAllowed = Role.SERVICE)
    public void token(final BotController bot) {
        bot.send("`%s`".formatted(bot.getSession().getUser().generateToken(bot.getEnvVars().getTokenSalt())), null);
    }

    @Async
    @QueryMapper("/makereport")
    @RestrictedAccess(minAllowed = Role.USER)
    public void makeReport(final BotController bot) {
        final Session session = bot.getSession();

        session.reset(jobService.findAllActive());

        bot.send(bot.i18n("select-type") + ":\n" + session.showInterval(),
                showJobsNavigation(bot));
    }

    @Async
    @QueryMapper("/updatereport")
    @RestrictedAccess(minAllowed = Role.USER)
    public void updateReport(final BotController bot) {
        final List<CompletedJob> added = completedJobService.findAllByUser(bot.getSession().getUser());

        bot.send(bot.i18n("update-report"), new InlineKeyboardMarkup(
                added.stream().limit(5).map(j ->
                        button("%10s - [%s] %s (%s) : %s".formatted(
                                DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(j.getCreationTime()),
                                        j.getJob().getMaster(), j.getJob().getTitle(), j.getJob().getMeasurement(),
                                        NumberFormat.getInstance(Locale.getDefault()).format(j.getCount())),
                                "update-report " + j.getId()))
                        .map(List::of).toList()));
    }

    @Async
    @QueryMapper("/users")
    @RestrictedAccess(minAllowed = Role.MANAGER)
    public void users(final BotController bot) {
        bot.send(userService.findAll().stream().map(Object::toString).collect(Collectors.joining("\n")),
                null);
    }

    @Async
    @QueryMapper("/promote")
    @QueryArgs(regex = {"\\d+"})
    @RestrictedAccess(minAllowed = Role.ADMIN)
    public void promote(final BotController bot, final Long otherUserId) {
        if (otherUserId == bot.getEnvVars().getBotSuperuserId()) {
            throw new BotException(bot, BotErrorType.NOT_ALLOWED);
        }

        bot.send(bot.i18n("decide-promote-user"), new InlineKeyboardMarkup(
                Arrays.stream(Role.values()).filter(bot.getSession().getUser().getRole()::isAllowed).map(r ->
                        List.of(button(r.name(), "promote %d %s".formatted(otherUserId, r.ordinal())))
                ).toList()
        ));
    }

    @Async
    @QueryMapper("/en")
    public void en(final BotController bot) {
        bot.getSession().getResourcesHandler().changeLocale(Locale.ENGLISH);

        bot.send(bot.i18n("to-en"), keyboardButtons(bot));
    }

    @Async
    @QueryMapper("/ru")
    public void ru(final BotController bot) {
        bot.getSession().getResourcesHandler().changeLocale(new Locale("ru", "RU"));

        bot.send(bot.i18n("to-ru"), keyboardButtons(bot));
    }

    @Async
    @QueryMapper("/lang")
    @QueryArgs(regex = {"[a-z]{2}|[A-Z]{2}"})
    public void lang(final BotController bot, final String locale) {
        bot.send(bot.i18n("new-lang"), null);

        bot.getSession().getResourcesHandler().changeLocale(new Locale(locale));

        bot.send(bot.i18n("to-" + locale.toLowerCase()), keyboardButtons(bot));
    }

    @Async
    @QueryMapper("/reload")
    public void reload(final BotController bot) {
        bot.getSession().setUser(userService.findById(bot.userId()));

        bot.send(bot.i18n("done"), keyboardButtons(bot));
    }

    @Async
    @QueryMapper("/help")
    public void help(final BotController bot) {
        final String[][] availableButtons = bot.getSession().getUser().getRole().getAvailableButtons();
        final String description = Arrays.stream(availableButtons).flatMap(Arrays::stream).map(cmd -> {
            final String cmdName = "/" + cmd.replace("-", "") + " - ";
            try {
                return cmdName + bot.i18n("description-" + cmd);
            } catch (final MissingResourceException ignored) {
                return cmdName + bot.i18n("unknown-error");
            }
        }).collect(Collectors.joining("\n"));

        bot.send(bot.i18n("description") + "\n\n" + description + "\n\n" + bot.i18n("help"), null);
    }
}
