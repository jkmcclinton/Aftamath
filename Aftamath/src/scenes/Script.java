package scenes;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Stack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import entities.Barrier;
import entities.Entity;
import entities.Entity.DamageType;
import entities.Mob;
import entities.Mob.AIState;
import entities.Path;
import entities.SpeechBubble;
import entities.SpeechBubble.PositionType;
import entities.TextBox;
import handlers.Camera;
import handlers.Evaluator;
import handlers.GameStateManager;
import handlers.Pair;
import handlers.Vars;
import main.Game;
import main.Main;
import main.Main.InputState;


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
	public Stack<Operation> operations;
	public HashMap<String, Object> localVars;
	public boolean paused, limitDistance, forcedPause, dialog;
	//	public HashMap<Integer, int[]> choiceTypes;
	//	public HashMap<Integer, String[]> messages;
	public int current, index; 
	public float waitTime, time;

	//outside references
	private Entity owner;
	private Object activeObj;
	private Main main;

	private ScriptType type;
	private String currentName, inputVariable;
	private LinkedHashMap<Integer, Pair<Integer, Integer>> conditions;
	private LinkedHashMap<String, Pair<Integer, Integer>> subScripts;
	private HashMap<String, Integer> checkpoints; 
	private HashMap<Integer, Choice> choiceIndicies;

	public static enum ScriptType{
		ATTACKED, DISCOVER, EVENT, DIALOGUE, SCENE
	}

	public Script(String scriptID, ScriptType type, Main m, Entity owner){
		this.owner = owner;
		this.type = type;
		ID = scriptID;
		main = m;

		choiceIndicies = new HashMap<>();
		conditions = new LinkedHashMap<>();
		operations = new Stack<>();
		checkpoints = new HashMap<>();
		localVars = new HashMap<>();

		loadScript(scriptID);
		if(source!=null){
			findIndicies();
			getDistanceLimit();

//			int i=0;
//			System.out.println();
//			for(String s : source){
//				System.out.println(i+": "+s);
//				i++;
//			}

			activeObj = new Entity();
			setIndex();
		}
	}

	public void update(){
		if(main.analyzing){
			if(!paused){
				if(activeObj instanceof Entity)
					if (!((Entity) activeObj).controlled){
						analyze();
					}
			} else {
				if (waitTime > 0){
					time+=Vars.DT;
					if (time >= waitTime){ 
						waitTime = 0;
						paused = false;
					}
				} else{
					if(activeObj instanceof Camera)
						if (!main.getCam().moving && !forcedPause && !dialog){
							paused = false;
							activeObj = new Entity();
						}
				}
			}
		}
	}

	public void analyze(){
		//		System.out.println("---"+(index+1)+"---- "+source.get(index));

		Operation o;
		dialog = false;
		
		if(index>=source.size)
			index = current;
		
		String line = source.get(index);
		String command;//, s;
		String[] tmp, args;
		Entity obj = null;
		Entity target=null;
		Vector2 loc = null;
		Object var;
		int c;
		dialog = false;

		if (!line.startsWith("#")){
			line = line.trim(); //trim leading spaces
			if (line.indexOf("(") == -1)
				if(line.startsWith("["))
					command = line.substring(line.indexOf("[")+1, line.indexOf("]"));
				else command = line;
			else command = line.substring(0, line.indexOf("("));

			switch(command.toLowerCase()){

			case "addpartner":
				obj = findObject(firstArg(line));
				if(obj!=null)
					if(obj instanceof Mob)
						main.player.goOut((Mob) obj, lastArg(line));
				break;
			case "attack":
				obj = findObject(firstArg(line));
				if(countArgs(line)==2)
					target = findObject(lastArg(line));

				if(obj != null)
					if(obj instanceof Mob){
						if(target!=null)
							if(obj.equals(main.character))
								if(((Mob) obj).getPowerType()==DamageType.PHYSICAL)
									main.player.doRandomPower(target.getPosition());
								else
									((Mob) obj).attack(target.getPosition());
							else
								((Mob) obj).attack(target.getPosition());
						else 
							if(obj.equals(main.character)){
								if(((Mob) obj).getPowerType()==DamageType.PHYSICAL)
									main.player.doRandomPower(null);
							} else
								((Mob) obj).attack();
					}
					else
						System.out.println("Cannot find \""+firstArg(line)+"\" to perform attack; "
								+ "Line: "+(index+1)+"\tScript: "+ID);

				break;
			case "burn":
				obj = findObject(firstArg(line));

				if(obj!=null)
					obj.ignite();

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
				String value = lastArg(line);
				if(value.contains("{")) {
					value = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
					value = getSubstitutions(value);
				}
				//				System.out.println("\nn: "+ variableName +"\ns: "+scope+"\nt: "+type+"\nv: "+value);

				try{
					switch(type.toLowerCase()){
					case"integer":
						if(scope.equalsIgnoreCase("local")) declareVariable(variableName, Integer.parseInt(value));
						else if(scope.equalsIgnoreCase("global")) main.history.declareVariable(variableName, Integer.parseInt(value));
						else System.out.println("Invalid scope \"" + scope +"\"; Line: "+(index+1)+"\tScript: "+ID);
						break;
					case"float":
						if(scope.equalsIgnoreCase("local")) declareVariable(variableName, Float.parseFloat(value));
						else if(scope.equalsIgnoreCase("global")) main.history.declareVariable(variableName, Float.parseFloat(value));
						else System.out.println("Invalid scope \"" + scope +"\"; Line: "+(index+1)+"\tScript: "+ID);
						break;
					case "string":
						if(scope.equalsIgnoreCase("local")) declareVariable(variableName, value);
						else if(scope.equalsIgnoreCase("global")) main.history.declareVariable(variableName, value);
						else System.out.println("Invalid scope \"" + scope +"\"; Line: "+(index+1)+"\tScript: "+ID);
						break;
					case"flag":
						if(scope.equalsIgnoreCase("local")){
							System.out.println("declaring local flag");
							boolean b = false;
							try{ b = Boolean.parseBoolean(value); }
							catch(Exception e){
								System.out.println("Invalid boolean for "+scope+" variable \"" + variableName + "\"; Line: "+(index+1)+"\tScript: "+ID);
							}
							declareVariable(variableName, b);
						}else if (scope.equalsIgnoreCase("global")) main.history.addFlag(variableName, false);
						else System.out.println("Invalid scope \"" + scope +"\"; Line: "+(index+1)+"\tScript: "+ID);
						break;
					default:
						System.out.println("Could not declare "+scope+" variable \"" + variableName + "\" of type \"" + type +
								"\" with value \"" + value + "\"; Line: "+(index+1)+"\tScript: "+ID);
					}
				} catch(Exception e){
					System.out.println("Could not declare "+scope+" variable \"" + variableName + "\" of type \"" + type +
							"\" with value \"" + value + "\"; Line: "+(index+1)+"\tScript: "+ID);
				}
				break;
			case "doaction":
				obj = findObject(firstArg(line));
				if (obj != null){
					if(obj instanceof Mob){
						args = args(line);
						((Mob) obj).doAction(args[1]);
						if(args.length<3)
							if(obj.controlled)
								activeObj = obj;
					}else
						obj.doAction(convertToNum(line));
				} else
					System.out.println("Cannot find \""+firstArg(line)+"\" to perform action; Line: "+(index+1)+"\tScript: "+ID);
				break;
			case "dotimedaction":
				obj = findObject(firstArg(line));
				if (obj != null) { 
					if(obj instanceof Mob){
						float f = convertToNum(line);
						args = args(line);
						((Mob) obj).doTimedAction(args[1], f);
						if(args.length<4)
							if(obj.controlled)
								activeObj = obj;
					}
				} else
					System.out.println("Cannot find \""+firstArg(line)+" to perform action; Line: "+(index+1)+"\tScript: "+ID);
				break;
			case "else":
				index = operations.peek().end;
				operations.pop();
				break;
			case "end":
				if(!operations.isEmpty()){
					if(index==operations.peek().end){
						operations.pop();

						if(!operations.isEmpty())
							if(operations.peek().type=="setchoice"){
								index = operations.peek().end+1;
								System.out.println(operations);
								operations.pop();
							}
					}
				} else
					System.out.println("Extra end statement found at Line: "+(index+1)+"\tScript: "+ID);
				break;
			case "endgame":
				main.getGSM().setState(GameStateManager.TITLE, true);
				break;
			case "eventmusic":
				if(!Game.SONG_LIST.contains(firstArg(line), false))
					System.out.println("\""+firstArg(line)+"\" is not a valid song name; Line: "+(index+1)+"\tScript: "+ID);
				else{
					if(!main.getSong().title.equals(new Song(firstArg(line)))){
						stopEventBGM();
						main.addTempSong(new Song(firstArg(line)));
					}
				}
				break;
			case "face":
			case "faceobject":
				obj = findObject(firstArg(line));
				target = findObject(lastArg(line));

				if (obj != null && target != null)
					obj.faceObject(target);
				break;
			case "fanfare":
				if(!Game.SONG_LIST.contains(firstArg(line), false))
					System.out.println("\""+firstArg(line)+"\" is not a valid fanfare name; Line: "+(index+1)+"\tScript: "+ID);
				else
					main.addTempSong(new Song(firstArg(line), false));
				break;
			case"find":
			case"findobject":
				obj=findObject(firstArg(line));
				var = getVariable(lastArg(line));
				type = "localflag";
				
				if(var==null){
					type = "globalar";
					var = main.history.getVariable(lastArg(line));
				} if(var==null){
					type = "globalflag";
					var = main.history.getFlag(lastArg(line));
				}
				
				if(var!=null){
					boolean b = (obj!=null);
					switch(type){
					case "globalflag":
						main.history.setFlag(lastArg(line), b);
						break;
					case "localvar":
						setVariable(lastArg(line), b);
						break;
					case "globalvar":
						main.history.setVariable(lastArg(line), b);
						break;
					}
				}
				
				break;
			case "focus":
			case "focuscamera":
				obj = findObject(lastArg(line));

				if (obj != null) {
					paused = true;
					main.getCam().setFocus(obj);
					activeObj = main.getCam();
				}
				break;
			case "forcefollow":
				if (main.player.getPartner() != null){
					if (main.player.getPartner().getName()!=null){
						try{
							main.player.stopPartnerDisabled = Boolean.parseBoolean(lastArg(line));
						}catch(Exception e){
							System.out.println("Value \""+lastArg(line)+"\" is not a boolean; Line: "+(index+1)+"\tScript: "+ID);
						}
					}
				}
				break;
			case "follow":
				obj = findObject(firstArg(line));
				target = findObject(lastArg(line));
				if (obj != null && target != null){
					if (obj instanceof Mob){
						((Mob) obj).setState(AIState.FOLLOWING, obj);
					}else obj.faceObject(target);
				}
				break;
			case "freeze":
				obj = findObject(firstArg(line));

				if(obj!=null)
					obj.freeze();
				break;
			case "giveitem":
				System.out.println("Inventory system not yet implemented. Sorry!");
				break;
			case "hasitem":
				System.out.println("Inventory system not yet implemented. Sorry!");
				break;
			case "hidestats":
				break;
			case "hidedialog":
				main.hud.hide();
				break;
			case "if":
				Evaluator eval = new Evaluator(main);
				if (!eval.evaluate(removeCommand(line), this)){
					if(conditions.get(index).getKey()==-1){
						index = conditions.get(index).getValue();
					}else{
						o = new Operation("else", conditions.get(index).getKey(), 
								conditions.get(index).getValue());
						operations.add(o);
						index = conditions.get(index).getKey();
					}
				} else {
					o = new Operation("if", conditions.get(index));
					operations.add(o);
				}
				break;
			case "input":
				paused = true;
				main.setStateType(InputState.KEYBOARD);
				inputVariable = lastArg(line);
				Game.resetInput();

				break;
			case "introevent":
				paused = true;
				main.hud.hide();

				main.addBodyToRemove(main.character.getBody());
				main.character = new Mob(main.character.getName(), 
						String.valueOf(getVariable("playergender")) + "player" + getVariable("playertype"),
						main.getScene().getSpawnPoint(), Vars.BIT_PLAYER_LAYER);
				main.createPlayer(main.getScene().getSpawnPoint());
				main.addObject(main.character);

				main.setStateType(InputState.GENDERCHOICE);
				break;
			case "lockplayer":
				main.setStateType(InputState.LISTEN);
				break;
			case "lock":
				break;
			case "movecamera":
				createFocus(line);
				break;
			case "move":
			case "moveobject":
//				move(objectName, x, y, [run]) //accepts Tile Location
//				move(objectName, pathName, [run]) //accepts a path
//				move(objectName, targetName, [run]) //accepts a mob
				args = args(line);
				loc = null;
				boolean run = false;
				
				if(lastArg(line).contains("true") || lastArg(line).contains(lastArg("false")))
					run = Boolean.parseBoolean(lastArg(line));
				
				try{
					//is object a mob or entity in the map?
					obj = findObject(args[0]);
					if(obj==null)
						return;

					//is argument a pathing object?
					target = findPath(args[1]);
					if(target != null)
						if(target instanceof Mob){
						Path path = (Path) target;
						// make object move to path
						((Mob) obj).moveToPath(path);
						return;
					}

					//is target a mob?
					target = findObject(args[1].trim());
					if(target != null){
						float dx = target.getPosition().x - obj.getPosition().x;
						int x = (int) (Math.abs(dx)/dx) * 10;
						loc = new Vector2(target.getPosition().x*Vars.PPM + x, target.getPosition().y*Vars.PPM);
					}

					// is target a tile vector?
					loc = parseTiledVector(args, 1);

					if(loc != null)
						if(obj instanceof Mob){
							if(run);
								//make mob run to target location
							((Mob) obj).setGoal(loc.x);
							activeObj = obj;
						} else
							System.out.println("Cannot find \""+middleArg(line)+"\" to move; Line: "+(index+1)+"\tScript: "+ID);
				}
				catch (Exception ArrayIndexOutofBoundsException){
					System.out.println("Insufficient arguments provided; Line: "+(index+1)+"\tScript: "+ID);
				}
				break;
			case"playsound":
				if(countArgs(line)>1){
					//find location
				} else
					main.playSound(firstArg(line));
				break;
			case "unfocus":
			case "unfocuscamera":
			case "removefocus":
				main.getCam().removeFocus();
				break;
			case "remove":
			case "removeObject":
				obj = findObject(firstArg(line));
				if (obj!= null){
					if(!(obj instanceof Barrier))
						main.playSound("beep5");
					main.addBodyToRemove(obj.getBody());
				}else
					System.out.println("Cannot find \""+firstArg(line)+"\" to remove; Line: "+(index+1)+"\tScript: "+ID);
				break;
			case "removepartner":
				main.player.breakUp();
				break;
			case "resetstate":
				obj = findObject(lastArg(line));
				if (obj != null){
					if(obj instanceof Mob)
						((Mob) obj).resetState();
				}
				break;
			case "return":
				getCheckpoint(firstArg(line));
				break;
			case "say":
				c = countArgs(line);
				String msg = getDialogue(index);
				boolean selfKill = true;

				if(c==1){
					//text only
					obj = main.character;
				} else if (c==2) {
					//text and unpause
					if(Vars.isBoolean(lastArg(line)))
						selfKill = Boolean.parseBoolean(lastArg(line));
					else {
						//text and object
						obj = findObject(firstArg(line));
						if(obj == null)
							obj = main.character;
					}

				} else if (c==3) {
					obj = findObject(firstArg(line));
					if(obj == null)
						obj = main.character;

					if(Vars.isBoolean(lastArg(line)))
						selfKill = Boolean.parseBoolean(lastArg(line));
					else selfKill = false;
				}

				TextBox t = new TextBox(obj, msg, selfKill);
				if(obj instanceof Mob)
					((Mob) obj).facePlayer();

				if(!selfKill){
					paused = forcedPause = dialog = true;
					activeObj = t;
					main.setStateType(InputState.LISTEN);
				}

				break;
			case "setattacktype":
				obj = findObject(firstArg(line));

				if(obj!=null)
					if(obj instanceof Mob)
						((Mob) obj).setAttackType(lastArg(line));
				break;
			case "setchoice":
				if(subScripts.get(currentName)!=null){
					Choice choice = choiceIndicies.get(index);
					if(choice!=null){
						o = new Operation("setchoice", choice.start, choice.end);
						operations.add(o);

						Array<Option> temp = new Array<>();
						for(Option o1 : choice.options)
							if(o1.isAvailable())
								temp.add(o1);

						main.displayChoice(temp);
						main.setStateType(InputState.CHOICE);
						main.choosing = true;
						paused = true;
					} 
				}
				//				else
				//					System.out.println("No choices found for this choice set; Line: "+(index+1)+"\tScript: "+ID);
				break;
			case "setdefaultstate":
				obj = findObject(firstArg(line));
				if(obj!=null){
					if(obj instanceof Mob)
						((Mob)obj).setDefaultState(lastArg(line));
				}
				break;
			case "setdialog":
			case "setdialogue":
				obj = findObject(firstArg(line));
				
				if(obj!=null)
					obj.setDialogueScript(lastArg(line));
				else
					System.out.println("Cannot find object \"" + firstArg(line)+ "\" to set a dialogue script; Line: "+(index+1)+"\tScript: "+ID);
				
				break;
			case "setflag":
				try{
					boolean bool = true;
					if(args(line).length==2) 
						bool = Boolean.parseBoolean(lastArg(line));
					
					if(main.history.getFlag(firstArg(line))!=null)
						main.history.setFlag(firstArg(line), bool);
					else
						main.history.addFlag(firstArg(line), bool);
				} catch (Exception e){
					System.out.println("Could not set flag \"" +firstArg(line) + "\" to value \""+lastArg(line)+"\"; Line: "+(index+1)+"\tScript: "+ID);
				}
				break;
			case "setevent":
				String description = lastArg(line);
				
				if(description.contains("{") && description.contains("}")){
					description = getSubstitutions(description);
					description = description.substring(description.indexOf("{"), description.indexOf("{"));
				} else {
					var = getVariable(description); // get local var
					if(var==null)
						var = main.history.getVariable(description);
					if(var!=null){
						if(var.getClass().getSimpleName().toLowerCase().equals("string"))
							description = (String) var;
						else description = "none";
					} else {
						description = "none";
						System.out.println("Invalid description for event \""+firstArg(line)+"\"; Line: "+(index+1)+"\tScript: "+ID);
					}
				}
				
				main.history.setEvent(firstArg(line), description);
				break;
			case "setlayer":
				obj = findObject(firstArg(line));
				if(obj!=null)
					try{
						Field f = Vars.class.getField("BIT_"+lastArg(line).toUpperCase());
						short layer = f.getShort(f);
						obj.changeLayer(layer);
					} catch(Exception e){
						System.out.println("Error changing layer to \""+lastArg(line)+"\" for \""+firstArg(line)+"\"; Line: "+(index+1)+"\tScript: "+ID);
					}	
			break;
			case "setresponse":
				obj = findObject(firstArg(line));

				if(obj!=null)
					if(obj instanceof Mob)
						((Mob) obj).setResponseType(lastArg(line));
				break;
			case "setscript":
				setIndex(lastArg(line));
				break;
			case "setspeaker":
				Entity speaker = null;
				speaker = findObject(firstArg(line));

				if(speaker != null) {
					if(speaker instanceof Mob){
						if( main.hud.getFace() != speaker) 
							main.hud.changeFace((Mob) speaker);
					}
					else main.hud.changeFace(null);
					if(countArgs(line) == 2)
						main.getCam().setFocus(speaker);
				} else {
					System.out.println("Cannot find \""+firstArg(line)+"\" to set as speaker; Line: "+(index+1)+"\tScript: "+ID);
					main.hud.changeFace(null);
				}

				break;
			case "setstate":
				obj = findObject(firstArg(line));

				if(obj!=null)
					if(obj instanceof Mob)
						((Mob) obj).setState(lastArg(line));
				break;
			case "showstats":
				main.getHud().showStats = true;
				break;
			case "spawn":
				//spawn(NPC, image, name, x, y, layer)
				args = args(line);
				if(args[0].equals("NPC") && args.length>=5){
					loc = parseTiledVector(args, 3);
					if(loc!=null){
						short layer = Vars.BIT_LAYER3;
						if(args.length==6)
							try{
								Field f = Vars.class.getField("BIT_"+lastArg(line).toUpperCase());
								layer = f.getShort(f);
							} catch(Exception e){
								System.out.println("Error finding layer \""+lastArg(line)+"\"; Line: "+(index+1)+"\tScript: "+ID);
							}
						
						//find mob from save data
						//main.findFromSave(args[2].trim());
						
						//create new mob if not found
						int sceneID = main.createSceneID();
						
						Mob e = new Mob(args[2].trim(), args[1].trim(), sceneID, loc.x, loc.y, layer);
						e.setDefaultState(AIState.FACEPLAYER);
						e.setDialogueScript("generic_1");
						spawn(e);
					} else
						System.out.println("Cannot spawn \""+args[2]+"\" at given location; Line: "+(index+1)+"\tScript: "+ID);
				} else
					System.out.println("Error spawning \""+args[2]+"\" into level; Line: "+(index+1)+"\tScript: "+ID);
				break;
			case "splash":
				main.getHud().setSplash(firstArg(line));
				break;
			case "statupdate":
				int nice, bravery, max;
				boolean failed=false;
				float niceScale, braveScale;
				tmp = args(line);

				if(countArgs(line)==4) max = 4;
				else {
					if(tmp.length==1){
						System.out.println("Invlaid number of arguments; Line: "+(index+1)+"\tScript: "+ID);
						break;
					} else max = 2;
				}

				for(int i = 0;i<max;i++)
					if(!Vars.isNumeric(tmp[i])){
						failed = true;
						System.out.println("All values must be numbers; Line: "+(index+1)+"\tScript: "+ID);
					}

				if(!failed){
					nice = Integer.parseInt(tmp[0]);
					bravery = Integer.parseInt(tmp[1]);
					niceScale = Float.parseFloat(tmp[2]);
					braveScale = Float.parseFloat(tmp[3]);

					main.player.setNicenessScale(niceScale);
					main.player.setBraveryScale(braveScale);
					main.player.setNiceness(nice);
					main.player.setBravery(bravery);
				}

				break;
			case "stop":
				finish();
				break;
			case "stopeventmusic":
				stopEventBGM();
				break;
			case "text":
				text(line);
				break;
			case "toggleStats":
				main.hud.showStats = Boolean.parseBoolean(firstArg(line));
				break;
			case "wait":
				time = 0;
				waitTime = Float.parseFloat(lastArg(line));
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
					position = main.character.getPosition();
				if(countArgs(line) == 2){
					obj = findObject(lastArg(line));
					if(obj!=null)
						position = obj.getPosition();
					else
						System.out.println("Could not find object \""+lastArg(line)+"\" to play sound + \""+src+"; Line: "+(index+1)+"\tScript: "+ID);
				}if(countArgs(line) == 3){
					try{
						float x = Float.parseFloat(middleArg(line)),
								y= Float.parseFloat(lastArg(line));
						position = new Vector2(x, y);
					} catch(Exception e){
						System.out.println("Error coverting (\""+middleArg(line)+"\", \"" +lastArg(line)+"\") into a vector; Line: "+(index+1)+"\tScript: "+ID);
					}
				}

				if(position!=null)
					try{
						Gdx.audio.newMusic(new FileHandle("/assets/sounds/"+src+".wav"));
						main.playSound(position, sound);
					} catch(Exception e){
						System.out.println("No such sound \""+ src +"\"");
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
		localVars = new HashMap<>();
		checkpoints = new HashMap<>();
		main.setStateType(InputState.MOVE);
		main.analyzing = false;
		index = current;
		if(main.hud.raised)
			main.hud.hide();

		if(main.tempSong && main.getSong().looping)
			main.removeTempSong();

		main.getCam().removeFocus();
		main.currentScript = null;
		main.hud.changeFace(null);

		for (Entity d : main.getObjects()){
			if (d instanceof Mob)
				((Mob) d).resetState();
		}

		switch(type){
		case ATTACKED:
			if(owner instanceof Mob){
				Mob o = (Mob) owner;
				switch(o.getAttackType()){
				case ON_ATTACKED:
					o.fight(main.character);
					break;
				case ON_DEFEND:
					o.attack(main.character.getPosition());
					break;
				case RANDOM:
					double chance = Math.random();
					if(chance>.8d)
						o.fight(main.character);
					else if(chance>.5d)
						o.attack(main.character.getPosition());
					break;
				default:
					break;
				}
			}

			break;
		case DIALOGUE:
			break;
		case DISCOVER:
			if(owner instanceof Mob){
				Mob o = (Mob) owner;
				switch(o.getResponseType()){
				case ATTACK:
					o.setState(AIState.FIGHTING);
					break;
				case FOLLOW:
					o.follow(main.character);
					break;
				case EVADE:
					o.evade(main.character);
					break;
				default:
					break;
				}
			}
			break;
		case EVENT:
			break;
		default:
			break;
		}

		//redisplay speechbubble
		if (main.character.getInteractable() == owner)
			new SpeechBubble(owner, owner.getPixelPosition().x + 6, owner.rh + 5  +
					owner.getPixelPosition().y, 0, "...", PositionType.LEFT_MARGIN);
	}

	public void stopEventBGM(){
		if(main.tempSong)
			main.removeTempSong();
	}

	public void setIndex(String key){
		try{
			current = subScripts.get(key).getKey();
			currentName = key;
		} catch(Exception e){
			System.out.println("Invalid subscript name \""+key+"\"; Line: "+(index+1)+"\tScript: "+ID);
		}
	}

	public void setIndex(){
		for (String key : subScripts.keySet())
			if(subScripts.get(key).getKey() < source.size - 1){
				index = subScripts.get(key).getKey();
				currentName=key;
				//				System.out.println(currentName +":"+subScripts.get(currentName).getKey());
				return;
			}
	}

	private void loadScript(String path) {
		try{
			BufferedReader br = new BufferedReader(new FileReader("assets/scripts/" + path + ".txt"));
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
		} catch(Exception e){
		}
	}

	public void getDistanceLimit(){
		for (String line : source){
			if(line.toLowerCase().startsWith("limit")){
				try{
					limitDistance = Boolean.parseBoolean(lastArg(line));
					return;
				} catch(Exception e){
					System.out.println("\""+lastArg(line)+"\" is not a boolean; Line: "+(index+1)+"\tScript: "+ID);
				}
			}
		}
	}

	//retrieve all the indicies for every operation used in script
	private void findIndicies(){
		subScripts = new LinkedHashMap<String, Pair<Integer, Integer>>();
		String line;
		Pair<Integer, Integer> bounds, b;
		int end = source.size-1;

		for (int i = 0; i< source.size; i++){
			line = source.get(i).trim();
			if(line.toLowerCase().startsWith("script"))
				subScripts.put(line.substring("script ".length()), 
						findBounds("script", i, end));
			if(line.toLowerCase().startsWith("setchoice")){
				bounds = findBounds("setchoice", i, end);
				Array<Option> tmp = new Array<>();
				String mes, num;
				String[] messages, args = args(line);
				HashMap<String, Integer> choices = new HashMap<>();
				messages = new String[args.length];

				if (lastArg(line).toLowerCase().equals("yesno")){
					messages = new String[2];
					choices.put("yes", 6); messages[0] = "Yes";
					choices.put("no", 7); messages[1] = "No";
				} else 
					for (int j = 0; j < args.length; j++){
						num = args[j].split(":")[0];
						mes = args[j].split(":")[1];
						messages[j] = new String(mes);
						choices.put(messages[j].toLowerCase(), Integer.parseInt(num.trim())); 
					}

				for(String m : messages){
					if(!tmp.contains(new Option(m, this), false))
						tmp.add(new Option(m, choices.get(m.toLowerCase()), this));
				}
				choiceIndicies.put(i, new Choice(bounds.getKey(), bounds.getValue(), tmp));

				String s1;
				//get choice handling indicies
				for (int j = i+1;j<bounds.getValue();j++){
					s1=source.get(j).trim();
					if(s1.toLowerCase().startsWith("[choice")){;
					for(String m : messages)
						if(s1.substring(s1.toLowerCase().indexOf("[choice")+"[choice".length()+1,
								s1.indexOf("]")).toLowerCase().equals(m.toLowerCase())){
							b=findBounds("choice", j, bounds.getValue());
							Option o = choiceIndicies.get(i).get(m);
							o.setBounds(b);
							if(s1.replace(" ", "").contains("][")){
								String s2 = s1.substring(0, s1.indexOf("]")+1);
								String condition = s1.substring(s2.length());
								o.setCondition(condition);
							}
							break;
						}
					}
				}
			} if(line.toLowerCase().startsWith("if"))
				conditions.put(i, findBounds("if", i, end));
			if(line.toLowerCase().startsWith("elseif"))
				conditions.put(i, findBounds("elseif", i, end));
		}


//		if(ID.equals("choice test")){
//				System.out.println("\nID: "+ID);
//				System.out.println("scripts:    "+subScripts);
//				System.out.println("conditions: "+conditions);
//				System.out.println("choices:    "+choiceIndicies);
//		}
	}

	private Pair<Integer, Integer> findBounds(String type, int start, int end){
		Pair<Integer, Integer> bounds = null;
		String s;
		int e=-1;

		for(int i = start+1; i<end; i++){
			if(bounds!=null) break;
			s=source.get(i).toLowerCase().trim();
			if(s.startsWith("[choice")||s.startsWith("elseif")||
					s.startsWith("if")||s.startsWith("setchoice")){
				i=skip(i+1, end);
			}

			switch(type.toLowerCase()){
			case"if":
			case"elseif":
				if(s.startsWith("else"))
					e = i;
				if((s.startsWith("elseif")||(s.startsWith("end"))&&!s.contains("endgame")))
					bounds = new Pair<>(e, i);
				break;
			case"choice":
			case"setchoice":
				if(s.startsWith("end")&&!s.contains("endgame") )
					bounds = new Pair<>(start, i);
				break;
			case "script":
				if(s.startsWith("done")){
					bounds = new Pair<>(start, i);}
				break;
			}
		}
		if(bounds!=null)
			return bounds;

		//		System.out.println("Missing a \"done\" statement in script \""+ID+"\"");
		return new Pair<>(start, end);
	}

	private int skip(int i, int limit){
		String s;
		while(i<limit){
			s = source.get(i).toLowerCase().trim();
			if(s.startsWith("[choice")||s.startsWith("elseif")||s.startsWith("if")||
					/*s.startsWith("else")||*/s.startsWith("setchoice"))
				i=skip(i+1, limit);
			else if(s.startsWith("end")&&!s.contains("endgame")) 
				return i;
			i++;
		}
		return i;
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
			tmp = tmp.substring(0, tmp.indexOf("{")) + "&str&" + tmp.substring(tmp.indexOf("}")+1);
		}

		String[] a = tmp.split(",");
		for(int i =0;i<a.length;i++)
			if(a[i].trim().equals("&str&"))
				a[i]=str;
		return a;
	}

	private Entity findObject(String objectName){
		Entity object = null;

		if(Vars.isNumeric(objectName)){
			for(Entity d : main.getObjects())
				if (d.getSceneID() == Integer.parseInt(objectName)) return d;
		} else switch(objectName) {
		case "player":
			object = main.character;
			break;
		case "partner":
			if(main.player.getPartner().getName() != null)
				object = main.player.getPartner();
			break;
		case "narrator":
			object = main.narrator;
			break;
		case "this":
			object = owner;
			break;
		default:
			for(Entity d : main.getObjects()){
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
	
	private Path findPath(String pathName){
		System.out.println(pathName);
		return main.getPath(pathName);
	}
	
	// create a vector from an array of arguments starting from given index
	private Vector2 parseTiledVector(String[] args, int index){
		if(index+1>=args.length)
			return null;
		
		try{
			int x = Integer.parseInt(args[index].trim()) * Vars.TILE_SIZE;
			int y =	main.getScene().height - Integer.parseInt(args[index+1].trim()) * Vars.TILE_SIZE;
			Vector2 v = new Vector2(x, y);
			return v;
		} catch(Exception e){ }
		
		return null;
	}

	public void getChoiceIndex(String choice){
		if(choiceIndicies.get(operations.peek().start)!=null){
			Choice c = choiceIndicies.get(operations.peek().start);
			Option op = c.get(choice.toLowerCase());
			if(op!=null){
				Operation o = new Operation("choice", op.getBounds());
				operations.add(o);
				index = o.start;
				main.setStateType(InputState.LISTEN);
			} else {
				System.out.println("No handling for choice \""+choice+"\" found for setChoice at Line: "+(index+1)+"\tScript: "+ID);
				index = operations.peek().end + 1;
			}
		} else
			System.out.println("No choice handling found for setChoice at Line: "+(index+1)+"\tScript: "+ID);
	}

	private void createFocus(String line){
		Vector2 focus = null;
		String object = middleArg(line);

		if (Vars.isNumeric(object))
			focus = new Vector2(Float.parseFloat(object), (float) convertToNum(line));

		if (focus != null){
			paused = true;;
			main.getCam().setFocus(focus);
		}
	}

	private void text(String line){
		try{
			ArrayDeque<Pair<String, Integer>> displayText = new ArrayDeque<>();
			String txt;

			while(source.get(index).toLowerCase().trim().startsWith("text")&&index<source.size-1){
				txt = getDialogue(index);

				Field f;
				try {
					f = Mob.class.getField(firstArg(source.get(index)).toUpperCase());
					int emotion = f.getInt(f);
					displayText.add(new Pair<>(txt, emotion));
				} catch (NoSuchFieldException e) {
					System.out.println("No such emotion \""+ firstArg(source.get(index)) +"\"; Line: "+(index+1)+"\tScript: "+ID);
					displayText.add(new Pair<>(txt, 0));
				};

				index++;
			}

			if (countArgs(line) == 3)
				dialog = true;
			else
				paused = dialog = true;

			index--;
			main.setDispText(displayText);
			main.speak();
			if(!main.stateType.equals(Main.InputState.LISTEN) && 
					!main.stateType.equals(Main.InputState.MOVELISTEN))
				main.setStateType(Main.InputState.LISTEN);
		} catch (SecurityException e) { e.printStackTrace();
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) { }
	}
	
//	string substitutions
	private String getSubstitutions(String txt){
		while(txt.contains("/playergps")){
			String g = "his";
			if(main.character.getGender().equals("female")) g="her";
			txt = txt.substring(0, txt.indexOf("/playergps")) +g + 
					txt.substring(txt.indexOf("/playergps") + "/playergps".length());
		} while(txt.contains("/playergp")){
			String g = "his";
			if(main.character.getGender().equals("female")) g="hers";
			txt = txt.substring(0, txt.indexOf("/playergp")) + g + 
					txt.substring(txt.indexOf("/playergp") + "/playergp".length());
		} while(txt.contains("/playergo")){
			String g = "he";
			if(main.character.getGender().equals("female")) g="she";
			txt = txt.substring(0, txt.indexOf("/playergo")) + g + 
					txt.substring(txt.indexOf("/playergo") + "/playergo".length());
		} while(txt.contains("/playerg")){
			String g = "guy";
			if(main.character.getGender().equals("female")) g="girl";
			txt = txt.substring(0, txt.indexOf("/playerg")) + g + 
					txt.substring(txt.indexOf("/playerg") + "/playerg".length());
		} while(txt.contains("/player")){
			txt = txt.substring(0, txt.indexOf("/player")) + main.character.getName() + 
					txt.substring(txt.indexOf("/player") + "/player".length());
		} while(txt.contains("/partnergps")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="her";
				else g = "his"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnergps")) + g + 
					txt.substring(txt.indexOf("/partnergps") + "/partnergps".length());
		} while(txt.contains("/partnergp")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="hers";
				else g = "his"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnergp")) + g + 
					txt.substring(txt.indexOf("/partnergp") + "/partnergp".length());
		} while(txt.contains("/partnergo")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="she";
				else g = "he"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnergo")) + g + 
					txt.substring(txt.indexOf("/partnergo") + "/partnergo".length());
		} while(txt.contains("/partnerg")){
			String g = "";
			if(main.player.getPartner()!=null){
				if(main.player.getPartner().getGender().equals("female")) g="girl";
				else g = "guy"; 
			}
			txt = txt.substring(0, txt.indexOf("/partnerg")) + g + 
					txt.substring(txt.indexOf("/partnerg") + "/partnerg".length());
		} while (txt.contains("/partnert")) {
			txt = txt.substring(0, txt.indexOf("/partnert")) + main.player.getPartnerTitle() + 
					txt.substring(txt.indexOf("/partnergt") + "/partnergt".length());
		} while(txt.contains("/partner")){
			String s = "";
			if(main.player.getPartner()==null)
				s=main.player.getPartner().getName();
			txt = txt.substring(0, txt.indexOf("/partner")) + s + 
					txt.substring(txt.indexOf("/partner") + "/partner".length());
		} while(txt.contains("/house")){
			txt = txt.substring(0, txt.indexOf("/house")) + main.player.getHome().getType() + 
					txt.substring(txt.indexOf("/house") + "/house".length());
		} while(txt.contains("/address")){
			txt = txt.substring(0, txt.indexOf("/address")) + main.player.getHome().getType() + 
					txt.substring(txt.indexOf("/address") + "/address".length());
		} while(txt.contains("/variable[")&& txt.indexOf("]")>=0){
			String varName = txt.substring(txt.indexOf("/variable[")+"/variable[".length(), txt.indexOf("]"));
			Object var = getVariable(varName);
			if (var==null) var = main.history.getVariable(varName);
			if(var!= null) {
				txt = txt.substring(0, txt.indexOf("/variable[")) + var +
						txt.substring(txt.indexOf("/variable[")+"/variable[".length()+ varName.length() + 1);
			} else
				System.out.println("No variable with name \""+ varName +"\" found; Line: "+(index+1)+"\tScript: "+ID);
		}
		return txt;
	}

	private String getDialogue(int index){
		try{
			String txt = source.get(index).substring(source.get(index).indexOf("{") + 1, source.get(index).indexOf("}"));
			txt = getSubstitutions(txt);
			return Vars.formatDialog(txt, true);
		} catch(Exception e){
			System.out.println("Missing bracket pair to initialize text; Line: "+(index+1)+"\tScript: "+ID);
		}
		
		return "";
	}

	public void applyInput(){
		changeValue("value(set, "+inputVariable+", {" + Game.getInput()+"})");
		paused = false;
		Game.resetInput();
		main.setStateType(main.prevStateType);
	}

	private void changeValue(String line){
		//		line = line.substring(line.indexOf(" ") + 1);
		String function = firstArg(line);
		String target = middleArg(line);
		String value = lastArg(line);
		// System.out.println("changeValue: " + function +"|"+ target +"|"+ value);
		boolean successful = false;

		//		System.out.println(function +":"+target+":"+value);

		if (value.contains("{") && value.contains("}")) {
			value = value.substring(value.indexOf("{") + 1, value.indexOf("}"));
			value = getSubstitutions(value);
		} else {
			Object var = getVariable(value);
			if(var==null)
				var = main.history.getVariable(value);
			if(var!=null)
				value = String.valueOf(var);
		}

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
							main.player.addFunds(Float.parseFloat(value));
							break;
						case "love":
						case "relationship": 
							main.player.setRelationship(Float.parseFloat(value));
							break;
						case "niceness":
							main.player.setNiceness(Float.parseFloat(value));
							break;
						case "bravery":
							main.player.setBravery(Float.parseFloat(value));
							break;
						case "power":
						case "level":
							if(object instanceof Mob)
								((Mob)object).levelUp();
							break;
						default:
							System.out.println("\"" + target +"\" is an invalid property to add to for \"" + object +
									"\"; Line: "+(index+1)+"\tScript: "+ID);
						}
					} catch (Exception e) {
						System.out.println("Could not add \"" + value +"\" to \"" + target + 
								"\"; Line: "+(index+1)+"\tScript: "+ID);
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
							if(object instanceof Mob)
								if(value.toLowerCase().equals("male")||value.toLowerCase().equals("boy")||
										value.toLowerCase().equals("man")){
									((Mob)object).setGender("male");
									successful = true; 	}
								else if(value.toLowerCase().equals("female")||value.toLowerCase().equals("girl")||
										value.toLowerCase().equals("woman")){
									((Mob)object).setGender("female");
									successful = true; 	}
								else
									System.out.println("\""+value+"\" is not a valid gender; Line: "+(index+1)+"\tScript: "+ID);
							break;
						case "money":
							float g = Float.parseFloat(value);
							main.player.addFunds(g - main.player.getMoney());
							successful = true;
							break;
						case "love":
						case "relationship":
							main.player.resetRelationship(Float.parseFloat(value));
							successful = true;
							break;
						case "niceness":
							main.player.resetNiceness(Float.parseFloat(value));
							successful = true;
							break;
						case "bravery":
							main.player.resetBravery(Float.parseFloat(value));
							successful = true;
							break;
						case "lovescale":
							main.player.setLoveScale(Float.parseFloat(value));
							successful = true;
							break;
						case "nicenessscale":
							main.player.setNicenessScale(Float.parseFloat(value));
							successful = true;
							break;
						case "braveryscale":
							main.player.setBraveryScale(Float.parseFloat(value));
							successful = true;
							break;
						case "powertype":
							if(object instanceof Mob){
								DamageType type = DamageType.valueOf(value.toUpperCase());
								if(type!=null)
									((Mob)object).setPowerType(type);
								else System.out.println("\""+value+"\" is not a valid power type; Line: "+(index+1)+"\tScript: "+ID);
							}
							successful = true;
							break;
						default:
							System.out.println("\"" + target +"\" is an invalid property to modify for \"" + object + "\"; Line: "+(index+1)+"\tScript: "+ID);
							successful = true;
						}
					} catch (Exception e) {
						System.out.println("Could not set \"" + value +"\" to \"" + target + "\"; Line: "+(index+1)+"\tScript: "+ID);
						successful = true;
					}
					break;
				default:
					System.out.println(1);
					System.out.println("\""+function+"\" is not a valid operation for modifying values; Line: "+(index+1)+"\tScript: "+ID);
				}

				if (!successful)
					System.out.println("\"" + target +"\" is an invalid property to modify for object type "
							+ "\"" + object.getClass().getSimpleName() + "\"; Line: "+(index+1)+"\tScript: "+ID);
			}
		} else {
			//			System.out.println("finding var \""+target+"\"");
			try{
				//find local variable
				Object obj = getVariable(target);

				if(obj!=null){
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
						break;
					default:
						System.out.println("\""+function+"\" is not a valid operation for modifying values; Line: "+(index+1)+"\tScript: "+ID);

					}

				} else {
					obj = main.history.getVariable(target);
					System.out.println(value);
					if(obj!=null){
						switch(function.toLowerCase()){
						case "add":
							switch(obj.getClass().getSimpleName().toLowerCase()){
							case "float":
								main.history.setVariable(target, (float) obj + Float.parseFloat(value));
								break;
							case "integer":
								main.history.setVariable(target, (int) obj + Integer.parseInt(value));
								break;
							case"string":
								main.history.setVariable(target, ((String)obj).concat(value));
								break;
							}
							break;
						case "set":
							switch(obj.getClass().getSimpleName().toLowerCase()){
							case "float":
								main.history.setVariable(target, Float.parseFloat(value));
								break;
							case "integer":
								main.history.setVariable(target, Integer.parseInt(value));
								break;
							default:
								main.history.setVariable(target, value);
							}
							break;
						default:
							System.out.println(2);
							System.out.println("\""+function+"\" is not a valid operation for modifying values; Line: "+(index+1)+"\tScript: "+ID);

						}
					} else {
						System.out.println("No variable  locally or globally called \""+target+"\"; Line: "+(index+1)+"\tScript: "+ID);
					}
				}
			} catch (Exception e) {
				System.out.println("Error casting \"" + value +"\" to the value of \"" + target + 
						"\" for modification; Line: "+(index+1)+"\tScript: "+ID);
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
	
	//combines the first and third elements of an array<> into a single boolean by the condition of the second element
	//assumes values are boolean
	//used in the evaluation of an if statement
	public Array<String> combine(Array<String> arguments){
		Array<String> result = new Array<>();
		if(arguments.size<3)
			return arguments;

		boolean value1 = Boolean.parseBoolean(arguments.get(0));
		boolean value2 = Boolean.parseBoolean(arguments.get(2));
		Boolean combined = null;
		String condition = arguments.get(1);

		if(condition.equals("and"))
			combined = value1 && value2;
		if (condition.equalsIgnoreCase("or"))
			combined = value1 || value2;
		if(combined==null){
			System.out.println("\""+condition+"\" is and invalid condition; Line: "+(index+1)+"\tScript: "+ID);
			combined = false;
		}

		result.add(String.valueOf(combined));
		for(int i=3; i<arguments.size;i++)
			result.add(arguments.get(i));
		return result;
	}

	//create instance of a local variable
	public boolean declareVariable(String variableName, Object value){
		for(String p : localVars.keySet())
			if (p.equals(variableName)){
				System.out.println("Variable \""+variableName +"\" already exists locally; Line: "+(index+1)+"\tScript: "+ID);
				return false;
			}
		if (!(value instanceof String) && !(value instanceof Integer) && !(value instanceof Float) &&!(value instanceof Boolean))
			return false;

		localVars.put(variableName, value);
		return true;
	}

	//find local variable with given name
	public Object getVariable(String variableName){
		for(String p : localVars.keySet())
			if (p.equals(variableName)){
				return localVars.get(p);
			}
		return null;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
	}

	public void setVariable(String var, Object val){
		for(String p : localVars.keySet()){
			if(p.equals(var)){
				String type = localVars.get(p).getClass().getSimpleName();
				if(!var.getClass().getSimpleName().equals(type)){
					try{
						if(type.toLowerCase().equals("float"))
							localVars.put(p, (float) val);
						if(type.toLowerCase().equals("integer"))
							localVars.put(p,(int) val);
						if(type.toLowerCase().equals("string"))
							localVars.put(p,(String) val);
						if(type.toLowerCase().equals("boolean"))
							localVars.put(p, (boolean) val);
					} catch (Exception e){System.out.println("Wrong type");}
				} else 
					localVars.put(p,val);
			}
		}
	}

	private boolean addCheckpoint(String name, Integer index){
		if(checkpoints.containsKey(name)){
			System.out.println("\""+name+"\" is already a set checkpoint in script; Line: "+(this.index+1));
			return false;
		}

		checkpoints.put(name, index);
		return true;
	}

	private void getCheckpoint(String name){
		if(!checkpoints.containsKey(name)){
			System.out.println("The checkpoint \""+name+"\" has not been set in script; Line: "+(index+1)+"\tScript: "+ID);
		} else 
			index = checkpoints.get(name);
	}

	public void spawn(Entity e){
		main.addObject(e);
	}
	
	public String toString(){
		return ID;
	}

	public void setPlayState(Main gs) { main = gs; }
	public Entity getOwner(){ return owner; }
	public Object getActiveObject(){ return activeObj; }
	public void setActiveObj(Object obj){ activeObj = obj; }
	public String getCurrentName() { return currentName; }

	public class Operation{
		public String type;
		public int start, end;

		public Operation(String type, int start, int end){
			this.type = type;
			this.start = start;
			this.end = end;
		}

		public Operation(String type, Pair<Integer, Integer> bounds){
			this.type = type;
			this.start = bounds.getKey();
			this.end = bounds.getValue();
		}

		public String toString(){
			return "{"+type+", "+"("+start+", "+end+")}";
		}
	}

	public class Choice {
		public int start, end;
		private Array<Option> options;

		public Choice(int start, int end, Array<Option> options){
			this.start = start;
			this.end = end;
			this.options = options;
		}

		public String toString(){
			return "\n["+start+", "+end+"]\t{"+options.toString()+"}";
		}

		public boolean contains(String key){
			return options.contains(new Option(key, null), false);
		}

		public Option get(String key){
			int index = options.indexOf(new Option(key, null), false);
			if(index>=0)
				return options.get(index);
			else return null;
		}
	}

	public class Option{
		public String condition, message;
		public int start, end, type;
		
		private Script script;

		public Option(String name, int type, Script script){
			this.start = -1;
			this.end = -1;
			this.message = name;
			this.type = type;
			this.script = script;
			condition ="";
		}

		public Option(String name, Script script){
			this(name, 0, script);
		}

		public boolean isAvailable(){
			if(!condition.isEmpty()){
				Evaluator eval= new Evaluator(main);
				return eval.evaluate(condition, script);
			}
			return true;
		}

		public void setCondition(String s){ condition = s; }

		public Pair<Integer, Integer> getBounds(){
			return new Pair<Integer, Integer>(start, end);
		}

		public void setBounds(Pair<Integer, Integer> b){
			start = b.getKey();
			end = b.getValue();
		}

		public String toString(){
			if(condition.isEmpty())				
				return message +"{"+type+"} ["+start+", "+end+"]";
			return message +"{"+type+"} ["+start+", "+end+"] :: "+condition;
		}

		public boolean equals(Object o){
			if (o instanceof Option)
				return ((Option) o).message.toLowerCase().equals(message.toLowerCase());
			return false;
		}
	}
}
