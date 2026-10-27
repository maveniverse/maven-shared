/*
 * Copyright (c) 2023-2026 Maveniverse Org.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 */
package eu.maveniverse.maven.shared.extension;

import static eu.maveniverse.maven.shared.extension.RuntimeRequirementEnforcerLifecycleParticipant.normalizeJavaVersion;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.aether.util.version.GenericVersionScheme;
import org.eclipse.aether.version.VersionScheme;
import org.junit.jupiter.api.Test;

public class RuntimeRequirementEnforcerLifecycleParticipantTest {
    @Test
    void normalizeJavaVersions() {
        assertEquals("8.0_392", normalizeJavaVersion("1.8.0_392"));
        assertEquals("17.0.2", normalizeJavaVersion("17.0.2"));
        assertEquals("21", normalizeJavaVersion("21-ea"));
        assertEquals("25.0.1", normalizeJavaVersion("25.0.1-ea"));
    }

    @Test
    void earlyAccessSatisfiesRange() throws Exception {
        VersionScheme scheme = new GenericVersionScheme();
        assertTrue(scheme.parseVersionConstraint("[21,)")
                .containsVersion(scheme.parseVersion(normalizeJavaVersion("21-ea"))));
        assertTrue(scheme.parseVersionConstraint("[8,)")
                .containsVersion(scheme.parseVersion(normalizeJavaVersion("1.8.0_392"))));
    }
}
