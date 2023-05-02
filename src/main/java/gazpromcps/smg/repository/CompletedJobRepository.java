package gazpromcps.smg.repository;

import gazpromcps.smg.entity.CompletedJob;
import gazpromcps.smg.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface CompletedJobRepository extends JpaRepository<CompletedJob, Long> {
    @Transactional
    @Modifying
    @Query("update CompletedJob c set c.count = ?2 where c.id = ?1")
    void updateCountById(long id, double count);
    @Query("select c from CompletedJob c where c.creationTime >= ?1")
    List<CompletedJob> findCompletedSince(Timestamp creationTime);

    List<CompletedJob> findAllByUserOrderByCreationTimeDesc(User user);
}
