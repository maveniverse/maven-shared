/*
 * Copyright (c) 2023-2024 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.shared.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very basic Mojo support class.
 */
public abstract class MojoSupport extends AbstractMojo {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Parameter(defaultValue = "false", property = "skip")
    protected boolean skip;

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            skipMojo();
            return;
        }

        executeMojo();
    }

    protected abstract void executeMojo() throws MojoExecutionException, MojoFailureException;

    protected void skipMojo() throws MojoExecutionException, MojoFailureException {
        logger.info("Skipped");
    }
}
