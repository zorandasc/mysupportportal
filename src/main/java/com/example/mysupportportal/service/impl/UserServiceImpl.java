package com.example.mysupportportal.service.impl;

import com.example.mysupportportal.domain.User;
import com.example.mysupportportal.domain.UserPrincipal;
import com.example.mysupportportal.enumeration.Role;
import com.example.mysupportportal.exception.domain.*;
import com.example.mysupportportal.repository.UserRepository;
import com.example.mysupportportal.service.EmailService;
import com.example.mysupportportal.service.LoginAttemptService;
import com.example.mysupportportal.service.UserService;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.mail.MessagingException;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.example.mysupportportal.constant.FileConstant.*;
import static com.example.mysupportportal.constant.UserServiceImplConstant.*;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.springframework.http.MediaType.*;

@Service
@Transactional
@Qualifier("userDetailsService")
public class UserServiceImpl implements UserService, UserDetailsService {



    private Logger LOGGER= LoggerFactory.getLogger(UserServiceImpl.class);
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;


    @Autowired
    public UserServiceImpl(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, LoginAttemptService loginAttemptService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.emailService = emailService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user=userRepository.findUserByUsername(username);

        if(user==null){
            LOGGER.error(NO_USER_FOUND_BY_USERNAME + username);
            throw new UsernameNotFoundException(NO_USER_FOUND_BY_USERNAME + username);
        }else{

            //BRUTE FORCE PASSWORD ATACK PROTECTION
            validateLoginAttempt(user);

            user.setLastLoginDateDisplay(user.getLastLoginDate());
            user.setLastLoginDate(new Date());

            userRepository.save(user);

            UserPrincipal userPrincipal=new UserPrincipal(user);
            LOGGER.info(RETURNING_FOUND_USER_BY_USERNAME + username);
            return userPrincipal;
        }

    }

