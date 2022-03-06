package com.example.mysupportportal.resource;

import com.example.mysupportportal.domain.HttpResponse;
import com.example.mysupportportal.domain.User;
import com.example.mysupportportal.domain.UserPrincipal;
import com.example.mysupportportal.exception.ExceptionHandling;
import com.example.mysupportportal.exception.domain.*;
import com.example.mysupportportal.service.UserService;
import com.example.mysupportportal.utility.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

import static com.example.mysupportportal.constant.FileConstant.*;
import static com.example.mysupportportal.constant.SecurityConstant.JWT_TOKEN_HEADER;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.IMAGE_JPEG_VALUE;

//U PRODUKCIJI BI JOS TREBAL VALIDACIJA USER INPUTA
//PAGINACIJA
@RestController
@RequestMapping(path = {"/","/user"})//"/" to nam treba da bi mogli hendlovato /error
public class UserResource extends ExceptionHandling {

    public static final String EMAIL_SENT = "A email with a new password was sent to: ";
    public static final String USER_DELETED_SUCCESSFULLY = "User deleted successfully.";
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private  final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public UserResource(
            UserService userService,
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody User user ) throws UserNotFoundException, EmailExistException, UsernameExistException, MessagingException {
        User newUser=userService.register(user.getFirstName(), user.getLastName(),user.getUsername(), user.getEmail());
        return new ResponseEntity<>(newUser, OK);
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody User user )  {
        //ovo ce generistai exception ako je autentifikacija incorect
        authenticate(user.getUsername(), user.getPassword());
        User loginUser=userService.findUserByUsername(user.getUsername());
        UserPrincipal userPrincipal=new UserPrincipal(loginUser);
        HttpHeaders jwtHeader=getJwtHeader(userPrincipal);
        return new ResponseEntity<>(loginUser,jwtHeader, OK);

    }

    @PostMapping("/add")
    public ResponseEntity<User> addNewUser(@RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("username") String username,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isNonLocked") String isNonLocked,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam(value="profileImage", required = false) MultipartFile profileImage)
            throws UserNotFoundException, EmailExistException, IOException, UsernameExistException, NotAnImageFileException, MessagingException {
        User newUser=userService.addNewUser(firstName, lastName, username,email, role,
                Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive), profileImage);
        return new ResponseEntity<>(newUser, HttpStatus.OK);
    }

    @PutMapping("/update")
    public ResponseEntity<User> update(@RequestParam("currentUsername") String currentUsername,
                                           @RequestParam("firstName") String firstName,
                                           @RequestParam("lastName") String lastName,
                                           @RequestParam("username") String username,
                                           @RequestParam("email") String email,
                                           @RequestParam("role") String role,
                                           @RequestParam("isNonLocked") String isNonLocked,
                                           @RequestParam("isActive") String isActive,
                                           @RequestParam(value="profileImage", required = false) MultipartFile profileImage)
            throws UserNotFoundException, EmailExistException, IOException, UsernameExistException, NotAnImageFileException {
        User updatedUser=userService.updateUser(currentUsername,firstName,lastName,username,email, role,
                Boolean.parseBoolean(isNonLocked), Boolean.parseBoolean(isActive),profileImage);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @GetMapping("/find/{username}")
    public ResponseEntity<User> getUser(@PathVariable("username") String username){
        User user=userService.findUserByUsername(username);
        return  new ResponseEntity<>(user, OK);
    }

    @GetMapping("/list")
    public ResponseEntity<List<User>> getAllUsers(){
        List<User> users=userService.getUsers();
        return  new ResponseEntity<>(users, OK);
    }

    @GetMapping("/resetPassword/{email}")
    public ResponseEntity<HttpResponse> resetPassword(@PathVariable("email") String email) throws EmailNotFoundException, MessagingException {
        userService.resetPassword(email);
        return response(HttpStatus.OK, EMAIL_SENT + email);
    }

    @DeleteMapping("/delete/{username}")
    @PreAuthorize("hasAnyAuthority('user:delete')")
    public ResponseEntity<HttpResponse> deleteUser(@PathVariable("username") String username) throws IOException {
        userService.deleteUser(username);
        return response(HttpStatus.OK, USER_DELETED_SUCCESSFULLY);
    }

    @PostMapping("/updateProfileImage")
    public ResponseEntity<User> update(@RequestParam("username") String username,
                                       @RequestParam("profileImage") MultipartFile profileImage) throws UserNotFoundException, EmailExistException, IOException, UsernameExistException, NotAnImageFileException {
        User user=userService.updateProfileImage(username, profileImage);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }


    //Ovaj request ce doci od browsera kad parsira user objekat
    //jer ce u nemu biti ova putanja za sliku
    //"profileImageUrl": "http://localhost:8081/user/image/rick/rick.jpg",
    //Ovaj api je kad je korisnik uplovdoavo svoju sliku
    @GetMapping(path = "/image/{username}/{fileName}", produces =IMAGE_JPEG_VALUE )
    public byte[] getProfileImage(@PathVariable("username") String username, @PathVariable("fileName") String fileName) throws IOException {
        //citaj bajtove iz "user.home"+ "/supportportal/user/rick/rick.jpg"
        return Files.readAllBytes(Paths.get(USER_FOLDER+username+FORWARD_SLASH+fileName));
    }

    //ovaj api je za robohashe sajt
    //"profileImageUrl": "http://localhost:8081/user/image/profile/rick",
    @GetMapping(path = "/image/profile/{username}", produces =IMAGE_JPEG_VALUE )
    public byte[] getTempProfileImage(@PathVariable("username") String username) throws IOException {
        URL url=new URL(TEMP_PROFILE_IMAGE_BASE_URL+ username);
        ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();

        //otvori coneciju prema sajtu "https://robohash.org/" i citaj bajtove
        // pisi u otputstream
        //i na kraju konvertuju u bytearray
        try(InputStream inputStream=url.openStream()) {
            int bytesRead;
            byte[] chunk=new byte[1024];
            while ((bytesRead=inputStream.read(chunk))>0){
                byteArrayOutputStream.write(chunk,0,bytesRead);
            }
        }
        return byteArrayOutputStream.toByteArray();

    }

    private ResponseEntity<HttpResponse> response(HttpStatus httpStatus, String message) {
        HttpResponse body=new HttpResponse(httpStatus.value(), httpStatus,httpStatus.getReasonPhrase().toUpperCase(),
                message.toUpperCase());
        return new ResponseEntity<>(body, httpStatus);
    }

    //pohrani korisnik u spring context
    //ovo ce generistai exception ako je autentifikacija incorect
    private void authenticate(String username, String password) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(username,password));
    }


    private HttpHeaders getJwtHeader(UserPrincipal userPrincipal) {
        HttpHeaders headers=new HttpHeaders();
        headers.add(JWT_TOKEN_HEADER,jwtTokenProvider.generateJwtToken(userPrincipal));
        return headers;
    }
}
