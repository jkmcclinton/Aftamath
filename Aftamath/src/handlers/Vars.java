package handlers;

import java.util.HashMap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import main.Main;

public class Vars {
	
	//collision layer data
	public static final short BIT_LAYERSPECIAL = 2;
	public static final short BIT_LAYERSPECIAL2 = 4;
	public static final short BIT_GROUND = 8;
	public static final short BIT_HALFGROUND = 16;
	public static final short BIT_BATTLE = 32;
	public static final short BIT_LAYER1 = 64;
	public static final short BIT_PLAYER_LAYER = 128;
	public static final short BIT_LAYER3 = 256;	
	
	//global constants
	public static final float DT = 1/60f;
	public static final float PPM = 100f;
	public static final float GRAVITY = -9.81f;
	public static final float ANIMATION_RATE = .21f;
	public static final float ACTION_ANIMATION_RATE = .09f;
	public static final float ALPHA = 254/255f;
	public static final int FONT_OFFSET = -33;
	public static final int OBJ_SCALE = 2;
	public static final int LIGHT_RAYS = 150;
	public static final int TILE_SIZE = 16;
	
	//light colors
	public static final Color DAY_LIGHT = new Color(0, 0, 0, ALPHA);
	public static final Color INDOOR_LIGHT = new Color(0, 0, 0, ALPHA);
	public static final Color NIGHT_LIGHT = new Color (14/255f, 11/255f, 28/255f, ALPHA);
	public static final Color DARK_LIGHT = new Color (1/255f, 1/255f, 1/255f, ALPHA);
	
	public static final Color DAY_OVERLAY = new Color(1, 1, 1, ALPHA);
	public static final Color SUNSET_GOLD = new Color(255/255f, 185/255f, 0/255f,ALPHA);
	public static final Color SUNSET_ORANGE = new Color(255/255f, 124/255f, 0/255f, ALPHA);
	public static final Color SUNSET_MAGENTA = new Color(213/255f, 37/255f, 109/255f, ALPHA);
	public static final Color NIGHT_OVERLAY = new Color(44/255f, 48/255f, 107/255f, ALPHA);
	public static final Color INDOOR = new Color(1, 1, 1, ALPHA);
	public static final Color SUNRISE = new Color(109/255f, 163/255f, 187/255f, ALPHA);
	public static final Color FROZEN_OVERLAY = new Color(139/255f, 195/255f, 217/255f, ALPHA/2f);
	
	public static final int PLAYER_SCENE_ID = 0;
	public static final int NARRATOR_SCENE_ID = 1000;
	public static final Array<Color> DEFAULT_COLOR_LIST = new Array<Color>();
	public static final Array<String> MALES = new Array<String>(new String[] {"doctordisco","narrator2","gangster1","gangster2","boyfriend1","boyfriend2","boyfriend3","boyfriend4",
					"kid1","kid2","richguy","burly1","burly2","reaper","magician","oldman1","oldman2","maleplayer1","maleplayer2","maleplayer3","maleplayer4",
					"bballer","boss1","boss2","cashier","hero1","hero2", "hobo","villain3", "villain4","biker1","robot1","policeman1",
					"policeman2","civilian1","civilian2","civilian3","civilian4"});
	public static final HashMap<String, Float> VOICES = new HashMap<>();


	/**
	 * removes all numbers from the given string
	 * @param s the original string
	 * @return
	 */
	public static String trimNumbers(String s){
		String newS = "";
		for (int i = 0; i< s.length();i++){
			if(!isNumeric(s.substring(i, i + 1))) newS += s.substring(i, i + 1);
			else return newS;
		}
		return newS;
	}

	/**
	 * removes every index of the given indicator from the source
	 * @param src the original string
	 * @param r the string to be removed from the source
	 * @return the resulting string  
	 */
	public static String remove(String src, String r){
		String[] tmp = src.split(r);
		String s= "";
		for (String i : tmp)
			s += i;
		return s;
	}
	
	/**
	 * limits each line of text to a maximum width
	 * @param dialog the original text
	 * @param hud if true, the maximum width is adjusted to the screen's width. else, the maximum width is adjusted to a textbox
	 * @return the original text with '/l' values inserted at the proper locations
	 */
	public static String formatDialog(String dialog, boolean hud){
		Array<String> text = new Array<>(dialog.split("/l"));
		
		int max;
		if(hud) max = 50;
		else max = 20;
		
		String s;
		int lastSpace = 0;
		for(int y = 0; y < text.size; y++){
			s = text.get(y);
			lastSpace=0;
			for(int x = 0; x< s.length(); x++){
				if (s.substring(x, x + 1).equals(" "))
					lastSpace = x;
				if(x > max){
					String extra = "";
					if(lastSpace!=0){
						extra = text.get(y).substring(lastSpace+1);
						text.set(y,  text.get(y).substring(0, lastSpace));
					} else {
						extra = text.get(y).substring(max+1);
						text.set(y, text.get(y).substring(0, max) + "-");
					}
					
					if(y <= text.size - 2)
						text.set(y+1, extra + " " + text.get(y+1));
					else
						text.add(extra);
					break;
				}
			}
		}
		
		String result = "";
		for(String l : text)
			result += l + "/l";
		result = result.substring(0, result.length() - 2);
		return result;
	}
	
