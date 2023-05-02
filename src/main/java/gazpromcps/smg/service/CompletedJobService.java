package gazpromcps.smg.service;

import gazpromcps.smg.entity.CompletedJob;
import gazpromcps.smg.entity.User;
import gazpromcps.smg.repository.CompletedJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompletedJobService {
    private final CompletedJobRepository completedJobRepository;

    public CompletedJob save(final CompletedJob completedJob) {
        return completedJobRepository.save(completedJob);
    }

    public CompletedJob findById(final long id) {
        return completedJobRepository.findById(id).orElse(null);
    }

    public List<CompletedJob> findAll() {
        return completedJobRepository.findAll();
    }

    public List<CompletedJob> summary(final Date since) {
        return findFromDate(since).stream().collect(Collectors.groupingBy(c -> c.getJob().getMaster()))
                .values().stream().map(completedJobs -> CompletedJob.builder()
                            .job(completedJobs.get(0).getJob())
                            .count(completedJobs.stream().mapToDouble(CompletedJob::getCount).sum()).build()
                ).toList();
    }

    public List<CompletedJob> findFromDate(final Date since) {
        return completedJobRepository.findCompletedSince(Timestamp.from(since.toInstant()));
    }

    public List<CompletedJob> findAllByUser(final User user) {
        return completedJobRepository.findAllByUserOrderByCreationTimeDesc(user);
    }

    public void updateCompletedJob(final long id, final double count) {
        completedJobRepository.updateCountById(id, count);
    }
}
