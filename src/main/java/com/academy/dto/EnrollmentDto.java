package com.academy.dto;

import com.academy.validation.groups.OnCreate;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnrollmentDto {

    private String id;

    @NotNull(groups = OnCreate.class, message = "enrollment date is required")
    private LocalDateTime enrollmentDate;

    @Valid
    @NotNull(groups = OnCreate.class, message = "student is required")
    private StudentDto student;

    @Valid
    @NotEmpty(groups = OnCreate.class, message = "at least one course is required")
    private List<@Valid CourseDto> courses;
}
