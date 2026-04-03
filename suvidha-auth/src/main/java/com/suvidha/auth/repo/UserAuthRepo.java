package com.suvidha.auth.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import com.suvidha.auth.model.UsersAuth;

@Repository

public interface UserAuthRepo extends JpaRepository<UsersAuth, String> {
    Optional<UsersAuth> findByMobile(String mobile);

    Optional<UsersAuth> findByAadhar(String aadhar);

    Optional<UsersAuth> findByAadharAndMobile(String aadhar, String mobile);
}
