package scenes;

import handlers.GameStateManager;
import handlers.Pair;
import handlers.SuperMob;
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
 *  
 *  you shouldn't have to debug this class for the most part
 *  if there is an issue, make sure you are writing the script correctly
 * ----------------------------------------------------------------------  */
public class Script {

	public String ID;
	public Array<String> source;
	public Stack<HashMap<String, Integer>> choiceIndices; 
	public Array<Pair<String, Object>> localVars;
	public boolean paused, limitDistance, forcedPause, dialog;
	public int[] choices;
	public String[] messages;
	public int current, index, waitTime, time;

	//outside references
	private Entity activeObj, owner;
	private Play play;
	private Player player;
	private HashMap<String, Integer> subScripts, checkpoints;

	//private int c;

	public Script(String scriptID, Play p, Player player, Entity owner){
		try {
			ID = scriptID;
			loadScript(scriptID);
			getIndicies();
			choiceIndices = new Stack<>();
			checkpoints = new HashMap<>();
			localVars = new Array<>();
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
//		System.out.println(paused+": "+source.get(index));
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

	@SuppressWarnings("unused")
	public void analyze(){
//		System.out.println(source.get(index));

		if(!play.analyzing && !paused) {
			index = current;
			play.analyzing = true;
		}

		String line = source.get(index);
		String command;//, s;
		String[] tmp;
		Entity obj = null;
		Entity target=null;

		if (!line.startsWith("#")){
			line = line.trim(); //trim leading spaces
			if (line.indexOf("(") == -1)
				if(line.startsWith("["))
					command = line.substring(line.indexOf("[")+1, line.indexOf("]"));
				else command = line;
			else command = line.substring(0, line.indexOf("("));

//			System.out.println(command);
			switch(command.toLowerCase()){
			case "addpartner":
				obj = findObject(firstArg(line));
				if(obj!=null)
					if(obj instanceof NPC)
						player.goOut((NPC) obj, lastArg(line));
				break;
			case "attack":
				break;
			case "action":
				doAction(line);
				break;
			case "value":
			case "changeval":
				changeValue(line);
				break;
			case "checkpoint":
				addCheckpoint(firstArg(line), index);
				break;
			case "declare":
				String variableName = firstArg(line);
				String scope = firstArg(middleArg(line));
				String type = lastArg(middleArg(line));
				String value ="";
				if(lastArg(line).contains("{"))
					source.get(index).substring(source.get(index).indexOf("{") + 1, source.get(index).indexOf("}"));
				else value = lastArg(line);
//				System.out.println("\nn: "+ variableName +"\ns: "+scope+"\nt: "+type+"\nv: "+value);

				try{
					switch(type.toLowerCase()){
					case"integer":
						if(scope.equalsIgnoreCase("local")) declareVariable(variableName, Integer.parseInt(value));
						else if(scope.equalsIgnoreCase("global")) play.history.declareVariable(variableName, Integer.parseInt(value));
						else System.out.println("Invalid scope \"" + scope +"\"");
						break;
					case"float":
						if(scope.equalsIgnoreCase("local")) declareVariable(variableName, Float.parseFloat(value));
						else if(scope.equalsIgnoreCase("global")) play.history.declareVariable(variableName, Float.parseFloat(value));
						else System.out.println("Invalid scope \"" + scope +"\"");
						break;
					case "string":
						if(scope.equalsIgnoreCase("local")) declareVariable(variableName, value);
						else if(scope.equalsIgnoreCase("global")) play.history.declareVariable(variableName, value);
						else System.out.println("Invalid scope \"" + scope +"\"");
						break;
					default:
						System.out.println("Could not declare "+scope+" variable \"" + variableName + "\" of type \"" + type +
								"\" with value \"" + value + "\"");
					}
				} catch(Exception e){
					System.out.println("Could not declare "+scope+" variable \"" + variableName + "\" of type \"" + type +
							"\" with value \"" + value + "\"");
				}
				//				System.out.println(localVars);
				break;
			case "else":
				while(!source.get(index).toLowerCase().trim().startsWith("endif"))
					index++;
				index--;
				break;
			case "endgame":
				play.getGSM().setState(GameStateManager.TITLE, true);
				break;
			case "endoption":
				int count = 0;
				while(count!=choiceIndices.size()&&index<source.size-1){
					index++;
					if(source.get(index).trim().toLowerCase().startsWith("endchoice"))
						count++;
				}
				index--;
				break;
			case "face":
			case "faceobject":
				findObject(obj, target, line);

				if (obj != null && target != null)
					if (obj instanceof NPC)
						((NPC) obj).setState(NPC.FACEOBJECT, obj);
					else obj.faceObject(target);
				break;
			case "forcefollow":
				if (player.getPartner() != null){
					if (player.getPartner().getName()!=null){
						player.stopPartnerDisabled = Boolean.parseBoolean(lastArg(line));
					}
				}
				break;
			case "follow":
				findObject(obj, target, line);

				if (obj != null && target != null)
					if (obj instanceof NPC)
						((NPC) obj).setState(NPC.FOLLOWING, obj);
					else obj.faceObject(target);
				break;
			case "giveitem":
				System.out.println("Inventory system not yet implemented. Sorry!");
				break;
			case "hasitem":
				System.out.println("Inventory system not yet implemented. Sorry!");
				break;
			case "hidestats":
				break;
			case "if":
				if (!compare(removeCommand(line))){
					while(!source.get(index).trim().toLowerCase().startsWith("endif") && 
							!source.get(index).trim().toLowerCase().startsWith("else")){

						if(source.get(index).toLowerCase().trim().startsWith("setchoice")){
							while(!source.get(index).toLowerCase().trim().startsWith("endchoice"))
								index++;
						}
						
						index++;
					}
				}
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
				obj = findObject(lastArg(line));

				if (obj != null) {
					focus = obj.getPosition();
					paused = true;
					play.getCam().setFocus(focus);
				}
				break;
			case "moveobject":
				obj = findObject(middleArg(line));
				if (obj != null) {
					obj.setGoal(convertToNum(line) - obj.getPosition().x * Vars.PPM);
					activeObj = obj;
				} else
					System.out.println("Cannot find \""+middleArg(line)+"\" to move");
				break;
			case "moveplayer":
				player.setGoal(convertToNum(line));
				activeObj = player;
				break;
			case "unfocus":
			case "unfocuscamera":
			case "removefocus":
				play.getCam().removeFocus();
				break;
			case "remove":
			case "removeObject":
				obj = findObject(firstArg(line));
				if (obj!= null)
					play.addBodyToRemove(obj.getBody());
				else
					System.out.println("Cannot find \""+firstArg(line)+"\" to remove");
				break;
			case "removepartner":
				player.breakUp();
				break;
			case "resetstate":
				obj = findObject(lastArg(line));
				if (obj != null)
					if(obj instanceof NPC)
						((NPC) obj).resetState();
				break;
			case "return":
				getCheckpoint(firstArg(line));
				break;
			case "setspeaker":
				Entity speaker = null;

				speaker = findObject(firstArg(line));

				if(speaker != null) {
					if(speaker instanceof Mob){
						if( play.hud.getFace() != speaker) 
							play.hud.changeFace((Mob) speaker);
					}
					else play.hud.changeFace(null);
					if(countArgs(line) == 2){
						Vector2 loc = speaker.getPosition();
						play.getCam().setFocus(loc.x, loc.y + play.getCam().YOFFSET/Vars.PPM);
					}
				} else
					System.out.println("Cannot find \""+firstArg(line)+"\" to set as speaker");

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

				int i = index+1; int j = 0;
				HashMap<String, Integer> tmp1 = new HashMap<>();
				String s1;
				while((j != choices.length+1 &&
						!source.get(i).toLowerCase().trim().startsWith("endchoice")) && i < source.size &&
						!source.get(i).toLowerCase().trim().startsWith("done")){
					if(source.get(i).toLowerCase().trim().startsWith("setchoice")){
						while(!source.get(i).toLowerCase().trim().startsWith("endchoice"))
							i++;
					}
					
					if (source.get(i).toLowerCase().trim().startsWith("[")) {
						s1 = source.get(i).trim();
						s1 = s1.substring(1, s1.indexOf("]"));
						s1 = s1.split(" ")[1].toLowerCase();
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
			case "setflag":
				try{
					if(play.history.getFlag(firstArg(line))!=null)
						play.history.setFlag(firstArg(line), Boolean.parseBoolean(lastArg(line)));
					else
						play.history.addFlag(firstArg(line), Boolean.parseBoolean(lastArg(line)));
				} catch (Exception e){
					System.out.println("Could not set flag \"" +firstArg(line) + "\"");
				}
				break;
			case "setscript":
				setIndex(lastArg(line));
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
			case "toggleStats":
				play.hud.showStats = Boolean.parseBoolean(firstArg(line));
				break;
			case "wait":
				time = 0;
				waitTime = convertToNum(line);
				paused = true;
				break;
			case "pause":
				paused = forcedPause = true;
				break;
			case "playSound":
				Music sound = null;
				String src = firstArg(line);
				Vector2 position = null;
				
				if(countArgs(line) == 1)
					position = player.getPosition();
				if(countArgs(line) == 2){
					obj = findObject(lastArg(line));
					if(obj!=null)
						position = obj.getPosition();
					else
						System.out.println("Could not find object \""+lastArg(line)+"\" to play sound + \""+src);
				}if(countArgs(line) == 3){
					try{
						float x = Float.parseFloat(middleArg(line)),
								y= Float.parseFloat(lastArg(line));
						position = new Vector2(x, y);
					} catch(Exception e){
						System.out.println("Error coverting (\""+middleArg(line)+"\", \"" +lastArg(line)+"\") into a vector");
					}
				}
				
				if(position!=null)
					try{
						Gdx.audio.newMusic(new FileHandle("/res/sounds/"+src+".wav"));
						play.playSound(position, sound);
					} catch(Exception e){
						System.out.println("No such sound \""+ src +"\"");
					}
				break;
//			case "end":
//				while(!source.get(index + 1).toLowerCase().startsWith("endchoice"))
//					index++;
//				break;
			case "endchoice":
				try {
					choiceIndices.pop();
				} catch (EmptyStackException e){
					System.out.println("Could not end choices because there are no more encapsulations.");
					System.out.println("Make sure you have the right number of \"setChoice\" commands as well as \"endChoice\".");
//					e.printStackTrace();
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
		localVars = new Array<>();
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
			if(line.toLowerCase().startsWith("limit")){
				try{
					limitDistance = Boolean.parseBoolean(lastArg(line));
					return;
				} catch(Exception e){
					System.out.println("\""+lastArg(line)+"\" is not a boolean");
				}
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

	private String firstArg(String s){ return (args(s)[0]).trim(); }

	//only use if #args >= 3
	//if #args > 3, must use in succession
	private String middleArg(String src){
		String s = "";
		String[] tmp = args(src);
		if(tmp.length<=2) return tmp[0];

		for (int i = 1; i < tmp.length - 1; i++)
			s+=tmp[i]+",";
		if(s.endsWith(","))
			s=s.substring(0, s.length()-1);
		return s.trim();
	}

	private String lastArg(String s){
		String[] args = args(s);
		return (args[args.length-1]).trim();
	}

	private int countArgs(String line){
		return args(line).length;
	}

	//	private String trim(String s){
	//		for(int i =0; i<s.length()-1;i++){
	//			if(!s.substring(i, i+1).equals(" "))
	//				return s.substring(i);
	//		}
	//		return s;
	//	}

	private String remove(String s, String r){
		String[] tmp = s.split(r);
		s = "";
		for (String i : tmp)
			s += i;
		return s;
	}

	private String removeCommand(String line){
		if(line.indexOf("(")==-1) return line;
		return line.substring(line.indexOf("(")+1, line.length() - 1);
	}

	//return everything inside (), seperated by commas
	private String[] args(String line){
		if(line.indexOf("(") == -1 || line.indexOf(")") == -1)
			return line.split(",");
		String tmp = line.substring(line.indexOf("(")+1 , line.indexOf(")"));

		//preserve string argument
		String str="";
		if(tmp.contains("{") && tmp.contains("}")){
			str = tmp.substring(tmp.indexOf("{"), tmp.indexOf("}") +1);
			if(tmp.length() - tmp.indexOf("}")>1)
				tmp = tmp.substring(0, tmp.indexOf("{")) + "&str&" + tmp.substring(tmp.indexOf("}"));
			else tmp = tmp.substring(0, tmp.indexOf("{")) + "&str&";
		}

		String[] a = tmp.split(",");
		for(int i =0;i<a.length;i++)
			if(a[i].equals("&str&"))
				a[i]=str;
		return a;
	}

	private void findObject(Entity obj, Entity target, String line){
		obj = findObject(firstArg(line));	
		target = findObject(lastArg(line));
	}

	private Entity findObject(String objectName){
		Entity object = null;

		if(Vars.isNumeric(objectName)){
			for(Entity d : play.getObjects())
				if (d.getSceneID() == Integer.parseInt(objectName)) return d;
		} else switch(objectName) {
		case "player":
			object = player;
			break;
		case "partner":
			if(player.getPartner().getName() != null)
				object = player.getPartner();
			break;
		case "narrator":
			object = play.narrator;
			break;
		case "this":
			object = owner;
			break;
		default:
			for(Entity d : play.getObjects()){
				if(d instanceof Mob){
					if (((Mob)d).getName().toLowerCase().equals(objectName.toLowerCase()))
						return d;
				} else
					if (d.ID.toLowerCase().equals(objectName.toLowerCase()))
						return d;
			}
		}
		return object;
	}

	public void getChoiceIndex(String choice){
		try{
			index = choiceIndices.peek().get(choice.toLowerCase());
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
			
			while(source.get(index).toLowerCase().trim().startsWith("text")&&index<source.size-1){
				txt = source.get(index).substring(source.get(index).indexOf("{") + 1, source.get(index).indexOf("}"));

				if(txt.contains("/player")){
					txt = txt.substring(0, txt.indexOf("/player")) + player.getName() + 
							txt.substring(txt.indexOf("/player") + "/player".length());
				} if(txt.contains("/partner")){
					txt = txt.substring(0, txt.indexOf("/partner")) + player.getPartner().getName() + 
							txt.substring(txt.indexOf("/partner") + "/partner".length());
				} if(txt.contains("/house")){
					txt = txt.substring(0, txt.indexOf("/house")) + player.getHome().getType() + 
							txt.substring(txt.indexOf("/house") + "/house".length());
				} if(txt.contains("/address")){
					txt = txt.substring(0, txt.indexOf("/address")) + player.getHome().getType() + 
							txt.substring(txt.indexOf("/address") + "/address".length());
				} if(txt.contains("/variable[")&& txt.indexOf("]")>=0){
					String varName = txt.substring(txt.indexOf("/varialble[")+"/variable[".length(), txt.indexOf("]"));
					Object var = getVariable(varName);
					if (var==null) var = play.history.getVariable(varName);
					if(var!= null) {
						txt = txt.substring(0, txt.indexOf("/varialble[")+"/variable[".length()) + var +
								txt.substring(txt.indexOf("/varialble[")+"/variable[".length()+ varName.length() + 1);
					} else
						System.out.println("No variable with name \""+ varName +"\" found");
				}

				Field f;
				try {
					f = Mob.class.getField(firstArg(source.get(index)).toUpperCase());
					int emotion = f.getInt(f);
					displayText.add(new Pair<>(txt, emotion));
				} catch (NoSuchFieldException e) {
					System.out.println("No such emotion \""+ firstArg(source.get(index)) +"\"");
					displayText.add(new Pair<>(txt, 0));
				};
				
				index++;
			}

			if (countArgs(line) == 3);
//				paused = dialog = true;
			else
				paused = true;

			index--;
			play.setDispText(displayText);
			play.speak();
		} catch (SecurityException e) { e.printStackTrace();
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) { }
	}

	private void changeValue(String line){
//		line = line.substring(line.indexOf(" ") + 1);
		String function = firstArg(line);
		String target = middleArg(line);
		String value = lastArg(line);
		boolean successful = false;
		
//		System.out.println(function +":"+target+":"+value);

		if(target.contains(".")){
			String obj = target.substring(0, target.indexOf("."));
			target = target.substring(target.indexOf(".")+1);
			Entity object = findObject(obj);

			if(object!=null){
				switch(function.toLowerCase()){
				case "add":
					try{
						switch(target.toLowerCase()){
						case "health":
							if(object instanceof Mob){
								float val = Float.parseFloat(value);
								if (val>=0) ((Mob)object).heal(val);
								else ((Mob)object).damage(val);
							}
							break;
						case "money":
							if(object instanceof Player) 
								((Player)object).addFunds(Float.parseFloat(value));
							break;
						case "love":
						case "relationship":
							if(object instanceof Player) 
								((Player)object).setRelationship(Float.parseFloat(value));
							break;
						case "niceness":
							if(object instanceof Player)
								((Player)object).setNiceness(Float.parseFloat(value));
							break;
						case "bravery":
							if(object instanceof Player) 
								((Player)object).setBravery(Float.parseFloat(value));
							break;
						case "power":
						case "level":
							if(object instanceof SuperMob)
								((SuperMob)object).levelUp();
							break;
						default:
							System.out.println("\"" + target +"\" is an invalid property to add to for \"" + object + "\"");
						}
					} catch (Exception e) {
						System.out.println("Could not add \"" + value +"\" to \"" + target + "\"");
					}
					break;
				case "set":
					try{
						switch(target.toLowerCase()){
						case "name":
							if(object instanceof Mob){
								((Mob)object).setName(value);
								successful = true; 	}
							break;
						case "gender":
							if(object instanceof Player)
								if(value.toLowerCase().equals("male")||value.toLowerCase().equals("boy")||
										value.toLowerCase().equals("man")){
									((Player)object).setGender("male");
									successful = true; 	}
								else if(value.toLowerCase().equals("female")||value.toLowerCase().equals("girl")||
										value.toLowerCase().equals("woman")){
									((Player)object).setGender("female");
									successful = true; 	}
								else
									System.out.println("\""+value+"\" is not a valid gender");
							break;
						case "money":
							if(object instanceof Player){ 
								((Player)object).resetMoney(Float.parseFloat(value));
								successful = true; }
							break;
						case "love":
						case "relationship":
							if(object instanceof Player) {
								((Player)object).resetRelationship(Float.parseFloat(value));
								successful = true;}
							break;
						case "niceness":
							if(object instanceof Player){
								((Player)object).resetNiceness(Float.parseFloat(value));
								successful = true;}
							break;
						case "bravery":
							if(object instanceof Player) {
								((Player)object).resetBravery(Float.parseFloat(value));
								successful = true;}
							break;
						case "lovescale":
							if(object instanceof Player) {
								((Player)object).setLoveScale(Float.parseFloat(value));
								successful = true;}
							break;
						case "nicenessscale":
							if(object instanceof Player){
								((Player)object).setNicenessScale(Float.parseFloat(value));
								successful = true;}
							break;
						case "braveryscale":
							if(object instanceof Player) {
								((Player)object).setBraveryScale(Float.parseFloat(value));
								successful = true;}
							break;
						case "powertype":
							if(object instanceof SuperMob){
								if(Vars.isNumeric(value))
									((SuperMob)object).setPowerType(Integer.parseInt(value));
								else {
									Field f = SuperMob.class.getField(value.toUpperCase());
									((SuperMob)object).setPowerType(f.getInt(f));
								}
								successful = true;}
							break;
						default:
							System.out.println("\"" + target +"\" is an invalid property to modify for \"" + object + "\"");
							successful = true;
						}
					} catch (Exception e) {
						System.out.println("Could not set \"" + value +"\" to \"" + target + "\"");
						successful = true;
					}
					break;
					default:
						System.out.println(1);
						System.out.println("\""+function+"\" is not a valid operation for modifying values");
				}

				if (!successful)
					System.out.println("\"" + target +"\" is an invalid property to modify for object type"
							+ "\"" + object.getClass().getSimpleName() + "\"");
			}
		} else {
//			System.out.println("finding var \""+target+"\"");
			try{
				//find local variable
				Object obj = getVariable(target);
				
				if(obj!=null){
//					System.out.println(function);
					switch(function.toLowerCase()){
					case "add":
						switch(function.toLowerCase()){
						case "add":
							switch(obj.getClass().getSimpleName().toLowerCase()){
							case "float":
								setVariable(target, (float) obj + Float.parseFloat(value));
								break;
							case "integer":
								setVariable(target, (int) obj + Integer.parseInt(value));
								break;
							case"string":
								setVariable(target, ((String)obj).concat(value));
								break;
							}
							break;
						case "set":
							switch(obj.getClass().getSimpleName().toLowerCase()){
							case "float":
								setVariable(target, Float.parseFloat(value));
								break;
							case "integer":
								setVariable(target, Integer.parseInt(value));
								break;
							default:
								setVariable(target, value);
							}
						default:
							System.out.println("\""+function+"\" is not a valid operation for modifying values");

						}
					}
				} else {
					obj = play.history.getVariable(target);
					System.out.println(value);
					if(obj!=null){
						switch(function.toLowerCase()){
						case "add":
							switch(obj.getClass().getSimpleName().toLowerCase()){
							case "float":
								play.history.setVariable(target, (float) obj + Float.parseFloat(value));
								break;
							case "integer":
								play.history.setVariable(target, (int) obj + Integer.parseInt(value));
								break;
							case"string":
								play.history.setVariable(target, ((String)obj).concat(value));
								break;
							}
							break;
						case "set":
							switch(obj.getClass().getSimpleName().toLowerCase()){
							case "float":
								play.history.setVariable(target, Float.parseFloat(value));
								break;
							case "integer":
								play.history.setVariable(target, Integer.parseInt(value));
								break;
							default:
								play.history.setVariable(target, value);
							}
							break;
						default:
							System.out.println(2);
							System.out.println("\""+function+"\" is not a valid operation for modifying values");

						}
					} else {
						System.out.println("No variable  locally or globally called \""+target+"\"");
					}
				}
			} catch (Exception e) {
				System.out.println("Error casting \"" + value +"\" to the value of \"" + target + 
						"\" for modification");
				e.printStackTrace();
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

	//compares all the arguments of the if statement from the script
	private boolean compare(String statement){
		Array<String> arguments = new Array<>();
		String tmp = new String(statement);
//		System.out.println(tmp);

		//separate arguments and conditions
		if(!tmp.contains(" ")&& !tmp.contains("[")&&!tmp.contains("(")){
			for(int i = 0; i<tmp.length()-1; i++){				
				if(tmp.substring(i, i+1).equals(" ")){
					arguments.add(tmp.substring(0, i));
					tmp = tmp.substring(i + 1);
					i = -1;
				} else if(tmp.substring(i, i+1).equals("[")){
					arguments.add(tmp.substring(i+1, tmp.indexOf("]")));
					int x;
					if(tmp.length() - tmp.indexOf("]")>1)
						x=2;
					else x=1;
					tmp = tmp.substring(tmp.indexOf("]")+x);
					i = -1;
				} else if(tmp.substring(i, i+1).equals("(")){
					if(tmp.indexOf(")")==-1){
						System.out.println("Error evaluating: \"" +statement +
								"\"\nMissing a \")\"");
						return false;
					}
					arguments.add(String.valueOf(compare(tmp.substring(i+1, tmp.indexOf(")")))));
					int x;
					if(tmp.length() - tmp.indexOf(")")>1)
						x=2;
					else x=1;
					tmp = tmp.substring(tmp.indexOf(")")+x);
					i = -1;
				}
			}
		}
		else arguments.add(tmp);

		for(int j = 0; j<arguments.size;j+=2){
			arguments.set(j, String.valueOf(evaluate(arguments.get(j))));
		}System.out.println(statement+":"+arguments);

		while(arguments.size>=3)
			arguments = combine(arguments);

		if(arguments.size==0){
			System.out.println("Statement \""+ statement +"\" is written incorrectly");
			return false;
		}
		return Boolean.parseBoolean(arguments.get(0));
	}

	//combines the first and third elements of an array<> into a single boolean by the condition of the second element
	//assumes values are boolean
	//used in the evaluation of an if statement
	private Array<String> combine(Array<String> arguments){
		Array<String> result = new Array<>();
		if(arguments.size<3)
			return arguments;
		if(arguments.size%2==0){
			System.out.println("Invalid list of arguments in if statement. Removing last argument.");
			System.out.println("Source: "+source.get(index));
			arguments.removeIndex(arguments.size - 1);
		}

		boolean value1 = Boolean.parseBoolean(arguments.get(0));
		boolean value2 = Boolean.parseBoolean(arguments.get(2));
		Boolean combined = null;
		String condition = arguments.get(1);

		if(condition.equals("and"))
			combined = value1 && value2;
		if (condition.equalsIgnoreCase("or"))
			combined = value1 || value2;
		if(combined==null){
			System.out.println("\""+condition+"\" is and invalid condition");
			combined = false;
		}

		result.add(String.valueOf(combined));
		for(int i=3; i<arguments.size;i++)
			result.add(arguments.get(i));
		return result;
	}

	private boolean evaluate(String statement){
		String obj, prop, property = null;
		Object object = null;
		boolean not=false, result=false;

		if(statement.contains(">") || statement.contains("<") || statement.contains("=")){
			String value = "";

			//separate value and condition from tmp
			obj = "";
			String condition = "";
			String tmp = remove(statement, " "), index;
			int first=-1;
			for(int i = 0; i < tmp.length() - 1; i++){
				index = tmp.substring(i,i+1);
				if((index.equals(">") || index.equals("<") || index.equals("="))
						&& condition.length()<2){
					if(first==-1){
						condition += index;
						first = i;
					} else if (i-first==1)
						condition+=index;
				}
			}

			if(tmp.indexOf(condition)<1){
				System.out.println("No object found to compare with in statement: "+statement);
				return false;
			}

			obj = tmp.substring(0, tmp.indexOf(condition));
			value = tmp.substring(tmp.indexOf(condition)+condition.length());

			if(obj.contains(".")){
				prop = obj.substring(obj.indexOf(".")+1);
				obj = obj.substring(0, obj.indexOf("."));
				if(obj.startsWith("!")){
					obj = obj.substring(1);
					not = true;
				}

				switch(obj){
				case "player": object = player; break;
				case "narrator": object = play.narrator; break;
				case "partner": object = player.getPartner(); 	break;
				case "this": object = owner; break;
				default: object = findObject(obj);	
				}

				if (object==null){
					System.out.println("Could not find object with name \"" +obj+"\"");
					return false;
				}

				switch(prop.toLowerCase()){
				case "name":
					if(object instanceof Mob)
						property = ((Mob)object).getName();
					break;
				case "health":
					if(object instanceof Mob) 
						property = String.valueOf(((Mob)object).getHealth());
					break;
				case "money":
					if(object instanceof Player) 
						property = String.valueOf(((Player)object).getMoney());
					break;
				case "gender":
					if(object instanceof Mob) 
						property = ((Mob)object).getGender();
					break;
				case "love":
				case "relationship":
					if(object instanceof Player) 
						property = String.valueOf(((Player)object).getRelationship());
					break;
				case "niceness":
					if(object instanceof Player)
						property = String.valueOf(((Player)object).getNiceness());
					break;
				case "bravery":
					if(object instanceof Player) 
						property = String.valueOf(((Player)object).getBravery());
					break;
				case "lovescale":
					if(object instanceof Player) 
						property = String.valueOf(((Player)object).getLoveScale());
					break;
				case "nicenessscale":
					if(object instanceof Player)
						property = String.valueOf(((Player)object).getNicenessScale());
					break;
				case "braveryscale":
					if(object instanceof Player) 
						property = String.valueOf(((Player)object).getBraveryScale());
					break;
				case "house":
					if(object instanceof Player) 
						property = ((Player)object).getHome().getType();
					break;
//				case "haspartner":
//					if(object instanceof Player)
//						if (player.getPartner()!=null){
//							if(player.getPartner().getName()!=null)
//								if(!player.getPartner().getName().equals(""))
//									property = String.valueOf(true);
//						} else
//							property = String.valueOf(false);
//					break;
				case "location":
					if(object instanceof Entity)
						property = String.valueOf(((Entity)object).getPosition().x);
					break;
				case "power":
				case "level":
					if(object instanceof SuperMob)
						property = String.valueOf(((SuperMob)object).getLevel());
					break;
				case "powertype":
					if(object instanceof SuperMob)
						property = String.valueOf(((SuperMob)object).getPowerType());
					break;
				default:
					property = null;
				}

				if(property == null){
					System.out.println("\""+prop+"\" is an invalid object property for object \""+ obj+"\"");
					return false;
				}
			} else {
				//find local variable
				object = getVariable(obj);
				if (object==null)
					object = play.history.getVariable(obj);

				if (object==null){
					System.out.println("No variable locally or globally called \""+obj+"\"");
					return false;
				}

				if(object instanceof String)
					property = (String) object;
				else if(object instanceof Float)
					property = String.valueOf((Float) object);
				else if(object instanceof Integer)
					property = String.valueOf((Integer) object);
				else
					property = null;

				if (property==null){
					System.out.println("The type \""+object.getClass().getSimpleName()+"\" has not properly been handled for variable types.");
					return false;
				}
			}

//			System.out.println("p: " + property + "\nc: " + condition + "\nv: " + value);

			//actual comparator
			try{
				switch (condition){
				case "=":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						return Float.parseFloat(property) == Float.parseFloat(value);
					else
						return property.equals(value);
				case ">":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						return Float.parseFloat(property) > Float.parseFloat(value);
				case ">=":
				case "=>":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						return Float.parseFloat(property) >= Float.parseFloat(value);
				case "<":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						return Float.parseFloat(property) < Float.parseFloat(value);
				case "<=":
				case "=<":
					if (Vars.isNumeric(property) && Vars.isNumeric(value))
						return Float.parseFloat(property) <= Float.parseFloat(value);
				}
			} catch(Exception e){
				System.out.println("Could not compare \""+property+"\" with \""+value+"\" by condition \""+condition+"\"");
				e.printStackTrace();
				return false;
			}
		} else {
			System.out.println("hello");
			if(statement.startsWith("!")){
				not = true;
				statement = statement.substring(1);
			}

			if(statement.contains(".")) {
				obj = statement.split(".")[0];
				prop = statement.split(".")[1];

				result = false;
			} else if(play.history.getFlag(statement)!=null){
				result = play.history.getFlag(statement);
			} else if(play.history.findEvent(statement)){
				result = true;
			} 
		}

		if(not) return !result;
		return result;
	}

	public boolean declareVariable(String variableName, Object value){
		if (value instanceof Boolean) return false;
		for(Pair<String, Object> p : localVars)
			if (p.getKey().equals(variableName)){
				System.out.println("Variable \""+variableName +"\" already exists locally");
				return false;
			}
		if (!(value instanceof String) && !(value instanceof Integer) && !(value instanceof Float))
			return false;

		localVars.add(new Pair<>(variableName, value));
		return true;
	}

	public Object getVariable(String variableName){
		for(Pair<String, Object> p : localVars)
			if (p.getKey().equals(variableName))
				return p.getValue();
		return null;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
	}

	public void setVariable(String var, Object val){
		for(Pair<String, Object> p : localVars)
			if(p.getKey().equals(var)){
				String type = p.getValue().getClass().getSimpleName();
				if(!var.getClass().getSimpleName().equals(type)){
					try{
						if(type.toLowerCase().equals("float"))
							p.setValue((float) val);
						if(type.toLowerCase().equals("integer"))
							p.setValue((int) val);
						if(type.toLowerCase().equals("string"))
							p.setValue((String) val);
					} catch (Exception e){System.out.println();}
				} else 
					p.setValue(val);
			}
	}
	
	private boolean addCheckpoint(String name, Integer index){
		if(checkpoints.containsKey(name)){
			System.out.println("\""+name+"\" is already a set checkpoint in script.");
			return false;
		}
		
		checkpoints.put(name, index);
		return true;
	}
	
	private void getCheckpoint(String name){
		if(!checkpoints.containsKey(name)){
			System.out.println("The checkpoint \""+name+"\" has not been set in script.");
		} else 
			index = checkpoints.get(name);
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

	//must be rewritten
	private Entity getObjectType(String indicator, Vector2 location, Short layer){
		Class<?> c;
		try {
			c = Class.forName(indicator);
			Class<?> C = c.getSuperclass();

			while(C != null) {
				if (C.getSimpleName().toLowerCase().equals("scene"))
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
