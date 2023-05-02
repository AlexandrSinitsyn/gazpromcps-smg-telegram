package gazpromcps.smg.utils;

import gazpromcps.smg.entity.Job;
import gazpromcps.smg.entity.User;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class Session {
    private static final int INTERVAL_LENGTH = 8;

    @Getter
    @Setter
    private User user;
    private List<Job> jobs;
    private List<Job> visibleJobs;

    @Getter
    private int step;

    @Getter
    private int pointer;

    @Getter
    @Setter
    private String confirmation;

    @Getter
    @Setter
    private Job selectedJob;

    @Getter
    private final ResourcesHandler resourcesHandler = new ResourcesHandler(Locale.getDefault());

    @Getter
    @Setter
    private Object holding;

    public Session(final User user) {
        this.user = user;
    }

    public void inc() {
        pointer = Math.min(pointer + INTERVAL_LENGTH, visibleJobs.size() - INTERVAL_LENGTH);
    }

    public void dec() {
        pointer = Math.max(pointer - INTERVAL_LENGTH, 0);
    }

    /**
     * 0 - no
     * 1 - left
     * 2 - right
     * 3 - both
     */
    public int hitBounds() {
        int res = 0;

        if (pointer > 0) {
            res += 1;
        }
        if (pointer + INTERVAL_LENGTH < visibleJobs.size()) {
            res += 2;
        }

        return res;
    }

    public List<Job> interval() {
        return visibleJobs.subList(pointer, Math.min(visibleJobs.size(), pointer + INTERVAL_LENGTH));
    }

    public void forward() {
        step++;

        jobsByStep(jobs);
    }

    public void backwards() {
        step--;
        pointer = 0;

        jobsByStep(jobs);
    }

    public void reset(final List<Job> jobs) {
        pointer = 0;
        step = 0;
        this.jobs = jobs;
        jobsByStep(jobs);
    }

    public String showInterval() {
        final AtomicInteger index = new AtomicInteger(pointer);
        return interval().stream().map(j -> index.incrementAndGet() + ") " + switch (step) {
            case 0 -> j.getStage();
            case 1 -> j.getMaster();
            case 2 -> j.getObject();
            case 3 -> "%s (%s)".formatted(j.getTitle(), j.getMeasurement());
            default -> throw new IllegalStateException();
        }).collect(Collectors.joining("\n"));
    }

    private void jobsByStep(final List<Job> jobs) {
        if (jobs == null) {
            throw new IllegalStateException("Jobs can not be null");
        }

        visibleJobs = jobs.stream().<Map<String, Job>>collect(HashMap::new, (m, j) -> m.put(switch (step) {
            case 0 -> j.getStage();
            case 1 -> j.getMaster();
            case 2 -> j.getObject();
            case 3 -> "%s (%s)".formatted(j.getTitle(), j.getMeasurement());
            default -> throw new IllegalStateException();
        }, j), Map::putAll).entrySet().stream().sorted(Entry.comparingByKey()).map(Entry::getValue).toList();
    }
}
