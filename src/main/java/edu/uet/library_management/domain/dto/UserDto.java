package edu.uet.library_management.domain.dto;

import edu.uet.library_management.domain.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String displayName;
    private String birthday;
    private String phoneNumber;
    private String photoUrl;
    private Role role;
    private LocalDateTime createdAt;
}
