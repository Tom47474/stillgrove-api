package com.stillgrove.stillgroveapi.account;

import com.stillgrove.stillgroveapi.auth.mapper.AuthMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {
    private final AccountMapper account;
    private final AuthMapper auth;
    private final PasswordEncoder encoder;
    public AccountService(AccountMapper account,AuthMapper auth,PasswordEncoder encoder) {
        this.account=account;this.auth=auth;this.encoder=encoder;
    }
    @Transactional public void delete(long userId,String password) {
        var user=auth.findActiveById(userId);
        if(user==null || !encoder.matches(password,user.getHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"密码错误");
        if(account.disableUser(userId)!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"账号已注销");
        for(long id:account.activeMemories(userId)) {
            if(account.eraseMemory(id,userId)==1) account.enqueueDelete(id);
        }
        account.eraseMessages(userId);
        account.eraseSummaries(userId);
        account.eraseConversations(userId);
        account.eraseFeedback(userId);
        account.eraseSafetyEvents(userId);
        account.eraseProfile(userId);
        account.eraseCodes(user.getEmail());
        account.eraseCredential(userId);
        account.recordDeletion(userId);
    }
}
