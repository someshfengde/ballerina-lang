package io.ballerina.cli.cmd;

import io.ballerina.cli.utils.ProjectWatcher;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static io.ballerina.cli.cmd.CommandOutputUtils.getOutput;
import static io.ballerina.projects.util.ProjectConstants.DIST_CACHE_DIRECTORY;

/**
 * Tests for the --watch flag in the run command.
 *
 * @since 2201.11.0
 */
public class ProjectWatcherTest extends BaseCommandTest {
    private static final String WATCH_FLAG = "--watch";
    private static final int THREAD_SLEEP_DURATION_IN_MS = 8000;

    private Path watchTestResources;
    private Path testDistCacheDirectory;
    private ProjectEnvironmentBuilder projectEnvironmentBuilder;
    private Thread watcherThread;
    private AtomicReference<ProjectWatcher> watcher;
    private CountDownLatch latch = new CountDownLatch(1);

    // TODO: scenarios
    //  Negatives
    //  1. Compilation error
    //  2. Single file no service
    //  3. Project no service
    //  4. Invalid file changes - target/., tests/blah.bal /blah/blah.bal, modules/blah.bal, blah.json
    //  5. Invalid file creation, deletion
    //
    // TODO
    //  1. Valid file changes - resources/*, /blah.bal,
    //  2. Valid file creation, deletion
    //  3. Single file service
    //  4. Project service
    //  5. Deleting a service - should terminate immediately.
    //  6. --offline, --sticky

    @BeforeClass
    public void setup() throws IOException {
        super.setup();
        try {
            Path testResources = super.tmpDir.resolve("build-test-resources");
            this.watchTestResources = testResources.resolve("watchFlagResources");
            Path testBuildDirectory = Paths.get("build").toAbsolutePath();
            this.testDistCacheDirectory = testBuildDirectory.resolve(DIST_CACHE_DIRECTORY);
            Path customUserHome = Paths.get("build", "user-home");
            Environment environment = EnvironmentBuilder.getBuilder().setUserHome(customUserHome).build();
            this.projectEnvironmentBuilder = ProjectEnvironmentBuilder.getBuilder(environment);
            URI testResourcesURI = Objects.requireNonNull(
                    getClass().getClassLoader().getResource("test-resources")).toURI();
            Files.walkFileTree(Paths.get(testResourcesURI),
                    new BuildCommandTest.Copy(Paths.get(testResourcesURI), testResources));
            watcher = new AtomicReference<>();
        } catch (URISyntaxException e) {
            Assert.fail("error loading resources");
        }
    }

    @Test(description = "Run bal file containing syntax error with --watch")
    public void testRunWatchBalFileWithSyntaxError() throws IOException, InterruptedException {
        Path balFilePath = this.watchTestResources.resolve("service.bal");
        RunCommand runCommand = new RunCommand(balFilePath, printStream, false);
        new CommandLine(runCommand).parseArgs(WATCH_FLAG, balFilePath.toString());
        watcherThread = new Thread(() -> {
            try {
                watcher.set(new ProjectWatcher(runCommand, balFilePath, printStream));
                latch.countDown();
                watcher.get().watch();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        watcherThread.start();
        latch.await();
        Thread.sleep(THREAD_SLEEP_DURATION_IN_MS);
        replaceFileContent(balFilePath, this.watchTestResources.resolve("service-updated.bal"));
        Thread.sleep(THREAD_SLEEP_DURATION_IN_MS);
        String buildLog = readOutput(true);
        Assert.assertEquals(buildLog.replace("\r", ""), getOutput("watch-single-file-service.txt"));
    }

    @AfterMethod
    public void afterMethod() {
        try {
            if (watcherThread != null && watcher.get() != null) {
                stopProjectWatcher(watcherThread, watcher.get());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void replaceFileContent(Path filePath, Path copyFrom) {
        try {
            String newContent = Files.readString(copyFrom);
            Files.writeString(filePath, newContent);
        } catch (IOException e) {
            Assert.fail("Error occurred while writing to the file: " + e);
        }
    }

    private void stopProjectWatcher(Thread thread, ProjectWatcher watcher) throws InterruptedException {
        watcher.stopWatching();
        thread.join();
    }
}
