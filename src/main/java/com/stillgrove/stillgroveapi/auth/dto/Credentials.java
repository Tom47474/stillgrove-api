package com.stillgrove.stillgroveapi.auth.dto;
import jakarta.validation.constraints.*;
public record Credentials(@NotBlank @Email String email, @NotBlank @Size(min=8,max=72) String password) {}
