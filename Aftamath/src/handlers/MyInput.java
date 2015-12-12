package handlers;

import java.util.HashMap;

public class MyInput {
	
	public static boolean isCaps = false; 
	
	public static HashMap<Input, Boolean> keys = new HashMap<>(), pkeys = new HashMap<>();
	static{
		for(Input k : Input.values()){
			keys.put(k, false);
			pkeys.put(k,  false);
		}
	}
	
	public static enum Input{
	 UP,
	 DOWN,
	 LEFT,
	 RIGHT,
	 JUMP,
	 INTERACT,
	 USE,
	 ATTACK,
	 SPECIAL,
	 RUN,
	 PAUSE,
	 ENTER,
	 DEBUG_UP,
	 DEBUG_DOWN,
	 DEBUG_LEFT,
	 DEBUG_RIGHT,
	 DEBUG_LEFT1,
	 DEBUG_RIGHT1,
	 DEBUG_LEFT2,
	 DEBUG_RIGHT2,
	 DEBUG_CENTER,
	 LIGHTS,
	 COLLISION,
	 DEBUG,
	 DEBUG2,
	 DEBUG_TEXT,
	 ZOOM_IN,
	 ZOOM_OUT,
	 CAPS
	}
	
	public static void update(){
		for(Input k : Input.values())
			pkeys.put(k, keys.get(k));
	}
	
	public static void setKey(Input k, boolean b) { keys.put(k, b);  }
	public static boolean isDown(Input k) { return keys.get(k) && pkeys.get(k); }
	public static boolean isDown(){
		for(Input k : keys.keySet())
			if(keys.get(k))
				return true;
		return false;
	}
	public static boolean isPressed(Input k) { return keys.get(k) && !pkeys.get(k); }
	public static boolean isUp(Input k) { return !keys.get(k) && pkeys.get(k); }
	
}