	public static String formatHundreds(String s, int length){
		switch(length){
		case 1:
			s+="0";
		case 2:
			s+="0";
		}
		return s;
	}
	
	public static String addSpaces(String orig, int length){
		String spaces = "";
		for(int i = 0; i<length-orig.length(); i++)
			spaces+=" ";
		return spaces;
	}
	
	/**
	 * 
	 * @param s
	 * @return whether the given string is a numerical value
	 */
	public static boolean isNumeric(String s){
		if(s==null) return false;
		if(s.contains("E")){
			String mantissa = s.substring(0, s.indexOf("E"));
			String power = s.substring(s.indexOf("E")+1);
			return isNumeric(mantissa) && isNumeric(power);
		}
		
		if (s.matches("((-|\\+)?[0-9]+(\\.[0-9]+)?)+")) return true;
	    return false;
	}
	
	public static boolean isBoolean(String s){
		if(s==null) return false;
		return "true".equals(s.toLowerCase()) || "false".equals(s.toLowerCase());
	}
	
	/**
	 * assuming 's' is in format 'Word1 Word2 ...', this trims the given string and formats it
	 * in the proper camel case
	 * 
	 * NOTE: not accurate; only recognizes a select few of method names
	 * @param s 
	 * @return method name
	 */
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
	
	/**
	 * formats the time to the game's day night time
	 * @param time the time in seconds
	 * @param full whether or not 24hr time is used
	 * @return String time of day in the format 00:00 or 00:00 a.m/p.m.
	 */
	public static String formatDayTime(float time, boolean full){
		String h =  String.valueOf((int)(time*(24f/Main.DAY_TIME)));
		String m =  String.valueOf((int)((time*60f*(24f/Main.DAY_TIME))%60f));
		if(m.length()<2) m = 0 + m;
		
		if(full){
			if(h.length()<2) h = 0 + h;
			return h+":"+m;
		} else {
			String d = "";
			if(Integer.parseInt(h)>12){
				h= String.valueOf(Integer.parseInt(h)-12);
				if(h.length()<2) h = 0 + h;
				d=" p.m.";
			} else 
				d = " a.m.";
			return h +":"+ m +d;
		}
	}
	
	/**
	 * converts the given time to a string
	 * @param timeSecs time in seconds
	 * @return String in the format hrs:min:sec
	 */
	public static String formatTime(float timeSecs){
		String h = String.valueOf((int) (timeSecs/3600f));
		String m = String.valueOf((int) ((timeSecs/60f)%60f));
		String s = String.valueOf((int) (timeSecs%60f));
		
		if(m.length()<2) m = 0 + m;
		if(s.length()<2) s = 0 + s;
		
		return h+":"+m+":"+s;
	}
	
	/**
	 * mixes the RGB values of the two colors
	 * @param first 
	 * @param second
	 * @return the mixed color
	 * @see blendColors
	 */
	public static Color blendColors(Color first, Color second){
		return blendColors(1, 0, 2, first, second);
	}
	
	/**
	 * linearly mixes the RGB values of the two colors with relation to time
	 * @param t the current time; if outside the parameters start and end, the value is constained automatically
	 * @param start initial time
	 * @param end maximum time
	 * @param first the beginning color
	 * @param last the final color
	 * @return the mixed color; if either color is null, the resulting color is the instantiated one. 
	 * If both are null, the resulting color is Color.WHITE
	 */
	public static Color blendColors(float t, float start, float end, Color first, Color last){
		if(first==null || last==null){
			if(first==null && last==null)
				return Color.WHITE;
			if(first==null)
				return last;
			return first;
		}
		
		if(t>end) t = end;
		if(t<start) t = start;
		
		float mr = (last.r - first.r)/(float)(end - start);
		float mg = (last.g - first.g)/(float)(end - start);
		float mb = (last.b - first.b)/(float)(end - start);
		float ma = (last.a - first.a)/(float)(end - start);

		return new Color (
				mr*(t-start)+first.r, 
				mg*(t-start)+first.g, 
				mb*(t-start)+first.b, 
				ma*(t-start)+first.a);
	}
	
	/**
	 * produces a resultant vector in the direction of the target with the given magnitude
	 * @param start start position
	 * @param target end position
	 * @param magnitude
	 * @return
	 */
	public static Vector2 getVelocity(Vector2 start, Vector2 target, float magnitude){
		Vector2 direction = new Vector2(target.x - start.x, target.y - start.y);
		Vector2 bearing = new Vector2(0, 1);
		float cross = b2Cross(direction, bearing);		//reorder???
		
		if(cross == -0)
			return new Vector2(0, -magnitude);
		if(cross == 0)
			return new Vector2(0, magnitude);
		float angle = (float) (Math.atan2(direction.y, direction.x));
		
		return new Vector2((float) (magnitude*Math.cos(angle)), (float) (magnitude*Math.sin(angle)));
	}
	
