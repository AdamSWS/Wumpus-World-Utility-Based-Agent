import java.util.LinkedList;


public class WumpusWorldTree {
    WumpusWorldNode root;

    public WumpusWorldTree(WumpusWorldNode root) {
        this.root = root;
    }

    // function to expand tree to depth (d)
    public void expandTree(int d) {
        root.expandNode( d);
    }

    // Returns best action from the DFS Algorithm
    public int findBestAction() {
        SearchResult result = new SearchResult();
        findBestActionDFS(root, result, new LinkedList<>());
        if (root.glitter)
            return Action.GRAB;
        if (result.bestAction == 6 && !root.getIsArrowShot())
            if ((root.getPosition()[0] == 1 && root.getPosition()[1] == 1 && root.stench) || (root.isFacingWumpus(root.getDirection(), root.getPosition())))
                return 5;
        return result.bestAction;
    }

    // DFS algorithm to search the decision network
    private void findBestActionDFS(WumpusWorldNode node, SearchResult result, LinkedList<Integer> path) {
        if (node.getAction() > Action.START_TRIAL && node.getAction() < Action.END_TRIAL)
            path.addLast(node.getAction());

        if (node.getChildren().isEmpty()) {
            // summation of each branches' expected utility
            double cumulativeScore = node.getCumulativeReward();

            if (!path.isEmpty() && cumulativeScore > result.bestScore) {
                result.bestScore = cumulativeScore;
                if (path.size() >= 2)
                    result.bestAction = path.get(1);
                else if (!path.isEmpty())
                    result.bestAction = path.getFirst();
            }
        } else {
            for (WumpusWorldNode child : node.getChildren())
                findBestActionDFS(child, result, new LinkedList<>(path)); // Recursive DFS call with a copy of the path
        }
        if (!path.isEmpty())
            path.removeLast();
    }

    // best action
    private static class SearchResult {
        int bestAction = Action.NO_OP;
        double bestScore = Double.NEGATIVE_INFINITY;
    }
}



