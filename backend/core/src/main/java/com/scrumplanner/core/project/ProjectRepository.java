package com.scrumplanner.core.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    boolean existsByKey(String key);

    Optional<Project> findByKey(String key);
}
