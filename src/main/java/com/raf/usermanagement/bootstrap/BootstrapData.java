package com.raf.usermanagement.bootstrap;

import com.raf.usermanagement.model.Permission;
import com.raf.usermanagement.model.RoleType;
import com.raf.usermanagement.model.User;
import com.raf.usermanagement.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class BootstrapData implements CommandLineRunner {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public BootstrapData(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("-----------> Pokrecem run i ucitavam podatke. <-----------");
        User user = new User();
        user.setEmail("luka@raf.rs");
        List<Permission> roles = new ArrayList<>();
        Permission p1 = new Permission();
        Permission p2 = new Permission();
        Permission p3 = new Permission();
        Permission p4 = new Permission();
        Permission v1 = new Permission();
        Permission v2 = new Permission();
        Permission v3 = new Permission();
        Permission v4 = new Permission();
        Permission v5 = new Permission();
        Permission v6 = new Permission();
        p1.setRole(RoleType.can_create);
        p2.setRole(RoleType.can_delete);
        p3.setRole(RoleType.can_update);
        p4.setRole(RoleType.can_read);
        v1.setRole(RoleType.vacuum_search);
        v2.setRole(RoleType.vacuum_add);
        v3.setRole(RoleType.vacuum_remove);
        v4.setRole(RoleType.vacuum_start);
        v5.setRole(RoleType.vacuum_stop);
        v6.setRole(RoleType.vacuum_discharge);
        roles.add(p1);
        roles.add(p2);
        roles.add(p3);
        roles.add(p4);
        roles.addAll(List.of(v1, v2, v3, v4, v5, v6));
        user.setRoles( roles );
        user.setFirstname("Luka");
        user.setLastname("Mitrovic");
        user.setPassword(this.passwordEncoder.encode("luka"));

        this.userRepository.save(user);

        User user2 = new User();
        user2.setEmail("proba@raf.rs");
        user2.setPassword(this.passwordEncoder.encode("proba"));
        user2.setFirstname("Proba");
        user2.setLastname("Probic");
        Permission p5 = new Permission();
        p5.setRole(RoleType.can_read);
        //user2.setRoles(List.of(p5));

        this.userRepository.save(user2);
        System.out.println("-----------> Podaci ucitani. <-----------");
    }
}
