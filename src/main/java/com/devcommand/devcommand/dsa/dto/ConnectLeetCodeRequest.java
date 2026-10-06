package com.devcommand.devcommand.dsa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConnectLeetCodeRequest {
    @NotBlank
    @Size(max = 255)
    private String username;
}
