package com.agustinbollati.cruddemo.dao;

import com.agustinbollati.cruddemo.entity.Student;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StudentDAOImpl implements StudentDAO {
	// Definir campo para el EntityManager
	private final EntityManager entityManager;

	@Autowired
	// Inyectar el EntityManager usando constructor injection
	public StudentDAOImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	// Implementar el método save() para guardar el estudiante en la base de datos
	@Override
	@Transactional
	public void save(Student student) {
		this.entityManager.persist(student);
	}

	@Override
	public Student findById(Integer id) {
		return this.entityManager.find(Student.class, id);
	}
}
