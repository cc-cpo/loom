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

package builders.loom.core;

import java.nio.file.Path;

import builders.loom.api.RuntimeConfiguration;

public class RuntimeConfigurationImpl implements RuntimeConfiguration {

    private final Path projectBaseDir;
    private final boolean cacheEnabled;
    private final String version;
    private final boolean moduleBuild;

    public RuntimeConfigurationImpl(final Path projectBaseDir, final boolean cacheEnabled,
                                    final String version, final boolean moduleBuild) {
        this.projectBaseDir = projectBaseDir;
        this.cacheEnabled = cacheEnabled;
        this.version = version;
        this.moduleBuild = moduleBuild;
    }

    @Override
    public Path getProjectBaseDir() {
        return projectBaseDir;
    }

    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public boolean isModuleBuild() {
        return moduleBuild;
    }

}
