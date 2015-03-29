package scenes;

import handlers.Vars;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Stack;

import main.Play;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import entities.Entity;
import entities.Mob;
import entities.NPC;
import entities.Player;
import entities.SpeechBubble;


/* ----------------------------------------------------------------------
 *  this class contains a file that controls the object it's attached to
 *  and also provides the necessary functions to parse the script file
 * ----------------------------------------------------------------------  */
public class Script {

	public String ID;
	public Array<String> source;
	public Stack<int[]> choiceIndices;
	public boolean[] flags;
	public boolean paused, limitDistance, forcedPause, dialog;
	public int[] choices;
	public int current, index, waitTime, time;
	private int[] indices;

	//outside references
	private Entity activeObj, owner;
	private Play play;
	private Player player;

	//private int c;

	public Script(String scriptID, Play p, Player player, Entity owner){
		try {
			ID = scriptID;
			loadScript(scriptID);
			getIndicies();
			choiceIndices = new Stack<>();
			getFlags();
			getDistanceLimit();

			activeObj = new Entity();

			setIndex(0);

			play = p;
			this.player = player;
			this.owner = owner;
		} catch (Exception e) {
			System.out.println("!! Script issue !!");
			e.printStackTrace();
		}
	}

	public void update(){
		if (player == null) player = play.player;

		if(play.analyzing){
			if(!paused){
				if (!activeObj.controlled) 
					analyze();
			}
			else {
				if (waitTime > 0){
					time++;
					if (time/50 >= waitTime){ 
						waitTime = 0;
						paused = false;
					}
				} else {
					if (play.getCam().reached && !play.getCam().moving && 
							!forcedPause && !dialog) {
						paused = false;
						//System.out.println("\n!!!  unpausing   !!!\n");
					}
				}
			}
		}
	}

