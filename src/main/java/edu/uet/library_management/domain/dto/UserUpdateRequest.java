package edu.uet.library_management.domain.dto;

import edu.uet.library_management.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    private String displayName;
    private String birthday;
    private String phoneNumber;
    private String photoUrl;
    private Role role;
    private boolean disabled;
    private String password; // optional password reset
}
