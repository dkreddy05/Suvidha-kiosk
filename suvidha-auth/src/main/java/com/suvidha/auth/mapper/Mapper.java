package com.suvidha.auth.mapper;

import com.suvidha.auth.Dto.UserAuthDto;
import com.suvidha.auth.model.Citizen;

import java.time.Instant;

public class Mapper {

    public static UserAuthDto toDto(Citizen userAuth) {
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

    public static Citizen toEntity(UserAuthDto dto) {
        if (dto == null) {
            return null;
        }

        Citizen usersAuth = new Citizen();
        usersAuth.setMobile(dto.getMobile());
        usersAuth.setAadhar(dto.getAadhar());
        usersAuth.setName(dto.getName());
        usersAuth.setLanguagePreference(dto.getLanguagePreference());
        usersAuth.setCreatedAt(Instant.now());
        return usersAuth;
    }
}
