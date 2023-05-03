package gazpromcps.smg.controller.handlers;

import gazpromcps.smg.annotations.*;
import gazpromcps.smg.controller.Bot;
import gazpromcps.smg.controller.BotController;
import gazpromcps.smg.entity.CompletedJob;
import gazpromcps.smg.entity.Job;
import gazpromcps.smg.entity.Role;
import gazpromcps.smg.entity.User;
import gazpromcps.smg.exceptions.BotErrorType;
import gazpromcps.smg.exceptions.BotException;
import gazpromcps.smg.exceptions.BotLogicError;
import gazpromcps.smg.service.ExcelService;
import gazpromcps.smg.service.ExcelService.XlsxReader;
import gazpromcps.smg.utils.Session;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Slf4j
@Component
public class QueryHandler extends AbstractQueryHandler {
    @SuppressWarnings("DataFlowIssue")
    @Async
    public void buttonClicked(final BotController bot) {
        final Update update = bot.getUpdate();
        final Session session = bot.getSession();

        final long userId = bot.userId();

        final Message message = update.getCallbackQuery().getMessage();
        final String data = update.getCallbackQuery().getData();


        try {
            switch (data.split("\\s+")[0]) {
                case "left" -> {
                    session.dec();
                    editJobs(message, bot);
                }
                case "right" -> {
                    session.inc();
                    editJobs(message, bot);
                }
                case "back" ->  {
                    session.backwards();
                    editJobs(message, bot);
                }
                case "all" -> {
                    session.reset(jobService.findAll());
                    editJobs(message, bot);
                }
                case "promote" -> self(QueryHandler.class).promote(bot, null, null);
                case "make-report" -> // make-report, userId, step, index
                        self(QueryHandler.class).makeReport(bot, null, null, null);
                case "yes" -> {
                    if (bot.getSession().awaitResponse()) {
                        bot.getSession().<JobListHolder>getHolding(bot).acceptAll();

                        self(QueryHandler.class).saveNewJobs(bot);
                    } else {
                        self(QueryHandler.class).attachMedia(bot);
                    }
                }
                case "no" -> {
                    if (bot.getSession().awaitResponse()) {
                        final JobListHolder jobList = session.getHolding(bot);

                        session.reset(jobList.getJobs().stream().collect(Collectors.groupingBy(Job::getMaster)).values().stream()
                                .map(line -> line.get(0)).toList());

                        session.forward();

                        editJobs(message, bot);
                    } else {
                        self(QueryHandler.class).noMedia(bot);
                    }
                }
                case "accept" -> self(QueryHandler.class).acceptUser(bot, null, null);
                case "decline" -> self(QueryHandler.class).declineUser(bot, null, null);
                case "finish-registration" -> ((Bot) bot.getBot()).runCommandHandler(bot, "/reload");
                case "update-report" -> self(QueryHandler.class).updateReport(bot, null);
                case "choose-new-master" -> self(QueryHandler.class).acceptMaster(bot, null);
                case "submit-master" -> self(QueryHandler.class).saveNewJobs(bot);
                default -> throw new BotException(bot, BotErrorType.INVALID_COMMAND_USAGE);
            }
        } catch (final BotException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new BotLogicError(bot, """
                                Some inner-logic arguments' types are not as they meant to be.
                                At:
                                    buttonClicked()
                                Input data:
                                    %s
                                Exception:
                                    %s
                                """.stripIndent().formatted(data, e.toString()));
        }
    }

    @QueryArgs(regex = {"\\d+", "\\w+"})
    @AskOnEnd(question = "accepted", edit = true)
    public void promote(final BotController bot,
                        @SuppressWarnings("DataFlowIssue") final Long otherUserId,
                        final Integer roleOrdinal) {
        final Role role = Role.values()[roleOrdinal];

        if (!userService.findById(bot.userId()).getRole().isAllowed(role)) {
            throw new BotException(bot, BotErrorType.NOT_ALLOWED);
        }

        userService.promote(otherUserId, role);
        final User target = bot.getUserById(otherUserId);

        bot.send(target.getChatId(), bot.i18n("promoted"), null);
        bot.send(bot.chatId(), bot.i18n("successful-promotion").formatted(target.getName(), role.name()), null);

        if (role.isAllowed(Role.SERVICE)) {
            bot.send(target.getChatId(), bot.i18n("token"), null);
            bot.send(target.getChatId(), "`%s`".formatted(target.generateToken(bot.getEnvVars().getTokenSalt())), null);
        }
    }


