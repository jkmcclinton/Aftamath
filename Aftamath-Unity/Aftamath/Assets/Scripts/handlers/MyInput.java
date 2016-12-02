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
	 RESPAWN,
	 RENDER,
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
	
	
	public class XBox360Pad {
	    /*
	     * It seems there are different versions of gamepads with different ID 
	     Strings.
	     * Therefore its IMO a better bet to check for:
	     * if (controller.getName().toLowerCase().contains("xbox") &&
	                   controller.getName().contains("360"))
	     *
	     * Controller (Gamepad for Xbox 360)
	       Controller (XBOX 360 For Windows)
	       Controller (Xbox 360 Wireless Receiver for Windows)
	       Controller (Xbox wireless receiver for windows)
	       XBOX 360 For Windows (Controller)
	       Xbox 360 Wireless Receiver
	       Xbox Receiver for Windows (Wireless Controller)
	       Xbox wireless receiver for windows (Controller)
	     */
	    //public static final String ID = "XBOX 360 For Windows (Controller)";
	    public static final int BUTTON_X = 2;
	    public static final int BUTTON_Y = 3;
	    public static final int BUTTON_A = 0;
	    public static final int BUTTON_B = 1;
	    public static final int BUTTON_BACK = 6;
	    public static final int BUTTON_START = 7;
	    public static final int BUTTON_LB = 4;
	    public static final int BUTTON_L3 = 8;
	    public static final int BUTTON_RB = 5;
	    public static final int BUTTON_R3 = 9;
	    public static final int AXIS_LEFT_X = 1; //-1 is left | +1 is right
	    public static final int AXIS_LEFT_Y = 0; //-1 is up | +1 is down
	    public static final int AXIS_LEFT_TRIGGER = 4; //value 0 to 1f
	    public static final int AXIS_RIGHT_X = 3; //-1 is left | +1 is right
	    public static final int AXIS_RIGHT_Y = 2; //-1 is up | +1 is down
	    public static final int AXIS_RIGHT_TRIGGER = 4; //value 0 to -1f
	}
	
}
