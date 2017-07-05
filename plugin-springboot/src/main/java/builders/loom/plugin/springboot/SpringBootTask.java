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

package builders.loom.plugin.springboot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import builders.loom.api.AbstractTask;
import builders.loom.api.BuildConfig;
import builders.loom.api.DependencyResolverService;
import builders.loom.api.DependencyScope;
import builders.loom.api.TaskStatus;
import builders.loom.api.product.AssemblyProduct;
import builders.loom.api.product.ClasspathProduct;
import builders.loom.api.product.CompilationProduct;
import builders.loom.api.product.ProcessedResourceProduct;
import builders.loom.util.Iterables;
import builders.loom.util.Preconditions;

public class SpringBootTask extends AbstractTask {

    private static final String SPRING_BOOT_APPLICATION_ANNOTATION =
        "org.springframework.boot.autoconfigure.SpringBootApplication";

    private final BuildConfig buildConfig;
    private final SpringBootPluginSettings pluginSettings;

    public SpringBootTask(final BuildConfig buildConfig,
                          final SpringBootPluginSettings pluginSettings) {
        this.buildConfig = buildConfig;
        this.pluginSettings = pluginSettings;
    }

    @Override
    public TaskStatus run() throws Exception {
        final Path baseDir = Paths.get("loombuild", "springboot");
        FileUtils.cleanDir(baseDir);

        final Path buildDir = baseDir.resolve("build");

        final Path jarFile = baseDir.resolve(String.format("%s-%s.jar",
            buildConfig.getProject().getArtifactId(),
            buildConfig.getProject().getVersion()));

        final Path assemblyFile = baseDir.resolve(jarFile.getFileName());

        // copy resources
        final ProcessedResourceProduct resourcesTreeProduct = getUsedProducts()
            .readProduct("processedResources", ProcessedResourceProduct.class);
        final Path classesDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "classes")));
        FileUtils.copyFiles(resourcesTreeProduct.getSrcDir(), classesDir);

        // copy classes
        final CompilationProduct compilationProduct = getUsedProducts()
            .readProduct("compilation", CompilationProduct.class);
        FileUtils.copyFiles(compilationProduct.getClassesDir(), classesDir);

        // scan for @SpringBootApplication
        final String applicationClassname = scanForApplicationStarter(compilationProduct);

        // copy libs
        final ClasspathProduct compileDependenciesProduct = getUsedProducts()
            .readProduct("compileDependencies", ClasspathProduct.class);
        final Path libDir = Files.createDirectories(
            buildDir.resolve(Paths.get("BOOT-INF", "lib")));
        FileUtils.copyFiles(compileDependenciesProduct.getEntries(), libDir);

        // copy spring boot loader
        copySpringBootLoader(resolveSpringBootLoaderJar(), buildDir);

        // assemble jar
        new JarAssembler(pluginSettings).assemble(buildDir, assemblyFile, applicationClassname);

        return complete(TaskStatus.OK, assemblyFile);
    }

    private Path resolveSpringBootLoaderJar() {
        final DependencyResolverService mavenDependencyResolver = getServiceLocator()
            .getService("mavenDependencyResolver", DependencyResolverService.class);

        final String springBootLoaderArtifact =
            "org.springframework.boot:spring-boot-loader:" + pluginSettings.getVersion();

        final List<Path> resolvedArtifacts = mavenDependencyResolver.resolve(
            Collections.singletonList(springBootLoaderArtifact),
            DependencyScope.COMPILE, "springBootApplication");

        return Iterables.getOnlyElement(resolvedArtifacts);
    }

    private String scanForApplicationStarter(final CompilationProduct compilationProduct)
        throws IOException {

        final String applicationClassname = new ClassScanner()
            .scanArchives(compilationProduct.getClassesDir(), SPRING_BOOT_APPLICATION_ANNOTATION);

        if (applicationClassname == null) {
            throw new IllegalStateException("Couldn't find class with "
                + SPRING_BOOT_APPLICATION_ANNOTATION + " annotation");
        }

        return applicationClassname;
    }

    private void copySpringBootLoader(final Path springBootLoaderJar, final Path baseDir)
        throws IOException {

        Preconditions.checkState(Files.isRegularFile(springBootLoaderJar));

        try (final JarFile jarFile = new JarFile(springBootLoaderJar.toFile())) {
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                final JarEntry jarEntry = entries.nextElement();
                if (jarEntry.getName().startsWith("META-INF/")) {
                    continue;
                }
                if (jarEntry.isDirectory()) {
                    Files.createDirectories(baseDir.resolve(jarEntry.getName()));
                } else {
                    try (final InputStream inputStream = jarFile.getInputStream(jarEntry)) {
                        Files.copy(inputStream, baseDir.resolve(jarEntry.getName()),
                            StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            }
        }
    }

    private TaskStatus complete(final TaskStatus status, final Path assemblyFile) {
        getProvidedProducts().complete("springBootApplication", new AssemblyProduct(assemblyFile));
        return status;
    }

}