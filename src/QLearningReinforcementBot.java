import org.dreambot.api.methods.container.impl.Inventory;
import org.dreambot.api.methods.container.impl.bank.Bank;
import org.dreambot.api.methods.interactive.GameObjects;
import org.dreambot.api.methods.interactive.Players;
import org.dreambot.api.methods.map.Area;
import org.dreambot.api.methods.walking.impl.Walking;
import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;
import org.dreambot.api.utilities.Sleep;
import org.dreambot.api.wrappers.interactive.GameObject;
import org.dreambot.api.wrappers.items.Item;

import java.awt.*;
import java.io.IOException;

@ScriptManifest(category = Category.UTILITY, name = "Q Learning mining bot", author = "Deep Slayer", version = 0.1)
public class QLearningReinforcementBot extends AbstractScript {
    private static final String Q_TABLE_FILE_PATH = "D:\\QLearningSaveFile.dat";
    private LearningAgent agent;
    private State currentState;
    private Action currentAction;
    double reward;
    private final Action[] actions = Action.values();
    private final Area MINING_AREA1 = new Area(3174, 3368, 3176, 3366, 0);
    private final Area MINING_AREA2 = new Area(3284, 3371, 3288, 3367, 0);
    boolean receivedOre;

    @Override
    public void onStart() {
        try {
            agent = LearningAgent.loadQTable(Q_TABLE_FILE_PATH);
            log("Loaded Q-table.");
        } catch (IOException | ClassNotFoundException e) {
            log("Q-table not found, starting fresh.");

            int numberOfStates = State.values().length;
            int numberOfActions = actions.length;
            agent = new LearningAgent(numberOfStates, numberOfActions);
        }
        currentState = State.IN_BANK; //start the bot with the bank screen open and a pickaxe in inventory
        // agent.setEpsilon(0.05); // Manually set epsilon to the minimum value for testing
    }

    @Override
    public int onLoop() {
        int currentStateIndex = currentState.ordinal();
        int actionIndex = agent.chooseAction(currentStateIndex);

        if (currentStateIndex >= State.values().length || actionIndex >= actions.length) {
            log("Invalid state or action index.");
            return 100;
        }

        log("Current State: " + currentState + ", Action: " + actions[actionIndex]);

        performAction(actions[actionIndex]);

        int nextStateIndex = getState().ordinal();
        double reward = getReward(currentState, actions[actionIndex], nextStateIndex);

        log("Next State: " + State.values()[nextStateIndex] + ", Reward: " + reward);

        agent.updateQValue(currentStateIndex, actionIndex, nextStateIndex, reward);

        currentState = State.values()[nextStateIndex];
        log("---------------------------------------------------------------------------------------------------");
        return 100;
    }

    @Override
    public void onExit() {
        try {
            agent.saveQTable(Q_TABLE_FILE_PATH);
            log("Saved Q-table to " + Q_TABLE_FILE_PATH);
        } catch (IOException e) {
            log("Failed to save Q-table: " + e.getMessage());
        }
    }

    private void performAction(Action action) {
        currentAction = action;
        switch (action) {
            case MINE_ORE:
                mineOre();
                break;
            case WALK_SPOT1:
                walkToMiningSpot1();
                break;
            case WALK_SPOT2:
                walkToMiningSpot2();
                break;
            case WALK_TO_BANK:
                walkToBank();
                break;
            case BANK_ORES:
                bankOres();
                break;
        }
    }

    private void bankOres() {
        if (Bank.isOpen()) {
            Item pickaxe = Inventory.get(item -> item != null && item.getName().contains("pickaxe"));
            Bank.depositAllExcept(pickaxe.getName());
            Sleep.sleepUntil(() -> Inventory.onlyContains(pickaxe.getName()), 5000);
            Bank.close();
        }
    }

    private void walkToBank() {
        if (!Bank.isOpen()) {
            Bank.open();
            sleep(2000);
            Sleep.sleepUntil(() -> !Players.getLocal().isMoving(), 10000);
        }
    }

    private void walkToMiningSpot2() {
        Walking.walk(MINING_AREA2);
        sleep(2000);
        Sleep.sleepUntil(() -> !Players.getLocal().isMoving(), 10000);
    }

