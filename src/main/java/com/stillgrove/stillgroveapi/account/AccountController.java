package com.stillgrove.stillgroveapi.account;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {
    private final AccountService service;
    public AccountController(AccountService service) { this.service=service; }
    public record DeleteRequest(@NotBlank String password) {}
    private long user(Authentication auth) { return Long.parseLong(auth.getName()); }
    @DeleteMapping @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(Authentication auth,@Valid @RequestBody DeleteRequest body,HttpServletRequest request) {
        service.delete(user(auth),body.password());
        SecurityContextHolder.clearContext();
        var session=request.getSession(false);
        if(session!=null) session.invalidate();
    }
}
