package com.suvidha.auth.repo;

import com.suvidha.auth.model.DataBreach;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BreachRepo extends JpaRepository<DataBreach, String> {
}
