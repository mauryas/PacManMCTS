package entrants.pacman.Evolution;
import java.util.Random;

public class Genome {
	private int[] gene = new int[8];//valid move, pill, power, ghost, Edible1, Edible2, Edible3, Edible4
	private int fitness = 0;
	public void createRandom(){
		Random r = new Random();
		//Randomly Initialize the gene
		for(int i = 0; i<gene.length;i++){
			if(i==1){
				gene[i] = r.nextInt(100);
			}else if(i==2 || i==1){
				gene[i] = r.nextInt(500);
			}else{
				gene[i] = r.nextInt(1500);
			}
			
		}
	}
	//return gene
	public int[] getGene(){
		return this.gene;
	}
	
	//randomly initialize the genes
	public Genome(){
		//Randomly populate the genes
		createRandom();
	}
		
	//mutate the genes
	public Genome[] mutation(Genome[] mutation){
		Random r = new Random();
		//Randomly increment the one of the values in the array
		for(int i = 0; i<mutation.length;i++){
			mutation[i].gene[r.nextInt(gene.length-1)]+=5;
		}
		
		return mutation;
	}
	
	//1 point cross over of two randomly generated genes
	public Genome[] crossover(Genome[] genomeCross){
		//1 point crossover
		Random r = new Random();
		
		for(int i = 0; i<genomeCross.length;i++){
			int firstGene = r.nextInt(genomeCross.length);
			int secondGene = r.nextInt(genomeCross.length);
			int crossOverPos = r.nextInt(genomeCross[0].gene.length);//Pick the position to swap the genes
			
			//swap the values
			genomeCross[firstGene].gene[crossOverPos] += genomeCross[secondGene].gene[crossOverPos];
			genomeCross[secondGene].gene[crossOverPos] += genomeCross[firstGene].gene[crossOverPos] - genomeCross[secondGene].gene[crossOverPos];
			genomeCross[firstGene].gene[crossOverPos] += genomeCross[secondGene].gene[crossOverPos] - genomeCross[secondGene].gene[crossOverPos];
		}
		
		return genomeCross;
	}	
	
	public void setFitness(int score){
		this.fitness = score;
	}
	
	public int getFitness(){
		return this.fitness;
	}
	
	public Genome[] selection(Genome[] genomeSelection){
		Genome[] tempGenome = new Genome[genomeSelection.length];
		
		Random r = new Random();
		int first = -1, second = -1;
		for(int i = 0; i< genomeSelection.length;i++){
			tempGenome[i] = genomeSelection[i];
		}
		//tournament: randomly select 2 and then the gene with max. fitness is 
		//added to the genome 
		for(int i = 0; i<genomeSelection.length;i++){
			first = r.nextInt(tempGenome.length);
			second = r.nextInt(tempGenome.length);
			
			if(genomeSelection[first].getFitness()>genomeSelection[second].getFitness()){
				tempGenome[i] = genomeSelection[first];
			} else {
				tempGenome[i] = genomeSelection[second];
			}
		}
		
		return tempGenome;
		
	}
}