package entrants.ghosts.mauryas;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Random;

public class ManusGhosts extends MASController {

    public ManusGhosts() {
        this(50);
    }

    public ManusGhosts(int TICK_THRESHOLD) {
        super(true, new EnumMap<GHOST, IndividualGhostController>(GHOST.class));
        controllers.put(GHOST.BLINKY, new ManusGhost(GHOST.BLINKY, TICK_THRESHOLD));
        controllers.put(GHOST.INKY, new ManusGhost(GHOST.INKY, TICK_THRESHOLD));
        controllers.put(GHOST.PINKY, new ManusGhost(GHOST.PINKY, TICK_THRESHOLD));
        controllers.put(GHOST.SUE, new ManusGhost(GHOST.SUE, TICK_THRESHOLD));
    }
}

class ManusGhost extends IndividualGhostController{
    private final static int PILL_PROXIMITY = 35;        //if Ms Pac-Man is this close to a power pill, back away
    Random rnd = new Random();
    private static int[] closestGhost = {-1,-1,-1,-1};
    private int TICK_THRESHOLD;
    private int lastPacmanIndex = -1;
    private int tickSeen = -1;

    public ManusGhost(GHOST ghost) {
        this(ghost, 5);
    }

    public ManusGhost(GHOST ghost, int TICK_THRESHOLD) {
        super(ghost);
        this.TICK_THRESHOLD = TICK_THRESHOLD;
    }

    @Override
    public MOVE getMove(Game game, long timeDue) {
        // Housekeeping - throw out old info
    	Messenger messenger = game.getMessenger();
    	//Everyone should be aware about address of other ghost
        messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.I_AM, game.getGhostCurrentNodeIndex(ghost), game.getCurrentLevelTime()));
    	
        int currentTick = game.getCurrentLevelTime();
        if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD) {
            lastPacmanIndex = -1;
            tickSeen = -1;
        }

        // Can we see PacMan? If so tell people and update our info
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);
        //tell everyone about the pacman's position, if visible
        if (pacmanIndex != -1) {
            lastPacmanIndex = pacmanIndex;
            tickSeen = game.getCurrentLevelTime();
            if (messenger != null) {
                messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex, game.getCurrentLevelTime()));
             }
        }

        // Has anybody else seen PacMan if we haven't?
        if (pacmanIndex == -1 && game.getMessenger() != null) {
            for (Message message : messenger.getMessages(ghost)) {
                if (message.getType() == BasicMessage.MessageType.PACMAN_SEEN) {
                    if (message.getTick() > tickSeen && message.getTick() < currentTick) { // Only if it is newer information
                        lastPacmanIndex = message.getData();
                        tickSeen = message.getTick();
                    }
                }
            }
        }
        if (pacmanIndex == -1) {
            pacmanIndex = lastPacmanIndex;
        }
        
        Boolean requiresAction = game.doesGhostRequireAction(ghost);
        if (requiresAction != null && requiresAction)        //if ghost requires an action
        {
            if (pacmanIndex != -1) {//Pacman is visible
                if (game.getGhostEdibleTime(ghost) > 0 || closeToPower(game))    //retreat from Ms Pac-Man if edible or if Ms Pac-Man is close to power pill
                {
                    try {
                        return game.getApproximateNextMoveAwayFromTarget(game.getGhostCurrentNodeIndex(ghost),
                                game.getPacmanCurrentNodeIndex(), game.getGhostLastMoveMade(ghost), DM.PATH);
                    } catch (ArrayIndexOutOfBoundsException e) {
                        System.out.println(e);
                        System.out.println(pacmanIndex + " : " + currentIndex);
                    }
                } else {//attack the pacman
                	//Coordinate with other ghost, using their location
                	//Rank the ghost based on proximity with pacman
                    this.findNearestGhost(messenger,game,ghost,currentTick,lastPacmanIndex);
                    //The closest ghost attacks the pacman and other ghost attack the neighbours
                    return this.fetchGhostMove(game,ghost,pacmanIndex);
                }
            } else {            	
                MOVE[] possibleMoves = game.getPossibleMoves(game.getGhostCurrentNodeIndex(ghost), game.getGhostLastMoveMade(ghost));
                return possibleMoves[rnd.nextInt(possibleMoves.length)];
            }
        }
        return null;
    }

	//This helper function checks if Ms Pac-Man is close to an available power pill
    private boolean closeToPower(Game game) {
        int[] powerPills = game.getPowerPillIndices();

        for (int i = 0; i < powerPills.length; i++) {
            Boolean powerPillStillAvailable = game.isPowerPillStillAvailable(i);
            int pacmanNodeIndex = game.getPacmanCurrentNodeIndex();
            if (pacmanNodeIndex == -1) {
                pacmanNodeIndex = lastPacmanIndex;
            }
            if (powerPillStillAvailable == null || pacmanNodeIndex == -1) {
                return false;
            }
            
            if (powerPillStillAvailable && game.getShortestPathDistance(powerPills[i], pacmanNodeIndex) < PILL_PROXIMITY) {
                return true;
            }
        }

        return false;
    }
    //This method will help in finding the ghost which is nearest the pacman
    private void findNearestGhost(Messenger msg, Game game, GHOST ghost, int currentTick, int lastPacmanIndex){
    	GHOST ghostSender ;
    	double[] dist = {250.0,250.0,250.0,250.0}; 
    	for (Message message : msg.getMessages(ghost)) {
            if (message.getType() == BasicMessage.MessageType.I_AM && lastPacmanIndex != -1) {
                ghostSender = message.getSender();
            	if(ghostSender==GHOST.BLINKY){//Find the distance from PacMan
                	dist[0] = game.getShortestPathDistance(message.getData(), lastPacmanIndex);
                }else if(ghostSender==GHOST.INKY ){
                	dist[1] = game.getShortestPathDistance(message.getData(), lastPacmanIndex);
                }else if(ghostSender == GHOST.PINKY ){
                	dist[2] = game.getShortestPathDistance(message.getData(), lastPacmanIndex);
                }else if(ghostSender == GHOST.SUE){
                	dist[3] = game.getShortestPathDistance(message.getData(), lastPacmanIndex);
                }
                
            	//The message sent by current Ghost is not available in the Messages of Game
            	if(ghost==GHOST.BLINKY){//Find the distance from PacMan
                	dist[0] = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), lastPacmanIndex);
                }else if(ghost==GHOST.INKY ){
                	dist[1] = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), lastPacmanIndex);
                }else if(ghost == GHOST.PINKY ){
                	dist[2] = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), lastPacmanIndex);
                }else if(ghost == GHOST.SUE){
                	dist[3] = game.getShortestPathDistance(game.getGhostCurrentNodeIndex(ghost), lastPacmanIndex);
                }
            	
            }
        }
