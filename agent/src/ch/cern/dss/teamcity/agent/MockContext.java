/**
 * Copyright (c) 2012-2013 by European Organization for Nuclear Research (CERN)
 * Author: Justin Salmon <jsalmon@cern.ch>
 *
 * This file is part of the Mock TeamCity plugin.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package ch.cern.dss.teamcity.agent;

import ch.cern.dss.teamcity.common.MockConstants;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Utility class to hold context for an individual mock chroot build thread object (MockCallable).
 */
public class MockContext {
    private final String chrootName;
    private final List<String> srpms;
    private final Map<String, String> runnerParameters;
    private final Map<String, String> environmentVariables;
    private final String artifactsPath;

    public MockContext(@NotNull String chrootName,
                       @NotNull List<String> srpms,
                       @NotNull Map<String, String> runnerParameters,
                       @NotNull String artifactsPath,
                       @NotNull Map<String, String> environmentVariables) {
        this.chrootName = chrootName;
        this.runnerParameters = runnerParameters;
        this.environmentVariables = environmentVariables;
        this.srpms = srpms;
        this.artifactsPath = artifactsPath;
    }

    public String getChrootName() {
        return chrootName;
    }

    public String getMockConfigDirectory() {
        return runnerParameters.get(MockConstants.CONFIG_DIR);
    }

    public List<String> getSrpms() {
        return srpms;
    }

    public String getArtifactsPath() {
        return artifactsPath;
    }

    public String getRpmMacros() {
        // Environment variable can override runner params
        return (environmentVariables.containsKey("RPMDEFS"))
                ? environmentVariables.get("RPMDEFS")
                : runnerParameters.get(MockConstants.RPM_MACROS);
    }
}
