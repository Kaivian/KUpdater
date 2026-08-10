package io.github.kaivian.kupdater.features.tools.common.economy;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Economy provider adapter using reflection to safely interact with Vault Economy API without compile-time hard coupling.
 */
public class VaultEconomyAdapter implements EconomyProvider {

    private final Logger logger;
    private Object vaultEconomy = null;

    private Method getBalanceMethod;
    private Method hasMethod;
    private Method withdrawMethod;
    private Method depositMethod;

    public VaultEconomyAdapter(Logger logger) {
        this.logger = logger != null ? logger : Logger.getLogger("VaultEconomyAdapter");
        initVault();
    }

    private void initVault() {
        if (Bukkit.getServer() == null || Bukkit.getPluginManager() == null) {
            return;
        }

        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            return;
        }

        try {
            Class<?> econClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServer().getServicesManager().getRegistration(econClass);
            if (rsp != null) {
                this.vaultEconomy = rsp.getProvider();
                this.getBalanceMethod = econClass.getMethod("getBalance", org.bukkit.OfflinePlayer.class);
                this.hasMethod = econClass.getMethod("has", org.bukkit.OfflinePlayer.class, double.class);
                this.withdrawMethod = econClass.getMethod("withdrawPlayer", org.bukkit.OfflinePlayer.class, double.class);
                this.depositMethod = econClass.getMethod("depositPlayer", org.bukkit.OfflinePlayer.class, double.class);
                logger.info("[VaultEconomyAdapter] Successfully linked Vault Economy provider.");
            }
        } catch (Throwable t) {
            logger.fine("[VaultEconomyAdapter] Vault economy registration not found: " + t.getMessage());
            this.vaultEconomy = null;
        }
    }

    @Override
    public boolean isAvailable() {
        if (vaultEconomy == null) {
            initVault();
        }
        return vaultEconomy != null;
    }

    @Override
    public double getBalance(Player player) {
        if (player == null || !isAvailable()) return 0.0;
        try {
            Object res = getBalanceMethod.invoke(vaultEconomy, player);
            return res instanceof Number ? ((Number) res).doubleValue() : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    @Override
    public boolean has(Player player, double amount) {
        if (player == null || !isAvailable()) return false;
        if (amount <= 0.0) return true;
        try {
            Object res = hasMethod.invoke(vaultEconomy, player, amount);
            return Boolean.TRUE.equals(res);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        if (player == null || !isAvailable()) return false;
        if (amount <= 0.0) return true;
        try {
            Object response = withdrawMethod.invoke(vaultEconomy, player, amount);
            if (response != null) {
                Method transactionSuccessMethod = response.getClass().getMethod("transactionSuccess");
                Object success = transactionSuccessMethod.invoke(response);
                return Boolean.TRUE.equals(success);
            }
        } catch (Exception e) {
            logger.warning("[VaultEconomyAdapter] Failed to withdraw " + amount + " from player " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deposit(Player player, double amount) {
        if (player == null || !isAvailable()) return false;
        if (amount <= 0.0) return true;
        try {
            Object response = depositMethod.invoke(vaultEconomy, player, amount);
            if (response != null) {
                Method transactionSuccessMethod = response.getClass().getMethod("transactionSuccess");
                Object success = transactionSuccessMethod.invoke(response);
                return Boolean.TRUE.equals(success);
            }
        } catch (Exception e) {
            logger.warning("[VaultEconomyAdapter] Failed to deposit " + amount + " to player " + player.getName() + ": " + e.getMessage());
        }
        return false;
    }
}
