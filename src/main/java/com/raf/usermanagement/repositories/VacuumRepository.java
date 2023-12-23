package com.raf.usermanagement.repositories;

import com.raf.usermanagement.model.User;
import com.raf.usermanagement.model.Vacuum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VacuumRepository extends JpaRepository<Vacuum, Long> {
    public List<Vacuum> findAllByAddedBy(User user);
}
