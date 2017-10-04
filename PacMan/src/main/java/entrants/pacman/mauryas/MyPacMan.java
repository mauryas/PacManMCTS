package entrants.pacman.mauryas;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import pacman.Executor;
import pacman.controllers.PacmanController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Constants.MOVE;
import pacman.game.Constants;
import pacman.game.Game;
import entrants.pacman.Evolution.Genome;
import entrants.pacman.MCTS.MCTSTree;

public class MyPacMan extends PacmanController {
	Genome genomePAC = new Genome();
	private  int[] GameConstvalues = {216,106,518,2313,1293,13485,1162,338};//new int[8];//valid move, pill, power, ghost, Edible1, Edible2, Edible3, Edible4
	int gameScore = 0;
	
//	public mauryas(Genome g){
//		this.GameConstvalues = g.getGene();
//		this.genomePAC = g;
//	}
	
	public static void main(String[] args) throws IOException {
        Executor po = new Executor(true, true, true);
        po.setDaemon(true);
        //Optimization using evolutionary algorithm
        //evolutionaryOtimization(po);
		po.runGame(new MyPacMan(), new POCommGhosts(50), true, 40);

    }

	private static void evolutionaryOtimization(Executor po) throws IOException {
		//Genome based weight optimization
        Genome[] genome = new Genome[20];//20
        Genome[] crossGenome = new Genome[15];//15
        Genome[] mutation = new Genome[5];//5
        Random r = new Random();
        
        String file = "C:/Users/ShivamMaurya/Desktop/OvGU - Masters DKE/Semester 3/Computational Intelligence in Games/Programming/genome.csv";
        FileWriter writer = new FileWriter(file,true);
        //Initialize genomes randomly
        for(int i = 0; i<genome.length;i++){
        	Genome temp = new Genome();
        	genome[i] = temp;
        }
        
        //K = no. of generations
        for(int k = 0; k<50;k++){//100
	        for(int i = 0; i<genome.length;i++){
	        	int[] tempGene = genome[i].getGene();
	        	int localFitness = 0;
	        	//Run for 10 different combination and find the mean
	        	for(int j = 0;j<10;j++){
	        		genome[i].setFitness(0);
//	        		po.runGame(new mauryas(genome[i]), new POCommGhosts(50), false, 40);
	        		localFitness += genome[i].getFitness();
	        	}
	        	//Write the result in CSV file
	        	for(int j =0; j<tempGene.length;j++){
					writer.append(Integer.toString(tempGene[j]));
					writer.append(',');
				}
	        	System.out.println("Local Fitness: "+localFitness);
				writer.append(Integer.toString(localFitness/10));//Average no of 
				writer.append('\n');
	        } 
	        
	        //Selection 
	        genome = genome[0].selection(genome);
	        
	        //Select gene for crossover and mutation 
		    for(int i = 0,c=0,m=0;i<genome.length;i++){
		    //15 crossover 
			  if(r.nextDouble()>=0.25){
				  if(c<crossGenome.length){//to avoid array out of bounds error
					  crossGenome[c++] = genome[i]; 
				  }else{
					  mutation[m++] = genome[i];
				  }
			   	
			  }else{
				  if (m < mutation.length){//to avoid array out of bounds error 
					  mutation[m++] = genome[i];
				  } else {
					  crossGenome[c++] = genome[i]; 
				  }
			   	
			   }  
		    }
		    
		    crossGenome = crossGenome[0].crossover(crossGenome);
		    mutation = mutation[0].mutation(mutation);
		    
		    for(int i = 0; i<crossGenome.length;i++){
		    	genome[i] = crossGenome[i];
		    }
		    for(int i = 0; i<mutation.length;i++){
		    	genome[i+crossGenome.length] = mutation[i];
		    }
		    
		    System.out.println("-- Generation --");
        }
        
        writer.flush();
        writer.close();
	}

