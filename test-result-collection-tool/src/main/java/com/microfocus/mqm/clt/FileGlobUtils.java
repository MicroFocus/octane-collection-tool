/*
 *     Copyright 2015-2023 Open Text
 *
 *     The only warranties for products and services of Open Text and
 *     its affiliates and licensors ("Open Text") are as may be set forth
 *     in the express warranty statements accompanying such products and services.
 *     Nothing herein should be construed as constituting an additional warranty.
 *     Open Text shall not be liable for technical or editorial errors or
 *     omissions contained herein. The information contained herein is subject
 *     to change without notice.
 *
 *     Except as specifically indicated otherwise, this document contains
 *     confidential information and a valid license is required for possession,
 *     use or copying. If this work is provided to the U.S. Government,
 *     consistent with FAR 12.211 and 12.212, Commercial Computer Software,
 *     Computer Software Documentation, and Technical Data for Commercial Items are
 *     licensed to the U.S. Government under vendor's standard commercial license.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.microfocus.mqm.clt;

import java.io.IOException;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for resolving glob-style file patterns to lists of matching file paths.
 * <p>
 * Supported patterns:
 * <ul>
 *   <li>Literal path - {@code target/surefire-reports/TEST-foo.xml}</li>
 *   <li>Single-level wildcard - {@code target/surefire-reports/*.xml}</li>
 *   <li>Recursive wildcard - {@code target/site/&#42;&#42;/*.xml}</li>
 * </ul>
 * Patterns without glob characters are treated as literal file paths.
 * All patterns are resolved relative to the current working directory unless absolute.
 * <p>
 * Implementation uses Java NIO's {@link PathMatcher} and {@link Files#walk(Path, int, FileVisitOption...)}
 * - no external dependencies required.
 */
public final class FileGlobUtils {

    private static final String GLOB_STAR            = "*";
    private static final String GLOB_QUESTION        = "?";
    private static final String GLOB_RECURSIVE       = "**";
    private static final String GLOB_RECURSIVE_SLASH = "**/";
    private static final String BACKSLASH            = "\\";
    private static final String FORWARD_SLASH        = "/";
    private static final String GLOB_PREFIX          = "glob:";
    private static final String USER_DIR_PROPERTY    = "user.dir";
    private static final String CURRENT_DIR          = ".";

    private FileGlobUtils() {
    }

    /**
     * Resolves a glob pattern (or literal path) to a list of readable, regular-file paths.
     *
     * @param pattern glob pattern or literal file path
     * @return list of absolute paths of matching readable files; never {@code null}
     */
    public static List<String> resolveGlobPattern(String pattern) {
        // Normalize separators for cross-platform consistency
        String normalizedPattern = pattern.replace(BACKSLASH, FORWARD_SLASH);

        boolean hasGlob = normalizedPattern.contains(GLOB_STAR) || normalizedPattern.contains(GLOB_QUESTION);

        if (!hasGlob) {
            return resolveLiteralPath(pattern);
        }

        return resolveGlob(normalizedPattern);
    }

    /**
     * Handles literal (non-glob) paths - returns the single file if it exists and is readable.
     */
    private static List<String> resolveLiteralPath(String pattern) {
        String cleaned = stripLeadingSlashOnWindows(pattern);
        Path path = Paths.get(cleaned);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty(USER_DIR_PROPERTY)).resolve(cleaned);
        }
        path = path.normalize();
        if (Files.isRegularFile(path) && Files.isReadable(path)) {
            return Collections.singletonList(path.toAbsolutePath().toString());
        }
        return Collections.emptyList();
    }

    /**
     * Handles glob patterns - walks the base directory and filters with a {@link PathMatcher}.
     */
    private static List<String> resolveGlob(String normalizedPattern) {
        String cleaned = stripLeadingSlashOnWindows(normalizedPattern);

        // Split into path segments to find the base directory (segments before any glob char)
        String[] segments = cleaned.split(FORWARD_SLASH);
        StringBuilder baseDirBuilder = new StringBuilder();
        int globSegmentIndex = 0;

        for (int i = 0; i < segments.length; i++) {
            if (segments[i].contains(GLOB_STAR) || segments[i].contains(GLOB_QUESTION)) {
                globSegmentIndex = i;
                break;
            }
            if (baseDirBuilder.length() > 0) {
                baseDirBuilder.append(FORWARD_SLASH);
            }
            baseDirBuilder.append(segments[i]);
            globSegmentIndex = i + 1;
        }

        String baseDirStr = baseDirBuilder.length() > 0 ? baseDirBuilder.toString() : CURRENT_DIR;
        Path baseDir = Paths.get(baseDirStr);
        if (!baseDir.isAbsolute()) {
            baseDir = Paths.get(System.getProperty(USER_DIR_PROPERTY)).resolve(baseDir);
        }
        baseDir = baseDir.normalize();

        if (!Files.isDirectory(baseDir)) {
            return Collections.emptyList();
        }

        // Build the glob expression relative to the base directory
        StringBuilder relativeGlobBuilder = new StringBuilder();
        for (int i = globSegmentIndex; i < segments.length; i++) {
            if (relativeGlobBuilder.length() > 0) {
                relativeGlobBuilder.append(FORWARD_SLASH);
            }
            relativeGlobBuilder.append(segments[i]);
        }
        String relativeGlob = relativeGlobBuilder.toString();

        boolean isRecursive = relativeGlob.contains(GLOB_RECURSIVE);
        int maxDepth = isRecursive ? Integer.MAX_VALUE : 1;

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher(GLOB_PREFIX + relativeGlob);

        // NIO's ** in "**/*.xml" requires at least one directory level.
        // Typical glob tools match 0+ levels, so add a fallback matcher for root-level files.
        final PathMatcher rootMatcher;
        if (relativeGlob.startsWith(GLOB_RECURSIVE_SLASH)) {
            rootMatcher = FileSystems.getDefault().getPathMatcher(GLOB_PREFIX + relativeGlob.substring(GLOB_RECURSIVE_SLASH.length()));
        } else if (relativeGlob.startsWith(GLOB_RECURSIVE)) {
            rootMatcher = FileSystems.getDefault().getPathMatcher(GLOB_PREFIX + GLOB_STAR + relativeGlob.substring(GLOB_RECURSIVE.length()));
        } else {
            rootMatcher = null;
        }

        final Path root = baseDir;

        try (Stream<Path> stream = Files.walk(baseDir, maxDepth)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(Files::isReadable)
                    .filter(file -> {
                        Path relativePath = root.relativize(file);
                        return matcher.matches(relativePath)
                                || (rootMatcher != null && rootMatcher.matches(relativePath));
                    })
                    .map(path -> path.toAbsolutePath().normalize().toString())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("Warning: error while resolving pattern '" + normalizedPattern + "': " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * On Windows, URI-style paths like {@code /C:/foo/bar} are invalid for {@link Paths#get(String, String...)}.
     * This strips the leading slash if it precedes a drive letter.
     */
    private static String stripLeadingSlashOnWindows(String path) {
        if (path.length() >= 3 && path.charAt(0) == '/' && Character.isLetter(path.charAt(1)) && path.charAt(2) == ':') {
            return path.substring(1);
        }
        return path;
    }
}