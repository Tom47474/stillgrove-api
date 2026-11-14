package com.stillgrove.stillgroveapi.feedback;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/messages")
public class FeedbackController {
    private final FeedbackMapper mapper;
    public FeedbackController(FeedbackMapper mapper) { this.mapper=mapper; }
    public record Feedback(@NotNull Rating rating) {}
    public enum Rating { HELPFUL, NOT_HELPFUL }

    @PutMapping("/{messageId}/feedback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(Authentication authentication,@PathVariable long messageId,@Valid @RequestBody Feedback body) {
        long userId=Long.parseLong(authentication.getName());
        if(mapper.save(userId,messageId,body.rating().name(),null)==0)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"消息不存在");
    }
}
