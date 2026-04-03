package com.suvidha.auth.service;

import com.suvidha.auth.Dto.RegisterRequest;
import com.suvidha.auth.Dto.UserAuthDto;

public interface UserService {
    /**
     * STEP 3: REGISTER USER
     * Validates fields, checks for duplicate user, checks session is VERIFIED,
     * saves user to DB, and returns user details.
     */
    UserAuthDto registerUser(RegisterRequest request);
}