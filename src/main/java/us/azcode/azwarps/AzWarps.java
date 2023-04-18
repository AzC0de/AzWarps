package us.azcode.azwarps;

import net.milkbowl.vault.economy.Economy;
import us.azcode.azwarps.commands.WarpCommand;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class AzWarps extends JavaPlugin {
    private Economy econ = null;
    private File messagesFile;
    private FileConfiguration messages;
    private File warpsFile;
    private FileConfiguration warps;

    @Override
    public void onEnable() {
        // Comprueba si Vault está presente
        if (!setupEconomy()) {
            getLogger().severe("Vault no se encuentra. Deshabilitando WarpShop.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Carga la configuración y registra los comandos
        saveDefaultConfig();
        createMessagesFile();
        createWarpsFile();
        WarpCommand commandExecutor = new WarpCommand(this);
        getCommand("setpwarp").setExecutor(commandExecutor);
        getCommand("delpwarp").setExecutor(commandExecutor);
        getCommand("playerwarp").setExecutor(commandExecutor);
        getCommand("buywarp").setExecutor(commandExecutor);
        getCommand("listwarps").setExecutor(commandExecutor);
        getCommand("addwarpaccess").setExecutor(commandExecutor);
        getCommand("setwarpaccess").setExecutor(commandExecutor);
        getCommand("listpublicwarps").setExecutor(commandExecutor);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public Economy getEconomy() {
        return econ;
    }

    private void createMessagesFile() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            messagesFile.getParentFile().mkdirs();
            saveResource("messages.yml", false);
        }

        messages = new YamlConfiguration();
        try {
            messages.load(messagesFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    private void createWarpsFile() {
        warpsFile = new File(getDataFolder(), "warps.yml");
        if (!warpsFile.exists()) {
            warpsFile.getParentFile().mkdirs();
            saveResource("warps.yml", false);
        }

        warps = new YamlConfiguration();
        try {
            warps.load(warpsFile);
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getWarps() {
        return warps;
    }

    public void saveWarps() {
        try {
            warps.save(warpsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