	public static float b2Cross(Vector2 a, Vector2 b){
		return a.x * b.y - a.y * b.x;
	}
	
	public static Vector2 b2Cross(Vector2 a, float s){
		return new Vector2(s * a.y, -s * a.x);
	}

	static{
		DEFAULT_COLOR_LIST.add(Color.BLACK);
		DEFAULT_COLOR_LIST.add(Color.BLUE);
		DEFAULT_COLOR_LIST.add(Color.RED);
		DEFAULT_COLOR_LIST.add(Color.GREEN);
		DEFAULT_COLOR_LIST.add(Color.YELLOW);
		DEFAULT_COLOR_LIST.add(Color.PINK);
		DEFAULT_COLOR_LIST.add(Color.GRAY);
		DEFAULT_COLOR_LIST.add(Color.CLEAR);
		DEFAULT_COLOR_LIST.add(Color.CYAN);
		DEFAULT_COLOR_LIST.add(Color.DARK_GRAY);
		DEFAULT_COLOR_LIST.add(Color.MAGENTA);
		DEFAULT_COLOR_LIST.add(Color.MAROON);
		DEFAULT_COLOR_LIST.add(Color.NAVY);
		DEFAULT_COLOR_LIST.add(Color.OLIVE);
		DEFAULT_COLOR_LIST.add(Color.ORANGE);
		DEFAULT_COLOR_LIST.add(Color.PURPLE);
		DEFAULT_COLOR_LIST.add(Color.TEAL);
		DEFAULT_COLOR_LIST.add(Color.WHITE);

		VOICES.put("narrator1", .05f);
		VOICES.put("narrator2", -.1f);
		VOICES.put("gangster1", -.11f);
		VOICES.put("gangster2", -.1f);
		VOICES.put("boyfriend1", .1f);
		VOICES.put("boyfriend2", .1f);
		VOICES.put("boyfriend3", .1f);
		VOICES.put("girlfriend1", .23f);
		VOICES.put("girlfriend2", .08f);
		VOICES.put("girlfriend3", .21f);
		VOICES.put("burly1", -.51f);
		VOICES.put("burly2", -.54f);
		VOICES.put("oldman1", -.04f);
		VOICES.put("oldman2", .01f);
		VOICES.put("oldlady1", .17f);
		VOICES.put("oldlady2", .1f);
		VOICES.put("maleplayer1", -.05f);
		VOICES.put("maleplayer2", -.15f);
		VOICES.put("maleplayer3", -.2f);
		VOICES.put("maleplayer4", -.07f);
		VOICES.put("femaleplayer1", .18f);
		VOICES.put("femaleplayer2", .05f);
		VOICES.put("femaleplayer3", .2f);
		VOICES.put("femaleplayer4", .1f);
		VOICES.put("civilian1", -.1f);
		VOICES.put("civilian2", -.1f);
		VOICES.put("civilian3", -.1f);
		VOICES.put("civilian4", -.1f);
		VOICES.put("civilian5", .1f);
		VOICES.put("civilian6", .1f);
		VOICES.put("civilian7", .1f);
		VOICES.put("civilian8", .1f);
		VOICES.put("kid1", .1f);
		VOICES.put("kid2", .15f);
		VOICES.put("kid3", .29f);
		VOICES.put("kid4", .32f);
		VOICES.put("boss1", -.1f);
		VOICES.put("boss2", -.26f);
		VOICES.put("boss3", .2f);
		VOICES.put("coworker1", -.1f);
		VOICES.put("coworker2", -.1f);
		VOICES.put("coworker3", .2f);
		VOICES.put("coworker4", .3f);
		VOICES.put("robot1", .05f);
		VOICES.put("robot2", -.01f);
		VOICES.put("hero1", -.18f);
		VOICES.put("hero2", -.1f);
		VOICES.put("hero3", .1f);
		VOICES.put("hero4", .1f);
		VOICES.put("villain1", .01f);
		VOICES.put("villain2", .1f);
		VOICES.put("villain3", -.145f);
		VOICES.put("villain4", -.1f);
		VOICES.put("biker1", -.1f);
		VOICES.put("biker2", .1f);
		VOICES.put("policeman1", -.1f);
		VOICES.put("policeman2", -.1f);
		VOICES.put("policewoman1", .1f);
		VOICES.put("policewoman2", .1f);
		VOICES.put("richguy", -.09f);
		VOICES.put("hobo", -.38f);
		VOICES.put("bballer", -.2f);
		VOICES.put("cashier", -.01f);
		VOICES.put("reaper", .1f);
		VOICES.put("magician", -.1f);
		VOICES.put("witch", -.05f);
		VOICES.put("logger", -.15f);
		VOICES.put("hippie",  .05f);
	}
}
