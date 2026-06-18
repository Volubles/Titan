# Development workflow

## First import

Open this directory as a Gradle project in IntelliJ IDEA and select JDK 21 for
the Gradle JVM. The development server path is configured in
`gradle.properties`.

## Start and debug

Select the shared **TitanMC Server** run configuration and press **Debug**. Its
Gradle before-launch task builds the shaded plugin and copies it into the
server's `plugins` directory before the server starts.

## HotSwap method changes

1. Keep the server running under **Debug**.
2. Change the body of an existing Java method.
3. Use **Run > Debugging Actions > Reload Changed Classes**. IntelliJ compiles
   the changed class and asks the JVM to replace it in the running server.

Standard JVM HotSwap handles method-body changes. Restart the server after
adding or removing fields or methods, changing method signatures, modifying
inheritance, or changing startup logic such as listener registration in
`onEnable()`.

Do not use the Minecraft `/reload` command for this workflow. It reloads whole
plugin class loaders and can leave listeners, tasks, and static state behind.

## Useful Gradle commands

```powershell
.\gradlew.bat clean build
.\gradlew.bat deployPlugin
.\gradlew.bat classes
```

`deployPlugin` rebuilds and installs the complete plugin. `classes` is enough
when IntelliJ only needs freshly compiled class files for debugger HotSwap.
