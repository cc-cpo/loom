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

package builders.loom.plugin.java;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import builders.loom.api.AbstractModuleTask;
import builders.loom.api.LoomPaths;
import builders.loom.api.Module;
import builders.loom.api.TaskResult;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ProcessedResourceProduct;

public class JavaAssembleTask extends AbstractModuleTask {

    private static final Logger LOG = LoggerFactory.getLogger(JavaAssembleTask.class);

    private final JavaPluginSettings pluginSettings;

    public JavaAssembleTask(final JavaPluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    @Override
    public TaskResult run() throws Exception {
        final Optional<CompilationProduct> compilationProduct = useProduct(
            "compilation", CompilationProduct.class);

        final Optional<ProcessedResourceProduct> resourcesTreeProduct = useProduct(
            "processedResources", ProcessedResourceProduct.class);

        if (!compilationProduct.isPresent() && !resourcesTreeProduct.isPresent()) {
            return completeEmpty();
        }

        final Path jarFile = Files
            .createDirectories(resolveBuildDir("jar"))
            .resolve(String.format("%s.jar", getBuildContext().getModuleName()));

        final Optional<Path> modulesInfoClassFileOpt = compilationProduct
            .map(CompilationProduct::getClassesDir)
            .map(c -> c.resolve(LoomPaths.MODULE_INFO_CLASS))
            .filter(c -> Files.exists(c));

        final String automaticModuleName = modulesInfoClassFileOpt.isPresent() ? null
            : buildAutomaticModuleName();

        try (final JarOutputStream os = buildJarOutput(jarFile, automaticModuleName)) {
            // compilation & module-info.class first !
            if (compilationProduct.isPresent()) {
                final Path classesDir = compilationProduct.get().getClassesDir();

                if (modulesInfoClassFileOpt.isPresent()) {
                    final Path modulesInfoClassFile = modulesInfoClassFileOpt.get();
                    final JarEntry entry =
                        new JarEntry(classesDir.relativize(modulesInfoClassFile).toString());
                    entry.setTime(Files.getLastModifiedTime(modulesInfoClassFile).toMillis());
                    os.putNextEntry(entry);
                    os.write(extendedModuleInfoClass(modulesInfoClassFile));
                    os.closeEntry();
                }

                // module-info.class is skipped by this method:
                JavaFileUtil.copy(classesDir, os);
            }

            if (resourcesTreeProduct.isPresent()) {
                JavaFileUtil.copy(resourcesTreeProduct.get().getSrcDir(), os);
            }
        }

        return completeOk(new AssemblyProduct(jarFile, "Jar of compiled classes"));
    }

    private String buildAutomaticModuleName() {
        if (!Module.UNNAMED_MODULE.equals(getBuildContext().getModuleName())) {
            // Use configured module name
            return getBuildContext().getModuleName();
        }

        // Unnamed module -- better use default name
        LOG.warn("Building jar file without module name is discouraged! Configure a "
            + "module name by either adding a module-info.java, using the module/ "
            + "directory structure or add moduleName to module.yml");
        return null;
    }

    private byte[] extendedModuleInfoClass(final Path modulesInfoClassFile) throws IOException {
        final byte[] data = Files.readAllBytes(modulesInfoClassFile);

        if (pluginSettings.getMainClassName() == null) {
            return data;
        }

        return ModuleInfoExtender.extend(data, pluginSettings.getMainClassName());
    }

    private JarOutputStream buildJarOutput(final Path jarFile, final String automaticModuleName)
        throws IOException {

        // TODO this creates META-INF/MANIFEST.MF but no META-INF directory?!
        return new JarOutputStream(Files.newOutputStream(jarFile),
            prepareManifest(automaticModuleName));
    }

    private Manifest prepareManifest(final String automaticModuleName) {
        final Manifest newManifest = new Manifest();

        final ManifestBuilder manifestBuilder = new ManifestBuilder(newManifest);

        manifestBuilder
            .put(Attributes.Name.MANIFEST_VERSION, "1.0")
            .put("Created-By", "Loom " + System.getProperty("loom.version"))
            .put("Build-Jdk", String.format("%s (%s)", System.getProperty("java.version"),
                System.getProperty("java.vendor")));

        Optional.ofNullable(pluginSettings.getMainClassName())
            .ifPresent(s -> manifestBuilder.put(Attributes.Name.MAIN_CLASS, s));

        Optional.ofNullable(automaticModuleName)
            .ifPresent(s -> manifestBuilder.put("Automatic-Module-Name", s));

        return newManifest;
    }

}