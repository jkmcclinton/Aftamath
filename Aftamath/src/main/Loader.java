package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.utils.Array;

import entities.Warp;
import handlers.FadingSpriteBatch;
import handlers.Pair;
import handlers.Vars;
import scenes.Scene;
import scenes.Script;

public class Loader{
	
	public boolean finished = false;

	private Main main;
	private int index;
	private boolean loadwarps, document;
	private Progress progress = null;

	// variables for documenting data
	private TreeSet<String> IDs, variables, events;
	private Array<Integer> used;
	private Array<Pair<String, String>> entities; //used for determining duplication

	private enum Progress {
		RETRIEVE_OBJECTS, DOCUMENT_VARIABLES, CATALOGUE_WARPS, RETRIEVE_VARIABLES,
		FADING, NIL, CREATE_SCENE
	}

	public Loader(Main main, boolean loadwarps, boolean document){
		this.document = document;
		this.loadwarps = loadwarps;
		this.main = main;

		IDs = new TreeSet<>();
		variables = new TreeSet<>();
		events = new TreeSet<>();
		used = new Array<>();
		entities = new Array<>();
	}

	public void update(float dt){
		switch(progress){
		case RETRIEVE_OBJECTS:
			retrieveFromTMX();
			break;
		case RETRIEVE_VARIABLES:
			retrieveFromScript();
			break;
		case DOCUMENT_VARIABLES:
			documentVariables();
			if(loadwarps) resetProgress(Progress.CREATE_SCENE);
			else resetProgress(Progress.FADING);
			break;
		case CREATE_SCENE:
			if(!loadwarps) resetProgress(Progress.FADING); 
			else createScene();
			break;
		case CATALOGUE_WARPS:
			catalogueWarps();
			break;
		case FADING:
			if(main.getSpriteBatch().getFadeType()==FadingSpriteBatch.FADE_IN)
				progress = (Progress.NIL);
			break;
		case NIL:
			finished = true;
			break;
		}
	}

	public void start(){
		if(document) resetProgress(Progress.RETRIEVE_OBJECTS);
		else if(loadwarps) resetProgress(Progress.CREATE_SCENE);
	}

	/**
	 * precreates all existing warps across entire game and puts them to hashtable
	 */
	Scene s;
	public void catalogueWarps(){
			// create warps from level and add them to the hash
			Array<Warp> w;
			String l = Game.LEVEL_NAMES.get(index);
			w = s.createWarps();
			for(Warp i : w) main.warps.put(l+i.warpID, i);
			index++;
			progress = Progress.CREATE_SCENE;
			
	}
	
	public void createScene(){
		if(index>=Game.LEVEL_NAMES.size) {
			resetProgress(Progress.FADING);
			//link all warps together
			for(Warp i : main.warps.values()){
				i.setLink(main.warps.get(i.next + i.getLinkID()));
				//			System.out.println(i);
			}
		} else {
			s = new Scene(Game.LEVEL_NAMES.get(index));
			progress = Progress.CATALOGUE_WARPS;
		}
	}

	public void resetProgress(Progress prog){
		index =0;
		progress = prog;
		if(prog==Progress.FADING)
			main.getSpriteBatch().fade();
	}


