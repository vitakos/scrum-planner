package com.scrumplanner.core.workitem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkItemStateLogRepository extends JpaRepository<WorkItemStateLog, UUID> {
}
