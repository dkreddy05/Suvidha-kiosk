package com.suvidha.auth.service;

import com.suvidha.auth.Dto.RegisterRequest;
import com.suvidha.auth.Dto.UserAuthDto;

public interface UserService {

    UserAuthDto registerUser(RegisterRequest request);
}