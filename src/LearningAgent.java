
import java.io.*;

import static org.dreambot.api.utilities.Logger.log;
public class LearningAgent implements Serializable {
    private static final long serialVersionUID = 1L;
    double[][] qTable;
    private double alpha = 0.1;
    private double gamma = 0.9;
    private double epsilon = 0.6;
    private double minEpsilon = 0.05;
    private double decayRate = .995;
    private int iterationCount = 0;
    private boolean epsilonDecayed = false;

    public LearningAgent(int numStates, int numActions) {
        qTable = new double[numStates][numActions];
    }

     public int chooseAction(int state) {
        iterationCount++;
        if(epsilon <=  0.05){
            epsilon = -1; //stop exploring random actions
        }
        if (Math.random() < epsilon) { // Exploration

            int randomAction = (int) (Math.random() * qTable[state].length);
            log("Exploring action: " + randomAction + " for state: " + state);
            return randomAction;
        } else {
            int bestAction = getMaxAction(state); // Exploitation
            log("Exploiting action: " + bestAction + " for state: " + state);
            return bestAction;
        }
    }

    public State getBestMiningSpotState() {
        double qValueForSpot1 = qTable[State.WALKING_SPOT1.ordinal()][Action.WALK_SPOT1.ordinal()];
        double qValueForSpot2 = qTable[State.WALKING_SPOT2.ordinal()][Action.WALK_SPOT2.ordinal()];

        log("Q-value for WALKING_SPOT1: " + qValueForSpot1);
        log("Q-value for WALKING_SPOT2: " + qValueForSpot2);

        if (qValueForSpot1 > qValueForSpot2) {
            return State.WALKING_SPOT1;
        } else {
            return State.WALKING_SPOT2;
        }
    }


    public void updateQValue(int state, int action, int nextState, double reward) {
        double maxQ = qTable[nextState][getMaxAction(nextState)];
        qTable[state][action] = qTable[state][action] + alpha * (reward + gamma * maxQ - qTable[state][action]);


        System.out.println("Updated Q-value for state " + state + ", action " + action + " to " + qTable[state][action]);
        decayEpsilon();
    }







    private int getMaxAction(int state) {
        int maxAction = 0;
        double maxValue = qTable[state][0];
        for (int i = 1; i < qTable[state].length; i++) {
            if (qTable[state][i] > maxValue) {
                maxValue = qTable[state][i];
                maxAction = i;
            }
        }
        return maxAction;
    }

    public double getEpsilon() {
        return epsilon;
    }

    private void decayEpsilon() {
        if (epsilon > minEpsilon) {
            epsilon *= decayRate;
            if (epsilon < minEpsilon) {
                epsilon = minEpsilon;
                if (!epsilonDecayed) {
                    epsilonDecayed = true;
                    log("Epsilon has fully decayed. Exploration is minimal now.");
                }
            }

            log("Iteration: " + iterationCount + ", Decayed epsilon: " + epsilon);

        }
    }

    public double[][] getQTable() {
        return qTable;
    }

    public void saveQTable(String filePath) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(this);
        }
    }

    public static LearningAgent loadQTable(String filePath) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath))) {
            return (LearningAgent) in.readObject();
        }
    }
}
