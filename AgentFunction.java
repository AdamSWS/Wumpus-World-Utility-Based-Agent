/*
 * Class that defines the agent function.
 *
 * Written by James P. Biagioni (jbiagi1@uic.edu)
 * for CS511 Artificial Intelligence II
 * at The University of Illinois at Chicago
 *
 * Last modified 2/19/07
 *
 * DISCLAIMER:
 * Elements of this application were borrowed from
 * the client-server implementation of the Wumpus
 * World Simulator written by Kruti Mehta at
 * The University of Texas at Arlington.
 *
 */

import java.util.*;

class AgentFunction {

	// string to store the agent's name
	private String agentName = "theaustronaut";

	// default variables
	private boolean glitter;
	private boolean breeze;
	private boolean stench;
	private boolean scream;
	private boolean bump;
	private Agent agent;
	private char direction;
	private int[] currentLocation;
	private int prevAction;

	// variables for remembering events
	private boolean isArrowShot;
	private boolean wasArrowShotLastTurn;
	private boolean wumpusFound;
	private int[] wumpusLocation;
	private int pitsFound;

	// variables for model storage
	private int numPits = 2;
	private int numWumpus = 1;
	private int worldSize = 4;
	private int step;
	private List<int[]> known;
	private List<int[]> frontier;
	private List<int[]> unknown;

	// variables for probability distributions
	private char[][] pitProbabilities;
	private char[][] wumpusProbabilities;

	// variables for distributions
	private char safe = 's';
	private char possible = 'p';
	private char danger = 'd';

	public AgentFunction(Agent agent)
	{
		this.agent = agent;
		known = new ArrayList<>();
		frontier = new ArrayList<>();
		unknown = new ArrayList<>();

		pitProbabilities = new char[worldSize][worldSize];
		wumpusProbabilities = new char[worldSize][worldSize];

		initializeProbabilityDistributions();

		isArrowShot = wumpusFound = wasArrowShotLastTurn = false;
		wumpusLocation = new int[2];
		pitsFound = 0;
		step = 0;
		prevAction = 0;
	}

	// sets up the probabilty distributions for world model
	private void initializeProbabilityDistributions () {
		for (int x = 1; x <= worldSize; x++) {
			for (int y = 1; y <= worldSize; y++) {
				wumpusProbabilities[x-1][y-1] = possible;
				pitProbabilities[x-1][y-1] = possible;
				unknown.add(new int[]{x, y});
			}
		}
		unknown.remove(new int[]{1, 1});
		unknown.remove(new int[]{1, 2});
		unknown.remove(new int[]{2, 1});
		// makes sure to designate (1,1) as 100% safe
		wumpusProbabilities[0][0] = pitProbabilities[0][0] = safe;
	}

	// traverses a list of locations and returns weather or not it contains a target location
	private boolean isLocationInList(List<int[]> list, int[] location) {
		for (int[] entry : list) {
			if ((Arrays.equals(entry, location)))
				if (entry[0] == location[0] && entry[1] == location[1])
					return true;
		}
		return false;
	}

	// returns a list of the valid, adjacent squares to arg
	public List<int[]> getAdjacentValidSquares(int[] location) {
		List<int[]> squares = new ArrayList<>();
		if (location != null) {
			// Check and add the square to the north
			if (location[0] - 1 >= 1) {
				squares.add(new int[]{location[0] - 1, location[1]});
			}
			// Check and add the square to the south
			if (location[0] + 1 <= worldSize) {
				squares.add(new int[]{location[0] + 1, location[1]});
			}
			// Check and add the square to the east
			if (location[1] + 1 <= worldSize) {
				squares.add(new int[]{location[0], location[1] + 1});
			}
			// Check and add the square to the west
			if (location[1] - 1 >= 1) {
				squares.add(new int[]{location[0], location[1] - 1});
			}
		}
		return squares;
	}

	// Updates the lists of known, frontier and unknown locations
	private void updateExpansions() {
		if (!isLocationInList(known, currentLocation)) {
			known.add(currentLocation);
			frontier.removeIf(loc -> loc[0] == currentLocation[0] && loc[1] == currentLocation[1]);
		}
		for (int[] location : getAdjacentValidSquares(currentLocation)) {
			if (!isLocationInList(known, location) && !isLocationInList(frontier, location)) {
				frontier.add(location);
				unknown.removeIf(loc -> loc[0] == location[0] && loc[1] == location[1]);
			}
		}
	}

	// Update the pit distribution w/ correct values
	private void updateFoundPit(List<int[]> adjSquares) {
		for (int[] square : adjSquares) {
			if (pitProbabilities[square[0] - 1][square[1] - 1] == possible) {
				pitProbabilities[square[0] - 1][square[1] - 1] = danger;
				pitsFound++;
			}
		}
		if (pitsFound == 2) {
			for (int x = 0; x < worldSize; x++) {
				for (int y = 0; y < worldSize; y++) {
					pitProbabilities[x][y] = pitProbabilities[x][y] != danger ? safe : danger;
				}
			}
		}
	}

