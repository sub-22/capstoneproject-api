package com.jovinn.capstoneproject.service.impl;

import com.jovinn.capstoneproject.dto.UserProfile;
import com.jovinn.capstoneproject.dto.UserSummary;

import com.jovinn.capstoneproject.dto.adminsite.adminrequest.AdminLoginRequest;
import com.jovinn.capstoneproject.dto.adminsite.adminresponse.AdminViewUserResponse;

import com.jovinn.capstoneproject.dto.adminsite.adminresponse.CountUserResponse;
import com.jovinn.capstoneproject.dto.client.request.*;
import com.jovinn.capstoneproject.dto.client.response.ApiResponse;
import com.jovinn.capstoneproject.dto.client.response.JwtAuthenticationResponse;
import com.jovinn.capstoneproject.enumerable.AuthTypeUser;
import com.jovinn.capstoneproject.enumerable.UserActivityType;
import com.jovinn.capstoneproject.exception.ApiException;
import com.jovinn.capstoneproject.exception.JovinnException;
import com.jovinn.capstoneproject.exception.ResourceNotFoundException;
import com.jovinn.capstoneproject.exception.UnauthorizedException;
import com.jovinn.capstoneproject.model.*;
import com.jovinn.capstoneproject.repository.UserRepository;
import com.jovinn.capstoneproject.repository.payment.WalletRepository;
import com.jovinn.capstoneproject.security.JwtTokenProvider;
import com.jovinn.capstoneproject.security.UserPrincipal;
import com.jovinn.capstoneproject.service.ActivityTypeService;
import com.jovinn.capstoneproject.service.SellerService;
import com.jovinn.capstoneproject.service.UserService;
import com.jovinn.capstoneproject.util.EmailSender;
import com.jovinn.capstoneproject.util.WebConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.utility.RandomString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.*;

