package entrants.pacman.MCTS;


import pacman.game.Constants;

//package mcts;

import pacman.game.Game;
import java.util.Random;

import entrants.pacman.mauryas.MyPacMan;
import pacman.controllers.MASController;
import pacman.game.Constants.MOVE;


public class MCTSNode {
	
	public MOVE move;
	public double score;
	public static Random random = new Random();
	
	public MCTSNode(MOVE move){
		this.move = move;
		this.score = -1;
	}
	
	@Override
	public String toString(){
		return "<Node: previous_move = " + this.move.name() +
				"; score = " + this.score;
	}
	
	public void doSimulations(Game game, MASController ghosts, int simulations, int maximalLookahead){
		double[] scores = new double[simulations];
		
		for (int i = 0; i < simulations; i++){
			Game forwardCopy = game.copy();
	        
	        // Have to forward once before the loop - so that we aren't on a junction
//	        forwardCopy.advanceGame(move, ghosts.getMove(forwardCopy.copy(), 40));
	        int ghostScore = 0;
	        for (int j = 0; j < maximalLookahead; j++){
	        	// Repeat simulation till we find the next junction
		        while(!forwardCopy.isJunction(forwardCopy.getPacmanCurrentNodeIndex())){
		        	for (Constants.GHOST ghost : Constants.GHOST.values()) {
		                // If can't see these will be -1 so all fine there
		                if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0) {
		                	int ghostLocation = forwardCopy.getGhostCurrentNodeIndex(ghost);
		                	int pacLoc = forwardCopy.getPacmanCurrentNodeIndex();
		                	if(ghostLocation == pacLoc){
		                		ghostScore=10;
		                    }
		                }else  if (game.getGhostEdibleTime(ghost) > 0) {
		                	int ghostLocation = forwardCopy.getGhostCurrentNodeIndex(ghost);
		                	int pacLoc = forwardCopy.getPacmanCurrentNodeIndex();
		                	if(ghostLocation == pacLoc){
		                		ghostScore+=10;
		                	}
		                }
		            

		           }
		            forwardCopy.advanceGame(MyPacMan.nonJunctionSim(forwardCopy), ghosts.getMove(forwardCopy.copy(), 40));
		        }
		        
		        // once again leave the junction before extending the simulation
		        MOVE[] possibleMoves = forwardCopy.getPossibleMoves(forwardCopy.getPacmanCurrentNodeIndex());
		        forwardCopy.advanceGame(possibleMoves[random.nextInt(possibleMoves.length)], 
		        		ghosts.getMove(forwardCopy.copy(), 40));
	        }
	        
	        scores[i] = getValue(forwardCopy)+ghostScore;
		}
		
		this.score = mean(scores);
	}
	
	
	public void doSimulationJunction(Game game, MASController ghosts, int simulations, int maximalLookahead){
		double[] scores = new double[simulations];
		
		for (int i = 0; i < simulations; i++){
			Game forwardCopy = game.copy();
	        
//	        // Have to forward once before the loop - so that we aren't on a junction
//	        forwardCopy.advanceGame(move, ghosts.getMove(forwardCopy.copy(), 40));
	        int ghostScore = 0;
	        for (int j = 0; j < maximalLookahead; j++){
	        	// Repeat simulation till we find the next junction
		        while(!forwardCopy.isJunction(forwardCopy.getPacmanCurrentNodeIndex())){
		        	for (Constants.GHOST ghost : Constants.GHOST.values()) {
		                // If can't see these will be -1 so all fine there
		                if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0) {
		                	int ghostLocation = forwardCopy.getGhostCurrentNodeIndex(ghost);
		                	int pacLoc = forwardCopy.getPacmanCurrentNodeIndex();
		                	if(ghostLocation == pacLoc){
		                		ghostScore=10;
		                    }
		                }else  if (game.getGhostEdibleTime(ghost) > 0) {
		                	int ghostLocation = forwardCopy.getGhostCurrentNodeIndex(ghost);
		                	int pacLoc = forwardCopy.getPacmanCurrentNodeIndex();
		                	if(ghostLocation == pacLoc){
		                		ghostScore+=10;
		                	}
		                }
		            

		           }
		            forwardCopy.advanceGame(MyPacMan.nonJunctionSim(forwardCopy), ghosts.getMove(forwardCopy.copy(), 40));
		        }
		        
		        // once again leave the junction before extending the simulation
		        MOVE[] possibleMoves = forwardCopy.getPossibleMoves(forwardCopy.getPacmanCurrentNodeIndex());
		        forwardCopy.advanceGame(possibleMoves[random.nextInt(possibleMoves.length)], 
		        		ghosts.getMove(forwardCopy.copy(), 40));
	        }
	        
	        scores[i] = getValue(forwardCopy)+ghostScore;
		}
		
		this.score = mean(scores);
	}
	
	public static double mean(double[] m) {
	    double sum = 0;
	    for (int i = 0; i < m.length; i++) {
	        sum += m[i];
	    }
	    return sum / m.length;
	}
	
	
	public double getValue(Game game){		
		//double dist = game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(), ini);
		return game.getScore() + game.getPacmanNumberOfLivesRemaining()*200;
	}
}
