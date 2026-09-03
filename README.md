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
