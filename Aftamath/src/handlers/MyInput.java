package handlers;

public class MyInput {

	public static boolean[] keys;
	public static boolean[] pkeys;
	
	public static int NUM_KEYS = 76;
	public static final int UP = 0;
	public static final int DOWN = 1;
	public static final int LEFT = 2;
	public static final int RIGHT = 3;
	public static final int JUMP = 4;
	public static final int INTERACT = 5;
	public static final int USE = 7;
	public static final int ATTACK = 8;
	public static final int PAUSE = 9;
	public static final int ENTER = 10;
	public static final int DEBUG_UP = 11;
	public static final int DEBUG_DOWN = 12;
	public static final int DEBUG_LEFT = 13;
	public static final int DEBUG_RIGHT = 14;
	public static final int DEBUG = 15;
	public static final int DEBUG1 = 16;
	
	static {
		keys = new boolean[NUM_KEYS];
		pkeys = new boolean[NUM_KEYS];
	}
	
	public static void update(){
		for(int i = 0; i < NUM_KEYS; i++){
			pkeys[i] = keys[i];
		}
	}
	
	public static void setKey(int i, boolean b) { keys[i] = b; }
	public static boolean isDown(int i) { return keys[i]; }
	public static boolean isDown(){
		for(boolean b : keys)
			if(b)return b;
		return false;
	}
	public static boolean isPressed(int i) { return keys[i] && !pkeys[i]; }
	
}
