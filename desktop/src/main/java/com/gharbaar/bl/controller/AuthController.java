package com.gharbaar.bl.controller;

import com.gharbaar.bl.BusinessException;
import com.gharbaar.bl.domain.User;
import com.gharbaar.bl.domain.UserFactory;
import com.gharbaar.bl.service.PasswordHasher;
import com.gharbaar.bl.service.Session;
import com.gharbaar.db.UserRepository;

import java.util.List;

/**
 * =====================================================================
 * [BL - CONTROLLER] AuthController   (GRASP Controller)
 * UC-01 Register, UC-02 Login, Logout
 * =====================================================================
 */
public class AuthController {

    private final UserRepository userRepo;
    private final PasswordHasher hasher;

    public AuthController(UserRepository userRepo, PasswordHasher hasher) {
        this.userRepo = userRepo;
        this.hasher = hasher;
    }

    // ---------------------------------------------------------------
    // UC-01 Register  (role = "CUSTOMER" or "AGENT")
    // ---------------------------------------------------------------
    public User register(String username, String password, String name, String phone, String role) {
        List<String> errors = User.validateRegistration(username, password, name, phone);
        if (!errors.isEmpty()) throw new BusinessException(errors);                    // ext. 4b
        if (userRepo.existsByUsername(username.trim()))                                // ext. 3a
            throw new BusinessException("Username already exists. Please choose another one.");
        User u;
        try {
            u = UserFactory.createNew(role, username.trim(), hasher.hash(password), name.trim(), phone.trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(e.getMessage());
        }
        userRepo.save(u);                                                              // step 4
        return u;
    }

    // ---------------------------------------------------------------
    // UC-02 Login -> returns the home view for the user's role
    // ---------------------------------------------------------------
    public String login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isEmpty())
            throw new BusinessException("Please enter your username and password.");
        User u = userRepo.findByUsername(username.trim());
        if (u == null || !u.verifyPassword(password, hasher))                           // ext. 4a
            throw new BusinessException("Invalid username or password. Please re-enter the details.");
        if (!u.isActive())
            throw new BusinessException("Your account has been suspended by the Admin.");
        Session.getInstance().setCurrentUser(u);
        return u.getHomeView();                                                        // step 5 (polymorphic)
    }

    public void logout() {
        Session.getInstance().clear();
    }
}
