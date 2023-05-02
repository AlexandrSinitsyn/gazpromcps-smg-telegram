package gazpromcps.smg.repository;

import gazpromcps.smg.entity.Job;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {
    @Transactional
    @Modifying
    @Query("update Job j set j.active = false")
    void deactivateAll();

    @Transactional
    @Modifying
    @Query("update Job j set j.active = true where j.id = ?1")
    void activateById(long id);

    Job findByStageAndMasterAndObjectAndTitleAndMeasurement(final @NotNull @NotBlank String stage,
                               final @NotNull @NotBlank String master,
                               final @NotNull @NotBlank String object,
                               final @NotNull @NotBlank String title,
                               final @NotNull @NotBlank String measurement);

    @Transactional
    @Modifying
    @Query("select j from Job j where j.active = true")
    List<Job> findAllActive();
}
