package com.scrumplanner.core.workitem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WorkItemRepository extends JpaRepository<WorkItem, UUID> {
    List<WorkItem> findAllByProjectIdOrderBySeqAsc(UUID projectId);

    List<WorkItem> findAllByProjectIdAndTypeOrderBySeqAsc(UUID projectId, String type);

    List<WorkItem> findAllByParentId(UUID parentId);

    int countByParentId(UUID parentId);

    boolean existsByStateId(UUID stateId);
}
