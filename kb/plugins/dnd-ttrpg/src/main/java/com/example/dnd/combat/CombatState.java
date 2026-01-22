package com.example.dnd.combat;

import java.util.*;

/**
 * Tracks the state of an active combat encounter.
 */
public class CombatState {
    private final List<UUID> initiativeOrder = new ArrayList<>();
    private final Map<UUID, Integer> initiativeRolls = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    private int currentTurnIndex = 0;
    private TurnPhase currentPhase = TurnPhase.MOVEMENT;
    private boolean combatActive = false;

    // Per-turn resources
    private boolean actionUsed = false;
    private boolean bonusActionUsed = false;
    private int movementRemaining = 30;

    /**
     * Add a player to initiative.
     */
    public void addToInitiative(UUID playerId, String playerName, int roll) {
        initiativeRolls.put(playerId, roll);
        playerNames.put(playerId, playerName);
        rebuildInitiativeOrder();
    }

    /**
     * Remove a player from initiative.
     */
    public void removeFromInitiative(UUID playerId) {
        initiativeRolls.remove(playerId);
        playerNames.remove(playerId);
        rebuildInitiativeOrder();
    }

    /**
     * Rebuild the initiative order (sorted by roll, highest first).
     */
    private void rebuildInitiativeOrder() {
        initiativeOrder.clear();
        initiativeRolls.entrySet().stream()
            .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
            .forEach(e -> initiativeOrder.add(e.getKey()));
    }

    /**
     * Get the current player's UUID.
     */
    public UUID getCurrentPlayer() {
        if (!combatActive || initiativeOrder.isEmpty()) {
            return null;
        }
        return initiativeOrder.get(currentTurnIndex);
    }

    /**
     * Get the current player's name.
     */
    public String getCurrentPlayerName() {
        UUID current = getCurrentPlayer();
        return current != null ? playerNames.getOrDefault(current, "Unknown") : "None";
    }

    /**
     * Advance to the next turn.
     */
    public void nextTurn() {
        if (initiativeOrder.isEmpty()) return;
        currentTurnIndex = (currentTurnIndex + 1) % initiativeOrder.size();
        resetTurnResources();
    }

    /**
     * Reset resources for a new turn.
     */
    private void resetTurnResources() {
        currentPhase = TurnPhase.MOVEMENT;
        actionUsed = false;
        bonusActionUsed = false;
        movementRemaining = 30;
    }

    /**
     * Start combat.
     */
    public void startCombat() {
        if (initiativeOrder.isEmpty()) return;
        combatActive = true;
        currentTurnIndex = 0;
        resetTurnResources();
    }

    /**
     * End combat.
     */
    public void endCombat() {
        combatActive = false;
    }

    /**
     * Clear all initiative data.
     */
    public void clear() {
        initiativeOrder.clear();
        initiativeRolls.clear();
        playerNames.clear();
        currentTurnIndex = 0;
        combatActive = false;
    }

    /**
     * Check if it's a specific player's turn.
     */
    public boolean isPlayerTurn(UUID playerId) {
        return combatActive && playerId.equals(getCurrentPlayer());
    }

    // Getters
    public boolean isCombatActive() { return combatActive; }
    public List<UUID> getInitiativeOrder() { return Collections.unmodifiableList(initiativeOrder); }
    public Map<UUID, Integer> getInitiativeRolls() { return Collections.unmodifiableMap(initiativeRolls); }
    public Map<UUID, String> getPlayerNames() { return Collections.unmodifiableMap(playerNames); }
    public TurnPhase getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(TurnPhase phase) { this.currentPhase = phase; }
    public boolean isActionUsed() { return actionUsed; }
    public void setActionUsed(boolean used) { this.actionUsed = used; }
    public boolean isBonusActionUsed() { return bonusActionUsed; }
    public void setBonusActionUsed(boolean used) { this.bonusActionUsed = used; }
    public int getMovementRemaining() { return movementRemaining; }
    public void setMovementRemaining(int movement) { this.movementRemaining = movement; }

    /**
     * Get initiative info as a formatted string.
     */
    public String getInitiativeListString() {
        if (initiativeOrder.isEmpty()) {
            return "No combatants in initiative.";
        }
        StringBuilder sb = new StringBuilder("Initiative Order:\n");
        for (int i = 0; i < initiativeOrder.size(); i++) {
            UUID id = initiativeOrder.get(i);
            String name = playerNames.get(id);
            int roll = initiativeRolls.get(id);
            String marker = (combatActive && i == currentTurnIndex) ? " <-- CURRENT" : "";
            sb.append(String.format("%d. %s (%d)%s\n", i + 1, name, roll, marker));
        }
        return sb.toString();
    }
}
