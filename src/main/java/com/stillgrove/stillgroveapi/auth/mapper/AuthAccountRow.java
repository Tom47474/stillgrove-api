package com.stillgrove.stillgroveapi.auth.mapper;

import java.sql.Timestamp;

public class AuthAccountRow {
    private Long id;
    private String email;
    private String hash;
    private String status;
    private Timestamp lockedUntil;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Timestamp getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Timestamp lockedUntil) { this.lockedUntil = lockedUntil; }
}
