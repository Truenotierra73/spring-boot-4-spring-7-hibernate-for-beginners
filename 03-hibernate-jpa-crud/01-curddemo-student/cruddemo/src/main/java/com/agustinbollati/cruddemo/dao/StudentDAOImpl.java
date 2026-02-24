package com.agustinbollati.cruddemo.dao;

import com.agustinbollati.cruddemo.entity.Student;

import jakarta.persistence.EntityManager;

import jakarta.persistence.TypedQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public class StudentDAOImpl implements StudentDAO {
	// Definir campo para el EntityManager
	private final EntityManager entityManager;

	// Inyectar el EntityManager usando constructor injection
	@Autowired
	public StudentDAOImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	// Implementar el método save() para guardar el estudiante en la base de datos
	@Override
	@Transactional
	public void save(Student student) {
		this.entityManager.persist(student);
	}

	// Implementar el método findById() para leer un estudiante de la base de datos usando su id
	@Override
	public Student findById(Integer id) {
		return this.entityManager.find(Student.class, id);
	}

	// Implementar el método findAll() para leer todos los estudiantes de la base de datos
	@Override
	public List<Student> findAll() {
		TypedQuery<Student> query = this.entityManager.createQuery("SELECT s FROM Student s", Student.class);

		return query.getResultList();
	}

	// Implementar el método findAllOrderByLastName() para leer todos los estudiantes de la base de datos ordenados por apellido
	@Override
	public List<Student> findAllOrderByLastName() {
		TypedQuery<Student> query = this.entityManager.createQuery("SELECT s FROM Student s ORDER BY s.lastName ASC", Student.class);

		return query.getResultList();
	}
}
