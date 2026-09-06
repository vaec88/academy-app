package com.academy.dto;

import com.academy.validation.groups.OnCreate;
import com.academy.validation.groups.OnUpdate;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
public class StudentDto {

    private String id;

    @NotBlank(groups = OnCreate.class, message = "first name is required")
    @Size(groups = {OnCreate.class, OnUpdate.class}, max = 60, message = "first name must not exceed 60 characters")
    private String firstName;

    @NotBlank(groups = OnCreate.class, message = "last name is required")
    @Size(groups = {OnCreate.class, OnUpdate.class}, max = 60, message = "last name must not exceed 60 characters")
    private String lastName;

    @NotBlank(groups = OnCreate.class, message = "dni is required")
    @Size(groups = {OnCreate.class, OnUpdate.class}, min = 8, max = 20, message = "dni must be between 8 and 20 characters")
    private String dni;

    @NotNull(groups = OnCreate.class, message = "age is required")
    @Min(groups = {OnCreate.class, OnUpdate.class}, value = 1, message = "age must be greater than 0")
    @Max(groups = {OnCreate.class, OnUpdate.class}, value = 120, message = "age must not exceed 120")
    private Integer age;

    @NotBlank(groups = OnCreate.class, message = "email is required")
    @Email(groups = {OnCreate.class, OnUpdate.class}, message = "email must be a valid address")
    @Size(groups = {OnCreate.class, OnUpdate.class}, max = 120, message = "email must not exceed 120 characters")
    private String email;

    @NotNull(groups = OnCreate.class, message = "status is required")
    private Boolean status;

    private LocalDateTime createdAt;

    private LocalDateTime modifiedAt;
}
