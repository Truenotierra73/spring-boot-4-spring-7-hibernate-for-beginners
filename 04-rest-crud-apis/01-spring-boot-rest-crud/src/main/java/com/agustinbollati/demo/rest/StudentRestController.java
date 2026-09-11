package com.agustinbollati.demo.rest;

import com.agustinbollati.demo.entity.Student;
import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StudentRestController {

	private List<Student> theStudents;

	// definir @PostConstruct para cargar los estudiantes al iniciar
	@PostConstruct
	public void loadStudents() {
		this.theStudents = new ArrayList<>();
		this.theStudents.add(new Student("John", "Doe"));
		this.theStudents.add(new Student("Agustin", "Bollati"));
		this.theStudents.add(new Student("Anabella", "Gilli"));
	}

	// definir endpoint para obtener todos los estudiantes - retorno una lista de estudiantes con "/students"
	@GetMapping("/students")
	public List<Student> getStudents() {
		return this.theStudents;
	}

	// definir endpoint para obtener un estudiante por ID - retorno un estudiante con "/students/{studentId}"
	@GetMapping("/students/{studentId}")
	public Student getStudent(@PathVariable int studentId) {
		if (studentId >= this.theStudents.size() || studentId < 0) {
			throw new StudentNotFoundException("Student id not found - " + studentId);
		}

		return this.theStudents.get(studentId);
	}
}
