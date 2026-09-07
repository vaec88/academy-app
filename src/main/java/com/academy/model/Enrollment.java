package com.academy.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(collection = "enrollments")
public class Enrollment {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Field
    private LocalDateTime enrollmentDate;

    @Field
    private Student student;

    @Field
    private List<Course> courses;
}