import static com.jovinn.capstoneproject.util.GenerateRandom.getRandomNumberString;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityTypeService activityTypeService;
    private final WalletRepository walletRepository;
    private final EmailSender emailSender;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    @Autowired
    private SellerService sellerService;
    @Override
    public UserSummary getCurrentUser(UserPrincipal currentUser) {
        return new UserSummary(currentUser.getId(), currentUser.getUsername(), currentUser.getFirstName(),
                currentUser.getLastName());
    }

    @Override
    public User saveUser(User user) {
        log.info("Saving new user {} {} to the database",user.getFirstName(),user.getLastName());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Override
    public List<User> getUsers() {
        log.info("Fetching all users");
        return userRepository.findAll();
    }

    @Override
    public UserProfile getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "Username not found", username));

        return new UserProfile(user.getId(), user.getFirstName(), user.getLastName(), user.getUsername(),
                user.getEmail(), user.getPhoneNumber(), user.getGender(), user.getBirthDate(),
                user.getCity(), user.getCountry(), user.getAvatar());
    }

    @Override
    public ApiResponse update(UUID id, UserChangeProfileRequest request, UserPrincipal currentUser) {
        User existUser = userRepository.findById(id)
                .orElseThrow(() -> new JovinnException(HttpStatus.BAD_REQUEST, "Không tìm thấy thông tin người dùng"));
        if (existUser.getId().equals(currentUser.getId())) {
            existUser.setFirstName(request.getFirstName());
            existUser.setLastName(request.getLastName());
            if(request.getPhoneNumber() != null && !request.getPhoneNumber().equals(existUser.getPhoneNumber())) {
                User existPhoneNumber = userRepository.findUserByPhoneNumber(request.getPhoneNumber());
                if(existPhoneNumber != null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "Số điện thoại đã được sử dụng. vui lòng nhập số khác");
                } else {
                    existUser.setPhoneNumber(request.getPhoneNumber());
                }
            }
            existUser.setGender(request.getGender());
            existUser.setBirthDate(request.getBirthDate());
            existUser.setCity(request.getCity());
            existUser.setCountry(request.getCountry());
            existUser.setAvatar(request.getAvatar());
            userRepository.save(existUser);
            return new ApiResponse(Boolean.TRUE, "Cập nhật thông tin thành công");
        }

        ApiResponse apiResponse = new ApiResponse(Boolean.FALSE, "You don't have permission to update profile");
        throw new UnauthorizedException(apiResponse);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public ApiResponse resetPassword(ResetPasswordRequest request) {
        String token = request.getToken();
        String password = request.getPassword();
        User user = userRepository.findByResetPasswordToken(token);
        if (user == null) {
            return new ApiResponse(Boolean.FALSE,"Token đã hết hạn: " + token);
        } else {
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            String encodedPassword = passwordEncoder.encode(password);
            user.setResetPasswordToken(null);
            user.setPassword(encodedPassword);
            user.setResetPasswordToken(null);
            userRepository.save(user);
            return new ApiResponse(Boolean.TRUE,
            "Bạn đã đổi mật khẩu thành công");
        }
    }

    @Override
    public void updatePassword(User user, String newPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setResetPasswordToken(null);
        user.setPassword(encodedPassword);
        userRepository.save(user);
    }

    @Override
    public void updateResetPasswordToken(String token, String email) throws ResourceNotFoundException {
        User user = userRepository.findUserByEmail(email)
                .orElseThrow(() ->
                        new ApiException(HttpStatus.BAD_REQUEST, "Email không tồn tại"));

        if (user != null){
            user.setResetPasswordToken(token);
            userRepository.save(user);
        } else {
            throw new ApiException(HttpStatus.BAD_REQUEST, "email");
        }
    }

    @Override
    public User getByUserId(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Not found user by id"));
    }

    @Override
    public ApiResponse registerUser(SignUpRequest signUpRequest) {
        if (Boolean.TRUE.equals(userRepository.existsByUsername(signUpRequest.getUsername()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tên đăng nhập đã tồn tại, vui lòng nhập lại");
        }

        if (Boolean.TRUE.equals(userRepository.existsByEmail(signUpRequest.getEmail()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email đã được đăng kí sử dụng, vui lòng sử dụng email khác");
        }
        String verificationCode = RandomString.make(15);
        User user = new User();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setUsername(signUpRequest.getUsername().toLowerCase());
        user.setEmail(signUpRequest.getEmail().toLowerCase());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setJoinedAt(new Date());
        user.setVerificationCode(verificationCode);
        user.setIsEnabled(Boolean.FALSE);
        user.setAuthType(AuthTypeUser.LOCAL);
        user.setActivityType(activityTypeService.getByActivityType(UserActivityType.BUYER));

        Buyer buyer = new Buyer();
        buyer.setUser(user);
        buyer.setBuyerNumber(getRandomNumberString());
        buyer.setSuccessContract(0);
        user.setBuyer(buyer);

        String link = WebConstant.DOMAIN + "/auth/verifyAccount/" + verificationCode;
        try {
            emailSender.sendEmailVerify(signUpRequest.getEmail(), link);
        } catch (UnsupportedEncodingException | MessagingException exception){
            return null;
        }
        userRepository.save(user);
        return new ApiResponse(Boolean.TRUE, "Liên kết xác thực đã được gửi vào hòm thư của bạn, vui lòng xác nhận");
    }

    @Override
    public JwtAuthenticationResponse loginUser(LoginRequest loginRequest) {
        try {
            User user = getUserByUsernameOrEmail(loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail());
            if(!Objects.equals(activityTypeService.getActivityTypeByUserId(user.getId()), UserActivityType.ADMIN)
                    && activityTypeService.getActivityTypeByUserId(user.getId()) != null) {
                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(loginRequest.getUsernameOrEmail(), loginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                String jwt = jwtTokenProvider.generateToken(authentication);
                return new JwtAuthenticationResponse(jwt);
            } else {
                throw new JovinnException(HttpStatus.BAD_REQUEST, "Bạn không có quyền");
            }
        } catch (BadCredentialsException e) {
            throw new JovinnException(HttpStatus.BAD_REQUEST, "Tài khoản/email hoặc password không đúng");
        }
    }

    @Override
    public JwtAuthenticationResponse loginAdmin(AdminLoginRequest adminLoginRequest) {
        try {
            User user = getUserByUsernameOrEmail(adminLoginRequest.getAdminAccount(), adminLoginRequest.getAdminAccount());
            if (Objects.equals(activityTypeService.getActivityTypeByUserId(user.getId()), UserActivityType.ADMIN)
                    && activityTypeService.getActivityTypeByUserId(user.getId()) != null){

                Authentication authentication = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(adminLoginRequest.getAdminAccount(), adminLoginRequest.getPassword()));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                String jwt = jwtTokenProvider.generateToken(authentication);
                return new JwtAuthenticationResponse(jwt);
            } else {
                throw new BadCredentialsException("Bạn không có quyền");
            }
        } catch (BadCredentialsException e) {
            throw new JovinnException(HttpStatus.BAD_REQUEST, "Tài khoản/email hoặc password không đúng");
        }
    }

    @Override
    public User verifyRegistration(String verificationCode) throws ApiException {
        User user = userRepository.findUserByVerificationCode(verificationCode);
        if (user == null){
            throw new ApiException(HttpStatus.BAD_REQUEST, "Verification code not found");
        }
        user.setIsEnabled(Boolean.TRUE);
        user.setVerificationCode(null);
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setWithdraw(new BigDecimal(50));
        wallet.setIncome(new BigDecimal(0));
        user.setWallet(wallet);
        walletRepository.save(wallet);
        return userRepository.save(user);
    }

    @Override
    public ApiResponse changePassword(ChangePasswordRequest request, UserPrincipal currentUser) {
        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Not found user"));
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String recentPass = user.getPassword();
        String oldPass = request.getOldPass();
        String newPass = request.getNewPass();
        String rePass = request.getRePass();

        if(user.getId().equals(currentUser.getId())) {
            if (BCrypt.checkpw(oldPass, recentPass)) {
                if (newPass.equals(oldPass)) {
                    return new ApiResponse(Boolean.TRUE, "Mật khẩu trùng với mật khẩu cũ");
                } else {
                    if (newPass.equals(rePass)) {
                        user.setPassword(passwordEncoder.encode(newPass));
                        userRepository.save(user);
                        return new ApiResponse(Boolean.TRUE, "Bạn đã đổi mật khẩu thành công");
                    } else {
                        return new ApiResponse(Boolean.TRUE, "Repass phải trùng với newPass");
                    }
                }
            } else {
                throw new JovinnException(HttpStatus.BAD_REQUEST, "Mật khẩu hiện tại không chính xác");
            }
        }

        ApiResponse apiResponse = new ApiResponse(Boolean.FALSE, "You don't have permission");
        throw new UnauthorizedException(apiResponse);
    }

    @Override
    public List<UserProfile> getListUserInvitedByPostRequestId(UUID postRequest) {
        List<Seller> invitedSeller = sellerService.getListSellerBuyPostRequestId(postRequest);
        List<UserProfile> userProfiles = new ArrayList<>();
        for (Seller seller:invitedSeller){
            userProfiles.add(new UserProfile(seller.getUser().getId(),seller.getUser().getFirstName(),seller.getUser().getLastName(),seller.getUser().getUsername(),
                    seller.getUser().getEmail(),seller.getUser().getPhoneNumber(),seller.getUser().getGender(),seller.getUser().getBirthDate(),seller.getUser().getCity(),
                    seller.getUser().getCountry(),seller.getUser().getAvatar()));
        }
        return userProfiles;
    }

    @Override
    public CountUserResponse countUserById() {
        return new CountUserResponse(userRepository.count());
    }

    @Override
    public ApiResponse banOrUnbanUser(UUID userId) {
        User user = userRepository.findUserById(userId);
        if (user.getIsEnabled() == true){
            user.setIsEnabled(false);
        }else{
            user.setIsEnabled(true);
        }
        userRepository.save(user);
        return new ApiResponse(Boolean.TRUE, "Thay Đổi Trạng Thái Thành Công");
    }

    @Override
    public AdminViewUserResponse getUserById(UUID id) {
        User user = userRepository.findUserById(id);
        return new AdminViewUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getPhoneNumber(), user.getUsername(), user.getIsEnabled());
    }

    @Override
    public User getUserByUserName(String name) {
        return userRepository.findByUsername(name)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Người dùng không khả dụng"));
    }

    @Override
    public User getUserByUsernameOrEmail(String username, String email) {
        return userRepository.findByUsernameOrEmail(username, email)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Người dùng không khả dụng"));
    }
}
