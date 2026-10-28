package net.kdt.pojavlaunch.server;

import android.content.Context;

import java.io.File;
import java.util.List;

/**
 * Facade for on-device server create / install / start / stop / plugins / worlds.
 * Auto-resolves MultiRT Java path when starting.
 */
public class CsServerManager {

    private final Context context;
    private final CsServerStore store;
    private final CsServerProcessManager processManager;
    private final CsServerRamAdvisor ramAdvisor;
    private final CsVanillaServerInstaller vanillaInstaller = new CsVanillaServerInstaller();
    private final CsPaperServerInstaller paperInstaller = new CsPaperServerInstaller();
    private final CsFabricServerInstaller fabricInstaller = new CsFabricServerInstaller();
    private final CsPurpurServerInstaller purpurInstaller = new CsPurpurServerInstaller();
    private final CsForgeServerInstaller forgeInstaller = new CsForgeServerInstaller();
    private final CsNeoForgeServerInstaller neoForgeInstaller = new CsNeoForgeServerInstaller();
    private final CsQuiltServerInstaller quiltInstaller = new CsQuiltServerInstaller();
    private final CsViaVersionInstaller viaVersionInstaller = new CsViaVersionInstaller();
    private final CsLanAddressHelper lanAddressHelper;
    private final CsServerPluginManager pluginManager = new CsServerPluginManager();
    private final CsServerWorldManager worldManager = new CsServerWorldManager();

    public CsServerManager(Context context) {
        this.context = context.getApplicationContext();
        this.store = new CsServerStore(this.context);
        this.ramAdvisor = new CsServerRamAdvisor(this.context);
        this.processManager = new CsServerProcessManager(ramAdvisor);
        this.lanAddressHelper = new CsLanAddressHelper(this.context);
        store.load();
    }

    public CsServerStore getStore() { return store; }
    public CsServerProcessManager getProcessManager() { return processManager; }
    public CsServerRamAdvisor getRamAdvisor() { return ramAdvisor; }

    public List<CsServerConfig> getServers() { return store.getAll(); }

    public CsLanAddressHelper.LanInfo lanInfo(int port) {
        return lanAddressHelper.currentLanInfo(port);
    }

