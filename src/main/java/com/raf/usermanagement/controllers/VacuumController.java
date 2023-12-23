package com.raf.usermanagement.controllers;

import com.raf.usermanagement.model.RoleType;
import com.raf.usermanagement.model.User;
import com.raf.usermanagement.model.Vacuum;
import com.raf.usermanagement.services.UserService;
import com.raf.usermanagement.services.VacuumService;
import com.raf.usermanagement.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

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
//        if (!SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.vacuum_search)){
//            return ResponseEntity.status(403).body("Nemate dozvolu za pretragu usisivaca.");
//        }
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


    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> testiranje(@AuthenticationPrincipal User user, @RequestHeader(value = "Authorization") String token){
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            return ResponseEntity.ok().body(loggedInUser);
        }
        return ResponseEntity.status(401).build();
    }

    @DeleteMapping(value = "/remove/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> removeVacuum(@PathVariable("id") Long id, @RequestHeader(value = "Authorization") String token){
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
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            this.vacuumService.startVacuumAsync(id, loggedInUser);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).body("Greska prilikom pokretanja vacuum-a.");
    }

    @GetMapping(value = "/stop/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> stopVacuum(@PathVariable("id") Long id, @RequestHeader(value = "Authorization") String token){
        String email = null;
        String auth = null;
        if (token != null){
            auth = token.substring(7);
            email = this.jwtUtil.extractUsername(auth);
            User loggedInUser = this.userService.getUser(email);
            this.vacuumService.stopVacuumAsync(id, loggedInUser);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(401).body("Greska prilikom zaustavljanja vacuum-a.");
    }


}