	// Update the pit distribution w/ potentially correct values
	private void updatePitProbabilities() {
		List<int[]> adjSquares = getAdjacentValidSquares(currentLocation);
		// If a breeze is detected, update probabilities for adjacent squares
		if (!breeze) {
			// If no breeze is detected, adjacent squares can be marked safe
			for (int[] adjSquare : adjSquares) {
				pitProbabilities[adjSquare[0] - 1][adjSquare[1] - 1] = safe;
			}
		} else {
			int validAdjacentSquares = 0;
			for (int[] square : adjSquares) {
				if (pitProbabilities[square[0] - 1][square[1] - 1] != safe) {
					if (pitProbabilities[square[0] - 1][square[1] - 1] != danger) {
						pitProbabilities[square[0] - 1][square[1] - 1] = possible;
					}
					validAdjacentSquares++;
				}
			}
			if (validAdjacentSquares == 1) {
				updateFoundPit(adjSquares);
			}
		}
	}


	// identifies wumpus location and marks other squares as safe
	private void updateFoundWumpus(List<int[]> adjSquares) {
		wumpusFound = true;
		for (int[] square : adjSquares) {
			if (wumpusProbabilities[square[0] - 1][square[1] - 1] == possible) {
				wumpusLocation[0] = square[0] - 1;
				wumpusLocation[1] = square[1] - 1;
				wumpusProbabilities[square[0] - 1][square[1] - 1] = danger;
			}
		}
		for (int x = 0; x < worldSize; x++) {
			for (int y = 0; y < worldSize; y++) {
				wumpusProbabilities[x][y] = wumpusProbabilities[x][y] != danger ? safe : danger;
			}
		}
	}

	// Updates the array of pit probabilities
	private void updateWumpusProbabilities() {
		// If no stench is detected, adjacent squares can be marked safe (Wumpus probability = 0)
		List<int[]> adjSquares = getAdjacentValidSquares(currentLocation);
		if (!stench) {
			for (int[] adjSquare : adjSquares) {
				wumpusProbabilities[adjSquare[0] - 1][adjSquare[1] - 1] = safe;
			}
		} else {
			// Assign calculated stench probability to valid adjacent squares
			int validAdjacentSquares = 0;
			for (int[] square : adjSquares) {
				if (wumpusProbabilities[square[0] - 1][square[1] - 1] != safe) {
					wumpusProbabilities[square[0] - 1][square[1] - 1] = possible;
					validAdjacentSquares++;
				}
			}
			if (validAdjacentSquares == 1) {
				updateFoundWumpus(adjSquares);
			}
		}
	}

	// updates the Wumpus location, even if arrow is missed
	private void updateWumpusLocationAfterShooting() {
		int row = currentLocation[0] - 1;
		int col = currentLocation[1] - 1;

		switch (direction) {
			case 'N':
				// Clear probabilities in the column above the agent's current position
				for (int i = row; i < worldSize; i++) {
					wumpusProbabilities[i][col] = safe;
				}
				break;
			case 'S':
				// Clear probabilities in the column below the agent's current position
				for (int i = row; i >= 0; i--) {
					wumpusProbabilities[i][col] = safe;
				}
				break;
			case 'E':
				// Clear probabilities in the row to the right of the agent's current position
				for (int j = col; j < worldSize; j++) {
					wumpusProbabilities[row][j] = safe;
				}
				break;
			case 'W':
				// Clear probabilities in the row to the left of the agent's current position
				for (int j = col; j >= 0; j--) {
					wumpusProbabilities[row][j] = safe;
				}
				break;
		}
	}

	// updates the agent's percepts
	public void updatePercepts(TransferPercept tp) {
		glitter = tp.getGlitter();
		breeze = tp.getBreeze();
		stench = tp.getStench();
		scream = tp.getScream();
		bump = tp.getBump();
	}

	// updates the agent's position
	public void updatePosition() {
		direction = agent.getDirection();
		currentLocation = agent.getLocation().clone();
		currentLocation[0] += 1;
		currentLocation[1] += 1;
	}

	// Utility Based Agent that navigates Wumpus World by choosing action that maximized the expected utility
	public int process(TransferPercept tp)
	{
		// get current percepts
		updatePercepts(tp);

		// get current position
		updatePosition();

		// update Known, Frontier, and Unknown
		updateExpansions();

		updatePitProbabilities();

		if (wasArrowShotLastTurn == true) {
			updateWumpusLocationAfterShooting();
			wasArrowShotLastTurn = false;
		}

		if (!wumpusFound) {
			updateWumpusProbabilities();
		}
		if (stench && !wumpusFound)
			updateWumpusProbabilities();

		WumpusWorldTree descision_network = new WumpusWorldTree(new WumpusWorldNode(currentLocation, 0, step++, direction, null, prevAction, isArrowShot, wasArrowShotLastTurn,  wumpusFound, wumpusLocation, pitsFound, known, frontier, unknown, pitProbabilities, wumpusProbabilities, glitter, breeze, stench, scream));
		descision_network.expandTree(5);

		int act = descision_network.findBestAction();
		if (act == 5)
			isArrowShot = wasArrowShotLastTurn = true;
		prevAction = act;
		return act;
	}

	// public method to return the agent's name
	// do not remove this method
	public String getAgentName() {
		return agentName;
	}
}