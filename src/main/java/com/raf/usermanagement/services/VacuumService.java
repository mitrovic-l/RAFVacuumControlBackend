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
        if (vacuumList.isEmpty())
            return vacuumList;
        List<Vacuum> forReturn = new ArrayList<>();
        for (Vacuum v : vacuumList){
            if (v.getActive())
                forReturn.add(v);
        }
        return forReturn;
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
        if (!vacuumOptional.isPresent() || !vacuumOptional.get().getActive()){
            System.out.println("Nisam uspeo da pronadjem vacuum sa prosledjenim id-em.");
            return;
        } else if (vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.ON)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.DISCHARGING)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.PROCESSING)){
            if (scheduled){
                ErrorMessage message = new ErrorMessage();
                message.setVacuum(vacuumOptional.get());
                message.setUser(user);
                message.setDate(LocalDate.now());
                message.setMessage("Usisivac [" + vacuumOptional.get().getName() + "] nije u stanju da bude pokrenut ili je vec pokrenut!");
                message.setOperation("START");
                this.errorMessageRepository.save(message);
            }
            return;
        } else if (vacuumOptional.get().getAddedBy().getId() != user.getId()){
            return;
        }
        vacuumOptional.get().setStatus(Vacuum.VacuumStatus.PROCESSING);
        this.vacuumRepository.save(vacuumOptional.get());
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
        if (!vacuumOptional.isPresent() || !vacuumOptional.get().getActive()){
            return;
        } else if (vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.OFF)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.DISCHARGING)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.PROCESSING)){
            if (scheduled){
                ErrorMessage message = new ErrorMessage();
                message.setVacuum(vacuumOptional.get());
                message.setUser(user);
                message.setDate(LocalDate.now());
                message.setMessage("Usisivac [" + vacuumOptional.get().getName() + "] nije u stanju da bude zaustavljen ili je vec zaustavljen!");
                message.setOperation("STOP");
                this.errorMessageRepository.save(message);
            }
            return;
        } else if (vacuumOptional.get().getAddedBy().getId() != user.getId()){
            return;
        }
        vacuumOptional.get().setStatus(Vacuum.VacuumStatus.PROCESSING);
        this.vacuumRepository.save(vacuumOptional.get());
        //Provera da li treba da se isprazni
        if (vacuumOptional.get().getStartCount() % 3 == 0){
            //Nakon svakog treceg pokretanja potrebno je isprazniti
            vacuumOptional.get().setStatus(Vacuum.VacuumStatus.OFF);
            this.vacuumRepository.save(vacuumOptional.get());
            dischargeVacuumAsync(vacuumId, user, scheduled); //mozda staviti ovo na false jer ima vise smisla?
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                vacuumOptional.get().setStatus(Vacuum.VacuumStatus.OFF);
                this.vacuumRepository.save(vacuumOptional.get());
            } catch (ObjectOptimisticLockingFailureException exception) {
                this.stopVacuumAsync(vacuumId, user, scheduled);
            }
        }, CompletableFuture.delayedExecutor(18, TimeUnit.SECONDS));
    }

    @Async
    @Transactional
    public void dischargeVacuumAsync(Long vacuumId, User user, boolean scheduled){
        Optional<Vacuum> vacuumOptional = this.vacuumRepository.findById(vacuumId);
        if (!vacuumOptional.isPresent() || !vacuumOptional.get().getActive()){
            return;
        } else if (vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.ON)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.DISCHARGING)
                || vacuumOptional.get().getStatus().equals(Vacuum.VacuumStatus.PROCESSING)){
            if (scheduled){
                ErrorMessage message = new ErrorMessage();
                message.setVacuum(vacuumOptional.get());
                message.setUser(user);
                message.setDate(LocalDate.now());
                message.setMessage("Usisivac [" + vacuumOptional.get().getName() + "] nije u stanju da bude ispraznjen ili se trenutno prazni.");
                message.setOperation("DISCHARGE");
                this.errorMessageRepository.save(message);
            }
            return;
        } else if (vacuumOptional.get().getAddedBy().getId() != user.getId()){
            return;
        }
        vacuumOptional.get().setStatus(Vacuum.VacuumStatus.DISCHARGING);
        this.vacuumRepository.save(vacuumOptional.get());
        CompletableFuture.runAsync(() -> {
//            vacuumOptional.get().setStatus(Vacuum.VacuumStatus.OFF);
//            this.vacuumRepository.save(vacuumOptional.get());
            CompletableFuture.runAsync( () -> {
                vacuumOptional.get().setStatus(Vacuum.VacuumStatus.OFF);
                vacuumOptional.get().setStartCount(0);
                this.vacuumRepository.save(vacuumOptional.get());
            }, CompletableFuture.delayedExecutor(16, TimeUnit.SECONDS));
        }, CompletableFuture.delayedExecutor(16, TimeUnit.SECONDS));

    }


    @Async
    public void schedule(ScheduleRequest scheduleRequest, User user){
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
                if (statusList.contains(vacuum.getStatus()) && vacuum.getActive()){
                    forReturn.add(vacuum);
                }
            }
            return forReturn;
        }
        return this.vacuumRepository.findVacuums(name, dateFrom, dateTo, addedBy);
    }

    public Optional<Vacuum> findVacuumById(Long id){
        return this.vacuumRepository.findById(id);
    }
}
