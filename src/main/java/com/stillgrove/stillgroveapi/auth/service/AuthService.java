package com.stillgrove.stillgroveapi.auth.service;

import com.stillgrove.stillgroveapi.auth.dto.UserView;
import com.stillgrove.stillgroveapi.auth.mapper.AuthAccountRow;
import com.stillgrove.stillgroveapi.auth.mapper.AuthMapper;
import com.stillgrove.stillgroveapi.auth.mapper.NewUser;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {
    private final AuthMapper mapper;
    private final PasswordEncoder encoder;

    public AuthService(AuthMapper mapper, PasswordEncoder encoder) {
        this.mapper = mapper;
        this.encoder = encoder;
    }

    @Transactional
    public UserView register(String rawEmail, String password) {
        String email = normalize(rawEmail);
        try {
            NewUser user = new NewUser();
            mapper.insertUser(user);
            long id = user.getId();
            mapper.insertCredential(id, email, encoder.encode(password));
            mapper.insertProfile(id);
            mapper.insertSettings(id);
            return new UserView(id, email);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "邮箱已注册");
        }
    }

    public UserView login(String rawEmail, String password) {
        var row = mapper.findByEmail(normalize(rawEmail));
        if (row == null || !"ACTIVE".equals(row.getStatus()) || isLocked(row)) throw unauthorized();

        if (!encoder.matches(password, row.getHash())) {
            mapper.recordLoginFailure(row.getId());
            throw unauthorized();
        }

        mapper.recordLoginSuccess(row.getId());
        mapper.recordLastLogin(row.getId());
        return new UserView(row.getId(), row.getEmail());
    }

    public UserView currentUser(long id) {
        var row = mapper.findActiveById(id);
        if (row == null) throw unauthorized();
        return new UserView(row.getId(), row.getEmail());
    }

    private boolean isLocked(AuthAccountRow row) {
        return row.getLockedUntil() != null && row.getLockedUntil().after(new java.util.Date());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "邮箱或密码错误");
    }
}
