package handlers;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

public class CameraTrack {

	public int current;
	
	private Array<Pair<Vector2, Integer>> points;
	
	public CameraTrack(String points){
		
	}
	
	public void step(){
		if (current<points.size)
			current++;
	}
	
	public void reset(){ current = 0; }
	
	public Vector2 getPoint(){ return points.get(current).getKey(); }
}