    private void walkToMiningSpot1() {
        Walking.walk(MINING_AREA1);
        sleep(2000);
        Sleep.sleepUntil(() -> !Players.getLocal().isMoving(), 10000);
    }

    private void mineOre() {
        receivedOre = false;
        int initialOreCount = Inventory.count("Iron ore");
        GameObject ironore = GameObjects.closest("Iron rocks");

        if (ironore != null && ironore.distance() < 6) {
            ironore.interact("Mine");
            sleep(2000);
            Sleep.sleepUntil(() -> initialOreCount < Inventory.count("Iron ore"), 8000);
        }

        if (initialOreCount < Inventory.count("Iron ore")) {
            receivedOre = true;
            log("Successfully mined ore at: " + currentState);
        } else {
            receivedOre = false;
            log("Failed to mine ore at: " + currentState);
        }

        updateQValuesBasedOnMiningResult();
        sleep(1000);
    }

    private void updateQValuesBasedOnMiningResult() {
        double reward = receivedOre ? 1 : -25;

        if (currentState == State.AT_SPOT1) {
            // Update Q-value for walking to spot 1
            agent.updateQValue(State.WALKING_SPOT1.ordinal(), Action.WALK_SPOT1.ordinal(), State.AT_SPOT1.ordinal(), reward);
            log(receivedOre ? "Rewarding walking to spot 1 at Spot 1 due to success" : "Penalizing walking to spot 1 at Spot 1 due to failure");

            // If mining fails, reward walking to spot 2
            if (!receivedOre) {
                agent.updateQValue(State.WALKING_SPOT2.ordinal(), Action.WALK_SPOT2.ordinal(), State.AT_SPOT2.ordinal(), 2);
                log("Rewarding walking to Spot 2 due to mining failure at Spot 1");
            }
        } else if (currentState == State.AT_SPOT2) {
            // Update Q-value for walking to spot 2
            agent.updateQValue(State.WALKING_SPOT2.ordinal(), Action.WALK_SPOT2.ordinal(), State.AT_SPOT2.ordinal(), reward);
            log(receivedOre ? "Rewarding walking to spot 2 at Spot 2 due to success" : "Penalizing walking to spot 2 at Spot 2 due to failure");

            // If mining fails, reward walking to spot 1
            if (!receivedOre) {
                agent.updateQValue(State.WALKING_SPOT1.ordinal(), Action.WALK_SPOT1.ordinal(), State.AT_SPOT1.ordinal(), 2);
                log("Rewarding walking to Spot 1 due to mining failure at Spot 2");
            }
        }
    }

    private State getState() {
        if (!Inventory.isFull()) {
            State bestState = agent.getBestMiningSpotState();
            log("Best Mining state based on Q-value: " + bestState);

            if (bestState == State.WALKING_SPOT1 && !MINING_AREA1.contains(Players.getLocal())) {
                log("Choosing to walk to Spot 1");
                return State.WALKING_SPOT1;
            }

            if (bestState == State.WALKING_SPOT2 && !MINING_AREA2.contains(Players.getLocal())) {
                log("Choosing to walk to Spot 2");
                return State.WALKING_SPOT2;
            }

            if (MINING_AREA1.contains(Players.getLocal())) {
                return State.AT_SPOT1;
            }
            if (MINING_AREA2.contains(Players.getLocal())) {
                return State.AT_SPOT2;
            }
        }

        if (Inventory.isFull() && !Bank.isOpen()) {
            log("Inventory is full, walking to bank.");
            return State.WALK_BANK;
        }

        if (Bank.isOpen() && Inventory.contains("Iron ore")) {
            log("At bank and has ores, banking ores.");
            return State.IN_BANK;
        }

        // If none of the above, determine best mining spot to walk to
        if (!Inventory.isFull()) {
            State bestState = agent.getBestMiningSpotState();
            log("Best state based on Q-value for mining spot: " + bestState);

            if (bestState == State.WALKING_SPOT1) {
                log("Choosing to walk to Spot 1 as default action");
                return State.WALKING_SPOT1;
            } else if (bestState == State.WALKING_SPOT2) {
                log("Choosing to walk to Spot 2 as default action");
                return State.WALKING_SPOT2;
            }
        }

        // Default fallback, should not reach here
        log("Fallback action: walking to bank.");
        return State.WALK_BANK;
    }

