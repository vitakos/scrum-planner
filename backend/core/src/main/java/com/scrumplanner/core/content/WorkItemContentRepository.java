package com.scrumplanner.core.content;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface WorkItemContentRepository extends MongoRepository<WorkItemContentDocument, String> {
}
