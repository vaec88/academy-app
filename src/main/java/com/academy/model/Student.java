package com.academy.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Document(collection = "students")
public class Student {

    @Id
    @EqualsAndHashCode.Include
    private String id;

    @Field("first_name")
    private String firstName;

    @Field("last_name")
    private String lastName;

    @Field("dni")
    private String dni;

    @Field("age")
    private Integer age;

    @Field("email")
    private String email;

    @Field("status")
    private Boolean status;

    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("modified_at")
    private LocalDateTime modifiedAt;
}
