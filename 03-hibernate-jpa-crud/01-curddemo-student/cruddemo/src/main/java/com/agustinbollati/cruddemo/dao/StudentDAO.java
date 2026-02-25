package com.agustinbollati.cruddemo.dao;

import com.agustinbollati.cruddemo.entity.Student;

import java.util.List;

public interface StudentDAO {
	void save(Student student);

	Student findById(Integer id);

	List<Student> findAll();

	List<Student> findAllOrderByLastName();

	List<Student> findByLastName(String lastName);

	void update(Student student);
}
