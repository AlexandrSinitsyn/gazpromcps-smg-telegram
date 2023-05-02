package gazpromcps.smg.repository;

import gazpromcps.smg.entity.Role;
import gazpromcps.smg.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select u from User u where u.role > ?1")
    List<User> findAllRoleGreater(Role min);

    @Modifying
    @Transactional
    @Query("update User u set u.role = ?2 where u.id = ?1")
    void promoteToRoleById(long id, Role role);
}
