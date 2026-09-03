import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final String TERRARIA_BINARY =
            "TerrariaServer.bin.x86_64";

    private static final String CONFIG_FILE =
            "serverconfig.txt";

    public static void main(String[] args) {
        try {
            Path jarDirectory = getJarDirectory();

            Path terraria = jarDirectory.resolve(TERRARIA_BINARY);
            Path config = jarDirectory.resolve(CONFIG_FILE);

            System.out.println("========================================");
            System.out.println("       Terraria Java Wrapper");
            System.out.println("========================================");
            System.out.println("JAR directory: " + jarDirectory);
            System.out.println();

            // ----------------------------------------
            // Terraria binary
            // ----------------------------------------

            System.out.println("[1] Checking Terraria binary...");

            if (!Files.exists(terraria)) {
                System.err.println(
                        "ERROR: TerrariaServer.bin.x86_64 was not found!"
                );
                System.err.println(
                        "Expected: " + terraria
                );
                System.exit(1);
            }

            if (!Files.isRegularFile(terraria)) {
                System.err.println(
                        "ERROR: TerrariaServer.bin.x86_64 is not a file."
                );
                System.exit(1);
            }

            System.out.println("OK: Terraria binary found.");

            makeExecutable(terraria);

            // ----------------------------------------
            // Config
            // ----------------------------------------

            System.out.println();
            System.out.println("[2] Checking serverconfig.txt...");

            if (!Files.exists(config)) {
                System.err.println(
                        "ERROR: serverconfig.txt was not found!"
                );
                System.err.println(
                        "Expected: " + config
                );
                System.exit(1);
            }

            System.out.println("OK: serverconfig.txt found.");
            System.out.println("Config: " + config);

            // ----------------------------------------
            // Read config
            // ----------------------------------------

            System.out.println();
            System.out.println("[3] Reading config...");

            String worldPath = null;
            String worldName = null;
            String worldPathFromConfig = null;

            List<String> configLines =
                    Files.readAllLines(config);

            for (String line : configLines) {

                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                if (line.startsWith("world=")) {
                    worldPath = line.substring(6).trim();
                }

                if (line.startsWith("worldname=")) {
                    worldName = line.substring(10).trim();
                }

                if (line.startsWith("worldpath=")) {
                    worldPathFromConfig =
                            line.substring(10).trim();
                }
            }

            System.out.println(
                    "worldpath = " + worldPathFromConfig
            );

            System.out.println(
                    "worldname = " + worldName
            );

            System.out.println(
                    "world     = " + worldPath
            );

            // ----------------------------------------
            // Check world
            // ----------------------------------------

            System.out.println();
            System.out.println("[4] Checking world...");

            if (worldPath != null && !worldPath.isEmpty()) {

                Path world = Paths.get(worldPath);

                System.out.println(
                        "World path: " + world
                );

                if (Files.exists(world)) {

                    System.out.println(
                            "OK: World file found!"
                    );

                    System.out.println(
                            "World size: "
                                    + Files.size(world)
                                    + " bytes"
                    );

                } else {

                    System.err.println(
                            "WARNING: World file NOT found!"
                    );

                    System.err.println(
                            "Expected: " + world
                    );

                    System.err.println(
                            "Terraria may create a new world."
                    );
                }

            } else {

                System.err.println(
                        "WARNING: No world= entry found in config!"
                );
            }

            // ----------------------------------------
            // Start Terraria
            // ----------------------------------------

            System.out.println();
            System.out.println("[5] Starting Terraria...");
            System.out.println();

            List<String> command = new ArrayList<>();

            command.add(
                    terraria.toAbsolutePath().toString()
            );

            // IMPORTANT:
            // Always use the config located next to the JAR.
            command.add("-config");
            command.add(config.toAbsolutePath().toString());

            // ----------------------------------------
            // Extra arguments
            // ----------------------------------------

            // Any additional arguments passed to:
            //
            // java -jar server.jar ARG1 ARG2
            //
            // are forwarded to Terraria.

            Collections.addAll(command, args);

            System.out.println("Command:");

            for (String argument : command) {
                System.out.println("  " + argument);
            }

            System.out.println();

            ProcessBuilder processBuilder =
                    new ProcessBuilder(command);

            // Terraria working directory
            processBuilder.directory(
                    jarDirectory.toFile()
            );

            // ----------------------------------------
            // Terraria I/O
            // ----------------------------------------

            // stdout/stderr go straight to the console.
            // stdin stays a pipe so we can send it commands.

            processBuilder.redirectOutput(
                    ProcessBuilder.Redirect.INHERIT
            );

            processBuilder.redirectError(
                    ProcessBuilder.Redirect.INHERIT
            );

            Process process =
                    processBuilder.start();

            final boolean[] manualShutdown = { false };

            OutputStream terrariaStdin =
                    process.getOutputStream();

            PrintWriter terrariaWriter =
                    new PrintWriter(terrariaStdin, true);

            // ----------------------------------------
            // Forward console input to Terraria
            // ----------------------------------------

            Thread stdinForwarder = new Thread(() -> {

                try {

                    BufferedReader reader =
                            new BufferedReader(
                                    new InputStreamReader(System.in)
                            );

                    String line;

                    while ((line = reader.readLine()) != null) {

                        if (line.trim().equalsIgnoreCase("exit")) {
                            manualShutdown[0] = true;
                        }

                        terrariaWriter.println(line);

                        if (!process.isAlive()) {
                            break;
                        }
                    }

                } catch (IOException e) {
                    // stdin closed, nothing left to forward.
                }
            });

            stdinForwarder.setDaemon(true);
            stdinForwarder.start();

            // ----------------------------------------
            // Shutdown hook
            // ----------------------------------------

            // Stop Terraria gracefully when Java is stopped,
            // instead of killing the process right away.

            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {

                        if (!process.isAlive()) {
                            return;
                        }

                        System.out.println(
                                "Stopping Terraria gracefully..."
                        );

                        manualShutdown[0] = true;

                        terrariaWriter.println("exit");

                        try {

                            boolean stopped =
                                    process.waitFor(
                                            15,
                                            TimeUnit.SECONDS
                                    );

                            if (!stopped) {

                                System.out.println(
                                        "Terraria did not stop in time, forcing shutdown..."
                                );

                                process.destroy();
                            }

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            process.destroy();
                        }
                    })
            );

            int rawExitCode =
                    process.waitFor();

            int finalExitCode = rawExitCode;

            // A manual exit always reports code 0, so an
            // external restart policy doesn't treat it as
            // a crash.

            if (manualShutdown[0]) {
                finalExitCode = 0;
            }

            System.out.println();

            System.out.println(
                    "Terraria exited with code: " + rawExitCode
            );

            if (manualShutdown[0]) {
                System.out.println(
                        "Manual shutdown, reporting exit code 0."
                );
            }

            System.exit(finalExitCode);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.err.println(
                    "ERROR: Process interrupted."
            );

            System.exit(1);

        } catch (AccessDeniedException e) {

            System.err.println(
                    "ERROR: Permission denied for file: "
                            + e.getFile()
            );

            System.err.println(
                    "Check read/write permissions for that path."
            );

            System.exit(1);

        } catch (NoSuchFileException e) {

            System.err.println(
                    "ERROR: File not found: " + e.getFile()
            );

            System.exit(1);

        } catch (IOException e) {

            System.err.println(
                    "ERROR: I/O error: " + e.getMessage()
            );

            System.exit(1);

        } catch (Exception e) {

            System.err.println(
                    "ERROR: Unexpected error: " + e.getMessage()
            );

            e.printStackTrace();

            System.exit(1);
        }
    }

    // ----------------------------------------
    // Get JAR directory
    // ----------------------------------------

    private static Path getJarDirectory()
            throws Exception {

        Path location =
                Paths.get(
                        Main.class
                                .getProtectionDomain()
                                .getCodeSource()
                                .getLocation()
                                .toURI()
                );

        if (Files.isRegularFile(location)) {
            return location
                    .getParent()
                    .toAbsolutePath()
                    .normalize();
        }

        return location
                .toAbsolutePath()
                .normalize();
    }

    // ----------------------------------------
    // chmod +x
    // ----------------------------------------

    private static void makeExecutable(
            Path file
    ) throws IOException, InterruptedException {

        try {

            Set<PosixFilePermission> permissions =
                    Files.getPosixFilePermissions(file);

            permissions.add(
                    PosixFilePermission.OWNER_EXECUTE
            );

            permissions.add(
                    PosixFilePermission.GROUP_EXECUTE
            );

            permissions.add(
                    PosixFilePermission.OTHERS_EXECUTE
            );

            Files.setPosixFilePermissions(
                    file,
                    permissions
            );

            System.out.println(
                    "OK: Executable permission set."
            );

        } catch (UnsupportedOperationException e) {

            System.out.println(
                    "Using chmod +x..."
            );

            Process chmod =
                    new ProcessBuilder(
                            "chmod",
                            "+x",
                            file.toAbsolutePath().toString()
                    )
                            .inheritIO()
                            .start();

            int exitCode =
                    chmod.waitFor();

            if (exitCode != 0) {

                throw new IOException(
                        "chmod +x failed."
                );
            }

            System.out.println(
                    "OK: chmod +x successful."
            );
        }
    }
}
