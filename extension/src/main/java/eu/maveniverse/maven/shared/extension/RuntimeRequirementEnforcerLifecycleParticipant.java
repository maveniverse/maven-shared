/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.shared.extension;

import static java.util.Objects.requireNonNull;

import eu.maveniverse.maven.shared.core.maven.MavenUtils;
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
 * To use this: extend this class, provide required parameters in constructor, and make it a component in maven extension.
 */
public abstract class RuntimeRequirementEnforcerLifecycleParticipant extends AbstractMavenLifecycleParticipant {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final String name;
    private final String mavenRequirement;
    private final String javaRequirement;

    protected RuntimeRequirementEnforcerLifecycleParticipant(
            String name, String mavenRequirement, String javaRequirement) {
        this.name = requireNonNull(name);
        this.mavenRequirement = requireNonNull(mavenRequirement);
        this.javaRequirement = requireNonNull(javaRequirement);
    }

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        if (!checkRuntimeRequirements()) {
            throw new MavenExecutionException("Runtime requirements are not fulfilled", (Throwable) null);
        }
    }

    private boolean checkRuntimeRequirements() {
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
                logger.error("{} cannot operate in this environment: adapt your environment or remove extension", name);
            }
            return runtimeRequirements;
        } catch (InvalidVersionSpecificationException e) {
            throw new IllegalStateException("Maven and/or Java version could not be parsed", e);
        }
    }
}
