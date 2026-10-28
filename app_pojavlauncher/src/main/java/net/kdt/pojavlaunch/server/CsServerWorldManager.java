package net.kdt.pojavlaunch.server;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

public class CsServerWorldManager {

    public List<String> listServerWorlds(CsServerConfig config) {
        File dir = new File(config.serverDir);
        List<String> worlds = new ArrayList<>();
        File[] kids = dir.listFiles(File::isDirectory);
        if (kids == null) return worlds;
        for (File f : kids) {
            if (new File(f, "level.dat").exists() || new File(f, "session.lock").exists()) {
                worlds.add(f.getName());
            }
        }
        return worlds;
    }

    public List<File> listLauncherSaves() {
        File saves = new File(Tools.DIR_GAME_NEW, "saves");
        List<File> list = new ArrayList<>();
        if (!saves.isDirectory()) return list;
        File[] kids = saves.listFiles(File::isDirectory);
        if (kids != null) {
            for (File f : kids) list.add(f);
        }
        return list;
    }

    public File importWorldToServer(CsServerConfig config, File source, String targetName) throws IOException {
        File dest = new File(config.serverDir, targetName);
        if (dest.exists()) {
            throw new IOException("World already exists: " + targetName);
        }
        copyRecursive(source.toPath(), dest.toPath());
        return dest;
    }

    public void setActiveWorld(CsServerConfig config, String worldName) throws IOException {
        File world = new File(config.serverDir, worldName);
        if (!world.isDirectory()) throw new IOException("World not found: " + worldName);
        config.worldDir = worldName;
        CsServerPropertiesWriter.applyConfig(new File(config.serverDir, "server.properties"), config);
    }

    private void copyRecursive(Path src, Path dest) throws IOException {
        Files.walkFileTree(src, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path target = dest.resolve(src.relativize(dir));
                if (!Files.exists(target)) Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, dest.resolve(src.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
