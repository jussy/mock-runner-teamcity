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
import ch.cern.dss.teamcity.common.SystemCommandResult;
import ch.cern.dss.teamcity.common.Util;
import com.intellij.openapi.util.text.StringUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildFinishedStatus;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Callable (thread) implementation in which to run a single mock build.
 */
public class MockCallable implements Callable<BuildFinishedStatus> {

    private final MockContext context;
    private final BuildProgressLogger logger;

    /**
     * @param context the context utility class.
     * @param logger  the build progress logger.
     */
    public MockCallable(@NotNull MockContext context, @NotNull BuildProgressLogger logger) {
        this.context = context;
        this.logger = logger;
    }

    /**
     * Thread entry point.
     *
     * @return enum constant representing the exit state of this thread.
     * @throws RunBuildException
     */
    @Override
    public BuildFinishedStatus call() throws RunBuildException {

        // Initialize the chroot environment, if necessary.
        try {

            if (!new File(MockConstants.MOCK_CHROOT_DIR, context.getChrootName()).exists()) {
                initializeChrootEnvironment();
            }

            clean();
            rebuild();
            publishResults();
            publishManifest();

        } catch (Exception e) {
            logger.exception(e);
            return BuildFinishedStatus.FINISHED_FAILED;
        }

        return BuildFinishedStatus.FINISHED_SUCCESS;
    }

    /**
     * Initialize an individual chroot environment with mock.
     *
     * @throws RunBuildException
     */
    private void initializeChrootEnvironment() throws RunBuildException {
        logger.message("Initializing mock environment: " + context.getChrootName());

        String[] command = {MockConstants.MOCK_EXECUTABLE,
                "--init", "-r", context.getChrootName(),
                "--configdir=" + context.getMockConfigDirectory()};
        SystemCommandResult result;

        try {
            result = Util.runSystemCommand(command);
        } catch (Exception e) {
            throw new RunBuildException("Unable to initialize mock environment", e);
        }

        if (result.getReturnCode() != 0) {
            throw new RunBuildException("Unable to initialize mock environment: " + result.getOutput());
        }
    }

    /**
     * Cleanup old build results.
     *
     * @throws IOException
     */
    private void clean() throws IOException {
        FileUtils.deleteDirectory(new File(MockConstants.MOCK_CHROOT_DIR, context.getChrootName() + "/result"));
    }

    /**
     * Run the actual mock build.
     *
     * @throws RunBuildException
     */
    private void rebuild() throws RunBuildException {
        String[] command = {MockConstants.MOCK_EXECUTABLE,
                "--rebuild", "-r", context.getChrootName(),
                "--configdir=" + context.getMockConfigDirectory(),
                StringUtil.join(context.getSrpms(), " ")};

        // Append RPM macros if we have any
        if (context.getRpmMacros() != null) {
            command = Util.concatArrays(command, processRpmMacros(context.getRpmMacros()));
        }

        SystemCommandResult result;
        logger.message("Running mock: " + Arrays.toString(command));

        try {
            result = Util.runSystemCommand(command);
        } catch (Exception e) {
            throw new RunBuildException("Error running mock", e);
        }

        if (result.getReturnCode() != 0) {
            logger.warning("Mock exited with nozero code (" + result.getReturnCode() + "): " + result.getOutput());
        }

        // Check if rpms were created
        File rpmDirectory = new File(MockConstants.MOCK_CHROOT_DIR, context.getChrootName() + "/result");
        File[] files = rpmDirectory.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File directory, String name) {
                return name.endsWith(".rpm");
            }
        });

        if (files.length <= 0) throw new RunBuildException("Error running mock: RPMs not created");
    }

    /**
     * Process each user-defined RPM macro into a command-line ready state.
     *
     * @param macros the user-defined macro string.
     *
     * @return the processed macros.
     */
    private String[] processRpmMacros(String macros) {
        List<String> macroList = new ArrayList<String>();

        for (String entry : macros.split("--define=")) {
            if (entry.length() > 0) {
                String value = entry.replace("\n", "");
                value = value.trim();
                value = value.substring(1, value.length() - 1);
                macroList.add("--define");
                macroList.add(value);
            }
        }

        return macroList.toArray(new String[macroList.size()]);
    }

    /**
     * Copy the build results and the logs to the artifacts directory.
     *
     * @throws IOException
     */
    private void publishResults() throws IOException {
        // Make sure the results (artifacts) directory exists
        File artifactsDirectory = new File(context.getArtifactsPath());
        if (!artifactsDirectory.exists()) {
            artifactsDirectory.mkdirs();
        }

        // Create the destination directory if it doesn't exist
        File destinationDirectory = new File(artifactsDirectory, context.getChrootName());
        if (!destinationDirectory.exists()) {
            destinationDirectory.mkdirs();
        }

        // Create the log directory if it doesn't exist
        File logDirectory = new File(destinationDirectory, "logs");
        if (!logDirectory.exists()) {
            logDirectory.mkdirs();
        }

        // Move the results to the artifact directory
        File sourceDirectory = new File(MockConstants.MOCK_CHROOT_DIR, context.getChrootName() + "/result");
        for (File file : sourceDirectory.listFiles()) {
            if (file.getName().endsWith(".log")) {
                FileUtils.copyFile(file, new File(logDirectory, file.getName()));
            }

            if (file.getName().endsWith(".rpm")) {
                FileUtils.copyFile(file, new File(destinationDirectory, file.getName()));
            }
        }
    }

    /**
     * Write a metadata file inside the chroot result directory, for use by the abi-checker plugin.
     *
     * @throws IOException
     */
    private void publishManifest() throws IOException {
        File manifestFile = new File(context.getArtifactsPath(), context.getChrootName() + "/manifest.txt");
        if (!manifestFile.exists()) {
            manifestFile.createNewFile();
        }

        // Simply write the chroot name for now
        FileUtil.writeFile(manifestFile, "chroot=" + context.getChrootName(), "UTF-8");
    }
}