	/**
	 * load each level and collect data about NPCs and other Entities
	 */
	private void retrieveFromTMX(){
		String level = Game.LEVEL_NAMES.get(index);

		TiledMap tileMap = new TmxMapLoader().load("assets/maps/" + level + ".tmx");
		if(tileMap.getLayers().get("entities")!=null){
			MapObjects objects = tileMap.getLayers().get("entities").getObjects();
			for(MapObject object : objects) {
				Object o = object.getProperties().get("NPC");
				if(o!=null) {
					String ID = object.getProperties().get("NPC", String.class);		//name used for art file
					String sceneID = object.getProperties().get("ID", String.class);	//unique int ID across scenes
					String name = object.getProperties().get("name", String.class);		//character name
					String script = object.getProperties().get("script", String.class);		
					if(ID!=null && sceneID!=null && name!=null){
						if(Vars.isNumeric(sceneID))
							if(Integer.parseInt(sceneID)<0)
								continue;
						String s = " ";
						s = Vars.formatHundreds(s, sceneID.trim().length());
						s+=sceneID+" - "+name+"; "+ID;
						s+=Vars.addSpaces(s, 40)+": "+level+".tmx";
						if(script!=null) s+=Vars.addSpaces(s, 75)+": " + script;
						IDs.add(s);

						//conflicting entity!!!
						if(used.contains(Integer.parseInt(sceneID), false))
							s = "*"+s.substring(1, 4) + "*" + s.substring(5);
						else {
							used.add(Integer.parseInt(sceneID));
							entities.add(new Pair<>(name, ID));
						}
					}	
				}

				o = object.getProperties().get("Entity");
				if(o==null) o = object.getProperties().get("entity");
				if(o!=null) {
					String ID = object.getProperties().get("entity", String.class);		//name used for art file
					if(ID==null) ID = object.getProperties().get("Entity", String.class);
					String sceneID = object.getProperties().get("ID", String.class);	//unique int ID across scenes
					String script = object.getProperties().get("script", String.class);
					String name = object.getProperties().get("name", String.class);

					if(ID!=null && sceneID!=null){
						if(Vars.isNumeric(sceneID))
							if(Integer.parseInt(sceneID)<0)
								continue;
						String s = " ";
						s = Vars.formatHundreds(s, sceneID.trim().length());
						s+=sceneID+" - "+ID;
						s+=Vars.addSpaces(s, 40)+": "+level;
						if(script!=null) s+=Vars.addSpaces(s, 75)+": " + script;
						IDs.add(s);

						//conflicting entity!!!
						if(used.contains(Integer.parseInt(sceneID), false))
							s = "*"+s.substring(1, 4) + "*" + s.substring(5);
						else {
							used.add(Integer.parseInt(sceneID));
							entities.add(new Pair<>(name, ID));
						}
					}
				}
			}
		}

		index ++;
		if(index>=Game.LEVEL_NAMES.size) resetProgress(Progress.RETRIEVE_VARIABLES);
	}


	/**
	 * define variables and events from scripts, as well as spawned entities
	 */
	private void retrieveFromScript(){
		String script = (String) Game.SCRIPT_LIST.keySet().toArray()[index];

		try{
			BufferedReader br = new BufferedReader(new FileReader(Game.res.getScript(script)));
			try {
				String line = br.readLine();
				String command;
				while (line != null ) {
					//parse command
					if (!line.startsWith("#")){
						line = line.trim();
						if (line.indexOf("(") == -1)
							if(line.startsWith("["))
								command = line.substring(line.indexOf("[")+1, line.indexOf("]"));
							else command = line;
						else command = line.substring(0, line.indexOf("("));
						command = command.trim();

						String[] args = Script.args(line);
						if(command.toLowerCase().equals("declare")){
							if(args.length==4){
								String s = args[0];
								s+=Vars.addSpaces(s, 25) + ": " + args[2].toLowerCase();
								s+=Vars.addSpaces(s, 36) + ": " + args[1].toLowerCase();
								s+=Vars.addSpaces(s, 47) + ": " + script;
								variables.add(s);
							}
						} if(command.toLowerCase().equals("setevent")){
							String s = args[0];
							s+=Vars.addSpaces(s, 25)+ ": " + script;
							events.add(s);
						} if(command.toLowerCase().equals("setflag")){ 
							String s = args[0];
							s+=Vars.addSpaces(s, 25) + ": flag";
							s+=Vars.addSpaces(s, 36) + ": global";
							s+=Vars.addSpaces(s, 47) + ": " + script;
							variables.add(s);
						} if(command.toLowerCase().equals("spawn")){
							int sceneID = -1;
							if(args.length==6)
								if(Vars.isNumeric(args[5].trim()))
									sceneID = Integer.parseInt(args[5].trim());

							String name=args[2].trim(), ID=args[1].trim();

							//sceneID is given and a new mob is spawned
							Pair<String, String> p = new Pair<>(name, ID);
							if(name==null)p = new Pair<>("", ID);
							if(!entities.contains(p, false) && sceneID!=-1){
								String s = " ";
								s = Vars.formatHundreds(s, String.valueOf(sceneID).length());
								if(name!=null)
									s+=sceneID+" - "+name+"; "+ID;
								else
									s+=sceneID+" - "+ID;
								s+=Vars.addSpaces(s, 40)+": "+script+".txt";
								if(script!=null) s+=Vars.addSpaces(s, 75)+": ???";
								IDs.add(s);

								//conflicting entity!!!
								if(used.contains(sceneID, false))
									s = "*"+s.substring(1, 4) + "*" + s.substring(5);
								else {
									used.add(sceneID);
									entities.add(p);
								}
							}
						}
					}

					line = br.readLine();
				}
			} finally {
				br.close();
			}
		} catch(Exception e){
			e.printStackTrace();
		}

		index ++;
		if(index>=Game.SCRIPT_LIST.size()) resetProgress(Progress.DOCUMENT_VARIABLES);
	}
	
