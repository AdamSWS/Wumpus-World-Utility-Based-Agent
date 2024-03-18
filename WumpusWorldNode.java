import java.util.*;

public class WumpusWorldNode {

    // information about the agent
    private int[] position;
    private int step;
    private int action;
    private char direction;

    // current percepts
    boolean stench;
    boolean breeze;
    boolean glitter;
    boolean scream;

    // variables for probability distributions
    private char[][] pitProbabilities;
    private char[][] wumpusProbabilities;

    // map information
    private boolean isArrowShot;
    private boolean wasArrowShotLastTurn;
    private boolean wumpusFound;
    private int[] wumpusLocation;
    private int pitsFound;
    private double cumulativeReward;

    // world model info + actions
    private int[] actions;
    private List<int[]> known, frontier, unknown;
    private List<WumpusWorldNode> children = new ArrayList<>();

    // variables for distributions
    private char safe = 's';
    private char possible = 'p';
    private char danger = 'd';

    // constructor to generate |S| states X (observed) node, with each branch having a depth of 3
    public WumpusWorldNode(int[] position, double cumulativeReward, int step, char direction, WumpusWorldNode parent, int action, boolean isArrowShot, boolean wasArrowShotLastTurn, boolean wumpusFound, int[] wumpusLocation, int pitsFound, List<int[]> known, List<int[]> frontier, List<int[]> unknown, char[][] pitProbabilities, char[][] wumpusProbabilities, boolean glitter, boolean breeze, boolean stench, boolean scream) {
        this.position = position;
        this.cumulativeReward = cumulativeReward;
        this.step = step;
        this.direction = direction;
        this.action = action;
        this.isArrowShot = isArrowShot;
        this.wasArrowShotLastTurn = wasArrowShotLastTurn;
        this.wumpusFound = wumpusFound;
        this.wumpusLocation = wumpusLocation;
        this.pitsFound = pitsFound;
        this.known = known;
        this.frontier = frontier;
        this.unknown = unknown;
        this.pitProbabilities = pitProbabilities;
        this.wumpusProbabilities = wumpusProbabilities;
        this.stench = stench;
        this.breeze = breeze;
        this.glitter = glitter;
        this.scream = scream;

        actions = new int[] {1,2,3,6};
    }

    // Some getters
    public boolean getIsArrowShot() { return isArrowShot; }

    public double getCumulativeReward () { return cumulativeReward; }

    public int getAction() {
        return action;
    }

    public int[] getPosition() { return position; }

    public char getDirection () { return direction; }

    public List<WumpusWorldNode> getChildren() {
        return children;
    }

    // function to handle changes to direction based on previous action
    public char updateDirection(int action, char direction) {
        char[] directions = {'N', 'E', 'S', 'W'};
        int currentDirectionIndex = -1;
        for (int i = 0; i < directions.length; i++) {
            if (directions[i] == direction) {
                currentDirectionIndex = i;
                break;
            }
        }
        if (action == 2) {
            currentDirectionIndex = (currentDirectionIndex + 1) % directions.length;
        } else if (action == 3) {
            currentDirectionIndex = (currentDirectionIndex - 1 + directions.length) % directions.length;
        }
        char newDirection = directions[currentDirectionIndex];
        return newDirection;
    }

    // function to handle changes to location based on previous action
    public int[] calculateNewPosition(int action, int[] currentPosition, char direction) {
        if (action != 1) {
            return currentPosition;
        }
        int[] newPosition = Arrays.copyOf(currentPosition, currentPosition.length);
        switch (direction) {
            case 'N': newPosition[0] += 1; break;
            case 'S': newPosition[0] -= 1; break;
            case 'E': newPosition[1] += 1; break;
            case 'W': newPosition[1] -= 1; break;
        }
        if (newPosition[0] < 1 || newPosition[0] > 4 || newPosition[1] < 1 || newPosition[1] > 4)
            return currentPosition;
        return newPosition;
    }

    // function to check of a location is inside a list (usually a world model)
    private boolean isLocationInList(List<int[]> list, int[] location) {
        for (int[] entry : list) {
            if ((Arrays.equals(entry, location)))
                if (entry[0] == location[0] && entry[1] == location[1])
                    return true;
        }
        return false;
    }

    // function to check if we are facing the wumpus
    public boolean isFacingWumpus(char direction, int[] agentLocation) {
        if (!wumpusFound) return false;
        switch (direction) {
            case 'N':
                return agentLocation[1] == wumpusLocation[1] && agentLocation[0] < wumpusLocation[0];
            case 'S':
                return agentLocation[1] == wumpusLocation[1] && agentLocation[0] > wumpusLocation[0];
            case 'E':
                return agentLocation[0] == wumpusLocation[0] && agentLocation[1] < wumpusLocation[1];
            case 'W':
                return agentLocation[0] == wumpusLocation[0] && agentLocation[1] > wumpusLocation[1];
            default: return false;
        }
    }

    // find the expected utility from the world model
    double updateReward(int[] initialPosition, int action, int[] nextPosition) {
        double reward = 0.0;
        if (action != 1)
            if (action == 6)
                return 0;
            else
                return -1; // Minor penalty for turning without moving

        if (isLocationInList(known, nextPosition))
            return -1;

        if (initialPosition == nextPosition)
            return -1;

        // get status on the safety of the next position
        char pitStatus = pitProbabilities[nextPosition[0]-1][nextPosition[1]-1];
        char wumpusStatus = wumpusProbabilities[nextPosition[0]-1][nextPosition[1]-1];

        if (pitStatus == 'd' || wumpusStatus == 'd')
            return -1000;

        if (pitStatus == 'p' || wumpusStatus == 'p')
            if (isLocationInList(unknown, nextPosition))
                reward = 1000 / (16.0 - known.size());
            else
                reward = -1000 * ((2 - pitsFound / (16.0 - known.size())));
        else if (pitStatus == 's' && wumpusStatus == 's')
            reward = 1000 / (16.0 - known.size());
        else
            reward = -1000;

        return reward;
    }

    // expand each node so we have a tree w/ a depth of five
    public void expandNode(int limit) {
        if (limit == 0) return;
        for (int action : actions) {
            char newDirection = updateDirection(action, this.direction);
            int[] newPosition = calculateNewPosition(action, this.position, newDirection);
            double actionReward = updateReward(this.position, action, newPosition); // Assuming this correctly calculates the immediate reward of the action
            double newCumulativeReward = this.cumulativeReward + actionReward; // Now we're directly using cumulative reward from the parent
            WumpusWorldNode childNode = new WumpusWorldNode(newPosition, newCumulativeReward, this.step + 1, newDirection, this, action, this.isArrowShot, this.wasArrowShotLastTurn, this.wumpusFound, this.wumpusLocation, this.pitsFound, new ArrayList<>(this.known), new ArrayList<>(this.frontier), new ArrayList<>(this.unknown), this.pitProbabilities.clone(), this.wumpusProbabilities.clone(), glitter, breeze, stench, scream);
            this.children.add(childNode);
            childNode.expandNode(limit - 1);
        }
    }
}