    private double getReward(State state, Action action, int nextStateIndex) {
        reward = 0; // Ensure reward is zero initially

        log("Calculating reward for State: " + state + ", Action: " + action);

        switch (state) {
            case AT_SPOT1, AT_SPOT2:
                if (action == Action.MINE_ORE) {
                    reward = 2;
                    log("Reward for mining at spot: " + reward);
                } else {
                    reward = -2;
                    log("Penalty for wrong action at spot: " + reward);
                }
                break;
            case IN_BANK:
                reward = (action == Action.BANK_ORES) ? 20 : -10;
                log(action == Action.BANK_ORES ? "Reward for banking ores: " + reward : "Penalty for wrong action in bank: " + reward);
                break;
            case WALK_BANK:
                reward = (action == Action.WALK_TO_BANK) ? 1 : -5;
                log(action == Action.WALK_TO_BANK ? "Reward for walking to bank: " + reward : "Penalty for wrong action when supposed to walk to bank: " + reward);
                break;
            case WALKING_SPOT1:
                reward = (action == Action.WALK_SPOT1) ? 2 : -20;
                log(action == Action.WALK_SPOT1 ? "Reward for walking to spot 1: " + reward : "Penalty for wrong action when supposed to walk to spot 1: " + reward);
                break;
            case WALKING_SPOT2:
                reward = (action == Action.WALK_SPOT2) ? 2: -20;
                log(action == Action.WALK_SPOT2 ? "Reward for walking to spot 2: " + reward : "Penalty for wrong action when supposed to walk to spot 2: " + reward);
                break;
            default:
                reward = -0.5; // Default penalty for any undefined state-action combination
                log("Default penalty for undefined state-action combination: " + reward);
                break;
        }
        log("Final Reward: " + reward);
        log("Next State: " + State.values()[nextStateIndex] + ", Reward: " + reward);
        return reward;
    }

    @Override
    public void onPaint(Graphics g) {
        g.setColor(Color.WHITE);
        g.drawString(currentState.toString(), 30, 20);
        g.drawString(currentAction.toString(), 30, 40);
        g.drawString(String.valueOf(reward), 30, 60);
        g.drawString("Epsilon: " + agent.getEpsilon(), 30, 80);

        drawQTable(g);
    }

    private void drawQTable(Graphics g) {
        double[][] qTable = agent.getQTable();
        String[] stateNames = new String[State.values().length];
        String[] actionNames = new String[Action.values().length];

        for (int i = 0; i < State.values().length; i++) {
            stateNames[i] = State.values()[i].name();
        }

        for (int i = 0; i < Action.values().length; i++) {
            actionNames[i] = Action.values()[i].name();
        }

        int x = 10;
        int y = 350;
        int cellWidth = 110;
        int cellHeight = 20;
        int padding = 2;

        // Set font
        g.setFont(new Font("Monospaced", Font.BOLD, 14));

        // Draw state names
        for (int i = 0; i < stateNames.length; i++) {
            g.drawString(stateNames[i], x + (i + 1) * cellWidth + padding, y);
        }

        // Draw action names and Qvalues
        for (int i = 0; i < actionNames.length; i++) {
            g.drawString(actionNames[i], x, y + (i + 1) * cellHeight + padding);
            for (int j = 0; j < stateNames.length; j++) {
                double maxQValue = getMaxQValue(qTable[j]);

                // Highlight the maximum Q-value in blue
                if (qTable[j][i] == maxQValue) {
                    g.setColor(Color.CYAN);  // Highlight color
                } else {
                    g.setColor(Color.WHITE);  // Default color
                }

                String qValueStr = String.format("%.2f", qTable[j][i]);
                g.drawString(qValueStr, x + (j + 1) * cellWidth + padding, y + (i + 1) * cellHeight + padding);
            }
        }
    }

    private double getMaxQValue(double[] qValues) {
        double maxQValue = qValues[0];
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > maxQValue) {
                maxQValue = qValues[i];
            }
        }
        return maxQValue;
    }
}
