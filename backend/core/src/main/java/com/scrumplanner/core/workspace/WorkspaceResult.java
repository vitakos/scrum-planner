package com.scrumplanner.core.workspace;

import java.nio.file.Path;

/**
 * Outcome of {@link RepositoryWorkspaceService#ensureWorkspace}.
 *
 * AISC-18: an assistant run must never fail outright just because repository access
 * isn't available, so every outcome — including "no repo configured" and "clone/pull
 * failed" — is represented as data the caller can inspect, rather than an exception.
 */
public sealed interface WorkspaceResult {

    /** The repository was cloned into the workspace for the first time. */
    record Cloned(Path workspacePath) implements WorkspaceResult {}

    /** An existing workspace was refreshed with the latest changes from the remote. */
    record Pulled(Path workspacePath) implements WorkspaceResult {}

    /** No repository is configured for the project, so the clone step was skipped. */
    record Skipped(String reason) implements WorkspaceResult {}

    /** Cloning or pulling failed; the run should continue without repository context. */
    record Failed(String reason) implements WorkspaceResult {}
}
