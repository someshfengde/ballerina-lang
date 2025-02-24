/*
 *  Copyright (c) 2021, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.projects;

import com.google.gson.JsonSyntaxException;
import io.ballerina.projects.internal.model.BuildJson;
import io.ballerina.projects.util.FileUtils;
import io.ballerina.projects.util.ProjectConstants;
import io.ballerina.projects.util.ProjectUtils;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.ballerinalang.util.RepoUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test {@code ProjectUtils}.
 *
 * @since 2.0.0
 */
public class ProjectUtilsTests {

    private static final Path RESOURCE_DIRECTORY = Path.of("src/test/resources");
    private static final Path PROJECT_UTILS_RESOURCES = RESOURCE_DIRECTORY.resolve("project-utils");
    private static Path tempDirectory;
    private static BuildJson buildJson;

    @BeforeClass
    public void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("b7a-project-utils-test-" + System.nanoTime());
        buildJson = new BuildJson(1629359520, 1629259520, RepoUtils.getBallerinaShortVersion(),
                Collections.emptyMap());
    }

    @Test
    public void testReadBuildJson() {
        Path buildFilePath = PROJECT_UTILS_RESOURCES.resolve(ProjectConstants.BUILD_FILE);
        try {
            BuildJson buildJson = ProjectUtils.readBuildJson(buildFilePath);
            Assert.assertEquals(buildJson.lastBuildTime(), 1629359520);
            Assert.assertEquals(buildJson.lastUpdateTime(), 1629259520);
            Assert.assertEquals(buildJson.distributionVersion(), "slbeta4");
        } catch (Exception e) {
            Assert.fail("Reading Build Json failed");
        }
    }

    @Test()
    public void testReadBuildJsonForNonExistingBuildFile() {
        Path buildFilePath = PROJECT_UTILS_RESOURCES.resolve("xyz").resolve(ProjectConstants.BUILD_FILE);
        Assert.assertThrows(IOException.class, () -> ProjectUtils.readBuildJson(buildFilePath));
    }

    @Test()
    public void testReadBuildJsonForInvalidBuildFile() {
        Path buildFilePath = PROJECT_UTILS_RESOURCES.resolve("invalid-build");
        Assert.assertThrows(JsonSyntaxException.class, () -> ProjectUtils.readBuildJson(buildFilePath));
    }

    @Test()
    public void testDeleteSelectedFilesInDirectory() throws IOException {
        Path fromDir = PROJECT_UTILS_RESOURCES.resolve("delete-files");
        Path tempPackageDir = tempDirectory.resolve("delete-files");
        Files.createDirectory(tempPackageDir);
        List<Path> filesToKeep = new ArrayList<>();
        filesToKeep.add(tempPackageDir.resolve("test.txt"));
        filesToKeep.add(tempPackageDir.resolve("examples"));
        Files.walkFileTree(fromDir, new FileUtils.Copy(fromDir, tempPackageDir));
        ProjectUtils.deleteSelectedFilesInDirectory(tempPackageDir, filesToKeep);
        Assert.assertFalse(Files.exists(tempPackageDir.resolve("Ballerina.toml")));
        Assert.assertFalse(Files.exists(tempPackageDir.resolve("main.bal")));
        Assert.assertFalse(Files.exists(tempPackageDir.resolve("tests").resolve("main-test.bal")));
        Assert.assertTrue(Files.exists(tempPackageDir.resolve("test.txt")));
        Assert.assertTrue(Files.exists(tempPackageDir.resolve("examples").resolve("example.txt")));
    }
}