    @Async
    @QueryArgs(regex = {"\\d+", "\\d+", "\\d+"})
    public void makeReport(final BotController bot,
                           final Long requesterId, final Integer step,
                           @SuppressWarnings("DataFlowIssue") final Long index) {
        final Session session = bot.getSession();
        final Message message = bot.message();

        try {
            assert requesterId == bot.userId();
            assert step == session.getStep();
            session.setSelectedJob(jobService.findById(index));
        } catch (final AssertionError ignored) {
            throw new BotException(bot, BotErrorType.INVALID_REQUEST_SEQUENCE);
        }

        if (session.getStep() == 3) {
            bot.edit(message.getChatId(), message.getMessageId(), """
                            %s: <b>[%s] %s (%s)</b>
                            %s:
                            """.formatted(bot.i18n("work-type"),
                            session.getSelectedJob().getObject(), session.getSelectedJob().getTitle(), session.getSelectedJob().getMeasurement(),
                            bot.i18n("print-count")),
                    null);
        } else {
            session.forward();
            editJobs(message, bot);
        }
    }

    @Async
    public void attachMedia(final BotController bot) {
        bot.edit(bot.chatId(), bot.message().getMessageId(), bot.i18n("load-media"), null);
    }

    @Async
    @FinalAction
    public void noMedia(final BotController bot) {
        bot.edit(bot.chatId(), bot.message().getMessageId(),
                bot.i18n("accepted"), null);
    }

    @Async
    @QueryArgs(regex = {"\\d+", "\\d+"})
    public void acceptUser(final BotController bot,
                           @SuppressWarnings("DataFlowIssue") final Long otherUserId,
                           @SuppressWarnings("DataFlowIssue") final Long otherChatId) {
        userService.promote(otherUserId, Role.USER);

        final var otherUserLocale = bot.getBot().getSessions().get(otherUserId).getResourcesHandler();

        bot.send(otherChatId, otherUserLocale.variable("register-accepted"), null);
        bot.send(otherChatId, otherUserLocale.variable("end-registration"), new InlineKeyboardMarkup(List.of(
                List.of(button(otherUserLocale.variable("done"), "finish-registration"))
        )));

        bot.edit(bot.chatId(), bot.message().getMessageId(), bot.i18n("done"), null);
    }

    @Async
    @QueryArgs(regex = {"\\d+", "\\d+"})
    public void declineUser(final BotController bot,
                           @SuppressWarnings("DataFlowIssue") final Long otherUserId,
                           @SuppressWarnings("DataFlowIssue") final Long otherChatId) {
        userService.promote(otherUserId, Role.USER);

        bot.send(otherChatId, bot.getBot().getSessions().get(otherUserId).getResourcesHandler().variable("register-declined"), null);

        bot.edit(bot.chatId(), bot.message().getMessageId(), bot.i18n("done"), null);
    }

    @Async
    @QueryArgs(regex = "\\d+")
    public void updateReport(final BotController bot,
                             @SuppressWarnings({"SameParameterValue", "DataFlowIssue"}) final Long completedJobId) {
        final Session session = bot.getSession();
        final Job forUpdate = completedJobService.findById(completedJobId).getJob();

        session.setHolding(completedJobId);

        bot.edit(bot.chatId(), bot.message().getMessageId(), """
                            %s: <b>[%s] %s (%s)</b>
                            %s:
                            """.formatted(bot.i18n("work-type"),
                        forUpdate.getObject(), forUpdate.getTitle(), forUpdate.getMeasurement(),
                        bot.i18n("print-count")),
                null);
    }

    @Async
    @AskOnEnd(question = "accepted", edit = true)
    public void saveNewJobs(final BotController bot) {
        final JobListHolder jobList = bot.getSession().getHolding(bot);

        bot.getSession().setHolding(null);

        jobService.update(jobList.byMasters());
    }

    @Async
    @QueryArgs(regex = "\\d+")
    public void acceptMaster(final BotController bot,
                                    @SuppressWarnings({"SameParameterValue", "DataFlowIssue"}) final Long masterId) {
        final Session session = bot.getSession();

        try {
            final JobListHolder jobList = session.getHolding(bot);

            final String master = session.interval().get((int) (masterId - 1 - session.getPointer())).getMaster();

            jobList.acceptMaster(master);
        } catch (final NumberFormatException e) {
            throw new BotException(bot, BotErrorType.UNKNOWN_ERROR, e);
        }
    }

    @Async
    public void parseInline(final BotController bot, final String text) {
        final Map<String, String> available = Arrays.stream(bot.getSession().getUser().getRole().getAvailableButtons())
                .flatMap(Arrays::stream).collect(HashMap::new, (m, k) -> m.put(bot.i18n("button-" + k), k), Map::putAll);

        final String command = available.get(text);
        if (command != null) {
            ((Bot) bot.getBot()).runCommandHandler(bot, "/" + command.replace("-", ""));
        } else {
            throw new BotException(bot, BotErrorType.UNKNOWN_COMMAND);
        }
    }