//        	System.out.println(ghost+": "+dist[0]+" "+dist[1]+" "+dist[2]+" "+dist[3]);
    	double tempDist[] = {dist[0],dist[1],dist[2],dist[3]};
    	Arrays.sort(tempDist);
    	for(int i = 0; i<4;i++){
    		for(int j = 0; j<4;j++){
    			if(tempDist[i] == dist[j]){
    				closestGhost[i] = j;//Rank them based on their distance from PacMan
    			}
    		}
    	}
    }
    //If closest ghost attack the pacman else move towards its neighbors.
    private MOVE fetchGhostMove(Game game, GHOST ghost, int pacmanIndex) {
    	//If closest to ghost then attack it directly
    	if(ghost==GHOST.BLINKY && closestGhost[0]==0){
    		return game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
                  pacmanIndex, game.getGhostLastMoveMade(ghost), DM.PATH);
        }else if(ghost==GHOST.INKY && closestGhost[1]==0){
    		return game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
                    pacmanIndex, game.getGhostLastMoveMade(ghost), DM.PATH);
        }else if(ghost == GHOST.PINKY && closestGhost[2]==0){
    		return game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
                    pacmanIndex, game.getGhostLastMoveMade(ghost), DM.PATH);
        }else if(ghost == GHOST.SUE && closestGhost[3]==0){
    		return game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
                    pacmanIndex, game.getGhostLastMoveMade(ghost), DM.PATH);
        }
    	
    	//Else attack the neighboring nodes
    	ArrayList<Integer> pacNeighbors = new ArrayList<Integer>();
    	int[] nodes = game.getNeighbouringNodes(pacmanIndex);
    	for(int i =0; i<nodes.length;i++){
    		pacNeighbors.add(nodes[i]);
    		int[] nextNeigh = game.getNeighbouringNodes(nodes[i]);
    		for(int j = 0; j<nextNeigh.length;j++){
    			if(nextNeigh[j]!=pacmanIndex){
    				pacNeighbors.add(nextNeigh[j]);
    			}
    		}
    	}
    	    	
    	return game.getApproximateNextMoveTowardsTarget(game.getGhostCurrentNodeIndex(ghost),
                pacNeighbors.get(rnd.nextInt(pacNeighbors.size())), game.getGhostLastMoveMade(ghost), DM.PATH);
   	}
}