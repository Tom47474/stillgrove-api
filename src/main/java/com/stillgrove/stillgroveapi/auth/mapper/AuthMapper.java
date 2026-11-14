package com.stillgrove.stillgroveapi.auth.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AuthMapper {
    void insertUser(NewUser user);
    void insertCredential(@Param("userId") long userId, @Param("email") String email,
                          @Param("passwordHash") String passwordHash);
    void insertProfile(@Param("userId") long userId);
    void insertSettings(@Param("userId") long userId);
    AuthAccountRow findByEmail(@Param("email") String email);
    AuthAccountRow findActiveById(@Param("userId") long userId);
    void recordLoginFailure(@Param("userId") long userId);
    void recordLoginSuccess(@Param("userId") long userId);
    void recordLastLogin(@Param("userId") long userId);
}
