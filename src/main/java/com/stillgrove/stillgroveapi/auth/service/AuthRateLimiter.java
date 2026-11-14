package com.stillgrove.stillgroveapi.auth.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;

@Component
public class AuthRateLimiter {
    private final StringRedisTemplate redis;
    public AuthRateLimiter(StringRedisTemplate redis) { this.redis=redis; }

    public void check(String action,String ip,int limit,Duration window) {
        String key="stillgrove:rate:"+action+":"+ip;
        Long count=redis.opsForValue().increment(key);
        if(count!=null && count==1) redis.expire(key,window);
        if(count!=null && count>limit) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"请求过于频繁，请稍后再试");
    }
}
