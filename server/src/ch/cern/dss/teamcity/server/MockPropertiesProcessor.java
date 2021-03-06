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

package ch.cern.dss.teamcity.server;

import ch.cern.dss.teamcity.common.MockConstants;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.util.PropertiesUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class MockPropertiesProcessor implements PropertiesProcessor {

    /**
     * Validate the properties (settings) that the user specified in the web UI.
     *
     * @param properties the map of properties passed from the web UI form.
     *
     * @return a collection of InvalidProperties. Each invalid property has a key (which matches that given as the name
     *         attribute of the corresponding web UI element) and a message describing the reason for invalidity.
     */
    @Override
    public Collection<InvalidProperty> process(Map<String, String> properties) {
        final Collection<InvalidProperty> result = new ArrayList<InvalidProperty>();

        if (PropertiesUtil.isEmptyOrNull(properties.get(MockConstants.CHROOTS))) {
            result.add(new InvalidProperty(MockConstants.CHROOTS,
                    "At least one chroot must be specified"));
        }

        if (PropertiesUtil.isEmptyOrNull(properties.get(MockConstants.SOURCE_RPM_DIR))) {
            result.add(new InvalidProperty(MockConstants.SOURCE_RPM_DIR,
                    "Source RPM directory must be specified"));
        }

        if (PropertiesUtil.isEmptyOrNull(properties.get(MockConstants.SOURCE_RPMS))) {
            result.add(new InvalidProperty(MockConstants.SOURCE_RPMS,
                    "At least one source RPM must be specified"));
        }

        if (!PropertiesUtil.isEmptyOrNull(properties.get(MockConstants.RPM_MACROS))) {
            String rpmMacros = properties.get(MockConstants.RPM_MACROS);
            if (!rpmMacros.startsWith("--define=")) {
                result.add(new InvalidProperty(MockConstants.RPM_MACROS,
                        "RPM macro \"" + rpmMacros + "\" invalid (must begin with \"--define=\")"));
            }
        }

        return result;
    }
}