    public CsServerConfig createServer(String name, String minecraftVersion, CsServerSoftware software,
                                       int memoryMb, int port, String motd, boolean eulaAccepted) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name required");
        }
        if (!ramAdvisor.isAllocationSafe(memoryMb)) {
            throw new IllegalArgumentException("Unsafe RAM allocation: " + memoryMb + " MB");
        }
        String id = store.createId();
        File dir = store.defaultServerDir(id);
        CsServerConfig config = new CsServerConfig();
        config.id = id;
        config.name = name.trim();
        config.motd = motd != null ? motd : "A CS Launcher Minecraft Server";
        config.minecraftVersion = minecraftVersion;
        config.software = software.name();
        config.serverDir = dir.getAbsolutePath();
        config.port = port;
        config.memoryMb = memoryMb;
        config.eulaAccepted = eulaAccepted;
        store.save(config);
        return config;
    }

    public static class InstallOutcome {
        public final boolean success;
        public final String message;
        public final File jar;

        public InstallOutcome(boolean success, String message, File jar) {
            this.success = success;
            this.message = message;
            this.jar = jar;
        }
    }

    public InstallOutcome installSoftware(CsServerConfig config, boolean installViaVersion) {
        InstallOutcome base;
        String javaBin = CsJavaPathResolver.resolveForMinecraftVersion(config.minecraftVersion);

        switch (config.softwareEnum()) {
            case PAPER: {
                CsPaperServerInstaller.Result pr = paperInstaller.install(config);
                base = new InstallOutcome(pr.success,
                        pr.error != null ? pr.error : "Paper build " + pr.buildNumber + " installed",
                        pr.serverJar);
                break;
            }
            case PURPUR: {
                CsPurpurServerInstaller.Result ur = purpurInstaller.install(config);
                base = new InstallOutcome(ur.success, ur.message, ur.serverJar);
                break;
            }
            case FABRIC: {
                CsFabricServerInstaller.Result fr = fabricInstaller.install(config);
                base = new InstallOutcome(fr.success,
                        fr.error != null ? fr.error : "Fabric loader " + fr.loaderVersion + " installed",
                        fr.serverJar);
                break;
            }
            case QUILT: {
                CsQuiltServerInstaller.Result qr = quiltInstaller.install(config);
                base = new InstallOutcome(qr.success, qr.message, qr.serverJar);
                break;
            }
            case FORGE: {
                if (javaBin == null) {
                    base = new InstallOutcome(false, CsJavaPathResolver.missingRuntimeMessage(config.minecraftVersion), null);
                } else {
                    CsForgeServerInstaller.Result r = forgeInstaller.install(config, javaBin);
                    base = new InstallOutcome(r.success, r.message, new File(config.serverDir, "server.jar"));
                }
                break;
            }
            case NEOFORGE: {
                if (javaBin == null) {
                    base = new InstallOutcome(false, CsJavaPathResolver.missingRuntimeMessage(config.minecraftVersion), null);
                } else {
                    CsNeoForgeServerInstaller.Result r = neoForgeInstaller.install(config, javaBin);
                    base = new InstallOutcome(r.success, r.message, new File(config.serverDir, "server.jar"));
                }
                break;
            }
            case SPIGOT:
                base = new InstallOutcome(false,
                        "Spigot needs BuildTools (heavy on phone). Use Paper for the same API.", null);
                break;
            case VANILLA:
            default: {
                CsVanillaServerInstaller.Result vr = vanillaInstaller.install(config);
                base = new InstallOutcome(vr.success,
                        vr.error != null ? vr.error : "Vanilla installed",
                        vr.serverJar);
                break;
            }
        }

        if (base.success && installViaVersion) {
            CsServerSoftware sw = config.softwareEnum();
            if (sw == CsServerSoftware.PAPER || sw == CsServerSoftware.PURPUR || sw == CsServerSoftware.SPIGOT) {
                CsViaVersionInstaller.Result via = viaVersionInstaller.installForCrossVersion(config);
                return new InstallOutcome(base.success, base.message + " | " + via.message, base.jar);
            }
        }
        return base;
    }

    public CsViaVersionInstaller.Result installViaVersion(String serverId) {
        CsServerConfig config = store.getById(serverId);
        if (config == null) return new CsViaVersionInstaller.Result(false, "Server not found");
        return viaVersionInstaller.installForCrossVersion(config);
    }

    public CsServerConfig acceptEula(String serverId) {
        CsServerConfig config = store.getById(serverId);
        if (config == null) throw new IllegalArgumentException("Server not found");
        config.eulaAccepted = true;
        try {
            File eula = new File(config.serverDir, "eula.txt");
            java.nio.file.Files.write(eula.toPath(),
                    "# Accepted in CS Launcher\neula=true\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        store.save(config);
        return config;
    }

    public CsServerConfig updateSettings(CsServerConfig config) {
        CsServerPropertiesWriter.applyConfig(new File(config.serverDir, "server.properties"), config);
        store.save(config);
        return config;
    }

    /**
     * Start with explicit java binary path.
     * @return null on success, error message on failure
     */
    public String startServer(String serverId, String javaBin) {
        CsServerConfig config = store.getById(serverId);
        if (config == null) return "Server not found";
        return processManager.start(config, javaBin);
    }

    /**
     * Auto-resolve MultiRT Java for this server's MC version, then start.
     * @return null on success, error message on failure
     */
    public String startServerAuto(String serverId) {
        CsServerConfig config = store.getById(serverId);
        if (config == null) return "Server not found";
        String javaBin = CsJavaPathResolver.resolveForMinecraftVersion(config.minecraftVersion);
        if (javaBin == null) {
            return CsJavaPathResolver.missingRuntimeMessage(config.minecraftVersion);
        }
        return processManager.start(config, javaBin);
    }

    public void stopServer(String serverId, boolean force) {
        processManager.stop(serverId, force);
    }

    public boolean sendCommand(String serverId, String command) {
        return processManager.sendCommand(serverId, command);
    }

    public void deleteServer(String serverId, boolean deleteFiles) {
        processManager.stop(serverId, true);
        CsServerConfig config = store.getById(serverId);
        store.delete(serverId);
        if (deleteFiles && config != null) {
            deleteRecursive(new File(config.serverDir));
        }
    }

    public CsServerRuntimeStatus statusOf(String serverId) {
        return processManager.getStatus(serverId);
    }

    public List<String> listPlugins(String serverId) {
        CsServerConfig c = store.getById(serverId);
        return c != null ? pluginManager.list(c) : java.util.Collections.emptyList();
    }

    public List<String> listWorlds(String serverId) {
        CsServerConfig c = store.getById(serverId);
        return c != null ? worldManager.listServerWorlds(c) : java.util.Collections.emptyList();
    }

    public CsServerPluginManager getPluginManager() { return pluginManager; }
    public CsServerWorldManager getWorldManager() { return worldManager; }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] kids = f.listFiles();
            if (kids != null) for (File k : kids) deleteRecursive(k);
        }
        //noinspection ResultOfMethodCallIgnored
        f.delete();
    }
}
