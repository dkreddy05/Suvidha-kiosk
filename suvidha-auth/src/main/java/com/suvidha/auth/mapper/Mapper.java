package com.suvidha.auth.mapper;

import com.suvidha.auth.Dto.UserAuthDto;
import com.suvidha.auth.model.UsersAuth;

import java.time.Instant;

public class Mapper {

    public static UserAuthDto toDto(UsersAuth userAuth) {
        if (userAuth == null) {
            return null;
        }
        UserAuthDto userAuthDto = new UserAuthDto();
        userAuthDto.setId(userAuth.getId());
        userAuthDto.setMobile(userAuth.getMobile());
        userAuthDto.setAadhar(userAuth.getAadhar());
        userAuthDto.setName(userAuth.getName());
        userAuthDto.setLanguagePreference(userAuth.getLanguagePreference());
        return userAuthDto;
    }

    public static UsersAuth toEntity(UserAuthDto dto) {
        if (dto == null) {
            return null;
        }

        UsersAuth usersAuth = new UsersAuth();
        usersAuth.setMobile(dto.getMobile());
        usersAuth.setAadhar(dto.getAadhar());
        usersAuth.setName(dto.getName());
        usersAuth.setLanguagePreference(dto.getLanguagePreference());
        usersAuth.setCreatedAt(Instant.now());
        return usersAuth;
    }
}