	/**
	 * create a file listing all used scene IDs, event names, and used variable names from game
	 */
	private void documentVariables(){
		//document unused sceneIDs
		for(int i = 0; i<500; i++)
			if(!used.contains(i, false)){
				String s = " ";
				s = Vars.formatHundreds(s, String.valueOf(i).length());
				IDs.add(s+i+ " - ");
			}

		//collect default global variables
		HashMap<String, Object> varList = main.history.getVarlist();
		for(String v: varList.keySet()){
			String c = varList.get(v).getClass().getSimpleName().toLowerCase();
			String s = v+"";
			s+=Vars.addSpaces(s, 25) + ": " + c;
			s+=Vars.addSpaces(s, 36) + ": global";
			variables.add(s);
		}

		//write SceneID list to file
		try {
			FileHandle file = Gdx.files.local("assets/SceneID List.txt");
			BufferedWriter wr = new BufferedWriter(file.writer(false));

			String s = "Scene ID";
			s+=Vars.addSpaces(s, 40) + "Spawned by";
			s+=Vars.addSpaces(s, 75) + "Script";
			wr.write(s); wr.newLine();

			Iterator<String> it = IDs.iterator();
			while(it.hasNext()){
				wr.write(it.next());
				wr.newLine();
			}

			wr.flush();
			wr.close();
		} catch(Exception e){
			System.out.println("Could not write a sceneID list file.");
			e.printStackTrace();
		} 

		try{
			FileHandle file = Gdx.files.local("assets/Variable List.txt");
			BufferedWriter wr = new BufferedWriter(file.writer(false));

			String s = "Variable Name";
			s+=Vars.addSpaces(s, 25) + "Type";
			s+=Vars.addSpaces(s, 36) + "Scope";
			s+=Vars.addSpaces(s, 47) + "Script";
			wr.write(s); wr.newLine();

			Iterator<String> it = variables.iterator();
			while(it.hasNext()){
				wr.write(it.next());
				wr.newLine();
			}

			wr.flush();
			wr.close();
		} catch(Exception e){
			System.out.println("Could not write variable list file");
		}

		try{
			FileHandle file = Gdx.files.local("assets/Event List.txt");
			BufferedWriter wr = new BufferedWriter(file.writer(false));

			String s = "Event Name";
			s+=Vars.addSpaces(s, 25) + "Script";
			wr.write(s); wr.newLine();

			Iterator<String> it = events.iterator();
			while(it.hasNext()){
				wr.write(it.next());
				wr.newLine();
			}

			wr.flush();
			wr.close();
		} catch(Exception e){
			System.out.println("Could not write event list file");
		}
		
	}

	public Progress getProgress() { return progress; }
}
