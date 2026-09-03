# Terraria Java Wrapper

A minimal Java JAR wrapper for running TerrariaServer.bin.x86_64
inside a Java-only Linux container.

## Files

Place these files together:

server.jar
TerrariaServer.bin.x86_64
serverconfig.txt

## Usage

java -jar server.jar -config serverconfig.txt

The wrapper:

1. Finds TerrariaServer.bin.x86_64 next to the JAR.
2. Gives it executable permission.
3. Starts it using Java ProcessBuilder.
4. Forwards stdin/stdout/stderr.
5. Forwards all command-line arguments.
6. Uses the JAR directory as Terraria's working directory.
7. On "exit" or shutdown, saves and stops Terraria gracefully
   before the JVM exits (falls back to a forced kill after
   15 seconds if it doesn't stop in time).
8. Reports exit code 0 for a manual/graceful shutdown, and
   Terraria's own exit code otherwise, so restart policies
   (Docker/systemd) don't treat a normal stop as a crash.

## Notes

- On Pterodactyl, use the panel's Stop button rather than
  typing "exit" manually in the console. Wings only knows a
  shutdown was intentional when it initiates it itself, so a
  manually typed "exit" can still get logged as a crash even
  though Terraria saved and exited normally.
- A forced Kill (SIGKILL) cannot be intercepted by any
  program, including this wrapper. If Terraria is killed this
  way, it won't get a chance to save.

## Legal

This repository only contains the wrapper source code. It
does not include or distribute TerrariaServer.bin.x86_64 or
any other Terraria game files. You need your own legitimate
copy of Terraria to use this project.