	@Override
	public MOVE getMove(Game game, long timeDue) {
		//Find the current position of Pac-Man
		genomePAC.setFitness(game.getScore());
		int pacManusPos = game.getPacmanCurrentNodeIndex();
		
		int[] neighbCheckJunc = game.getNeighbouringNodes(pacManusPos);
		
		boolean neighbJunc = false;
		for(int i=0; i<neighbCheckJunc.length;i++ ){
			neighbJunc = game.isJunction(neighbCheckJunc[i]);
		}
		
		//If neighbour node is junction, use MCTS to find if a best move
		if(neighbJunc){
			return mctsJunc(game, timeDue);
		}
		//if pills, power pills are visible, use greedy method to move towards them		        
		if(game.getActivePillsIndices().length>0 || game.getActivePowerPillsIndices().length>0){
			double[] points = {-10000,-10000,-10000,-10000};
			//Pac-Man can take 4 direction in game. Create 4 arrays to calculate weights for each move
			ArrayList<Integer> up = new ArrayList<Integer>();
			ArrayList<Integer> down = new ArrayList<Integer>();
			ArrayList<Integer> left = new ArrayList<Integer>();
			ArrayList<Integer> right = new ArrayList<Integer>();
			double[] moveValue = {0.00,//left
								  0.00,//right
								  0.00,//up
								  0.00};//down
			//get neighbors and add into the available path option
			int[] neighbors = game.getNeighbouringNodes(pacManusPos);
			//Fetch available valid moves and initialize them with 5
			fetchMoveValues(moveValue, neighbors, left, right, up, down, pacManusPos);
			//Fetch How much the pills values are available in available moves 
			fetchPills(game, left, right, up, down,moveValue);
			//Fetch power pills value and add it to valid direction
			fetchPowerPills(game, left, right, up, down,moveValue);
			//Fetch ghost values and add them into the direction. Positive for edible ghost and negative for actuve
			fetchGhosts(game, left, right, up, down,moveValue);
			//find the best greedy move
			return fetchGreedyMove(points, left, right, up, down,moveValue);
		
		//No Pills around
		}else{
			if (game.isJunction(pacManusPos))
	    	{
	        	// return best direction determined through MCTS
	            return mcts(game, timeDue);
	    	} else {
	    		// follow along the path
	    		return nonJunctionSim(game);
	    	}
			
		}		
	}
	
	private MOVE fetchGreedyMove(double[] points, ArrayList<Integer> left, ArrayList<Integer> right,
			ArrayList<Integer> up, ArrayList<Integer> down, double[] moveValue) {
		MOVE m = null;
		if(!left.isEmpty()){
			points[0] = moveValue[0];
		}
		if(!right.isEmpty()){
			points[1] = moveValue[1];
		}
		if(!up.isEmpty()){
			points[2] = moveValue[2];
		}
		if(!down.isEmpty()){
			points[3] = moveValue[3];
		}			
		
		if(points[0]>=points[1] && points[0]>=points[2] && points[0]>=points[3]){
        	m = MOVE.LEFT;
        }else if(points[2]>=points[1] && points[2]>=points[3]){
        	m = MOVE.UP;
        }else if(points[1]>=points[3]){
        	m = MOVE.RIGHT;
        }else if(points[3]>=points[2]){
        	m = MOVE.DOWN;	
        }
		
		return m;
	}

	private void fetchMoveValues(double[] moveValue, int[] neighbors, ArrayList<Integer> left, ArrayList<Integer> right, ArrayList<Integer> up, ArrayList<Integer> down, int pacManusPos) {
		//Add them into the possible array of 4 paths
		
		for(int i = 0;i<neighbors.length;i++){
			if(neighbors[i] == pacManusPos - 1){
				left.add(neighbors[i]);
				moveValue[0]+=(double)GameConstvalues[0];//5.00;//score counter
			} else if(neighbors[i] == pacManusPos + 1){
				right.add(neighbors[i]);
				moveValue[1]+=(double)GameConstvalues[0];//5.00;
			} else if(neighbors[i]>pacManusPos){
				down.add(neighbors[i]);
				moveValue[3]+=(double)GameConstvalues[0];//5.00;//down
			} else if(neighbors[i]<pacManusPos){
				up.add(neighbors[i]);
				moveValue[2]+=(double)GameConstvalues[0];//5.00;//up
			}
		}
	}

