package com.raf.usermanagement.services;

import com.raf.usermanagement.model.User;
import com.raf.usermanagement.model.Vacuum;
import com.raf.usermanagement.repositories.VacuumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class VacuumService {
    private VacuumRepository vacuumRepository;

    @Autowired
    public VacuumService(VacuumRepository vacuumRepository) {
        this.vacuumRepository = vacuumRepository;
    }
    public Vacuum addVacuum(Vacuum vacuum, User user){
        vacuum.setAddedBy(user);
        vacuum.setStatus(Vacuum.VacuumStatus.OFF);
        vacuum.setDateAdded(LocalDate.now());
        vacuum.setActive(true);
        vacuum.setStartCount(0);
        return this.vacuumRepository.save(vacuum);
    }
    public List<Vacuum> findAllVacuumsForUser(User user){
        return this.vacuumRepository.findAllByAddedBy(user);
    }


}
