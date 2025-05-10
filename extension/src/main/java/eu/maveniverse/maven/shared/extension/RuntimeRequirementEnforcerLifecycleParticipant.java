/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.shared.extension;

import eu.maveniverse.maven.shared.core.maven.MavenUtils;
import java.util.Map;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.InvalidVersionSpecificationException;
import org.eclipse.aether.version.Version;
import org.eclipse.aether.version.VersionConstraint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lifecycle participant that enforces runtime requirements.
 * <p>
 * This class is intentionally self-contained and compiled with Java 8 to make it able to run on wide range of
 * Maven and Java versions, to report sane error for user why extension refuses to work in their environment.
 * <p>
 * To use this: create {@code runtime-requirements.properties} in classpath root of your extension with following
 * content (mavenRequirement and javaRequirement are ranges):
 * <pre>
 *     applicationName=My cool new Maven extension
 *     mavenRequirement=(3.9,)
 *     javaRequirement=[8,)
 * </pre>
 */
@Singleton
@Named
public final class RuntimeRequirementEnforcerLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        Optional<Map<String, String>> properties = MavenUtils.discoverProperties(
                RuntimeRequirementEnforcerLifecycleParticipant.class.getClassLoader(),
                "runtime-requirements.properties");
        if (properties.isPresent()) {
            Map<String, String> p = properties.orElseThrow(() -> new IllegalStateException("No value"));
            logger.debug("Found runtime-requirements.properties on classpath");
            String applicationName = p.getOrDefault("applicationName", "Maven extension");
            String mavenRequirement = p.get("mavenRequirement");
            String javaRequirement = p.get("javaRequirement");
            if (mavenRequirement != null && javaRequirement != null) {
                if (!checkRuntimeRequirements(applicationName, mavenRequirement, javaRequirement)) {
                    throw new MavenExecutionException("Runtime requirements are not fulfilled", (Throwable) null);
                } else {
                    logger.debug("Runtime requirements are fulfilled: {}", p);
                }
            } else {
                logger.warn("Incomplete runtime-requirements.properties: {}", p);
            }
        } else {
            logger.debug("Not found runtime-requirements.properties on classpath");
        }
    }

    private boolean checkRuntimeRequirements(String name, String mavenRequirement, String javaRequirement) {
        String mavenVersionString = MavenUtils.discoverArtifactVersion(
                Version.class.getClassLoader(), "org.apache.maven", "maven-core", null);
        String javaVersionString = System.getProperty("java.version");
        if (mavenVersionString == null || javaVersionString == null) {
            throw new IllegalStateException("Maven and/or Java version could not be determined");
        }
        try {
            GenericVersionScheme versionScheme = new GenericVersionScheme();
            VersionConstraint mavenConstraint = versionScheme.parseVersionConstraint(mavenRequirement);
            VersionConstraint javaConstraint = versionScheme.parseVersionConstraint(javaRequirement);
            Version mavenVersion = versionScheme.parseVersion(mavenVersionString);
            Version javaVersion = versionScheme.parseVersion(javaVersionString);

            boolean mavenOk = mavenConstraint.containsVersion(mavenVersion);
            boolean javaOk = javaConstraint.containsVersion(javaVersion);
            boolean runtimeRequirements = mavenOk && javaOk;
            if (!runtimeRequirements) {
                logger.warn("{} runtime requirements are not fulfilled:", name);
                if (!mavenOk) {
                    logger.warn(String.format(
                            "* Unsupported Maven version %s; supported versions are %s",
                            mavenVersion, mavenConstraint));
                }
                if (!javaOk) {
                    logger.warn(String.format(
                            "* Unsupported Java version %s; supported versions are %s", javaVersion, javaConstraint));
                }
                logger.error("{} cannot operate in this environment: adapt your environment for requirements", name);
            }
            return runtimeRequirements;
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalStateException("Maven and/or Java version could not be parsed", e);
        }
    }
}
