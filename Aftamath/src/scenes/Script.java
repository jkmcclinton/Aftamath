package scenes;

import handlers.Pair;
import handlers.Vars;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;

import main.Play;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
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
	public Stack<HashMap<String, Integer>> choiceIndices;
	public boolean paused, limitDistance, forcedPause, dialog;
	public int[] choices;
	public String[] messages;
	public int current, index, waitTime, time;

	//outside references
	private Entity activeObj, owner;
	private Play play;
	private Player player;
	private HashMap<String, Integer> subScripts;

	//private int c;

	public Script(String scriptID, Play p, Player player, Entity owner){
		try {
			ID = scriptID;
			loadScript(scriptID);
			getIndicies();
			choiceIndices = new Stack<>();
			getDistanceLimit();

			activeObj = new Entity();

			setIndex();

			play = p;
			this.player = player;
			this.owner = owner;
		} catch (Exception e) {
			System.out.println("!! Script init issue !!");
			e.printStackTrace();
		}
	}

	public void update(){
		if (player == null) player = play.player;

		if(play.analyzing){
			if(!paused){
				if (!activeObj.controlled) 
					analyze();
			} else {
				if (waitTime > 0){
					time++;
					if (time/50 >= waitTime){ 
						waitTime = 0;
						paused = false;
					}
				} else
					if (play.getCam().reached && !play.getCam().moving && 
							!forcedPause && !dialog)
						paused = false;
			}
		}
	}

	public void analyze(){
		//System.out.println(source.get(index));

		if(!play.analyzing && !paused) {
			index = current;
			play.analyzing = true;
		}

		String line = source.get(index);
		String command;//, s;
		String[] tmp;
		Entity obj = null;

		if (!line.startsWith("#")){
			if (line.indexOf("(") == -1)
				if(line.startsWith("["))
					command = line.substring(line.indexOf("[")+1, line.indexOf("]"));
				else command = line;
			else command = line.substring(0, line.indexOf("("));

			System.out.println(command);
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
			case "face":
			case "faceobject":
				Entity target=null;
				switch(firstArg(line)){
				case "player":
					obj = player;
					break;
				case "narrator":
					obj = play.narrator;
					break;
				case "partner":
					obj = player.getPartner();
					break;
				}
				switch(lastArg(line)){
				case "player":
					target = player;
					break;
				case "narrator":
					target = play.narrator;
					break;
				case "partner":
					target = player.getPartner();
					break;
				}
				
				if (obj != null && target != null)
					if (obj instanceof NPC)
						((NPC) obj).setState(NPC.FACEOBJECT, obj);
					else obj.faceObject(target);
				break;
			case "forcefollow":
				if (player.getPartner()!=null){
					if (player.getPartner().getName()!=null){
						player.stopPartnerDisabled = Boolean.parseBoolean(lastArg(line));
					}
				}
				break;
			case "follow":
				break;
			case "resetstate":
				obj = findObject(lastArg(line));
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
				Entity object = findObject(lastArg(line));

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
				obj = findObject(middleArg(line));
				if (obj != null) {
					obj.setGoal(convertToNum(line) - obj.getPosition().x * Vars.PPM);
					activeObj = obj;
				}
				break;
			case "moveplayer":
				player.setGoal(convertToNum(line));
				activeObj = player;
				break;
			case "setflag":
				try{
					play.history.setFlag(firstArg(line), Boolean.parseBoolean(lastArg(line)));
				} catch (Exception e){
					System.out.println("Could not set flag \"" +firstArg(line) + "\"");
				}
				break;
			case "setspeaker":
				Entity speaker = null;

				switch(firstArg(line)) {
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
					default:
						speaker = findObject(firstArg(line));
				}

				if(speaker != null) {
					if(speaker instanceof Mob){
						if( play.hud.getFace() != speaker) 
							play.hud.changeFace((Mob) speaker);
						}
					else play.hud.changeFace(null);
					if(countArgs(line) ==2)
						play.getCam().setFocus(speaker.getPosition());
				}
				
				break;
			case "setscript":
				setIndex(lastArg(line));
				break;
			case "setchoice":
				if (lastArg(line).toLowerCase().equals("yesno")){
					choices = new int[2]; messages = new String[2];
					choices[0] = 6; messages[0] = "Yes";
					choices[1] = 7; messages[1] = "No";
				} else {
					tmp = args(line);
					choices = new int[tmp.length];
					messages = new String[tmp.length];
					String num, mes;

					//syntax ---- typeNum:Message
					for (int j = 0; j < tmp.length; j++){
						num = tmp[j].split(":")[0];
						mes = tmp[j].split(":")[1];
						choices[j] = Integer.parseInt(num.replaceAll(" ", "")); 
						messages[j] = new String(mes);
					}
				}

				int i = index; int j = 0;
				HashMap<String, Integer> tmp1 = new HashMap<>();
				String s1;
				while((j != choices.length ||
						!source.get(i).toLowerCase().startsWith("endchoice")) && i < source.size){
					if (source.get(i).toLowerCase().startsWith("[")) {
						s1 = source.get(i);
						s1 = s1.substring(s1.indexOf(" ")+1, s1.indexOf("]"));
						tmp1.put(s1, i);
						j++;
					}
					i++;
				}

				choiceIndices.add(tmp1);
				play.displayChoice(choices, messages);
				play.setStateType(Play.CHOICE);
				play.choosing = true;
				paused = true;
				break;
			case "song":
			case "setsong":
				if(Vars.isNumeric(lastArg(line))){
					try{
						int s = convertToNum(line);
						Music song = Gdx.audio.newMusic(new FileHandle("res/music/"+Scene.BGM.get(s)+".wav"));
						play.addSong(song);
					} catch(Exception e){
						e.printStackTrace();
					}
				} else {
					String title = line.substring(line.indexOf(" "));

					try{
						Music song = Gdx.audio.newMusic(new FileHandle("res/music/"+title+".wav"));
						play.addSong(song);
					} catch (Exception e){
						e.printStackTrace();
					}
				}
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
				try {
				choiceIndices.pop();
				} catch (EmptyStackException e){
					System.out.println("Could not end choices because there are no more encapsulations.");
					e.printStackTrace();
				}
				break;
			case "zoom":
				//(Float.parseFloat(lastArg(line)));
				break;
			case "done":
				finish();
				break;
			}
		}

		//step analyzing; continue stepping if next line contains a note
		if(index < source.size-1) index++;
		while(source.get(index).startsWith("#"))
			if(index < source.size-1) index++;
	}

	private void finish(){
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
			new SpeechBubble(owner, owner.getPosition().x*Vars.PPM + 6, owner.rh + 5  +
					owner.getPosition().y*Vars.PPM, 0, "...", SpeechBubble.LEFT_MARGIN);
	}

	public void setIndex(String key){
		try{
			current = subScripts.get(key);
		} catch(Exception e){
			System.out.println("Invalid subscript name \""+key+"\"");
		}
	}

	public void setIndex(){
		int i = source.size - 1;
		for (String key : subScripts.keySet())
			if(subScripts.get(key) < i)
				i = subScripts.get(key);
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

	private void getIndicies(){
		subScripts = new HashMap<String, Integer>();
		String line;
		for (int i = 0; i< source.size; i++){
			line = source.get(i);
			if(line.toLowerCase().startsWith("script"))
				subScripts.put(line.substring("script ".length()), i);
		}
	}

	private int convertToNum(String s){
		return Integer.parseInt(lastArg(s));
	}

	private String firstArg(String s){ return args(s)[0]; }

	//only use if #args >= 3
	//if #args > 3, must use in succession
	private String middleArg(String src){
		String s = "";
		String[] tmp = args(src);
		if(tmp.length<=2) return tmp[0];

		for (int i = 1; i < tmp.length - 1; i++)
			s+=tmp[i];
		return s;
	}

	private String lastArg(String s){
		String[] args = args(s);
		return args[args.length-1];
	}

	private int countArgs(String line){
		int i = line.indexOf("(");
		if(i >= 0)
			return args(line.substring(i, line.indexOf(")"))).length;
		return 1;
	}
	
	@SuppressWarnings("unused")
	private String remove(String s, String r){
		String[] tmp = s.split(r);
		s = "";
		for (String i : tmp)
			s += i;
		return s;
	}

	//return everything inside ()
	public String[] args(String line){
		String[] a = {line};
		if(line.indexOf("(") == -1 || line.indexOf(")") == -1)
			return a;
		String tmp = line.substring(line.indexOf("(")+1 , line.indexOf(")"));
		return tmp.split(",");
	}

	private Entity findObject(String objectName){
		if(Vars.isNumeric(objectName)){
			for(Entity d : play.getObjects())
				if (d.getSceneID() == Integer.parseInt(objectName)) return d;
		} else 
			for(Entity d : play.getObjects()){
			if(d instanceof Mob){
				if (((Mob)d).getName().toLowerCase().equals(objectName.toLowerCase()))
					return d;
			} else
				if (d.ID.toLowerCase().equals(objectName.toLowerCase()))
					return d;
		}
		return null;
	}

	public void getChoiceIndex(String choice){
		try{
			index = choiceIndices.peek().get(choice);
			play.setStateType(Play.LISTEN);
		} catch(Exception e){
			System.out.println("\n------------------------\nMost likely not all choices have cases."
					+ "\nPlease recheck script: "+ ID);
			e.printStackTrace();
			System.out.println("------------------------");
		}
	}

	private void doAction(String line){
		Entity obj = findObject((middleArg(line)));
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
		try{
			ArrayDeque<Pair<String, Integer>> displayText = new ArrayDeque<>();
			String txt;

			while(source.get(index).toLowerCase().startsWith("text")){
				txt = source.get(index).substring(source.get(index).indexOf("{") + 1, source.get(index).indexOf("}"));

				if(txt.indexOf("/player") >= 0){
					txt = txt.substring(0, txt.indexOf("/player")) + player.getName() + 
							txt.substring(txt.indexOf("/player") + "/player".length());
				}

				if(txt.indexOf("/partner") >= 0){
					txt = txt.substring(0, txt.indexOf("/partner")) + player.getPartner().getName() + 
							txt.substring(txt.indexOf("/partner") + "/partner".length());
				}

				if(txt.indexOf("/house") >= 0){
					txt = txt.substring(0, txt.indexOf("/house")) + player.getHome().getType() + 
							txt.substring(txt.indexOf("/house") + "/house".length());
				}

				if(txt.indexOf("/address") >= 0){
					txt = txt.substring(0, txt.indexOf("/address")) + player.getHome().getType() + 
							txt.substring(txt.indexOf("/address") + "/address".length());
				}

				Field f = Mob.class.getField(firstArg(source.get(index)).toUpperCase());
				int emotion = f.getInt(f);
				displayText.add(new Pair<>(txt, emotion));
				index++;
			}

//			txt = line.substring(line.indexOf("{") + 1, line.indexOf("}"));
//			txt = txt.replaceAll(" ", "");
			if (countArgs(line) == 3)
				paused = dialog = true;
			else
				paused = true;

			index--;
			play.setDispText(displayText);
			play.speak();
		} catch (NumberFormatException e){

		} catch (NoSuchFieldException e) {
			System.out.println("No such emotion \"" + firstArg(source.get(index)) + "\"");
			e.printStackTrace();
		} catch (SecurityException e) { e.printStackTrace();
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) { }
	}

	private void changeValue(String line){
		line = line.substring(line.indexOf(" ") + 1);
		Entity obj = null;
		String function = firstArg(line);
		String target = middleArg(line);
		int value = 0;

		try{
			value = Integer.parseInt(lastArg(line));
		} catch(Exception e){
			e.printStackTrace();
		}


		if (countArgs(line)==4){
			obj = findObject(firstArg(target));
			if (obj == null) return;
			target = lastArg(target);
		} 

		if (function.toLowerCase().equals("add")){
			switch(target) {
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
			switch(target) {
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

	private boolean compare(String line){
		String arguments = middleArg(line);
		String object = firstArg(arguments);
		String condition = lastArg(arguments);
		String value = lastArg(line);

		//System.out.println("\no: " + object + "\nc: " + condition + "\nv: " + value);


		if (Vars.isNumeric(firstArg(object))){
			String type = middleArg(arguments);
			Entity obj = findObject(object);
			if (obj == null) return false;

			switch (type){
			case "location":
				object = String.valueOf((int) obj.getPosition().x);
			case "health":
				if (obj instanceof Mob) object = String.valueOf(((Mob) obj).getHealth());
				else return false;
				break;
			}
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
				object = player.getPartner().ID;
				break;
			case "house":
				object = player.getHome().getType();
				break;
			default:
				object = String.valueOf(play.history.getFlag(object));
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
