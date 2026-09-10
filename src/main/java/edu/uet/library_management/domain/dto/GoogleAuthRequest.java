package edu.uet.library_management.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleAuthRequest {
    @NotBlank(message = "Email is required")
    private String email;

    private String displayName;

    private String photoUrl;
}
