package io.github.kaivian.kupdater.features.tools.common.economy;

import org.bukkit.entity.Player;

/**
 * Economy provider abstraction isolating vault/economy integration details.
 */
public interface EconomyProvider {

    /**
     * Checks if economy service provider is available.
     *
     * @return true if available
     */
    boolean isAvailable();

    /**
     * Gets currency balance for player.
     *
     * @param player player
     * @return current balance
     */
    double getBalance(Player player);

    /**
     * Checks if player has sufficient funds.
     *
     * @param player player
     * @param amount required amount
     * @return true if player has at least amount
     */
    boolean has(Player player, double amount);

    /**
     * Withdraws currency from player.
     *
     * @param player player
     * @param amount amount to withdraw
     * @return true if successful
     */
    boolean withdraw(Player player, double amount);

    /**
     * Deposits currency to player (used for compensation rollback).
     *
     * @param player player
     * @param amount amount to deposit
     * @return true if successful
     */
    boolean deposit(Player player, double amount);
}
