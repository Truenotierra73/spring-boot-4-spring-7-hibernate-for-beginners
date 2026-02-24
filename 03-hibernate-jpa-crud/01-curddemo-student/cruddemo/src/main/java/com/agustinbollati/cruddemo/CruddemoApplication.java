package com.agustinbollati.cruddemo;

import com.agustinbollati.cruddemo.dao.StudentDAO;
import com.agustinbollati.cruddemo.entity.Student;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

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
//			createStudent(studentDAO);
//			createMultipleStudents(studentDAO);
//			readStudent(studentDAO);
//			queryForStudents(studentDAO);
			queryForStudentsOrderByLastName(studentDAO);
		};
	}

	private void queryForStudentsOrderByLastName(StudentDAO studentDAO) {
		// Obtener todos los estudiantes de la base de datos
		System.out.println("Getting all students...");
		List<Student> students = studentDAO.findAllOrderByLastName();

		// Mostrar los estudiantes obtenidos
//		students.forEach(System.out::println);
		students.forEach(student -> System.out.println(student));
	}

	private void queryForStudents(StudentDAO studentDAO) {
		// Obtener todos los estudiantes de la base de datos
		System.out.println("Getting all students...");
		List<Student> students = studentDAO.findAll();

		// Mostrar los estudiantes obtenidos
//		students.forEach(System.out::println);
//		students.forEach(student -> System.out.println(student));
		for (Student student : students) {
			System.out.println(student);
		}
	}

	private void readStudent(StudentDAO studentDAO) {
		// Crear un estudiante
		System.out.println("Creating new student object...");
		Student student = new Student("Adrian", "Lara", "adrian.lara@miempresa.com.ar");

		// Guardar el estudiante en la base de datos
		System.out.println("Saving the student...");
		studentDAO.save(student);

		// Obtener el id del estudiante guardado
		int studentId = student.getId();
		System.out.println("Saved student. Generated id: " + studentId);

		// Leer el estudiante de la base de datos usando el id: primary key
		System.out.println("Retrieving student with id: " + studentId);
		Student myStudent = studentDAO.findById(studentId);

		// Mostrar el estudiante leído
		System.out.println("Found the student: " + myStudent);
	}

	private void createMultipleStudents(StudentDAO studentDAO) {
		// Crear 3 objetos estudiante
		System.out.println("Creating 3 student objects...");
		Student student1 = new Student("Javier", "Vegas", "javi.vegas@miempresa.com.ar");
		Student student2 = new Student("Mailen", "Mancuso", "mailen.mancuso@miempresa.com.ar");
		Student student3 = new Student("Diego", "Vitulli", "diego.vitulli@miempresa.com.ar");

		// Guardar los estudiantes en la base de datos
		System.out.println("Saving the students...");
		studentDAO.save(student1);
		studentDAO.save(student2);
		studentDAO.save(student3);
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
