package gazpromcps.smg.controller.handlers;

import gazpromcps.smg.annotations.*;
import gazpromcps.smg.controller.BotController;
import gazpromcps.smg.entity.CompletedJob;
import gazpromcps.smg.entity.Role;
import gazpromcps.smg.exceptions.BotErrorType;
import gazpromcps.smg.exceptions.BotException;
import gazpromcps.smg.service.ExcelService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
@Slf4j
@Component
public class FileHandler extends AbstractQueryHandler {
    private Date since(final String variant) {
        final Calendar now = Calendar.getInstance(Locale.getDefault());

        final LocalDateTime today = LocalDate.ofInstant(now.toInstant(), now.getTimeZone().toZoneId()).atStartOfDay();

        return Date.from((switch (variant) {
            case "day" -> today.minusDays(1);
            case "week" -> today.minusWeeks(1);
            case "month" -> today.minusMonths(1);
            case "year" -> today.minusYears(1);
            case "total" -> today.minusYears(today.getYear());
            default -> throw new IllegalArgumentException("Unknown variant: " + variant);
        }).atZone(now.getTimeZone().toZoneId()).toInstant());
    }

    @Async
    @QueryMapper("/exporttext")
    @RestrictedAccess(minAllowed = Role.MANAGER)
    @WithResponseOf(question = "choose-period", buttons = {
            @ButtonRow({"day", "week", "month"}),
            @ButtonRow({"year", "total"})
    })
    public void exportText(final BotController bot, final String variant) {
        final Date since = since(variant);

        final StringBuilder res = new StringBuilder();

        res.append(String.join(" -- ", CompletedJob.header()))
                .append("\n`").append("-".repeat(50)).append("`\n");

        completedJobService.findFromDate(since).forEach(job -> res.append(String.join(" -- ", job.separateString())).append("\n"));

        res.append("\n").append(bot.i18n("excel-total")).append("\n");

        completedJobService.summary(since).forEach(job -> res.append(String.join(" -- ", job.minimalString())).append("\n"));

        bot.send(res.toString(), null);
    }

    @SneakyThrows
    @Async
    @QueryMapper("/exportcsv")
    @RestrictedAccess(minAllowed = Role.MANAGER)
    @WithResponseOf(question = "choose-period", buttons = {
            @ButtonRow({"day", "week", "month"}),
            @ButtonRow({"year", "total"})
    })
    public void exportCsv(final BotController bot, final String variant) {
        final Date since = since(variant);

        final File file = ExcelService.getStatistics()
                .resolve("save_" + new SimpleDateFormat("yMdHm").format(new Date()) + ".csv").toFile();

        try (final FileWriter writer = new FileWriter(file)) {
            writer.append(CompletedJob.header().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",")));

            for (final var job : completedJobService.findFromDate(since)) {
                    writer.append(job.separateString().stream().map(s -> "\"" + s + "\"")
                            .collect(Collectors.joining(","))).append("\n");
            }

            writer.append("\n\"\"\n\"%s\"\n".formatted(bot.i18n("excel-total")));

            for (final var job : completedJobService.summary(since)) {
                    writer.append(job.minimalString().stream().map(s -> "\"" + s + "\"")
                            .collect(Collectors.joining(","))).append("\n");
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        bot.sendDocument(file);
    }

    @SneakyThrows
    @Async
    @QueryMapper("/exportxlsx")
    @RestrictedAccess(minAllowed = Role.MANAGER)
    @WithResponseOf(question = "choose-period", buttons = {
            @ButtonRow({"day", "week", "month"}),
            @ButtonRow({"year", "total"})
    })
    public void exportXlsx(final BotController bot, final String variant) {
        final Date since = since(variant);

        final String filename = "save_" + new SimpleDateFormat("yMdHm").format(new Date()) + ".xlsx";

        try (final var writer = excelService.writeXlsx(filename)) {
            writer.page(bot.i18n("excel-sheet-name"));

            writer.write(0, CompletedJob.header());

            final var index = new AtomicInteger();
            completedJobService.findFromDate(since).forEach(job -> writer.write(index.incrementAndGet(), job.separateString()));

            writer.write(index.addAndGet(2), 0, bot.i18n("excel-total"));

            completedJobService.summary(since).forEach(job -> writer.write(index.incrementAndGet(), job.minimalString()));
        } catch (final Exception e) {
            throw new BotException(bot, BotErrorType.UNKNOWN_ERROR, e);
        }

        bot.sendDocument(excelService.file(filename));
    }

    @Async
    @QueryMapper("/importxlsx")
    @RestrictedAccess(minAllowed = Role.MANAGER)
    @AskOnEnd(question = "provide-the-file", buttons = {})
    public void importXlsx(final BotController bot) {}

    @Async
    @QueryMapper("/exportmedia")
    @RestrictedAccess(minAllowed = Role.MANAGER)
    public void media(final BotController bot) {
        try {
            bot.sendDocument(mediaService.getZip());
        } catch (IOException e) {
            throw new BotException(bot, BotErrorType.UNKNOWN_ERROR, e);
        }
    }
}
