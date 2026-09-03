import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

public class Main {

    private static final String TERRARIA_BINARY =
            "TerrariaServer.bin.x86_64";

    public static void main(String[] args) {
        try {
            Path jarDirectory = getJarDirectory();
            Path terraria = jarDirectory.resolve(TERRARIA_BINARY);

            System.out.println("=================================");
            System.out.println(" Terraria Java Wrapper");
            System.out.println("=================================");
            System.out.println("Working directory: " + jarDirectory);
            System.out.println("Terraria binary:   " + terraria);

            if (!Files.exists(terraria)) {
                System.err.println();
                System.err.println("ERROR: TerrariaServer.bin.x86_64 not found!");
                System.err.println("Place it next to server.jar.");
                System.exit(1);
            }

            if (!Files.isRegularFile(terraria)) {
                System.err.println("ERROR: Terraria binary is not a regular file.");
                System.exit(1);
            }

            makeExecutable(terraria);

            List<String> command = new ArrayList<>();
            command.add(terraria.toAbsolutePath().toString());

            // Forward all arguments:
            // java -jar server.jar -config serverconfig.txt
            // becomes:
            // TerrariaServer.bin.x86_64 -config serverconfig.txt
            Collections.addAll(command, args);

            System.out.println();
            System.out.println("Starting Terraria Server...");
            System.out.println("Arguments: " + String.join(" ", args));
            System.out.println();

            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Terraria runs from the same directory as server.jar
            processBuilder.directory(jarDirectory.toFile());

            // stdin -> Terraria
            // stdout -> Console
            // stderr -> Console
            processBuilder.inheritIO();

            Process process = processBuilder.start();

            // Try to stop Terraria when Java is terminated
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroy();
                }
            }));

            int exitCode = process.waitFor();

            System.out.println();
            System.out.println("Terraria Server exited.");
            System.out.println("Exit code: " + exitCode);

            System.exit(exitCode);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("ERROR: Process interrupted.");
            System.exit(1);

        } catch (IOException e) {
            System.err.println("ERROR: Could not start Terraria Server.");
            e.printStackTrace();
            System.exit(1);

        } catch (Exception e) {
            System.err.println("ERROR:");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Path getJarDirectory() throws Exception {
        Path location = Paths.get(
                Main.class
                        .getProtectionDomain()
                        .getCodeSource()
                        .getLocation()
                        .toURI()
        );

        if (Files.isRegularFile(location)) {
            return location.getParent();
        }

        return location.toAbsolutePath().normalize();
    }

    private static void makeExecutable(Path file) throws IOException, InterruptedException {

        try {
            Set<PosixFilePermission> permissions =
                    Files.getPosixFilePermissions(file);

            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);

            Files.setPosixFilePermissions(file, permissions);

            System.out.println("Executable permission set.");

        } catch (UnsupportedOperationException e) {

            // Fallback to chmod
            System.out.println("Using chmod +x...");

            Process chmod = new ProcessBuilder(
                    "chmod",
                    "+x",
                    file.toAbsolutePath().toString()
            )
                    .inheritIO()
                    .start();

            int exitCode = chmod.waitFor();

            if (exitCode != 0) {
                throw new IOException(
                        "chmod +x failed with exit code " + exitCode
                );
            }
        }
    }
}
