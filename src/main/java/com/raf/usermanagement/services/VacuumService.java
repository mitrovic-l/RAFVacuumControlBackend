package com.raf.usermanagement.services;

import com.raf.usermanagement.model.ErrorMessage;
import com.raf.usermanagement.model.User;
import com.raf.usermanagement.model.Vacuum;
import com.raf.usermanagement.repositories.ErrorMessageRepository;
import com.raf.usermanagement.repositories.VacuumRepository;
import com.raf.usermanagement.requests.ScheduleRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class VacuumService {
    //TODO: Pre svakog poziva dodati proveru da li je vacuum aktivan!
    //TODO: Implementirati svrhu ErrorMessage-a
    //TODO: Ubaciti "medjustanje" PROCESSING koje ce obezbediti da se ostale operacije ne mogu izvrsavati dok se izvrsava npr. startAsync!
    private VacuumRepository vacuumRepository;
    private TaskScheduler taskScheduler;
    private ErrorMessageRepository errorMessageRepository;
    @Autowired
    public VacuumService(VacuumRepository vacuumRepository, TaskScheduler taskScheduler, ErrorMessageRepository errorMessageRepository) {
        this.vacuumRepository = vacuumRepository;
        this.taskScheduler = taskScheduler;
        this.errorMessageRepository = errorMessageRepository;
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
        List<Vacuum> vacuumList = this.vacuumRepository.findAllByAddedBy(user);
        for (int i=0; i< vacuumList.size(); i++){
            if (!vacuumList.get(i).getActive())
                vacuumList.remove(i);
        }
        return vacuumList;
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
    public void startVacuumAsync(Long vacuumId, User user, boolean scheduled) {
        Optional<Vacuum> vacuumOptional = this.vacuumRepository.findById(vacuumId);
        if (!vacuumOptional.isPresent()){
            System.out.println("Nisam uspeo da pronadjem vacuum sa prosledjenim id-em.");
            return;
        } else if (vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.ON)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.DISCHARGING)){
            System.out.println("Vacuum nije u stanju da trenutno bude pokrenut.");
            //Doslo je do greske, proveramo ako je scheduled moramo sacuvati errorMessage
            if (scheduled){
                ErrorMessage message = new ErrorMessage();
                message.setVacuum(vacuumOptional.get());
                message.setUser(user);
                message.setDate(LocalDate.now());
                message.setMessage("Usisivac [" + vacuumOptional.get().getName() + "] nije u stanju da bude pokrenut ili je vec pokrenut!");
                message.setOperation("START");
                this.errorMessageRepository.save(message);
                System.out.println(" --- --- --- --- ---> Uspesno sacuvana poruka o gresci.");
            }
            return;
        } else if (vacuumOptional.get().getAddedBy().getId() != user.getId()){
            System.out.println("Vacuum nije u vlasnistvu korisnika koji pokusava da ga pokrene.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                vacuumOptional.get().setStartCount(vacuumOptional.get().getStartCount() + 1);
                // Dodati logiku za DISCHARGING
                vacuumOptional.get().setStatus(Vacuum.VacuumStatus.ON);
                this.vacuumRepository.save(vacuumOptional.get());
            } catch (ObjectOptimisticLockingFailureException exception) {
                this.startVacuumAsync(vacuumId, user, scheduled);
            }
        }, CompletableFuture.delayedExecutor(15, TimeUnit.SECONDS));
    }

    @Async
    @Transactional
    public void stopVacuumAsync(Long vacuumId, User user, boolean scheduled){
        Optional<Vacuum> vacuumOptional = this.vacuumRepository.findById(vacuumId);
        if (!vacuumOptional.isPresent()){
            System.out.println("Nisam uspeo da pronadjem vacuum sa prosledjenim id-em.");
            return;
        } else if (vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.OFF)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.DISCHARGING)){
            System.out.println("Vacuum nije u stanju da trenutno bude zaustavljen.");
            if (scheduled){
                ErrorMessage message = new ErrorMessage();
                message.setVacuum(vacuumOptional.get());
                message.setUser(user);
                message.setDate(LocalDate.now());
                message.setMessage("Usisivac [" + vacuumOptional.get().getName() + "] nije u stanju da bude zaustavljrn ili je vec zaustavljen!");
                message.setOperation("STOP");
                this.errorMessageRepository.save(message);
                System.out.println(" --- --- --- --- ---> Uspesno sacuvana poruka o gresci.");
            }
            return;
        } else if (vacuumOptional.get().getAddedBy().getId() != user.getId()){
            System.out.println("Vacuum nije u vlasnistvu korisnika koji pokusava da ga stopira.");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                vacuumOptional.get().setStatus(Vacuum.VacuumStatus.OFF);
                this.vacuumRepository.save(vacuumOptional.get());
                System.out.println("Promenjeno stanje na OFF u bazi.");
                //Provera da li treba da se isprazni
                if (vacuumOptional.get().getStartCount() % 3 == 0){
                    //Nakon svakog treceg pokretanja potrebno je isprazniti
                    dischargeVacuumAsync(vacuumId, user, scheduled); //mozda staviti ovo na false jer ima vise smisla?
                }
            } catch (ObjectOptimisticLockingFailureException exception) {
                this.stopVacuumAsync(vacuumId, user, scheduled);
            }
        }, CompletableFuture.delayedExecutor(18, TimeUnit.SECONDS));
        System.out.println("Odradjen stop...");
    }

    @Async
    @Transactional
    public void dischargeVacuumAsync(Long vacuumId, User user, boolean scheduled){
        Optional<Vacuum> vacuumOptional = this.vacuumRepository.findById(vacuumId);
        if (!vacuumOptional.isPresent()){
            System.out.println("Nisam uspeo da pronadjem vacuum sa prosledjenim id-em.");
            return;
        } else if (vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.ON)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.DISCHARGING)){
            System.out.println("Vacuum nije u stanju da trenutno bude ispraznjen.");
            if (scheduled){
                ErrorMessage message = new ErrorMessage();
                message.setVacuum(vacuumOptional.get());
                message.setUser(user);
                message.setDate(LocalDate.now());
                message.setMessage("Usisivac [" + vacuumOptional.get().getName() + "] nije u stanju da bude ispraznjen ili se trenutno prazni.");
                message.setOperation("DISCHARGE");
                this.errorMessageRepository.save(message);
                System.out.println(" --- --- --- --- ---> Uspesno sacuvana poruka o gresci.");
            }
            return;
        } else if (vacuumOptional.get().getAddedBy().getId() != user.getId()){
            System.out.println("Vacuum nije u vlasnistvu korisnika koji pokusava da ga isprazni.");
            return;
        }
        vacuumOptional.get().setStatus(Vacuum.VacuumStatus.DISCHARGING);
        this.vacuumRepository.save(vacuumOptional.get());
        System.out.println("--------------------> Discharging Status...");
        CompletableFuture.runAsync(() -> {
//            vacuumOptional.get().setStatus(Vacuum.VacuumStatus.OFF);
            System.out.println("----------------------------------------------------> DISCHARGING VACUUUM <-------");
//            this.vacuumRepository.save(vacuumOptional.get());
            CompletableFuture.runAsync( () -> {
                System.out.println("----------------------***------> STOPPING VACUUM!");
                vacuumOptional.get().setStatus(Vacuum.VacuumStatus.OFF);
                vacuumOptional.get().setStartCount(0);
                this.vacuumRepository.save(vacuumOptional.get());
            }, CompletableFuture.delayedExecutor(16, TimeUnit.SECONDS));
            System.out.println("-----------------------------------> Stopped Status...");
        }, CompletableFuture.delayedExecutor(16, TimeUnit.SECONDS));

    }


    @Async
    public void schedule(ScheduleRequest scheduleRequest, User user){
        //TODO: dodati sve provere kao iznad...
        taskScheduler.schedule( () -> {
            try {
                System.out.println("Izvrsavanje zakazane operacije u : " + LocalDateTime.now().toString());
                if (scheduleRequest.getOperation() == ScheduleRequest.VacuumOperation.START){
                    startVacuumAsync(scheduleRequest.getVacuumId(), user, true);
                } else if (scheduleRequest.getOperation() == ScheduleRequest.VacuumOperation.STOP){
                    stopVacuumAsync(scheduleRequest.getVacuumId(), user, true);
                } else if (scheduleRequest.getOperation() == ScheduleRequest.VacuumOperation.DISCHARGE){
                    dischargeVacuumAsync(scheduleRequest.getVacuumId(), user, true);
                } else{
                    System.out.println("Los format operacije.");
                    return;
                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }, Date.from(scheduleRequest.getScheduledTime().atZone(ZoneId.systemDefault()).toInstant()));
    }
    public List<Vacuum> searchByAll(String name, List<String> statuses, LocalDate dateFrom, LocalDate dateTo, User addedBy){
        List<Vacuum.VacuumStatus> statusList = new ArrayList<>();
        if (!statuses.isEmpty()){
            System.out.println(statuses);
            for (String str : statuses){
                if ((str.equalsIgnoreCase("ON") || str.equalsIgnoreCase("OFF") || str.equalsIgnoreCase("DISCHARGING"))){
                    statusList.add(Vacuum.VacuumStatus.valueOf(str.toUpperCase()));
                }
                else
                    System.out.println("------ --- --- ---> Prosledjen los status: " + str );
            }
            System.out.println("------ ---- -- ->Lista statusa: " + statusList);
            List<Vacuum> foundVacuums = this.vacuumRepository.findVacuums(name, dateFrom, dateTo, addedBy);
            List<Vacuum> forReturn = new ArrayList<>();
            for (Vacuum vacuum : foundVacuums){
                if (statusList.contains(vacuum.getStatus())){
                    forReturn.add(vacuum);
                }
            }
            return forReturn;
        }
        return this.vacuumRepository.findVacuums(name, dateFrom, dateTo, addedBy);
    }
}
