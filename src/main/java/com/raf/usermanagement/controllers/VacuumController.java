package com.raf.usermanagement.controllers;

import com.raf.usermanagement.model.RoleType;
import com.raf.usermanagement.model.User;
import com.raf.usermanagement.model.Vacuum;
import com.raf.usermanagement.requests.ScheduleRequest;
import com.raf.usermanagement.services.UserService;
import com.raf.usermanagement.services.VacuumService;
import com.raf.usermanagement.utils.JwtUtil;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/vacuum")
public class VacuumController {
    private final VacuumService vacuumService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public VacuumController(VacuumService vacuumService, UserService userService, JwtUtil jwtUtil) {
        this.vacuumService = vacuumService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping(value = "/add", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> addVacuum(@RequestBody Vacuum vacuum, @RequestHeader(value = "Authorization") String token){
        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_add)){
            return ResponseEntity.status(403).body("Nemate dozvolu za dodavanje novih usisivaca.");
        }
        String email = null;
        String auth = null;
        User loggedInUser = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            loggedInUser = this.userService.getUser(email);
        }
        if (loggedInUser == null){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        return ResponseEntity.ok(this.vacuumService.addVacuum(vacuum, loggedInUser));
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAll(@RequestHeader(value = "Authorization") String token){
        String email = null;
        String auth = null;
        if (token != null){
            System.out.println("GET VACUUMS: ima token");
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            return ResponseEntity.ok().body(this.vacuumService.findAllVacuumsForUser(loggedInUser));
        }
        return ResponseEntity.status(401).build();
    }

    @DeleteMapping(value = "/remove/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeVacuum(@PathVariable("id") Long id, @RequestHeader(value = "Authorization") String token){
        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_remove)){
            return ResponseEntity.status(403).body("Nemate dozvolu za uklanjanje usisivaca.");
        }
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            return ResponseEntity.ok().body(this.vacuumService.removeVacuum(id, loggedInUser));
        }
        return ResponseEntity.status(401).build();
    }

    @GetMapping(value = "/start/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> startVacuum(@PathVariable("id") Long id, @RequestHeader(value = "Authorization") String token){
        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_start)){
            return ResponseEntity.status(403).body("Nemate dozvolu za pokretanje usisivaca.");
        }
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            this.vacuumService.startVacuumAsync(id, loggedInUser, false);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).body("Greska prilikom pokretanja vacuum-a.");
    }

    @GetMapping(value = "/stop/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stopVacuum(@PathVariable("id") Long id, @RequestHeader(value = "Authorization") String token){
        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_stop)){
            return ResponseEntity.status(403).body("Nemate dozvolu za zaustavljanje usisivaca.");
        }
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            this.vacuumService.stopVacuumAsync(id, loggedInUser, false);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).body("Greska prilikom zaustavljanja vacuum-a.");
    }

    @GetMapping(value = "/discharge/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> dischargeVacuum(@PathVariable("id") Long id, @RequestHeader("Authorization") String token){
        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_discharge)){
            return ResponseEntity.status(403).body("Nemate dozvolu za praznjenje usisivaca, praznjenje ce se odraditi automatski nakon svakog treceg pokretanja.");
        }
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            this.vacuumService.dischargeVacuumAsync(id, loggedInUser, false);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).body("Greska prilikom praznjenja vacuum-a.");
    }

    @PostMapping(value = "/schedule", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> scheduleOperation(@RequestBody ScheduleRequest scheduleRequest, @RequestHeader("Authorization") String token){
        if (scheduleRequest.getOperation().toString().equalsIgnoreCase("START")){
            if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_start)){
                return ResponseEntity.status(403).body("Nemate dozvolu za pokretanje usisivaca.");
            }
        } else if (scheduleRequest.getOperation().toString().equalsIgnoreCase("STOP")){
            if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_stop)){
                return ResponseEntity.status(403).body("Nemate dozvolu za zaustavljanje usisivaca.");
            }
        } else if (scheduleRequest.getOperation().toString().equalsIgnoreCase("DISCHARGE")){
            if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_discharge)){
                return ResponseEntity.status(403).body("Nemate dozvolu za praznjenje usisivaca.");
            }
        } else {
            System.out.println("Lose prosledjena operacija: " + scheduleRequest.getOperation().toString());
            return ResponseEntity.badRequest().body("Operacija ne postoji.");
        }
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            this.vacuumService.schedule(scheduleRequest, loggedInUser);
            return ResponseEntity.ok("Operacija: " + scheduleRequest.getOperation() + " zakazana.");
        }
        return ResponseEntity.status(401).body("Greska prilikom zakazivanja operacije vacuum-a.");
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> searchVacuums(@RequestHeader("Authorization") String token, @RequestParam(value = "name", required = false) String name, @RequestParam(value = "statuses", required = false) List<String> statuses, @RequestParam(value = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom, @RequestParam(value = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo){
        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_search)){
            return ResponseEntity.status(403).body("Nemate dozvolu za pretragu usisivaca.");
        }
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            if (statuses == null)
                statuses = new ArrayList<>();
            return ResponseEntity.ok(this.vacuumService.searchByAll(name, statuses, dateFrom, dateTo, loggedInUser));
        }
        return ResponseEntity.status(401).body("Greska prilikom pretrage vacuum-a.");
    }
}
