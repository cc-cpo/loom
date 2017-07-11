/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package builders.loom.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class LoomPaths {

    public static final Path PROJECT_DIR;
    public static final Path SRC_MAIN_PATH;
    public static final Path SRC_TEST_PATH;
    public static final Path BUILD_MAIN_PATH;
    public static final Path BUILD_TEST_PATH;
    public static final Path REPORT_PATH;

    static {
        final Path currentDir = Paths.get("").toAbsolutePath().normalize();
        PROJECT_DIR = currentDir;

        SRC_MAIN_PATH = PROJECT_DIR.resolve(Paths.get("src", "main", "java"));
        SRC_TEST_PATH = PROJECT_DIR.resolve(Paths.get("src", "test", "java"));
        BUILD_MAIN_PATH = PROJECT_DIR.resolve(Paths.get("loombuild", "classes", "main"));
        BUILD_TEST_PATH = PROJECT_DIR.resolve(Paths.get("loombuild", "classes", "test"));
        REPORT_PATH = PROJECT_DIR.resolve(Paths.get("loombuild", "reports"));

        checkState(Files.exists(currentDir), "Invalid current directory");
    }

    private LoomPaths() {
    }

    private static void checkState(final boolean expression, final String errorMessage) {
        if (!expression) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public static Path relativize(final Path path) {
        return PROJECT_DIR.relativize(path.toAbsolutePath().normalize());
    }

}
