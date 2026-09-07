package com.scrumplanner.core.project;

import com.scrumplanner.core.common.ConflictException;
import com.scrumplanner.core.common.NotFoundException;
import com.scrumplanner.core.project.dto.CreateProjectRequest;
import com.scrumplanner.core.project.dto.UpdateProjectRequest;
import com.scrumplanner.core.workflow.DefaultWorkflowSeeder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final DefaultWorkflowSeeder defaultWorkflowSeeder;

    public ProjectService(ProjectRepository projectRepository, DefaultWorkflowSeeder defaultWorkflowSeeder) {
        this.projectRepository = projectRepository;
        this.defaultWorkflowSeeder = defaultWorkflowSeeder;
    }

    public List<Project> listProjects() {
        return projectRepository.findAll();
    }

    public Project getProject(UUID id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found: " + id));
    }

    /**
     * Creates the project and, since this is the first project setup wizard,
     * immediately seeds the predefined work item types' default workflows so
     * there is something usable/editable right away.
     */
    @Transactional
    public Project createProject(CreateProjectRequest request) {
        String key = request.key().toUpperCase();
        if (projectRepository.existsByKey(key)) {
            throw new ConflictException("A project with key '" + key + "' already exists");
        }

        Project project = projectRepository.save(new Project(key, request.name().trim(), trimToNull(request.description())));
        defaultWorkflowSeeder.seedDefaultWorkflows(project.getId());
        return project;
    }

    @Transactional
    public Project updateProject(UUID id, UpdateProjectRequest request) {
        Project project = getProject(id);
        project.updateRepositoryUrl(request.repositoryUrl());
        return project;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
