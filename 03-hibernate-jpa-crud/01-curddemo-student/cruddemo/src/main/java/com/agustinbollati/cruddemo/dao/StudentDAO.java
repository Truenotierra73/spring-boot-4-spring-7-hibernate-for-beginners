package com.agustinbollati.cruddemo.dao;

import com.agustinbollati.cruddemo.entity.Student;

public interface StudentDAO {
	public void save(Student student);

	Student findById(Integer id);
}
