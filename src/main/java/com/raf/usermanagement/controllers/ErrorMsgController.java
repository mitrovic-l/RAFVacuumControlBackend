package com.raf.usermanagement.controllers;

import com.raf.usermanagement.model.User;
import com.raf.usermanagement.services.ErrorMessageService;
import com.raf.usermanagement.services.UserService;
import com.raf.usermanagement.utils.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/errors")
public class ErrorMsgController {
    private final ErrorMessageService errorMessageService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    public ErrorMsgController(ErrorMessageService errorMessageService, UserService userService, JwtUtil jwtUtil) {
        this.errorMessageService = errorMessageService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> getAllErrors(@RequestHeader("Authorization") String token){
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
        return ResponseEntity.ok(this.errorMessageService.getAll(loggedInUser));
    }
}
