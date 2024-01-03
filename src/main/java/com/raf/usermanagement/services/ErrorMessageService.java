package com.raf.usermanagement.services;

import com.raf.usermanagement.model.ErrorMessage;
import com.raf.usermanagement.model.User;
import com.raf.usermanagement.repositories.ErrorMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ErrorMessageService {
    private ErrorMessageRepository errorMessageRepository;

    @Autowired
    public ErrorMessageService(ErrorMessageRepository errorMessageRepository) {
        this.errorMessageRepository = errorMessageRepository;
    }

    public List<ErrorMessage> getAll(User user){
        return this.errorMessageRepository.findAllByUser(user);
    }
}