	public void analyze(){
		//System.out.println(source.get(index));

		if(!play.analyzing && !paused) {
			index = indices[current];
			play.analyzing = true;
		}

		String line = source.get(index);
		String command;//, s;
		String[] tmp;
		Entity obj;

		if (line.indexOf(" ") == -1) command = line;
		else command = line.substring(0, line.indexOf(" "));

		switch(command.toLowerCase()){
		case "attack":
			break;
		case "action":
			doAction(line);
			break;
		case "value":
		case "changeval":
			changeValue(line);
			break;
		case "else":
			while(!source.get(index).toLowerCase().equals("end"))
				index++;
			index--;
			break;
		case "if":
			if (!compare(line)){
				while(!source.get(index).toLowerCase().startsWith("endif") && 
						!source.get(index).toLowerCase().startsWith("else"))
					index++;
			}
			break;
		case "faceplayer":
			if (Vars.isNumeric(lastArg(line)))
				obj = findObject(convertToNum(line));
			else
				obj = owner;

			if (obj != null)
				if (obj instanceof Mob){
					if (obj instanceof NPC)
						((NPC) obj).setState(NPC.FACEPLAYER);
					else
						((Mob) obj).facePlayer();
				}

			break;
		case "face":
		case "faceobject":
			if(Vars.isNumeric(middleArg(line)))
				obj = findObject(Integer.parseInt(middleArg(line)));
			else
				obj = owner;
			Entity e = findObject(convertToNum(line));

			if (obj != null && e != null)
				if (obj instanceof NPC)
					((NPC) obj).setState(NPC.FACEOBJECT, obj);
				else obj.faceObject(e);
			break;
		case "forcefollow":
			if (player.getPartner()!=null){
				if (player.getPartner().getName()!=null){
					player.stopPartnerDisabled = Boolean.parseBoolean(lastArg(line));
				}
			}
			break;
		case "resetstate":
			obj = findObject(convertToNum(line));
			if (obj != null)
				if(obj instanceof NPC)
					((NPC) obj).resetState();
			break;
		case "lockplayer":
			play.setStateType(Play.LISTEN);
			break;
		case "lock":
			break;
		case "lowerdialog":
			play.hud.hide();
			break;
		case "movecamera":
			createFocus(line);
			break;
		case "focus":
		case "focuscamera":
			Vector2 focus = null;
			Entity object = findObject(convertToNum(line));

			if (object != null) {
				focus = object.getPosition();
				paused = true;
				play.getCam().setFocus(focus);
			}
			break;
		case "unfocus":
		case "unfocuscamera":
		case "removefocus":
			play.getCam().removeFocus();
			break;
		case "moveobject":
			obj = findObject(Integer.parseInt(middleArg(line)));
			if (obj != null) {
				obj.setGoal(convertToNum(line) - obj.getPosition().x * Vars.PPM);
				activeObj = obj;
			}
			break;
		case "moveplayer":
			player.setGoal(convertToNum(line));
			activeObj = player;
			break;
		case "setemotion":
			String arg = lastArg(line);
			int emotion = 0;
			if(Vars.isNumeric(arg)){
				emotion = Integer.parseInt(arg);
				if (emotion > 4 || emotion < 0) emotion = 0;
			} else 
				try {
					Field f = Mob.class.getField(arg.toUpperCase());
					emotion = f.getInt(f);
				} catch(Exception e1){
					
				}
				
			play.currentEmotion = emotion;
		case "setflag":
			play.getScene().script.flags[Integer.parseInt(middleArg(line))] =
			Boolean.parseBoolean(lastArg(line));
			break;
		case "setspeaker":
			Entity speaker = null;

			if (Vars.isNumeric(lastArg(line)))
				speaker = findObject(convertToNum(line));
			
			else switch(lastArg(line)) {
			case "partner":
				if(player.getPartner().getName() != null)
					speaker = player.getPartner();
				break;
			case "narrator":
				speaker = play.narrator;
				break;
			case "this":
				speaker = owner;
				break;
			}

			if(speaker != null) {
				if(speaker instanceof Mob) 
				{if( play.hud.getFace() != speaker) play.hud.changeFace((Mob) speaker);}
				else play.hud.changeFace(null);
			}
			break;
		case "setscript":
			setIndex(Integer.parseInt(lastArg(line)) - 1);
			break;
		case "setchoice":
			if (lastArg(line).toLowerCase().equals("yesno")){
				choices = new int[2];
				choices[0] = 7;
				choices[1] = 8;
			} else {
				tmp = lastArg(line).split(",");
				choices = new int[tmp.length];
				for (int j = 0; j < tmp.length; j++)
					choices[j] = Integer.parseInt(tmp[j]); 
			}

			int i = index; int j = 0;
			int[] tmp1 = new int[choices.length];
			while((j != choices.length ||
					!source.get(i).toLowerCase().startsWith("endchoice")) && i < source.size){
				if (source.get(i).toLowerCase().startsWith("choice")) {
					tmp1[j] = i;
					j++;
				}
				i++;
			}
			choiceIndices.add(tmp1);

			play.displayChoice(choices);
			play.setStateType(Play.CHOICE);
			play.choosing = true;
			paused = true;
			break;
		case "spawn":
			spawn(line);
			break;
		case "text":
			text(line);
			break;
		case "wait":
			time = 0;
			waitTime = convertToNum(line);
			paused = true;
			break;
		case "pause":
			paused = forcedPause = true;
			break;
		case "end":
			while(!source.get(index + 1).toLowerCase().startsWith("endchoice"))
				index++;
			break;
		case "endchoice":
			choiceIndices.pop();
			break;
		case "zoom":
			if (middleArg(line).toLowerCase().equals("set"))
				//(Float.parseFloat(lastArg(line)));
				break;
		case "done":
			finish();
			break;
		}

		if(index < source.size-1) index++;
	}

