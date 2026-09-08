package com.scrumplanner.core.workspace;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * RepositoryContextReader (AISC-19): builds a small, bounded text summary of a cloned
 * workspace — a file tree plus the README, if present — so {@code GapAnalysisService}
 * can ground its prompt in the real repo without shipping the whole tree to the LLM.
 */
@Service
public class RepositoryContextReader {

    private static final int MAX_TREE_ENTRIES = 200;
    private static final int MAX_README_CHARS = 4000;
    private static final int MAX_TOTAL_CHARS = 8000;
    private static final List<String> README_CANDIDATES = List.of("README.md", "README.MD", "readme.md", "README");

    /**
     * Summarizes the given workspace directory. Returns a bounded string — never the
     * full repo — suitable for inclusion in an LLM prompt.
     */
    public String summarize(Path workspacePath) {
        StringBuilder summary = new StringBuilder();
        summary.append("Repository file structure (truncated):\n");
        summary.append(fileTree(workspacePath));

        readme(workspacePath).ifPresent(readmeText -> {
            summary.append("\n\nREADME:\n");
            summary.append(readmeText);
        });

        if (summary.length() > MAX_TOTAL_CHARS) {
            summary.setLength(MAX_TOTAL_CHARS);
            summary.append("\n... (truncated)");
        }
        return summary.toString();
    }

    private String fileTree(Path workspacePath) {
        try (Stream<Path> paths = Files.walk(workspacePath)) {
            return paths
                    .filter(path -> !path.equals(workspacePath))
                    .filter(path -> !isUnderGitDir(workspacePath, path))
                    .sorted(Comparator.comparing(Path::toString))
                    .limit(MAX_TREE_ENTRIES)
                    .map(path -> workspacePath.relativize(path).toString().replace('\\', '/'))
                    .collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read repository file tree at " + workspacePath, e);
        }
    }

    private boolean isUnderGitDir(Path workspacePath, Path path) {
        Path gitDir = workspacePath.resolve(".git");
        return path.startsWith(gitDir);
    }

    private Optional<String> readme(Path workspacePath) {
        for (String candidate : README_CANDIDATES) {
            Path readmePath = workspacePath.resolve(Paths.get(candidate));
            if (Files.isRegularFile(readmePath)) {
                try {
                    String content = Files.readString(readmePath, StandardCharsets.UTF_8);
                    if (content.length() > MAX_README_CHARS) {
                        content = content.substring(0, MAX_README_CHARS) + "\n... (truncated)";
                    }
                    return Optional.of(content);
                } catch (IOException e) {
                    throw new UncheckedIOException("Failed to read " + readmePath, e);
                }
            }
        }
        return Optional.empty();
    }
}
