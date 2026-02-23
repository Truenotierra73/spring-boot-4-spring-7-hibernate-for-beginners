package com.agustinbollati.cruddemo;

import com.agustinbollati.cruddemo.dao.StudentDAO;
import com.agustinbollati.cruddemo.entity.Student;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CruddemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CruddemoApplication.class, args);
	}

	// Será ejecutado después de que el contexto de la aplicación se haya cargado
	// y la aplicación esté lista para funcionar.
	@Bean
	public CommandLineRunner commandLineRunner(StudentDAO studentDAO) {
		return runner -> {
			createStudent(studentDAO);
		};
	}

	private void createStudent(StudentDAO studentDAO) {
		// crear el objeto estudiante
		System.out.println("Creating new student object...");
		Student student = new Student("Agustin", "Bollati", "agubollati@miempresa.com.ar");

		// guardar el estudiante en la base de datos
		System.out.println("Saving the student...");
		studentDAO.save(student);

		// mostrar el id del estudiante guardado
		System.out.println("Saved student. Generated id: " + student.getId());
	}

}
