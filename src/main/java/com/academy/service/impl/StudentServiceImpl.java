package com.academy.service.impl;

import org.springframework.stereotype.Service;

import com.academy.model.Student;
import com.academy.repository.IGenericRepository;
import com.academy.repository.IStudentRepository;
import com.academy.service.IStudentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StudentServiceImpl extends CrudServiceImpl<Student, String> implements IStudentService {

    private final IStudentRepository studentRepository;

    @Override
    protected IGenericRepository<Student, String> getRepository() {
        return studentRepository;
    }
}
