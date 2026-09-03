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
            System.out.println("Directory: " + jarDirectory);

            if (!Files.exists(terraria)) {
                System.err.println("ERROR: TerrariaServer.bin.x86_64 not found!");
                System.exit(1);
            }

            makeExecutable(terraria);

            List<String> command = new ArrayList<>();
            command.add(terraria.toAbsolutePath().toString());

            Collections.addAll(command, args);

            // إذا تم تمرير -config نتأكد أن الملف موجود
            for (int i = 0; i < args.length - 1; i++) {
                if (args[i].equals("-config")) {
                    Path config = jarDirectory.resolve(args[i + 1]);

                    System.out.println("Config file: " + config);

                    if (!Files.exists(config)) {
                        System.err.println(
                                "ERROR: Config file not found: "
                                        + config
                        );
                        System.exit(1);
                    }

                    System.out.println("Config file found.");
                }
            }

            System.out.println();
            System.out.println("Starting Terraria...");
            System.out.println("Command:");

            for (String arg : command) {
                System.out.println("  " + arg);
            }

            System.out.println();

            ProcessBuilder pb = new ProcessBuilder(command);

            // مهم جدًا:
            // Terraria سيعمل من نفس مجلد server.jar
            pb.directory(jarDirectory.toFile());

            // نقل Console input/output
            pb.inheritIO();

            Process process = pb.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    process.destroy();
                }
            }));

            int exitCode = process.waitFor();

            System.out.println(
                    "Terraria exited with code: " + exitCode
            );

            System.exit(exitCode);

        } catch (Exception e) {
            System.err.println("Failed to start Terraria:");
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

    private static void makeExecutable(Path file)
            throws IOException, InterruptedException {

        try {
            Set<PosixFilePermission> permissions =
                    Files.getPosixFilePermissions(file);

            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);

            Files.setPosixFilePermissions(file, permissions);

        } catch (UnsupportedOperationException e) {

            Process chmod = new ProcessBuilder(
                    "chmod",
                    "+x",
                    file.toAbsolutePath().toString()
            )
                    .inheritIO()
                    .start();

            int exitCode = chmod.waitFor();

            if (exitCode != 0) {
                throw new IOException("chmod failed");
            }
        }
    }
}
