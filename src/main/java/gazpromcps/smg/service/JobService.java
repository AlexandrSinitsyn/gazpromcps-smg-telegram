package gazpromcps.smg.service;

import gazpromcps.smg.entity.Job;
import gazpromcps.smg.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {
    private final JobRepository jobRepository;

    public Job save(final Job job) {
        return jobRepository.save(job);
    }

    public void deactivate() {
        jobRepository.deactivateAll();
    }

    public void update(final List<Job> list) {
        deactivate();

        final List<Job> newJobs = list.stream().filter(j -> {
            final var found = jobRepository.findByStageAndMasterAndObjectAndTitleAndMeasurement(
                    j.getStage(), j.getMaster(), j.getObject(), j.getTitle(), j.getMeasurement());

            if (found != null) {
                jobRepository.activateById(found.getId());
            }

            return found == null;
        }).toList();

        jobRepository.saveAll(newJobs);
    }

    public Job findById(final long id) {
        return jobRepository.findById(id).orElse(null);
    }

    public List<Job> findAll() {
        return jobRepository.findAll();
    }

    public List<Job> findAllActive() {
        return jobRepository.findAllActive();
    }
}
