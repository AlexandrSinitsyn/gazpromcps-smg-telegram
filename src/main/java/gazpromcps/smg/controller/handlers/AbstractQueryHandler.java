package gazpromcps.smg.controller.handlers;

import gazpromcps.smg.controller.BotController;
import gazpromcps.smg.service.*;
import gazpromcps.smg.utils.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Slf4j
@Component
public abstract class AbstractQueryHandler {
    protected static UserService userService;
    protected static JobService jobService;
    protected static CompletedJobService completedJobService;
    protected static ExcelService excelService;
    protected static MediaService mediaService;

    @Autowired
    private ApplicationContext applicationContext;

    public <T extends AbstractQueryHandler> T self(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }

    @Autowired
    public void setUserService(final UserService userService) {
        AbstractQueryHandler.userService = userService;
    }

    @Autowired
    public void setJobService(final JobService jobService) {
        AbstractQueryHandler.jobService = jobService;
    }

    @Autowired
    public void setCompletedJobService(final CompletedJobService completedJobService) {
        AbstractQueryHandler.completedJobService = completedJobService;
    }

    @Autowired
    public void setExcelService(final ExcelService excelService) {
        AbstractQueryHandler.excelService = excelService;
    }

    @Autowired
    public void setMediaService(final MediaService mediaService) {
        AbstractQueryHandler.mediaService = mediaService;
    }

    protected InlineKeyboardButton button(final String name, final String callback) {
        return InlineKeyboardButton.builder().text(name).callbackData(callback).build();
    }

    protected ReplyKeyboardMarkup keyboardButtons(final BotController bot) {
        final String[][] buttons = bot.getSession().getUser().getRole().getAvailableButtons();

        final var keyboardMarkup = new ReplyKeyboardMarkup(Arrays.stream(buttons).map(line ->
                new KeyboardRow(Arrays.stream(line).map(name -> bot.i18n("button-" + name)).map(KeyboardButton::new
                ).toList())).toList());
        keyboardMarkup.setOneTimeKeyboard(true);
        return keyboardMarkup;
    }

    protected void editJobs(final Message message, final BotController bot) {
        final Session session = bot.getSession();

        bot.edit(message.getChatId(), message.getMessageId(),
                bot.i18n("select-type") + ":\n" + session.showInterval(),
                showJobsNavigation(bot));
    }

    protected InlineKeyboardMarkup showJobsNavigation(final BotController bot) {
        final Session session = bot.getSession();

        final List<InlineKeyboardButton> navigation = switch (session.hitBounds()) {
            case 0 -> List.of();
            case 1 -> List.of(button("←", "left"));
            case 2 -> List.of(button("→", "right"));
            default -> List.of(button("←", "left"), button("→", "right"));
        };

        final AtomicInteger index = new AtomicInteger(session.getPointer());

        if (session.awaitResponse()) {
            final JobListHolder jobList = session.getHolding(bot);

            return new InlineKeyboardMarkup(List.of(
                    session.interval().stream().map(j ->
                            button(index.incrementAndGet() + (jobList.acceptMaster(j.getMaster()) ? "✓" : ""),
                                    "choose-new-master %d".formatted(index.get()))).toList(),
                    navigation,
                    List.of(button(bot.i18n("done"), "submit-master")))
            );
        } else {
            return new InlineKeyboardMarkup(List.of(
                    session.interval().stream().map(j ->
                            button(String.valueOf(index.incrementAndGet()),
                                    "make-report %d %d %d".formatted(bot.userId(), session.getStep(), j.getId()))).toList(),
                    navigation,
                    session.getStep() == 0 ? List.of() : List.of(button(bot.i18n("back"), "back")),
                    List.of(button(bot.i18n("all-jobs"), "all"))
            ));
        }
    }
}
