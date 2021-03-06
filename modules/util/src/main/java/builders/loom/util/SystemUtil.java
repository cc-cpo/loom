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

package builders.loom.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class SystemUtil {

    private SystemUtil() {
    }

    public static Path determineLoomBaseDir() {
        final String loomUserHome = System.getenv("LOOM_USER_HOME");
        if (loomUserHome != null) {
            return Paths.get(loomUserHome);
        }

        if (isWindowsOS()) {
            return determineWindowsBaseDir();
        }

        return determineGenericBaseDir();
    }

    private static boolean isWindowsOS() {
        final String osName = System.getProperty("os.name");
        return osName != null && osName.startsWith("Windows");
    }

    private static Path determineWindowsBaseDir() {
        final String localAppDataEnv = System.getenv("LOCALAPPDATA");

        if (localAppDataEnv == null) {
            throw new IllegalStateException("Windows environment variable LOCALAPPDATA missing");
        }

        final Path baseDir = Paths.get(localAppDataEnv);
        if (!Files.isDirectory(baseDir)) {
            throw new IllegalStateException("Windows environment variable LOCALAPPDATA points to "
                + "a non existing directory: " + baseDir);
        }

        try {
            return Files.createDirectories(baseDir.resolve(Paths.get("Loom", "Loom")));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Path determineGenericBaseDir() {
        final String userHomeVar = System.getProperty("user.home");
        final Path userHome = Paths.get(userHomeVar);

        if (!Files.isDirectory(userHome)) {
            throw new IllegalStateException("User home (" + userHomeVar + ") doesn't exist");
        }

        try {
            return Files.createDirectories(userHome.resolve(".loom"));
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