	public static MOVE nonJunctionSim(Game game){
    	// get the current position of PacMan (returns -1 in case you can't see PacMan)
    	int myNodeIndex = game.getPacmanCurrentNodeIndex();

    	for (Constants.GHOST ghost : Constants.GHOST.values()) {
            // If can't see these will be -1 so all fine there
            if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0) {
                int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
                if (ghostLocation != -1) {
                    if (game.getShortestPathDistance(myNodeIndex, ghostLocation) < 15) {
                        return game.getNextMoveAwayFromTarget(myNodeIndex, ghostLocation, Constants.DM.PATH);
                    }
                }
            }
        }

        /// Strategy 2: Find nearest edible ghost and go after them
        int minDistance = 30;
        Constants.GHOST minGhost = null;
        for (Constants.GHOST ghost : Constants.GHOST.values()) {
            // If it is > 0 then it is visible so no more PO checks
            if (game.getGhostEdibleTime(ghost) > 0) {
                int distance = game.getShortestPathDistance(myNodeIndex, game.getGhostCurrentNodeIndex(ghost));
                if (distance < minDistance) {
                    minDistance = distance;
                    minGhost = ghost;
                }
            }
        }

        if (minGhost != null) {
            return game.getNextMoveTowardsTarget(myNodeIndex, game.getGhostCurrentNodeIndex(minGhost), Constants.DM.PATH);
        }
    	// get all possible moves at the queried position
    	MOVE[] myMoves = game.getPossibleMoves(myNodeIndex);
    	
    	MOVE lastMove = game.getPacmanLastMoveMade();
		if (Arrays.asList(myMoves).contains(lastMove)){
			return lastMove;
		}
		
		// don't go back (corner)
		for (MOVE move : myMoves){
			if (move != lastMove.opposite()){
				return move;
			}
		}
		
