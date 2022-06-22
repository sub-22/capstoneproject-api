package com.jovinn.capstoneproject.controller;

import com.jovinn.capstoneproject.dto.UserProfile;
import com.jovinn.capstoneproject.dto.UserSummary;
import com.jovinn.capstoneproject.dto.request.ResetPasswordRequest;
import com.jovinn.capstoneproject.enumerable.RankSeller;
import com.jovinn.capstoneproject.enumerable.UserActivityType;
import com.jovinn.capstoneproject.exception.ApiException;
import com.jovinn.capstoneproject.exception.ResourceNotFoundException;
import com.jovinn.capstoneproject.model.Seller;
import com.jovinn.capstoneproject.model.User;
import com.jovinn.capstoneproject.security.CurrentUser;
import com.jovinn.capstoneproject.security.UserPrincipal;
import com.jovinn.capstoneproject.service.ActivityTypeService;
import com.jovinn.capstoneproject.service.SellerService;
import com.jovinn.capstoneproject.service.UserService;
import com.jovinn.capstoneproject.util.RequestUtility;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static com.jovinn.capstoneproject.util.GenerateRandomNumber.getRandomNumberString;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private SellerService sellerService;
    @Autowired
    private ActivityTypeService activityTypeService;
    private final JavaMailSender mailSender;

    @GetMapping("/me")
    public ResponseEntity<UserSummary> getCurrentUser(@CurrentUser UserPrincipal currentUser) {
        UserSummary userSummary = userService.getCurrentUser(currentUser);

        if(userSummary == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "User not found in database!!!");
        }

        return new ResponseEntity< >(userSummary, HttpStatus.OK);
    }

    @GetMapping("/profile/{id}")
    public User getUserProfile(@PathVariable UUID id) {
        return userService.getByUserId(id);
    }

    @GetMapping("/profileByName/{username}")
    public ResponseEntity<UserProfile> getUSerProfileByUserName(@PathVariable String username) {
        UserProfile userProfile = userService.getUserProfile(username);

        return new ResponseEntity< >(userProfile, HttpStatus.OK);
    }

    @GetMapping("")
    public List<User> getListUsers() {
        return userService.getUsers();
    }

    @PutMapping("/profile/{id}")
    public ResponseEntity<User> updateUser(@Valid @RequestBody User newUser,
                                           @PathVariable("id") UUID id, @CurrentUser UserPrincipal currentUser) {
        User updatedUSer = userService.update(newUser, id, currentUser);

        return new ResponseEntity< >(updatedUSer, HttpStatus.CREATED);
    }
    @PostMapping("/{id}/join-selling")
    public Seller joinSelling(@PathVariable UUID id, @RequestBody Seller seller) {
        User user = userService.getByUserId(id);
        seller.setUser(user);
        seller.setRankSeller(RankSeller.BEGINNER);
        seller.setSellerNumber(getRandomNumberString());
        seller.setVerifySeller(Boolean.FALSE);
        seller.setTotalOrderFinish(0);
        user.setSeller(seller);
        user.setActivityType(activityTypeService.getByActivityType(UserActivityType.SELLER));
        user.setJoinSellingAt(new Date());
        return sellerService.saveSeller(seller);
    }

    @PostMapping("/forgot_password")
    public String processForgotPassword(HttpServletRequest request) {
        String email = request.getParameter("email");
        String token = RandomString.make(10);
        try{
            userService.updateResetPasswordToken(token, email);
            String resetPasswordLink = RequestUtility.getSiteURL(request) + "/reset_password?token=" + token;
            sendEmail(email, resetPasswordLink);
        } catch (ResourceNotFoundException ex) {
            return "User not found with email: " + email;
        } catch (UnsupportedEncodingException | MessagingException e) {
            return "Error while sending email";
        }
        return token;
    }
    public void sendEmail(String recipientEmail, String link) throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom("duc24600@gmail.com", "Jovinn support");
        helper.setTo(recipientEmail);
        String subject = "Here's the link to reset your password";

        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password.</p>"
                + "<p>Click the link below to change your password:</p>"
                + "<p><a href=\"" + link + "\">Change my password</a></p>"
                + "<br>"
                + "<p>Ignore this email if you do remember your password, "
                + "or you have not made the request.</p>";

        helper.setSubject(subject);

        helper.setText(content, true);

        mailSender.send(message);
    }
    @PostMapping("/reset_password")
    public String processResetPassword(@RequestBody ResetPasswordRequest request) {
        String token = request.getToken();
        String password = request.getPassword();
        User user = userService.getUserByResetPasswordToken(token);
        if (user == null) {
            return "Invalid token: " + token;
        } else {
            userService.updatePassword(user, password);
            return "You have succcessfully changed your password.";
        }
    }
}
