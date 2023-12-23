package com.raf.usermanagement.utils;

import com.raf.usermanagement.model.Permission;
import com.raf.usermanagement.model.RoleType;
import com.raf.usermanagement.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class JwtUtil {
    private final String SECRET_KEY = "vranje123";

    public Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public List<RoleType> extractRoles(String token){
        return (List<RoleType>) extractAllClaims(token).get("roles");
    }

    public boolean isTokenExpired(String token){
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    public String generateToken(String email, List<Permission> permisije){
        Map<String, Object> claims = new HashMap<>();
        List<RoleType> roles = new ArrayList<>();
        for (Permission permission : permisije){
            roles.add( permission.getRole());
        }
        System.out.println("-----------> Pokusavam da generisem token, roles za korisnika su: " + roles);
        return Jwts.builder()
                .setClaims(claims)
                .claim("roles", roles)
                .setSubject(email)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(SignatureAlgorithm.HS512, SECRET_KEY).compact();
    }

    public boolean validateToken(String token, UserDetails user) {
        return (user.getUsername().equals(extractUsername(token)) && !isTokenExpired(token));
    }

    public boolean authorize(String path, String token){
        List<RoleType> roles = extractRoles(token);
        if (path.startsWith("/user")){
            String path2 = path.substring(6);
            if (path2.equals("all") && roles.contains(RoleType.can_read.toString())){
                return true;
            }
            if (path2.equals("create") && roles.contains(RoleType.can_create.toString())){
                return true;
            }
            if (path2.equals("update") && roles.contains(RoleType.can_update.toString())){
                return true;
            }
            if (path2.equals("delete") && roles.contains(RoleType.can_delete.toString())){
                return true;
            }
            return false;
        }
        return true; //ovde ce uci ako putanja nije .../user/...
    }
}
