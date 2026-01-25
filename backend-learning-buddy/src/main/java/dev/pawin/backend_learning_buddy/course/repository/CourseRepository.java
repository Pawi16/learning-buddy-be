package dev.pawin.backend_learning_buddy.course.repository;

import dev.pawin.backend_learning_buddy.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository <Course, Long> {
}
