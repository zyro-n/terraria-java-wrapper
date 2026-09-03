import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

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

            // Connect Terraria stdin/stdout/stderr
            // directly to the container console.
            processBuilder.inheritIO();

            Process process =
                    processBuilder.start();

            // Stop Terraria when Java is stopped.
            Runtime.getRuntime().addShutdownHook(
                    new Thread(() -> {

                        if (process.isAlive()) {

                            System.out.println(
                                    "Stopping Terraria..."
                            );

                            process.destroy();
                        }
                    })
            );

            int exitCode =
                    process.waitFor();

            System.out.println();
            System.out.println(
                    "Terraria exited with code: "
                            + exitCode
            );

            System.exit(exitCode);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            System.err.println(
                    "ERROR: Process interrupted."
            );

            System.exit(1);

        } catch (IOException e) {

            System.err.println(
                    "ERROR: I/O error."
            );

            e.printStackTrace();

            System.exit(1);

        } catch (Exception e) {

            System.err.println(
                    "ERROR: Unexpected error."
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
