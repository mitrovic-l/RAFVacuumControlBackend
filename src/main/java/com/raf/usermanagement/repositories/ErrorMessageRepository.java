package com.raf.usermanagement.repositories;

import com.raf.usermanagement.model.ErrorMessage;
import com.raf.usermanagement.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ErrorMessageRepository extends JpaRepository<ErrorMessage, Integer> {
    List<ErrorMessage> findAllByUser(User user);
}
