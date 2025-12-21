/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.shared.plugin;

import javax.inject.Inject;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.aether.util.ConfigUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very basic Mojo support class.
 */
public abstract class MojoSupport extends AbstractMojo {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Inject
    protected MavenSession mavenSession;

    @Parameter(defaultValue = "${mojo}", readonly = true, required = true)
    protected MojoExecution mojoExecution;

    /**
     * Plugin configuration to skip the Mojo. The Mojo can also be skipped by user property {@code $mojoName.skip}
     * or {@code $prefix.$mojoName.skip} as well (check specific Mojo).
     */
    @Parameter(defaultValue = "false")
    protected boolean skip;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (isSkipped()) {
            skipMojo();
            return;
        }

        executeMojo();
    }

    protected boolean isSkipped() {
        if (skip) {
            return true;
        }
        return ConfigUtils.getBoolean(mavenSession.getRepositorySession(), false, skipKeyNames());
    }

    /**
     * Override if custom message needed.
     */
    protected void skipMojo() throws MojoExecutionException, MojoFailureException {
        logger.info("Mojo '{}' skipped per user request.", mojoExecution.getGoal());
    }

    /**
     * Override if skipping requires observing more keys.
     */
    protected String[] skipKeyNames() {
        return new String[] {skipPrefix() + mojoExecution.getGoal() + ".skip"};
    }

    /**
     * Override if needed. If overridden, the returned string should ideally end with a dot.
     * Must not return {@code null}.
     */
    protected String skipPrefix() {
        return "";
    }

    /**
     * Implementation of this Mojo.
     */
    protected abstract void executeMojo() throws MojoExecutionException, MojoFailureException;
}
