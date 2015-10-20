package entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class Path extends Entity {

	public boolean completed;
	
	private int current = 0, maxRun;
	private float totCompleted;
	private boolean forward = true, reached;
	private Array<Vector2> points;
	private Behavior behavior;
	
	
	public enum Behavior {
		ONCE, RETURN, MULTIPLE, CONTINUOUS
	}
	
	public Path(){
		points = new Array<Vector2>();
		behavior = Behavior.ONCE;
	}
	
	public Path(String pathName, String behavior, Array<Vector2> points){
		this(pathName, behavior, points, 0);
	}
	
	public Path(String pathName, String behavior, Array<Vector2> points, int maxRun){
		this.points = points;
		this.maxRun = maxRun;
		this.ID = pathName;

		try{
			Behavior b = Behavior.valueOf(behavior.toUpperCase());
			this.behavior = b;
		}catch(Exception e){
			this.behavior = Behavior.ONCE;
		}
	}
	
	public Vector2 getCurrent(){ return points.get(current); }
	
	//set the current node to the next possible point
	public void stepIndex(){
		//is another point possible
		if(completed)
			return;
		
		//determine if last node has been reached
		//depending on direction
		if(!reached){
			if(forward)
				if(current<= points.size-2)
					current++;
				else{
					reached = true;
					totCompleted+=.5f;
				}
			else //backwards traversal
				if(current >= 1)
					current--;
				else {
					reached = true;
					totCompleted+=.5f;
				}
		}

		// determine if behavior has been met
		if(reached)
			switch(behavior){
			case ONCE: // go from start to end
				completed = true;
				break;
			case RETURN: // go from start to end and back to start
				if(totCompleted == 1)
					completed = true;
				else {
					reached = false;
					forward = !forward;
				}
				break;
			case MULTIPLE: // go back and forth multiple times
				if(totCompleted == maxRun)
					completed = true;
				else{
					reached = false;
					forward = !forward;
				}
				break;
			case CONTINUOUS: //traverse path indefinitely
				reached = false;
				forward = !forward;
				break;
			default:
				break;
			}
	}
	
	public String toString(){
		return ID+": ["+x+","+y+"]";
	}
	
	public boolean equals(Object o){
		if(o instanceof Path)
			return ID.equals(((Path) o).ID);
		return false;
	}
}
