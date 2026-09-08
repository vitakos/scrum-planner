package com.scrumplanner.core.intakesession;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

// IntakeSessionRepository (AISC-98): MongoDB repository for intake sessions.
@Repository
public interface IntakeSessionRepository extends MongoRepository<IntakeSessionDocument, String> {
    Optional<IntakeSessionDocument> findByProjectId(String projectId);
}