    @Override
    public User register(String firstName, String lastName, String username, String email) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        //OVAJ METOD VRACE: null, User, ili exception
        // u slucaju novog korisnika vraca null
        validateNewUsernameAndEmail(StringUtils.EMPTY, username, email);
        User user=new User();
        user.setUserId(generateUserId());
        String password=generatePassword();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodePassword(password));
        user.setActive(true);
        user.setNotLocked(true);
        user.setRole(Role.ROLE_USER.name());
        user.setAuthorities(Role.ROLE_USER.getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        user.setImageNameSuffiks();
        LOGGER.info("New User password: " + password);
        emailService.sendNewPasswordEmail(username,password,email);
        userRepository.save(user);
        return user;
    }

    @Override
    public User addNewUser(String firstName, String lastName, String username, String email, String role,
                           boolean isNonLocked, boolean isActive, MultipartFile profileImage) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException, NotAnImageFileException, MessagingException {
        validateNewUsernameAndEmail(StringUtils.EMPTY, username, email);
        User user=new User();
        user.setUserId(generateUserId());
        String password=generatePassword();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);
        user.setJoinDate(new Date());
        user.setPassword(encodePassword(password));
        user.setActive(isActive);
        user.setNotLocked(isNonLocked);
        user.setRole(getRoleEnumName(role).name());
        user.setAuthorities(getRoleEnumName(role).getAuthorities());
        user.setProfileImageUrl(getTemporaryProfileImageUrl(username));
        user.setImageNameSuffiks();
        LOGGER.info("New User password: " + password);
        LOGGER.info("New User : " + user);
        //emailService.sendNewPasswordEmail(username,password,email);
        //userRepository.save(user);
        //OVO AKO KORISNIK DODAJE SVOJU IMAGE
        saveProfileImage(user, profileImage);
        return user;
    }


    @Override
    public User updateUser(String currentUsername, String newFirstName, String newLastName, String newUsername,
                           String newEmail, String role, boolean isNonLocked, boolean isActive,
                           MultipartFile profileImage) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException, NotAnImageFileException {
        User currentUser=validateNewUsernameAndEmail(currentUsername, newUsername, newEmail);
        currentUser.setFirstName(newFirstName);
        currentUser.setLastName(newLastName);
        currentUser.setUsername(newUsername);
        currentUser.setEmail(newEmail);
        currentUser.setActive(isActive);
        currentUser.setNotLocked(isNonLocked);
        currentUser.setRole(getRoleEnumName(role).name());
        currentUser.setAuthorities(getRoleEnumName(role).getAuthorities());
        userRepository.save(currentUser);
        //OVO AKO KORISNIK DODAJE SVOJU IMAGE
        saveProfileImage(currentUser, profileImage);
        return currentUser;
    }

    @Override
    public void deleteUser(String username) throws IOException {
        User user=userRepository.findUserByUsername(username);
        //delete user folder
        Path userFolder= Paths.get(USER_FOLDER +user.getUsername()).toAbsolutePath().normalize();
        FileUtils.deleteDirectory(new File(userFolder.toString()));
        userRepository.deleteById(user.getId());
    }

    @Override
    public List<User> getUsers() {
        return userRepository.findAll();
    }

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username);
    }

    @Override
    public User findUserByEmail(String email) {
        return userRepository.findUserByEmail(email);
    }

    @Override
    public void resetPassword(String email) throws EmailNotFoundException, MessagingException {
            User user=userRepository.findUserByEmail(email);
            if(user == null) {
                throw new EmailNotFoundException(NO_USER_FOUND_BY_EMAIL + email);
            }
            String password=generatePassword();
            user.setPassword(encodePassword(password));
            userRepository.save(user);
            emailService.sendNewPasswordEmail(user.getUsername(),password,user.getEmail());
    }

    @Override
    public User updateProfileImage(String username, MultipartFile profileImage) throws UserNotFoundException, EmailExistException, UsernameExistException, IOException, NotAnImageFileException {
        User user=validateNewUsernameAndEmail(username, null,null);
        saveProfileImage(user,profileImage);
        return user;
    }

    //REAL LOCATION WHERE THE IMAGE ARE SAVED ON PC IS:
    //user/home/supportportal/user/rick/rick.jpg
    private void saveProfileImage(User user, MultipartFile profileImage) throws IOException, NotAnImageFileException {
        if(profileImage !=null){
            if(!Arrays.asList(IMAGE_JPEG_VALUE,IMAGE_PNG_VALUE,IMAGE_GIF_VALUE).contains(profileImage.getContentType())){
                throw new NotAnImageFileException(profileImage.getOriginalFilename()+"is not image file.");
            }
            //IME FOLDER CE BITI NPR.:userFolder= user/home/supportportal/user/rick
            Path userFolder= Paths.get(USER_FOLDER +user.getUsername()).toAbsolutePath().normalize();
            if(!Files.exists(userFolder)){
                //AKO NE POSTOJI FOLDER NA TOJ PUTANJI KREIRAJ
                Files.createDirectories(userFolder);
                LOGGER.info(DIRECTORY_CREATED, userFolder);
            }


            //OBRISI STARI IMAGE U TOM FOLDERU AKO POSTOJI
            Files.deleteIfExists(Paths.get(userFolder+FORWARD_SLASH+user.getUsername()+user.getImageNameSuffiks()+DOT+JPG_EXTENSION));

            user.setImageNameSuffiks();//kreiraj novi time sufiks za novu sliku za tog usera

            //copy imagestream to user/home/supportportal/user/rick/rick+time.jpg
            Files.copy(profileImage.getInputStream(),
                    userFolder.resolve(user.getUsername()+user.getImageNameSuffiks()+DOT+JPG_EXTENSION),REPLACE_EXISTING);
            user.setProfileImageUrl(setProfileImageUrl(user.getUsername(), user.getImageNameSuffiks()));

            userRepository.save(user);

            LOGGER.info(FILE_SAVED_IN_FILE_SYSTEM,profileImage.getOriginalFilename());
        }
    }

    //setting api point of image url on user object
    //"http://localhost:8081/user/image/rick/rick.jpg"
    private String setProfileImageUrl(String username, long sufix) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(USER_IMAGE_PATH+username+FORWARD_SLASH+username+sufix+DOT+JPG_EXTENSION).build().toUriString();
    }

    //OD KORISNIKA DOBIJEMO NPR: "user"
    private Role getRoleEnumName(String role) {
        return Role.valueOf(role.toUpperCase());
    }

    //BRUTE FORCE PASSWORD ATACK PROTECTION
    //AuthenticationFailureListener ce hvatatai i povecavati feilure attaempts
    private void validateLoginAttempt(User user) {
        if(user.isNotLocked()){
            //AKO NIJE VEC ZAKLJUCAN
            if(loginAttemptService.hasExceededMaxAttempts(user.getUsername())){
                //ZAKLJUCAJ
                user.setNotLocked(false);
            }else{
                //OTKLJUCAJ
                user.setNotLocked(true);
            }

        }else{
            //AKO JE ACCOUNT ZAKLJUCAN IZBACI IZ KESA
            loginAttemptService.evictUserFromLoginAttemptCache(user.getUsername());
        }
    }

    //setting api point of image url on user object
    private String getTemporaryProfileImageUrl(String username) {
        //ServletUriComponentsBuilder.fromCurrentContextPath() +formira base url, http://localhost:8081
        //"http://localhost:8081/user/image/profile/rick"
        return ServletUriComponentsBuilder.fromCurrentContextPath().path(DEFAULT_USER_IMAGE_PATH+username).build().toUriString();
    }

    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generatePassword() {
        return RandomStringUtils.randomAlphabetic(10);
    }

    private String generateUserId() {
        return RandomStringUtils.randomNumeric(10);
    }

    //OVAJ METOD SE KRISTI ZA VALIDAIJU KOD REGISTRACIJE NOVOG KORISNIKA
    //ILI KOD UPDAJTA POSTOJECEG KORSNIKA
    // VRACE: null, User, ili exception
    // u slucaju novog korisnika vraca null
    private User validateNewUsernameAndEmail(String currentUsername, String newUsername, String newEmail) throws UserNotFoundException, UsernameExistException, EmailExistException {
        User userByNewUsername=findUserByUsername(newUsername);
        User userByNewEmail=findUserByEmail(newEmail);

        if(StringUtils.isNoneBlank(currentUsername)){
            //u pitanju postojeci korisnik koji mijenja username ili email
            User currentUser=findUserByUsername(currentUsername);
            if(currentUser==null){
                throw new UserNotFoundException(NO_USER_FOUND_BY_USERNAME + currentUsername);
            }

            if(userByNewUsername!=null && !currentUser.getId().equals(userByNewUsername.getId())){
                throw new UsernameExistException(USERNAME_ALREADY_EXIST);
            }

            if(userByNewEmail !=null && !currentUser.getId().equals(userByNewEmail.getId())){
                throw new EmailExistException(EMAIL_ALREADY_EXIST);
            }
            return currentUser;
        }else{
            //u pitanju novi korisnik

            if(userByNewUsername!=null){
                throw new UsernameExistException(USERNAME_ALREADY_EXIST);
            }

            if(userByNewEmail !=null){
                throw new EmailExistException(EMAIL_ALREADY_EXIST);
            }
            return null;

        }
    }

}
