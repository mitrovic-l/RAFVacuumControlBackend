package com.raf.usermanagement.controllers;

import com.raf.usermanagement.model.Permission;
import com.raf.usermanagement.model.RoleType;
import com.raf.usermanagement.model.User;
import com.raf.usermanagement.services.UserService;
import com.raf.usermanagement.utils.JwtUtil;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, JwtUtil jwtUtil, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllUsers(){
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.can_read)){
            return ResponseEntity.ok(userService.findAll());
        } else {
            return ResponseEntity.status(403).body("Nemate dozvolu za pregled korisnika.");
        }
    }

    @PostMapping(value = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createUser(@RequestBody User user, @RequestParam(value = "roleIDs", required = false) Integer[] ids){
        System.out.println(SecurityContextHolder.getContext().getAuthentication().getAuthorities());
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.can_create)){
            user.setPassword(this.passwordEncoder.encode(user.getPassword()));
            List<Permission> permissions = this.userService.allPermissions();
            //Izmenjeno
            List<Permission> permissionsList = new ArrayList<>();
            if (ids.length != 0) {
                for (Integer id : ids) {
                    if (id == 1) {
                        System.out.println("Dodata permisija 1");
                        permissionsList.add(permissions.get(0));
                    }
                    if (id == 2) {
                        System.out.println("Dodata permisija 2");
                        permissionsList.add(permissions.get(1));
                    }
                    if (id == 3) {
                        System.out.println("Dodata permisija 3");
                        permissionsList.add(permissions.get(2));
                    }
                    if (id == 4) {
                        System.out.println("Dodata permisija 4");
                        permissionsList.add(permissions.get(3));
                    }
                    if (id == 5) {
                        permissionsList.add(permissions.get(4));
                    }
                    if (id == 6) {
                        permissionsList.add(permissions.get(5));
                    }
                    if (id == 7) {
                        permissionsList.add(permissions.get(6));
                    }
                    if (id == 8) {
                        permissionsList.add(permissions.get(7));
                    }
                    if (id == 9) {
                        permissionsList.add(permissions.get(8));
                    }
                    if (id == 10) {
                        permissionsList.add(permissions.get(9));
                    }
                }
            }
            user.setRoles( permissionsList );
            return ResponseEntity.ok(userService.createNewUser(user));
        } else {
            return ResponseEntity.status(403).body("Nemate dozvolu za kreiranje novog korisnika.");
        }
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateUser(@RequestBody User user,@PathVariable("id") Integer userId, @RequestParam(value = "roleIDs", required = false) Integer[] ids){
        System.out.println("UPDATE USER U KONTROLERU");
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.can_update)){
            Optional<User> user1 = userService.findById(userId);
            System.out.println(" ---------> naso korisinka:  " + user1.get().getEmail() + user1.get().getFirstname() + user1.get().getRoles()) ;
            user.setPassword(user1.get().getPassword());
            System.out.println("-----------> permisije");
            List<Permission> permissions = this.userService.allPermissions();
            List<Permission> permissionsList = new ArrayList<>();
            if (ids.length != 0) {
                for (Integer id : ids) {
                    if (id == 1) {
                        System.out.println("Dodata permisija 1");
                        permissionsList.add(permissions.get(0));
                    }
                    if (id == 2) {
                        System.out.println("Dodata permisija 2");
                        permissionsList.add(permissions.get(1));
                    }
                    if (id == 3) {
                        System.out.println("Dodata permisija 3");
                        permissionsList.add(permissions.get(2));
                    }
                    if (id == 4) {
                        System.out.println("Dodata permisija 4");
                        permissionsList.add(permissions.get(3));
                    }
                    if (id == 5) {
                        permissionsList.add(permissions.get(4));
                    }
                    if (id == 6) {
                        permissionsList.add(permissions.get(5));
                    }
                    if (id == 7) {
                        permissionsList.add(permissions.get(6));
                    }
                    if (id == 8) {
                        permissionsList.add(permissions.get(7));
                    }
                    if (id == 9) {
                        permissionsList.add(permissions.get(8));
                    }
                    if (id == 10) {
                        permissionsList.add(permissions.get(9));
                    }
                }
            }
            user.setRoles( permissionsList );
            user1.get().setRoles( permissionsList );
            user1.get().setFirstname(user.getFirstname());
            user1.get().setLastname(user.getLastname());
            System.out.println("Zovem save");
            return ResponseEntity.ok(userService.save(user1.get()));
        } else {
            return ResponseEntity.status(403).body("Nemate dozvolu da menjate podatke korisnicima.");
        }
    }

    @DeleteMapping(value = "/delete/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Integer id){
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.can_delete)){
            Optional<User> optionalUser = userService.findById(id);
            System.out.println(optionalUser);
            if (optionalUser.isPresent()){
                userService.deleteById(id);
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(403).body("Nemate dozvolu da brisete korisnike.");
    }

    @GetMapping(value = "/roles")
    public List<RoleType> getRoles(){
        return (List<RoleType>) SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    }
    @GetMapping(value = "/roles/{id}")
    public List<Permission> getPermissionsById(@PathVariable Integer id){
        if (SecurityContextHolder.getContext().getAuthentication().getAuthorities().contains(RoleType.can_read)) {
            return this.userService.permissions(id);
        }
        return null;
    }

    @GetMapping(value = "/find/{email}", produces = MediaType.APPLICATION_JSON_VALUE)
    public User findUser(@PathVariable String email){
        return this.userService.getUser(email);
    }

    @GetMapping(value = "/allRoles", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Permission> allRoles(){
        return this.userService.allPermissions();
    }


}