	private void finish(){
		//System.out.println("hi");

		play.setStateType(Play.MOVE);
		play.analyzing = false;
		if(play.hud.raised)
			play.hud.hide();

		play.getCam().removeFocus();
		play.currentScript = null;

		for (Entity d : play.getObjects()){
			if (d instanceof NPC)
				((NPC) d).resetState();
		}

		//redisplay speechbubble
		if (player.getInteractable() == owner)
			new SpeechBubble(owner, owner.getPosition().x*Vars.PPM + 6, owner.height + 5  +
					owner.getPosition().y*Vars.PPM, 1);
	}

	public void setIndex(int i){
		if(i < indices.length)
			current = i;
	}

	private void loadScript(String path) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader("res/scripts/" + path + ".txt"));
		try {
			source = new Array<>();
			String line = br.readLine();

			while (line != null ) {;
			source.add(line);
			line = br.readLine();
			}
		} finally {
			br.close();
		}
	}

	public void getDistanceLimit(){
		for (String line : source){
			if(firstArg(line.toLowerCase()).equals("limit:")){
				limitDistance = Boolean.parseBoolean(lastArg(line));
				return;
			}
		}
	}

	private void getFlags(){
		if (source.get(0).toLowerCase().startsWith("flags:")){
			flags = new boolean[Integer.parseInt(lastArg(source.get(0)))];
		}
	}

	private void getIndicies(){
		Array<String> tmp = new Array<>();
		for (int i = 0; i< source.size; i++)
			if(source.get(i).toLowerCase().startsWith("script")) 
				tmp.add(String.valueOf(i));

		indices = new int[tmp.size];

		for (int i = 0; i < tmp.size; i++)
			indices[i] = Integer.parseInt(tmp.get(i));

	}

	private int convertToNum(String s){
		return Integer.parseInt(lastArg(s));
	}

	private String firstArg(String s){
		int i = s.indexOf(" ");

		if (i != -1) return s.substring(0, i);
		else return "";
	}

	private String middleArg(String s){
		int i = s.lastIndexOf(" ");
		int c = s.indexOf(" ");
		if (i != -1 && c != -1) return s.substring(c + 1, i);
		else return "";
	}

	private String lastArg(String s){
		int i = s.lastIndexOf(" ");
		if (i != -1) return s.substring(i + 1);
		else return "";
	}

	private int countArgs(String line){
		int i = line.indexOf(" ");
		int c = 1;

		while(i >= 0){
			c++;
			line = line.substring(i + 1);
			i = line.indexOf(" ");
		}
		return c;
	}

	private String removeSpaces(String s){
		String[] tmp = s.split(" ");

		s = "";
		for (String i : tmp)
			s += i;
		return s;
	}

	private Entity findObject(int ID){
		for(Entity d : play.getObjects()){
			if (d.getSceneID() == ID) return d;
		}
		return null;
	}

	public void getChoiceIndex(int choice){
		index =  choiceIndices.peek()[choice];
		play.setStateType(Play.LISTEN);
	}

	private void doAction(String line){
		Entity obj = findObject(Integer.parseInt(middleArg(line)));
		if (obj != null) obj.doAction(convertToNum(line));
	}

	private void createFocus(String line){
		Vector2 focus = null;
		String object = middleArg(line);

		if (Vars.isNumeric(object))
			focus = new Vector2(Float.parseFloat(object), (float) convertToNum(line));

		if (focus != null){
			paused = true;;
			play.getCam().setFocus(focus);
		}
	}

	private void text(String line){
		ArrayDeque<String> displayText = new ArrayDeque<>();
		String s;

		while(source.get(index).toLowerCase().startsWith("text")){
			s = source.get(index).substring(source.get(index).indexOf("\"") + 1);

			if(s.indexOf("/player") >= 0){
				s = s.substring(0, s.indexOf("/player")) + player.getName() + 
						s.substring(s.indexOf("/player") + "/player".length());
			}

			if(s.indexOf("/partner") >= 0){
				s = s.substring(0, s.indexOf("/partner")) + player.getPartner().getName() + 
						s.substring(s.indexOf("/partner") + "/partner".length());
			}

			if(s.indexOf("/house") >= 0){
				s = s.substring(0, s.indexOf("/house")) + player.getHome().getType() + 
						s.substring(s.indexOf("/house") + "/house".length());
			}

			if(s.indexOf("/address") >= 0){
				s = s.substring(0, s.indexOf("/address")) + player.getHome().getType() + 
						s.substring(s.indexOf("/address") + "/address".length());
			}

			displayText.add(s);
			index++;
		}

		s = line.substring(line.indexOf("\"") + 1);
		s = removeSpaces(s);
		if (countArgs(line.substring(0, line.indexOf("\"")) + s) == 2)
			paused = dialog = true;

		index--;
		play.setDispText(displayText);
		play.speak();
	}

	private void changeValue(String line){
		line = line.substring(line.indexOf(" ") + 1);
		Entity obj = null;
		String function = firstArg(line);
		String object = middleArg(line);
		int value = 0;

		try{
			value = Integer.parseInt(lastArg(line));
		} catch(Exception e){
			e.printStackTrace();
		}


		if (Vars.isNumeric(firstArg(object))){
			obj = findObject(Integer.parseInt(firstArg(object)));
			if (obj == null) return;
			object = lastArg(object);
		} 

		if (function.toLowerCase().equals("add")){
			switch(object) {
			case "playerhealth":
				if(value >= 0) player.heal(value);
				else player.damage(value); 
				break;
			case "playermoney":
				if(value >= 0) player.addMoney(value);
				else player.subtractMoney(value);
				break;
			case "playerbravery":
				player.setBravery(player.getBravery() + value);
				break;
			case "playernicety":
				player.setNiceness(player.getNiceness() + value);
				break;
			case "love":
			case "relationship":
				if (value >= 0) player.increaseRelationship(value);
				else player.decreaseRelationship(value);
				break;
			case "health":
				if (obj != null) {
					if (value >= 0) ((Mob) obj).heal(value);
					else ((Mob) obj).damage(value);
				}
				break;
			}
		} else if (function.toLowerCase().equals("set")){
			double v;
			switch(object) {
			case "playerhealth":
				v = value - player.getHealth();
				if(v >= 0) player.heal(v);
				else player.damage(v);
				break;
			case "playermoney":
				v = value - player.getMoney();
				if(v >= 0) player.addMoney(v);
				else player.subtractMoney(v);
				break;
			case "playerbravery":
				player.setBravery(value);
				break;
			case "playernicety":
				player.setNiceness(value);
				break;
			case "love":
			case "relationship":
				v = value - player.getRelationship();
				if(v >= 0) player.increaseRelationship(v);
				else player.decreaseRelationship(v);
				break;
			case "health":
				if (obj != null) {
					v = value - ((Mob) obj).getHealth();
					if (v >= 0) ((Mob) obj).heal(v);
					else ((Mob) obj).damage(v);
				}
				break;
			}
		}
	}

	public String peek(){ 
		if (index <= source.size - 2)
			return firstArg(source.get(index + 1));
		else return null;
	}

	public void readNext(){
		analyze();
		index++;
	}

	private boolean compareFlag(String line){
		String arguments = line.substring(line.indexOf(" ") + 1);
		int flag = Integer.parseInt(firstArg(arguments));
		boolean value = Boolean.parseBoolean(lastArg(arguments));
		return play.getScene().script.flags[flag] = value;
	}

	private boolean compare(String line){
		String arguments = middleArg(line);
		String object = firstArg(arguments);
		String condition = lastArg(arguments);
		String value = lastArg(line);

		//System.out.println("\no: " + object + "\nc: " + condition + "\nv: " + value);


		if (Vars.isNumeric(firstArg(object))){
			String type = middleArg(arguments);
			Entity obj = findObject(Integer.parseInt(object));
			if (obj == null) return false;

			switch (type){
			case "location":
				object = String.valueOf((int) obj.getPosition().x);
			case "health":
				if (obj instanceof Mob) object = String.valueOf(((Mob) obj).getHealth());
				else return false;
				break;
			}
		} else if(object.toLowerCase().equals("flag")){
			return compareFlag(line);
		} else	{
			switch(object) {
			case "playerhealth":
				object = String.valueOf(player.getHealth());
				break;
			case "playergender":
				object = player.getGender();
				break;
			case "playerlocation":
				object = String.valueOf((int) player.getPosition().x);
				break;
			case "playermoney":
				object = String.valueOf(player.getMoney());
				break;
			case "playerbravery":
				object = String.valueOf(player.getBravery());
				break;
			case "playernicety":
				object = String.valueOf(player.getNiceness());
				break;
			case "love":
			case "relationship":
				object = String.valueOf(player.getRelationship());
				break;
			case "partnerid":
				object = player.getPartner().getID();
				break;
			case "house":
				object = player.getHome().getType();
			}
		}

		//actual comparator
		switch (condition){
		case "=":
			if (Vars.isNumeric(object) && Vars.isNumeric(value))
				return Double.parseDouble(object) == Double.parseDouble(value);
			else
				return object.equals(value);
		case ">":
			if (Vars.isNumeric(object) && Vars.isNumeric(value))
				return Double.parseDouble(object) > Double.parseDouble(value);
		case ">=":
			if (Vars.isNumeric(object) && Vars.isNumeric(value))
				return Double.parseDouble(object) >= Double.parseDouble(value);
		case "<":
			if (Vars.isNumeric(object) && Vars.isNumeric(value))
				return Double.parseDouble(object) < Double.parseDouble(value);
		case "<=":
			if (Vars.isNumeric(object) && Vars.isNumeric(value))
				return Double.parseDouble(object) <= Double.parseDouble(value);
		default:
			return false;
		}
	}

	public void spawn(int obj, Vector2 location){
		spawn("spawn " + obj, location);
	}

	private void spawn(String line){
		Vector2 location = new Vector2(Float.parseFloat(lastArg(middleArg(line))),
				Float.parseFloat(lastArg(line)));
		spawn(line, location);
	}

	private void spawn(String line, Vector2 location){
		int c = 0;
		Entity obj = getObjectType(firstArg(line), location);

		for(Entity e : play.getObjects())
			if (e.getSceneID() < c)
				c = e.getSceneID();

		if (obj != null){
			if (obj instanceof Mob)
				obj.setSceneID(c);
			play.addObject(obj);
		}
	}
	
	private Entity getObjectType(String indicator, Vector2 location) {
		return getObjectType(indicator, location, Vars.BIT_LAYER1);
	}

	private Entity getObjectType(String indicator, Vector2 location, Short layer){
		Class<?> c;
		try {
			c = Class.forName(indicator);
			Class<?> C = c.getSuperclass();

			while(C != null) {
				if (C.getName().toLowerCase().equals("scene"))
					break;
				C = C.getSuperclass();
			}
			
			Constructor<?> cr = c.getConstructor(Float.class, Float.class, Short.class);
			Object o = cr.newInstance(location.x, location.y, layer);
			return (Entity) o;
			
		} catch (NoSuchMethodException | SecurityException e) {
			System.out.println("no such constructor");
		} catch (InstantiationException e) {
			System.out.println("cannot instantiate object");
		} catch (IllegalAccessException e) {
			System.out.println("cannot access object");
			//e.printStackTrace();
		} catch (IllegalArgumentException e) {
			System.out.println("illegal argument");
			//e.printStackTrace();
		} catch (InvocationTargetException e) {
			System.out.println("cannot invoke target");
			//e.printStackTrace();
		} catch (ClassNotFoundException e1) {
			System.out.println("Class \"" + indicator + "\" not found.");
		}

		return null;
	}

	public void setPlayState(Play gs) { play = gs; }
	public Entity getOwner(){ return owner; }
	public Entity getActiveObject(){ return activeObj; }
}
