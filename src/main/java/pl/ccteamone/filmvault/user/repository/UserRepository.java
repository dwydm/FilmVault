package pl.ccteamone.filmvault.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.ccteamone.filmvault.user.MyUser;

import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<MyUser, UUID> {
//    Optional<MyUser> findByUserName(String userName);
}
