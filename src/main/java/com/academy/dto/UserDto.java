package com.academy.dto;

import com.academy.model.AssignedRole;
import com.academy.validation.groups.OnCreate;
import com.academy.validation.groups.OnUpdate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {

    private String id;

    @NotBlank(groups = OnCreate.class, message = "username is required")
    @Size(groups = {OnCreate.class, OnUpdate.class}, max = 50, message = "username must not exceed 50 characters")
    private String username;

    @NotBlank(groups = OnCreate.class, message = "password is required")
    @Size(groups = {OnCreate.class, OnUpdate.class}, min = 8, max = 100, message = "password must be between 8 and 100 characters")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @NotNull(groups = OnCreate.class, message = "status is required")
    private Boolean status;

    @Valid
    @NotEmpty(groups = OnCreate.class, message = "at least one role is required")
    private List<@Valid AssignedRole> roles;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;
}
