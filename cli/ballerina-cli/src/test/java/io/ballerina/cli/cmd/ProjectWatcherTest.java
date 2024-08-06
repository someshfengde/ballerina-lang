package io.ballerina.cli.cmd;

import io.ballerina.cli.utils.ProjectWatcher;
import io.ballerina.projects.ProjectEnvironmentBuilder;
import io.ballerina.projects.environment.Environment;
import io.ballerina.projects.environment.EnvironmentBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
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
    private static final String PROJECT_NAME_PLACEHOLDER = "INSERT_PROJECT_NAME";

    private Path watchTestResources;
    private Path testDistCacheDirectory;
    private ProjectEnvironmentBuilder projectEnvironmentBuilder;
    private Thread watcherThread;
    private AtomicReference<ProjectWatcher> watcher;

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

    @Test(description = "Run a correct bal service file and do a correct change")
    public void testRunWatchCorrectBalFileWithCorrectChange() throws IOException, InterruptedException {
        Path balFilePath = createTempFile("service.bal");
        RunCommand runCommand = new RunCommand(balFilePath, printStream, false);
        new CommandLine(runCommand).parseArgs(WATCH_FLAG, balFilePath.toString());
        CountDownLatch latch = new CountDownLatch(1);
        watcherThread = new Thread(() -> {
            try {
                watcher.set(new ProjectWatcher(runCommand, balFilePath, printStream));
                latch.countDown();
                watcher.get().watch();
            } catch (IOException e) {
                Assert.fail("Error occurred while watching the project: " + e);
            }
        });
        watcherThread.start();
        latch.await();
        Thread.sleep(THREAD_SLEEP_DURATION_IN_MS);
        replaceFileContent(balFilePath, this.watchTestResources.resolve("service-updated.bal"));
        Thread.sleep(THREAD_SLEEP_DURATION_IN_MS);
        String actualOutput = readOutput(true).replace("\r", "");
        String expectedOutput = readExpectedOutputFile("watch-correct-service-file-correct-change.txt", balFilePath);
        Assert.assertEquals(actualOutput, expectedOutput);
    }

    @Test(description = "Run a correct bal service file and do a erroneous change")
    public void testRunWatchCorrectBalFileWithErroneousChange() throws IOException, InterruptedException {
        Path balFilePath = createTempFile("service.bal");
        RunCommand runCommand = new RunCommand(balFilePath, printStream, false);
        new CommandLine(runCommand).parseArgs(WATCH_FLAG, balFilePath.toString());
        CountDownLatch latch = new CountDownLatch(1);
        watcherThread = new Thread(() -> {
            try {
                watcher.set(new ProjectWatcher(runCommand, balFilePath, printStream));
                latch.countDown();
                watcher.get().watch();
            } catch (IOException e) {
                Assert.fail("Error occurred while watching the project: " + e);
            }
        });
        watcherThread.start();
        latch.await();
        Thread.sleep(THREAD_SLEEP_DURATION_IN_MS);
        replaceFileContent(balFilePath, this.watchTestResources.resolve("service-error.bal"));
        Thread.sleep(THREAD_SLEEP_DURATION_IN_MS);
        String actualOutput = readOutput(true).replace("\r", "");
        String expectedOutput = readExpectedOutputFile("watch-correct-service-file-error-change.txt", balFilePath);
        Assert.assertEquals(actualOutput, expectedOutput);
    }

    @Test(description = "Run a erroneous bal service file and do a correct change")
    public void testRunWatchErroneousBalFileWithCorrectChange() throws IOException, InterruptedException {
        Path balFilePath = createTempFile("service-error.bal");
        RunCommand runCommand = new RunCommand(balFilePath, printStream, false);
        new CommandLine(runCommand).parseArgs(WATCH_FLAG, balFilePath.toString());
        CountDownLatch latch = new CountDownLatch(1);
        watcherThread = new Thread(() -> {
            try {
                watcher.set(new ProjectWatcher(runCommand, balFilePath, printStream));
                latch.countDown();
                watcher.get().watch();
            } catch (IOException e) {
                Assert.fail("Error occurred while watching the project: " + e);
            }
        });
        watcherThread.start();
        latch.await();
        Thread.sleep(THREAD_SLEEP_DURATION_IN_MS);
        replaceFileContent(balFilePath, this.watchTestResources.resolve("service.bal"));
        Thread.sleep(THREAD_SLEEP_DURATION_IN_MS);
        String actualOutput = readOutput(true).replace("\r", "");
        String expectedOutput = readExpectedOutputFile("watch-error-service-file-correct-change.txt", balFilePath);
        Assert.assertEquals(actualOutput, expectedOutput);
    }

    @AfterMethod
    public void afterMethod() {
        try {
            if (watcherThread != null && watcher.get() != null) {
                stopProjectWatcher(watcherThread, watcher.get());
            }
        } catch (InterruptedException e) {
            Assert.fail("Error occurred while stopping the project watcher. " +
                    "Please kill any stale java processes that were started by the project watcher tests: " + e);
        }
    }

    private Path createTempFile(String fileName) throws IOException {
        Path balFilePath = this.watchTestResources.resolve(fileName);
        Path tempFilePath = Files.createTempFile("service", ".bal");
        replaceFileContent(tempFilePath, balFilePath);
        return tempFilePath;
    }

    private void replaceFileContent(Path filePath, Path copyFrom) {
        try {
            String newContent = Files.readString(copyFrom);
            Files.writeString(filePath, newContent);
        } catch (IOException e) {
            Assert.fail("Error occurred while writing to the file: " + e);
        }
    }

    private String readExpectedOutputFile(String expectedOutputFile, Path balFilePath) throws IOException {
        return getOutput(expectedOutputFile)
                .replace("INSERT_PROJECT_NAME", balFilePath.getFileName().toString());
    }

    private void stopProjectWatcher(Thread thread, ProjectWatcher watcher) throws InterruptedException {
        watcher.stopWatching();
        thread.join();
    }
}
