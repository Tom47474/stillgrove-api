package com.stillgrove.stillgroveapi.chat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/conversations")
public class ChatController {
    private final ChatService service;
    public ChatController(ChatService service) { this.service=service; }
    private long user(Authentication auth) { return Long.parseLong(auth.getName()); }
    public record Rename(@NotBlank @Size(max=120) String title) {}
    public record Send(@NotBlank @Size(max=8000) String content,
                       @NotBlank @Pattern(regexp="[0-9a-fA-F-]{36}") String requestId) {}

    @GetMapping public List<Map<String,Object>> list(Authentication auth) { return service.list(user(auth)); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(Authentication auth) { return service.create(user(auth)); }
    @GetMapping("/{id}") public Map<String,Object> get(Authentication auth,@PathVariable long id) { return service.get(user(auth),id); }
    @PatchMapping("/{id}") public Map<String,Object> rename(Authentication auth,@PathVariable long id,@Valid @RequestBody Rename body) { return service.rename(user(auth),id,body.title()); }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Authentication auth,@PathVariable long id) { service.delete(user(auth),id); }
    @GetMapping("/{id}/messages") public List<Map<String,Object>> messages(Authentication auth,@PathVariable long id,@RequestParam(defaultValue="2147483647") int before) { return service.messages(user(auth),id,before); }

    @PostMapping(path="/{id}/messages/stream",produces="text/event-stream")
    public SseEmitter send(Authentication auth,@PathVariable long id,@Valid @RequestBody Send body) {
        if(!service.aiAvailable()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"未配置 DASHSCOPE_API_KEY");
        try { UUID.fromString(body.requestId()); }
        catch(IllegalArgumentException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"requestId 格式错误"); }
        var pending=service.prepare(user(auth),id,body.content().trim(),body.requestId());
        var emitter=new SseEmitter(120_000L);
        CompletableFuture.runAsync(()->{
            var full=new StringBuilder();
            try {
                service.stream(pending,chunk->{
                    if(full.length()+chunk.length()>20000) throw new IllegalStateException("回复过长");
                    full.append(chunk);
                    try { emitter.send(SseEmitter.event().name("delta").data(Map.of("text",chunk))); }
                    catch(Exception ex) { throw new RuntimeException(ex); }
                });
                service.finish(pending,full.toString(),null);
                emitter.send(SseEmitter.event().name("done").data(Map.of("messageId",pending.assistantMessageId())));
                emitter.complete();
            } catch(Exception ex) {
                service.finish(pending,full.toString(),"AI_STREAM_ERROR");
                try { emitter.send(SseEmitter.event().name("error").data("生成失败，请稍后重试")); } catch(Exception ignored) {}
                emitter.complete();
            }
        });
        return emitter;
    }
}
