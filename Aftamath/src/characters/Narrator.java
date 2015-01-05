package characters;

public class Narrator extends NPC {

	public Narrator(float x, float y, short layer) {
		super("narrator", "narrator", x, y, 0, 0, layer);
	}
	
	public void yawn(){}
	public void special(){}
	
	public void doAction(int action){
		int[] tmp = {0, 4, 1, 2, 2, 4, 2, 2, 3, 4, 3, 4};
		 actionLengths = tmp;
		 actionTypes = tmp.length;
	}

}
