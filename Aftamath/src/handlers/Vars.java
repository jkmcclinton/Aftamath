package handlers;

public class Vars {
	
	//collision type data
	public static final short BIT_GROUND = 2;
	public static final short BIT_HALFGROUND = 4;
	public static final short BIT_LAYER1 = 8;
	public static final short BIT_LAYER2 = 16;
	public static final short BIT_LAYER3 = 32;	
	public static final short BIT_PROJECTILE = 64;
	
	//global constants
	public static final float PPM = 100f;
	public static final float GRAVITY = -9.81f;
	public static final float ANIMATION_RATE = 1/5f;
	public static final float ACTION_ANIMATION_RATE = .08f;
	public static final int FONT_OFFSET = -33;
	public static final int OBJ_SCALE = 2;
	public static final int LIGHT_RAYS = 50;

	public static String trimNumbers(String s){
		String newS = "";
		for (int i = 0; i< s.length();i++){
			if(!isNumeric(s.substring(i, i + 1))) newS += s.substring(i, i + 1);
			else return newS;
		}
		return newS;
	}
	
	public static boolean isNumeric(String s){
		 if (s.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+")) return true;
	    return false;
	}
	
	//assuming 's' is in format 'Word1 Word2 ...'
	public static String formatMethodName(String s){
		if (s.toLowerCase().equals("resume"))
		 return "unpause";
		
		if (s.toLowerCase().equals("quit to menu"))
			return "quitToMenu";
		
		String r[] = s.split(" ");
		for(String w : r)
			w = w.substring(0, 1).toUpperCase() + w.substring(1).toLowerCase();
		r[0] = r[0].toLowerCase();
		
		s = "";
		for (String w : r)
			s += w;
		
		return s;
	}
}
