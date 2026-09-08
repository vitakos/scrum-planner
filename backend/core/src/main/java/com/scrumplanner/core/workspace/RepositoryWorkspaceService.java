package com.scrumplanner.core.workspace;

import com.scrumplanner.core.common.NotFoundException;
import com.scrumplanner.core.project.Project;
import com.scrumplanner.core.project.ProjectRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * RepositoryWorkspaceService (AISC-17): clones or refreshes a project's configured
 * repository into an isolated scratch workspace so an assistant run can inspect real
 * code instead of relying on the conversation alone.
 */
@Service
public class RepositoryWorkspaceService {

    private final ProjectRepository projectRepository;

    @Value("${app.workspace.root}")
    private String workspaceRoot;

    public RepositoryWorkspaceService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    /**
     * Ensures a scratch workspace exists for the project's configured repository:
     * clones it if the project's workspace directory is absent, or pulls the latest
     * changes if it already exists. Each project gets its own subdirectory (named by
     * project id) under the configured workspace root, so runs from different
     * projects cannot read each other's cloned content.
     *
     * <p>AISC-18: this never fails an assistant run just because repo access isn't
     * available — a project with no repository URL configured short-circuits to
     * {@link WorkspaceResult.Skipped} before any git call is made, and a clone/pull
     * failure (unreachable remote, invalid URL, ...) is reported as
     * {@link WorkspaceResult.Failed} rather than thrown.
     *
     * @throws NotFoundException if the project doesn't exist
     */
    public WorkspaceResult ensureWorkspace(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found: " + projectId));

        String repositoryUrl = project.getRepositoryUrl();
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            return new WorkspaceResult.Skipped("No repository URL configured for project " + projectId);
        }

        Path projectWorkspace = Paths.get(workspaceRoot, projectId.toString());

        try {
            if (isExistingGitWorkspace(projectWorkspace)) {
                pull(projectWorkspace);
                return new WorkspaceResult.Pulled(projectWorkspace);
            } else {
                clone(repositoryUrl, projectWorkspace);
                return new WorkspaceResult.Cloned(projectWorkspace);
            }
        } catch (IOException | GitAPIException e) {
            return new WorkspaceResult.Failed(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        }
    }

    private boolean isExistingGitWorkspace(Path workspaceDir) {
        return Files.isDirectory(workspaceDir.resolve(".git"));
    }

    private void clone(String repositoryUrl, Path targetDir) throws IOException, GitAPIException {
        Files.createDirectories(targetDir);
        try (Git git = Git.cloneRepository()
                .setURI(repositoryUrl)
                .setDirectory(targetDir.toFile())
                .call()) {
            // cloned into targetDir; nothing further to do
        }
    }

    private void pull(Path workspaceDir) throws IOException, GitAPIException {
        try (Git git = Git.open(workspaceDir.toFile())) {
            git.pull().call();
        }
    }
}