    @Async
    @QueryMapper("\\d+(\\.\\d+)?")
    @QueryArgs(regex = {"\\d+(\\.\\d+)?"},
            skipFirst = false)
    @AskOnEnd(question = "decide-load-media", buttons = {
            @ButtonRow({"yes", "no"}),
    })
    public void acceptCount(final BotController bot,
                             @SuppressWarnings("SameParameterValue") final Double count) {
        final Session session = bot.getSession();

        if (session.awaitResponse()) {
            completedJobService.updateCompletedJob(session.getHolding(bot), count);
            session.setHolding(null);
            return;
        }

        if (session.getStep() != 3) {
            throw new BotException(bot, BotErrorType.INVALID_REQUEST_SEQUENCE);
        }

        final var cjob = CompletedJob.builder()
                .user(session.getUser()).job(session.getSelectedJob()).count(count).build();

        completedJobService.save(cjob);
    }

    @Async
    public void fileProcessor(final BotController bot, final File file, final String filename) throws TelegramApiException {
        if (filename.endsWith(".xlsx")) {
            self(QueryHandler.class).loadNewJobList(bot, file, filename);
        } else if (filename.matches("^.*\\.(png|jpg|jpeg|tif|gif)$")) {
            self(QueryHandler.class).loadMedia(bot, file, filename);
        } else {
            throw new BotException(bot, BotErrorType.UNSUPPORTED_FILE_EXTENSION);
        }
    }

    @Async
    @AskOnEnd(question = "decide-choose-master", buttons = {
            @ButtonRow({"yes", "no"})
    })
    public void loadNewJobList(final BotController bot, final File file, final String filename) throws TelegramApiException {
        bot.send(bot.i18n("processing"), null);

        bot.getBot().downloadFile(file, ExcelService.getStatistics().resolve(filename).toFile());

        try (final XlsxReader reader = excelService.readXlsx(filename) ) {
            final List<Job> data = new ArrayList<>();

            final Pattern masterPattern = Pattern.compile("^[^(\n]*(\\(([^)]*)\\))?(\n(.*))?$");

            reader.pages().forEach(page -> {
                final AtomicReference<String> object = new AtomicReference<>(null);
                final AtomicReference<String> master = new AtomicReference<>(null);

                page.iterate(
                        row -> row.data(0).equals("№ п/п"),
                        row -> Set.of(
                                "СОГЛАСОВАНО:",
                                "Технические ресурсы", "Людские ресурсы",
                                "Потребность в людских и технических ресурсах"
                        ).contains(row.data(1)),
                        row -> {
                            final String name = row.data(1);
                            final String newMaster = row.data(2);
                            final String measurement = row.data(3);

                            if (name.isBlank()) {
                                return;
                            }

                            if (row.isGreen()) {
                                object.set(name);
                            }
                            if (object.get() == null && row.isBlue()) {
                                object.set(name);
                            }

                            if (!newMaster.isBlank()) {
                                final var matcher = masterPattern.matcher(newMaster);
                                while (matcher.find()) {
                                    //noinspection EscapedSpace
                                    master.set(matcher.group().replace("\s+", " ").strip());
                                }
                            }

                            if (!measurement.isBlank()) {
                                data.add(Job.builder()
                                        .stage(page.sheet().getSheetName())
                                        .master(master.get())
                                        .object(object.get() == null ? bot.i18n("excel-other") : object.get())
                                        .title(name)
                                        .measurement(measurement)
                                        .build());
                            }
                        }
                );
            });

            bot.getSession().setHolding(new JobListHolder(data));
        } catch (final Exception e) {
            throw new BotException(bot, BotErrorType.UNKNOWN_ERROR, e);
        }
    }

    @Async
    @AskOnEnd(question = "accepted")
    @FinalAction
    public void loadMedia(final BotController bot, final File file, final String filename) throws TelegramApiException {
        bot.getBot().downloadFile(file, mediaService.saveMedia(filename));
    }
}

@Getter
final class JobListHolder {
    private final List<Job> jobs;
    private final Set<String> acceptedMasters = new HashSet<>();
    private final Set<String> allMasters;

    public JobListHolder(final List<Job> jobs) {
        this.jobs = jobs;
        allMasters = jobs.stream().map(Job::getMaster).collect(Collectors.toSet());
    }

    public boolean acceptMaster(final String master) {
        if (allMasters.contains(master)) {
            if (acceptedMasters.contains(master)) {
                return !acceptedMasters.remove(master);
            } else {
                return acceptedMasters.add(master);
            }
        }

        return false;
    }

    public boolean isMasterAccepted(final String master) {
        return acceptedMasters.contains(master);
    }

    public void acceptAll() {
        acceptedMasters.addAll(allMasters);
    }

    public List<Job> byMasters() {
        return jobs.stream().filter(j -> acceptedMasters.contains(j.getMaster())).toList();
    }
}
