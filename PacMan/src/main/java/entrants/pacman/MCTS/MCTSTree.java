package entrants.pacman.MCTS;

import pacman.game.Constants.MOVE;
import pacman.game.info.GameInfo;
import pacman.game.internal.Ghost;

import java.util.LinkedList;

import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Game;

public class MCTSTree {

	private Game game;
	private MASController ghosts;
	private LinkedList<MCTSNode> simulatedMoves;
	
	private static final int MaxLookahead = 3; 
	private static final int SimulationsPerMove=8;
	
	private static final boolean Debug = false;
	
	public MCTSTree(Game game){
        Game coGame;
        GameInfo info = game.getPopulatedGameInfo();
        info.fixGhosts((ghost) -> new Ghost(
                ghost,
                game.getCurrentMaze().lairNodeIndex,
                -1,
                -1,
                MOVE.NEUTRAL
        ));
        coGame = game.getGameFromInfo(info);
        
		this.game = coGame;
		
        // Make some ghosts for simulation purposes
        this.ghosts = new POCommGhosts(50);
        simulatedMoves = new LinkedList<MCTSNode>();
	}
	
	
	public void simulate(double timeDue){
		if (Debug)
			System.out.println("Start simulation");
		
    	// get all possible moves at the queried position
    	int myNodeIndex = game.getPacmanCurrentNodeIndex();
    	MOVE[] myMoves = game.getPossibleMoves(myNodeIndex,game.getPacmanLastMoveMade());
    	simulatedMoves.clear();
    	
        for (MOVE move : myMoves) {
            
            // Create a node in the game tree and perform simulations on it
            MCTSNode node = new MCTSNode(move);
            simulatedMoves.add(node);
            node.doSimulations(game, ghosts, SimulationsPerMove, MaxLookahead);   
            if (Debug)
            	System.out.println(node.toString());
        }
	}
	
	public void simulateJunc(double timeDue){
		if (Debug)
			System.out.println("Start simulation");
		
    	// get all possible moves at the queried position
    	int myNodeIndex = game.getPacmanCurrentNodeIndex();
    	MOVE[] myMoves = game.getPossibleMoves(myNodeIndex,game.getPacmanLastMoveMade());
    	simulatedMoves.clear();
    	
        for (MOVE move : myMoves) {
            
            // Create a node in the game tree and perform simulations on it
            MCTSNode node = new MCTSNode(move);
            simulatedMoves.add(node);
            node.doSimulationJunction(game, ghosts, SimulationsPerMove, MaxLookahead);   
            if (Debug)
            	System.out.println(node.toString());
        }
	}
	
	
	public MOVE getBestMove(){
		double bestScore = -1;
		MOVE bestMove = MOVE.NEUTRAL;
		
		for (MCTSNode node : this.simulatedMoves){
			if (node.score > bestScore){
				bestMove = node.move;
				bestScore = node.score;
			}
		}
		if (Debug)
			System.out.println("best Move: " + bestMove.name());
		return bestMove;
	}
	
}