		// default
		return lastMove.opposite();
    }
	public MOVE atJunctionSim(){
    	
    	return MOVE.NEUTRAL;
    }
    
    
    public MOVE mcts(Game game, long timeDue){
    	// create MCTSTree object for simulation
        MCTSTree tree = new MCTSTree(game);
        tree.simulate(timeDue);
        
        return tree.getBestMove();
    }
    
    public MOVE mctsJunc(Game game, long timeDue){
    	// create MCTSTree object for simulation
        MCTSTree tree = new MCTSTree(game);
        tree.simulateJunc(timeDue);
        
        return tree.getBestMove();
    }
    
	/*
	 * This methods scan through the available pills and add them into 
	 * array which four directions 
	 */
	public void fetchPills(Game game, ArrayList<Integer> left, ArrayList<Integer> right, ArrayList<Integer> up, ArrayList<Integer> down, double[] moveValue){
		int[] pills = game.getPillIndices();
		
		//find the visible pills and divide them into pills less than Pac-Man's position or more.
		for (int i = 0; i < pills.length; i++) {
            //check which pills are available
			int leftDist=10000,rightDist=10000, upDist=10000, downDist=10000;
            Boolean pillStillAvailable = game.isPillStillAvailable(i);
            if (pillStillAvailable != null && pillStillAvailable) {
                if(!left.isEmpty()){
                	leftDist=game.getShortestPathDistance(left.get(0), pills[i]) + 1;
                }
                if(!right.isEmpty()){
                	rightDist = game.getShortestPathDistance(right.get(0), pills[i])+ 1;
                }
                if(!down.isEmpty()){
                	downDist = game.getShortestPathDistance(down.get(0), pills[i])+ 1;
                }
                if(!up.isEmpty()){
                	upDist = game.getShortestPathDistance(up.get(0), pills[i])+ 1;
                }
                //Add the pill to which ever path it will belong to
                if(leftDist<rightDist && leftDist<downDist && leftDist<upDist){
                	moveValue[0]+= (double)GameConstvalues[1]/(leftDist);//80
                }else if(rightDist<downDist && rightDist<upDist){
                	moveValue[1]+= (double)GameConstvalues[1]/(rightDist);//80
                }else if(upDist<downDist){
                	moveValue[2]+= (double)GameConstvalues[1]/(upDist);//80
                }else{
                	moveValue[3]+= (double)GameConstvalues[1]/(downDist);//80
                }
            }   
        }
	}
	
	public void fetchPowerPills(Game game, ArrayList<Integer> left, ArrayList<Integer> right, ArrayList<Integer> up, ArrayList<Integer> down,double[] moveValue){
		//Power Pills
		int[] powerPills = game.getPowerPillIndices();
		for(int i = 0;i<powerPills.length;i++){
			int leftDist=10000,rightDist=10000, upDist=10000, downDist=10000;
            Boolean pillStillAvailable = game.isPowerPillStillAvailable(i);
            if (pillStillAvailable != null && pillStillAvailable) {
                if(!left.isEmpty()){
                	leftDist=game.getShortestPathDistance(left.get(0), powerPills[i])+ 1;
                }
                if(!right.isEmpty()){
                	rightDist = game.getShortestPathDistance(right.get(0), powerPills[i])+ 1;
                }
                if(!down.isEmpty()){
                	downDist = game.getShortestPathDistance(down.get(0), powerPills[i])+ 1;
                }
                if(!up.isEmpty()){
                	upDist = game.getShortestPathDistance(up.get(0), powerPills[i])+ 1;
                }
                
                if(leftDist<rightDist && leftDist<downDist && leftDist<upDist){
                	moveValue[0]+=(double)GameConstvalues[2]/leftDist;//150
                }else if(rightDist<downDist && rightDist<upDist){
                	moveValue[1]+=(double)GameConstvalues[2]/rightDist;
                }else if(upDist<downDist){
                	moveValue[2]+=(double)GameConstvalues[2]/upDist;
                }else{
                	moveValue[3]+=(double)GameConstvalues[2]/downDist;
                }
            }   
		}
	}
	
	public void fetchGhosts(Game game, ArrayList<Integer> left, ArrayList<Integer> right, ArrayList<Integer> up, ArrayList<Integer> down,double[] moveValue){
		boolean ghostEdible = false;
		for (Constants.GHOST ghost : Constants.GHOST.values()) {
		int leftDist=10000,rightDist=10000, upDist=10000, downDist=10000;

        // If can't see these will be -1 so all fine there
	        int ghostLocation = game.getGhostCurrentNodeIndex(ghost);
	        if (ghostLocation != -1) {
	            if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0) {
	            	ghostEdible = false;
	            } else if (game.getGhostEdibleTime(ghost) > 0){
	            	ghostEdible = true;
	            }

	        	if(!left.isEmpty()){
	            	leftDist=game.getShortestPathDistance(left.get(0), ghostLocation)+ 1;
	            }
	            if(!right.isEmpty()){
	            	rightDist = game.getShortestPathDistance(right.get(0), ghostLocation)+ 1;
	            }
	            if(!down.isEmpty()){
	            	downDist = game.getShortestPathDistance(down.get(0), ghostLocation)+ 1;
	            }
	            if(!up.isEmpty()){
	            	upDist = game.getShortestPathDistance(up.get(0), ghostLocation)+ 1;
	
	            }
	            
	            if(ghostEdible){
		            if(leftDist<rightDist && leftDist<downDist && leftDist<upDist){
		            	moveValue[0]+=(double)GameConstvalues[4+game.getNumGhostsEaten()]/leftDist;
	                }else if(rightDist<downDist && rightDist<upDist){
	                	moveValue[1]+= (double)GameConstvalues[4+game.getNumGhostsEaten()]/rightDist;
	                }else if(upDist<downDist){
	                	moveValue[2]+= ((double)GameConstvalues[4+game.getNumGhostsEaten()]/upDist);
	                }else{
	                	moveValue[3]+= ((double)GameConstvalues[4+game.getNumGhostsEaten()]/downDist);
	                }
	            }else{
	            	if(leftDist<rightDist && leftDist<downDist && leftDist<upDist){
		            	moveValue[0]-=(double)GameConstvalues[3]/leftDist;//1000.00
	                }else if(rightDist<downDist && rightDist<upDist){
	                	moveValue[1]-=(double)GameConstvalues[3]/rightDist;//1000.00
	                }else if(upDist<downDist){
	                	moveValue[2]-=(double)GameConstvalues[3]/upDist;//1000.00
	                }else{
	                	moveValue[3]-=(double)GameConstvalues[3]/downDist;//1000.00
	                }
	            }
	        }
       
    }
		
	}
	
}
