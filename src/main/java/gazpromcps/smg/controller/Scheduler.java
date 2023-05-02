package gazpromcps.smg.controller;

import gazpromcps.smg.entity.Job;
import gazpromcps.smg.entity.Role;
import gazpromcps.smg.entity.User;
import gazpromcps.smg.service.CompletedJobService;
import gazpromcps.smg.service.JobService;
import gazpromcps.smg.utils.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class Scheduler {
    @Autowired
    private AbstractBot bot;
    @Autowired
    private JobService jobService;
    @Autowired
    private CompletedJobService completedJobService;

    // every day at 19:00 UTC+0
    @Scheduled(cron = "0 0 19 * * MON-FRI")
    // every 5 seconds
    // @Scheduled(cron = "0/5 * * * * *")
    public void listAcceptedMasters() {
        final Date now = Calendar.getInstance(Locale.getDefault()).getTime();
        final Set<String> all = jobService.findAllActive().stream().map(Job::getMaster).collect(Collectors.toSet());
        final Set<String> today = completedJobService.findFromDate(now).stream()
                .map(c -> c.getJob().getMaster()).collect(Collectors.toSet());

        all.removeAll(today);

        bot.getSessions().values().stream().map(Session::getUser).filter(u -> u.getRole().isAllowed(Role.ADMIN))
                .map(User::getChatId).forEach(id ->
                        bot.send(id, "Now (%s)%nIn:%n%s%n%nNot in%n%s%n".formatted(
                                DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault()).format(now),
                                String.join("\n", today),
                                String.join("\n", all)), null));
    }
}
