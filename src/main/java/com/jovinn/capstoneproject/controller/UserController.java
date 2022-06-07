package com.jovinn.capstoneproject.controller;

import com.jovinn.capstoneproject.dto.UserProfile;
import com.jovinn.capstoneproject.model.User;
import com.jovinn.capstoneproject.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    @Autowired
    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {

        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/users").toUriString());
        return ResponseEntity.created(uri).body(userService.getUsers());
    }

    @GetMapping("/user/{username}")
    public UserProfile getUserByUsername(@PathVariable String username) {
        return userService.getUserProfile(username);
    }

    @GetMapping("/listUsers")
    public List<User> getAll() {
        return userService.getUsers();
    }
//    @PostMapping("/auth/register")   //api method post : url :'http://localhost:8080/api/auth/register'
//    public ResponseEntity<User> register(@RequestBody User user){
////        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/user/save").toUriString());
//        return ResponseEntity.ok().body(userService.saveUser(user));
//    }
    //    @PostMapping("/role/save")
//    public ResponseEntity<Role> saveRole(@RequestBody Role role){
////        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/role/save").toUriString());
//        return ResponseEntity.ok().body(userService.saveRole(role));
//    }
//    @PostMapping("/role/addtouser")
//    public ResponseEntity<?> addRoleToUser(@RequestBody RoleToUserForm form){
//        userService.addRoleToUser(form.getUsername(),form.getRoleName());
//        return ResponseEntity.ok().build();
//    }
//    @GetMapping("/auth/token/refresh")
//    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
//        String authorizationHeader = request.getHeader(AUTHORIZATION);
//        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")){
//            try {
//                String refresh_token = authorizationHeader.substring("Bearer ".length());
//                Algorithm algorithm = Algorithm.HMAC256("secret".getBytes());
//                JWTVerifier verifier = JWT.require(algorithm).build();
//                DecodedJWT decodedJWT = verifier.verify(refresh_token);
//                String email = decodedJWT.getSubject();
//                User user = userService.getUser(email);
//                String access_token = JWT.create()
//                        .withSubject(user.getEmail())
//                        .withExpiresAt(new Date(System.currentTimeMillis() +10*60*1000))
//                        .withIssuer(request.getRequestURL().toString())
//                        .withClaim("roles", user.getActivity_type().toString())
//                        .sign(algorithm);
//                Map<String,String> tokens = new HashMap<>();
//                tokens.put("access_token",access_token);
//                tokens.put("refresh_token",refresh_token);
//                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//                new ObjectMapper().writeValue(response.getOutputStream(),tokens);
//            }catch (Exception exception){
//
//                response.setHeader("error",exception.getMessage());
//                response.setStatus(FORBIDDEN.value());
////                      response.sendError(FORBIDDEN.value());
//                Map<String,String> error = new HashMap<>();
////                      tokens.put("access_token",access_token);
//                error.put("error_message",exception.getMessage());
//                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
//                new ObjectMapper().writeValue(response.getOutputStream(),error);
//            }
//
//        }else{
//            throw new  RuntimeException("Refresh token is missing");
//        }
//    }
//
}
//@Data
//class RoleToUserForm{
//    private String username;
//    private String roleName;
//}
