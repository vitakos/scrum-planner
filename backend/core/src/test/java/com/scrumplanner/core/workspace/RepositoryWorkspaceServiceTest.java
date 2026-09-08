package com.scrumplanner.core.workspace;

import com.scrumplanner.core.project.Project;
import com.scrumplanner.core.project.ProjectRepository;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Covers AISC-17/AISC-109: {@link RepositoryWorkspaceService#ensureWorkspace} clones a
 * project's configured repository into an isolated per-project workspace when it's
 * absent, and pulls the latest changes into it when it already exists — isolated per
 * project id. Uses a local temp git repo as the "remote" so the test needs no network
 * access.
 *
 * <p>Also covers AISC-18/AISC-114: a project with no repository URL configured yields
 * {@link WorkspaceResult.Skipped} without any git call, and a clone failure (unreachable
 * remote) yields {@link WorkspaceResult.Failed} with a reason instead of throwing.
 */
@ExtendWith(MockitoExtension.class)
class RepositoryWorkspaceServiceTest {

    private static final UUID PROJECT_ID = UUID.randomUUID();

    @Mock
    private ProjectRepository projectRepository;

    private RepositoryWorkspaceService service;

    @TempDir
    Path remoteDir;

    @TempDir
    Path workspaceRoot;

    private Path remoteRepoPath;

    @BeforeEach
    void setUp() throws Exception {
        service = new RepositoryWorkspaceService(projectRepository);
        ReflectionTestUtils.setField(service, "workspaceRoot", workspaceRoot.toString());

        remoteRepoPath = remoteDir.resolve("remote-repo");
        try (Git remoteGit = Git.init().setDirectory(remoteRepoPath.toFile()).call()) {
            Files.writeString(remoteRepoPath.resolve("README.md"), "hello");
            remoteGit.add().addFilepattern(".").call();
            commit(remoteGit, "initial commit");
        }
    }

    @Test
    void clonesRepositoryIntoIsolatedProjectWorkspaceWhenAbsent() {
        stubProjectWithRepositoryUrl(PROJECT_ID, remoteRepoPath.toUri().toString());

        WorkspaceResult result = service.ensureWorkspace(PROJECT_ID);

        assertThat(result).isInstanceOf(WorkspaceResult.Cloned.class);
        Path workspace = ((WorkspaceResult.Cloned) result).workspacePath();
        assertThat(workspace).isEqualTo(workspaceRoot.resolve(PROJECT_ID.toString()));
        assertThat(workspace.resolve(".git")).isDirectory();
        assertThat(workspace.resolve("README.md")).exists();
    }

    @Test
    void pullsLatestChangesWhenWorkspaceAlreadyExists() throws Exception {
        stubProjectWithRepositoryUrl(PROJECT_ID, remoteRepoPath.toUri().toString());

        // First call clones the repo into the workspace.
        WorkspaceResult firstResult = service.ensureWorkspace(PROJECT_ID);
        Path workspace = ((WorkspaceResult.Cloned) firstResult).workspacePath();
        assertThat(workspace.resolve("NEW_FILE.md")).doesNotExist();

        // A new commit lands on the "remote" after the initial clone.
        try (Git remoteGit = Git.open(remoteRepoPath.toFile())) {
            Files.writeString(remoteRepoPath.resolve("NEW_FILE.md"), "added later");
            remoteGit.add().addFilepattern(".").call();
            commit(remoteGit, "second commit");
        }

        // Second call should pull the new commit rather than re-cloning.
        WorkspaceResult secondResult = service.ensureWorkspace(PROJECT_ID);

        assertThat(secondResult).isInstanceOf(WorkspaceResult.Pulled.class);
        assertThat(((WorkspaceResult.Pulled) secondResult).workspacePath()).isEqualTo(workspace);
        assertThat(workspace.resolve("NEW_FILE.md")).exists();
    }

    @Test
    void skipsWhenNoRepositoryUrlConfigured() {
        UUID noRepoProjectId = UUID.randomUUID();
        Project projectWithoutRepo = new Project("NOREPO", "No Repo Project", null);
        when(projectRepository.findById(noRepoProjectId)).thenReturn(Optional.of(projectWithoutRepo));

        WorkspaceResult result = service.ensureWorkspace(noRepoProjectId);

        assertThat(result).isInstanceOf(WorkspaceResult.Skipped.class);
        assertThat(((WorkspaceResult.Skipped) result).reason()).contains(noRepoProjectId.toString());
        assertThat(workspaceRoot.resolve(noRepoProjectId.toString())).doesNotExist();
    }

    @Test
    void reportsFailedWhenRepositoryUrlIsUnreachable() {
        UUID badRepoProjectId = UUID.randomUUID();
        stubProjectWithRepositoryUrl(badRepoProjectId, remoteDir.resolve("does-not-exist").toUri().toString());

        WorkspaceResult result = service.ensureWorkspace(badRepoProjectId);

        assertThat(result).isInstanceOf(WorkspaceResult.Failed.class);
        assertThat(((WorkspaceResult.Failed) result).reason()).isNotBlank();
    }

    private void stubProjectWithRepositoryUrl(UUID projectId, String repositoryUrl) {
        Project project = new Project("TST", "Test Project", null);
        project.updateRepositoryUrl(repositoryUrl);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
    }

    private static void commit(Git git, String message) throws Exception {
        git.commit()
                .setMessage(message)
                .setSign(false)
                .setAuthor("Test", "test@example.com")
                .setCommitter("Test", "test@example.com")
                .call();
    }
}
