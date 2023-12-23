package com.raf.usermanagement.services;

import com.raf.usermanagement.model.User;
import com.raf.usermanagement.model.Vacuum;
import com.raf.usermanagement.repositories.VacuumRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class VacuumService {
    //TODO: Implementirati svrhu ErrorMessage-a
    //TODO: Ubaciti "medjustanje" PROCESSING koje ce obezbediti da se ostale operacije ne mogu izvrsavati dok se izvrsava npr. startAsync!
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
    public Vacuum removeVacuum(Long id, User user){
        Optional<Vacuum> vacuum = this.vacuumRepository.findById(id);
        if (!vacuum.isPresent()){
            return null;
        }
        if (vacuum.get().getAddedBy().getId() != user.getId()){
            return null;
        }
        if (!vacuum.get().getStatus().equals(Vacuum.VacuumStatus.OFF))
            return null;
        vacuum.get().setActive(false);
        return this.vacuumRepository.save(vacuum.get());
    }

    //TODO: Razmotriti da start/stop ne budu void nego String kako bi se prosledjivalo nesto u body-u response-a?
    public void startVacuumAsync(Long vacuumId, User user) {
        Optional<Vacuum> vacuumOptional = this.vacuumRepository.findById(vacuumId);
        if (!vacuumOptional.isPresent()){
            System.out.println("Nisam uspeo da pronadjem vacuum sa prosledjenim id-em.");
            return;
        } else if (vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.ON)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.DISCHARGING)){
            System.out.println("Vacuum nije u stanju da trenutno bude pokrenut.");
            return;
        } else if (vacuumOptional.get().getAddedBy().getId() != user.getId()){
            System.out.println("Vacuum nije u vlasnistvu korisnika koji pokusava da ga pokrene.");
            return;
        }
//        try{
//            Thread.sleep(15000);
//            vacuumOptional.get().setStartCount(vacuumOptional.get().getStartCount() + 1);
//            //TODO: Dodati logiku da sam ode u DISCHARGING nakon treceg pokretanja, logicno odraditi u metodi koja ga stavlja u stanje OFF
//            vacuumOptional.get().setStatus(Vacuum.VacuumStatus.ON);
//            this.vacuumRepository.save(vacuumOptional.get());
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ObjectOptimisticLockingFailureException exception){
//            this.startVacuumAsync(vacuumId, user);
//        }
        CompletableFuture.runAsync(() -> {
            try {
                vacuumOptional.get().setStartCount(vacuumOptional.get().getStartCount() + 1);
                // Dodati logiku za DISCHARGING
                vacuumOptional.get().setStatus(Vacuum.VacuumStatus.ON);
                this.vacuumRepository.save(vacuumOptional.get());
            } catch (ObjectOptimisticLockingFailureException exception) {
                this.startVacuumAsync(vacuumId, user);
            }
        }, CompletableFuture.delayedExecutor(15, TimeUnit.SECONDS));
    }

    public void stopVacuumAsync(Long vacuumId, User user){
        Optional<Vacuum> vacuumOptional = this.vacuumRepository.findById(vacuumId);
        if (!vacuumOptional.isPresent()){
            System.out.println("Nisam uspeo da pronadjem vacuum sa prosledjenim id-em.");
            return;
        } else if (vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.OFF)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.DISCHARGING)){
            System.out.println("Vacuum nije u stanju da trenutno bude zaustavljen.");
            return;
        } else if (vacuumOptional.get().getAddedBy().getId() != user.getId()){
            System.out.println("Vacuum nije u vlasnistvu korisnika koji pokusava da ga pokrene.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                // Dodati logiku za DISCHARGING
                vacuumOptional.get().setStatus(Vacuum.VacuumStatus.OFF);
                this.vacuumRepository.save(vacuumOptional.get());
                System.out.println("Promenjeno stanje na OFF u bazi.");
            } catch (ObjectOptimisticLockingFailureException exception) {
                this.stopVacuumAsync(vacuumId, user);
            }
        }, CompletableFuture.delayedExecutor(18, TimeUnit.SECONDS));
        System.out.println("Odradjen stop...");
    }



}
