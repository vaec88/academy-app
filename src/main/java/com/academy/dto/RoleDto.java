package com.academy.dto;

import com.academy.validation.groups.OnCreate;
import com.academy.validation.groups.OnUpdate;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleDto {

    private String id;

    @NotBlank(groups = OnCreate.class, message = "name is required")
    @Size(groups = {OnCreate.class, OnUpdate.class}, max = 50, message = "name must not exceed 50 characters")
    private String name;

    @NotNull(groups = OnCreate.class, message = "status is required")
    private Boolean status;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;
}
