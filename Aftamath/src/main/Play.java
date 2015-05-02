package main;

import static handlers.Vars.PPM;
import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.MyContactListener;
import handlers.MyInput;
import handlers.Pair;
import handlers.PositionalAudio;
import handlers.Vars;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import scenes.Scene;
import scenes.Script;
import scenes.Street;
//import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
//import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import entities.Entity;
import entities.Ground;
import entities.HUD;
import entities.Mob;
import entities.NPC;
import entities.Player;
import entities.SpeechBubble;
import entities.Warp;

public class Play extends GameState {
	
	public Mob character;
	public Player player;
	public Warp warp;
	public NPC narrator;
	public HUD hud;
	public History history;
	public Script currentScript;
	public int stateType, currentEmotion;
	public boolean paused, analyzing, choosing, waiting;
	public boolean warping, warped; //for changing between scenes
	public boolean speaking; //are letters currently being drawn individually
	
	public float dayTime;
	public float waitTime, totalWait;
	
	private int sx, sy;
	private float speakTime, speakDelay = .025f;
	private ArrayDeque<Pair<String, Integer>> displayText; //all text pages to display
	private String[] speakText; //current text page displaying
	private World world;
	private Scene scene, nextScene;
	private Array<Body> bodiesToRemove;
	private ArrayList<Entity> objects;
	private ArrayList<PositionalAudio> sounds;
	private Box2DDebugRenderer b2dr;
	private RayHandler rayHandler;
	private MyContactListener cl = new MyContactListener(this);
	private int prevStateType;
	private SpeechBubble[] choices;
	
	//input states
	public static final int MOVE = 0; //can move
	public static final int MOVELISTEN = 1; //listening to text and can move
	public static final int LISTEN = 2; //listening to text only
	public static final int CHOICE = 3; //choosing an option
	public static final int LOCKED = 4; //lock all input
	public static final int PAUSED = 5; //in pause menu
	
	//debug
	
	//use these for determining hard-coded values
	//please, don't keep them as hard-coded
	//private int debugX;
	//private int debugY = 244;
	
	private boolean dbRender = false, rayHandling, render = true, debugtxt = true;
	private float light = .5f;
	private static ArrayList<Color> colors = new ArrayList<Color>();
	private int colorIndex;
	private String songTitle="";
	
	public Play(GameStateManager gsm) {
		super(gsm);
	}
	
	public void create(){
		bodiesToRemove = new Array<>();
		history = new History();

		objects = new ArrayList<Entity>();
		sounds = new ArrayList<PositionalAudio>();
		world = new World(new Vector2 (0, Vars.GRAVITY), true);
		world.setContactListener(cl);
		b2dr = new Box2DDebugRenderer();
//		b2dr.setDrawVelocities(true);
		rayHandler = new RayHandler(world);
		rayHandler.setAmbientLight(0.5f);
		player = new Player("TestName", "male", "player1");
		scene= new Street(world,this,player);
//		scene = new Room(world, this, "room1_1", null);
		cam.setBounds(Vars.TILE_SIZE*4, (scene.width-Vars.TILE_SIZE*4), 0, scene.height);
		b2dCam.setBounds((Vars.TILE_SIZE*4)/PPM, (scene.width-Vars.TILE_SIZE*4)/PPM, 0, scene.height/PPM);
		world.setGravity(scene.getGravity());
		
		stateType = MOVE;
		speakTime = 0;
		displayText = new ArrayDeque<Pair<String, Integer>>();
		
		scene.setRayHandler(rayHandler);
		scene.create();
		
		initEntities();
		createPlayer(scene.getSpawnpoint());
		hud = new HUD(player, hudCam);
		//new PointLight(rayHandler, Vars.LIGHT_RAYS, Color.RED, 1000000000, player.getPosition().x, player.getPosition().y + 10);
		
		setSong(scene.DEFAULT_SONG, Game.musicVolume);
	}
	
	public void update(float dt) {
//		if(warped)return;
		buttonTime += dt;
		hud.update(dt);
		
		if (!paused){
			speakTime += dt;
			debugText = "";
			
			if(currentScript != null && !waiting) currentScript.update();
			
			if (waiting){
				if(waitTime<totalWait) waitTime+=dt;
				else{
					waiting = false; 
					waitTime = totalWait = 0;
				}
			}

			handleInput();
			world.step(dt, 6, 2);
			
			for(PositionalAudio s : sounds)
				updateSound(s.location, s.sound);
			bodiesToRemove = cl.getBodies();
			
			if(currentScript != null){
				float dx = player.getPosition().x - currentScript.getOwner().getPosition().x;
				
				//if player gets too far from whatever they're talking to
				if(Math.abs(dx) > 50/PPM && currentScript.limitDistance){
					currentScript = null;
					player.setInteractable(null);
					speaking = false;
					hud.hide();
					stateType = MOVE;
				}
			}
 
			for (Entity d : objects){
				if (d instanceof Entity && !(d instanceof Ground)) {
					d.update(dt);

					//kill object
					if (d.getBody() == null) {d.create();}
					if (d.getPosition().x*PPM > scene.width + 50 || d.getPosition().x*PPM < -50 || 
							d.getPosition().y*PPM > scene.height + 100 || d.getPosition().y*PPM < -50) {
						if (d instanceof Player){
							//Game Over
							//System.out.println("die"); 
						} else {
							bodiesToRemove.add(d.getBody());
						}
					}
				}
			}

			for (Body b : bodiesToRemove){
				if (b != null) if (b.getUserData() != null){
					objects.remove(b.getUserData());
					world.destroyBody(b);
				}
			}

			bodiesToRemove.clear();

			if(warping){
				if (sb.getFadeType() == FadingSpriteBatch.FADE_IN)
					warp();
				if (!sb.fading && scene.equals(nextScene)){
					warping = false;
					nextScene = null;
				} else if(sb.fading &&nextScene.newSong){
					fadeSong(dt, true);
				}
			}
			
			if(changingSong){
				if(fadeOutSong(dt)){
					setSong(nextSong);
					changingSong = false;
				}
			}
			
//			Mob e1 = (Mob) objects.get(1);
			debugText= "Volume: "+song.getVolume();
			debugText+= "/l"+songTitle;
			debugText +="/l"+ character.getName() + " x: " + (int) (character.getPosition().x*PPM) + "    y: " + ((int) (character.getPosition().y*PPM) - character.height);
			debugText +="/lCamera" + " x: " + (int) (cam.position.x) + "    y: " + ((int) (cam.position.y));
//			debugText +="/l"+ e1.getName() + " x: " + (int) (e1.getPosition().x*PPM) + "    y: " + ((int) (e1.getPosition().y*PPM) - e1.height);
			rayHandler.lightList.first().setPosition(character.getPosition().x, character.getPosition().y - 1000);  // moving point light
			//debugText += "/lLight x: " + (int) (rayHandler.lightList.first().getX()*PPM) + "    y: " + (int) (rayHandler.lightList.first().getY()*PPM);
//			printObjects();
		} else {
			if (!quitting)
				handleInput();
		}

		cam.locate(dt);
		if(quitting)
			quit();
	}
	
	public void render() {
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		if(render){
			scene.renderBG(sb);

			sb.begin();
			sb.setProjectionMatrix(cam.combined);
			rayHandler.setCombinedMatrix(cam.combined);
			sb.end();

			scene.renderEnvironment(cam);
			sb.begin();
			for (Entity d : objects){
				d.render(sb);
		}
		
//		scene.renderFG(sb, cam);
		sb.end();
		}
		
		if (speaking) speak();
		if (rayHandling) rayHandler.updateAndRender();
		hud.render(sb, currentEmotion);
		
		b2dCam.setPosition(character.getPosition().x, character.getPosition().y + 15 / PPM);
		b2dCam.update();
		if (dbRender) b2dr.render(world, b2dCam.combined);

		if(debugtxt) updateDebugText();
	}
	
	public void updateDebugText() {
		sb.begin();
			drawString(sb, debugText, 0, Game.height/2 - font[0].getRegionHeight() - 2);
		sb.end();
	}

	public void handleInput() {
		if(MyInput.isPressed(MyInput.DEBUG_UP)) {
//			light += .1f; rayHandler.setAmbientLight(light);
			player.addMoney(100d);
		}
		
		if(MyInput.isPressed(MyInput.DEBUG_DOWN)){
//			light -= .1f; rayHandler.setAmbientLight(light);
			player.subtractMoney(100d);
		}
		
		if(MyInput.isPressed(MyInput.DEBUG_LEFT)) {
			if(colorIndex>0) 
				colorIndex--; 
			rayHandler.setAmbientLight(colors.get(colorIndex)); 
			rayHandler.setAmbientLight(light);
		}
		
		if(MyInput.isPressed(MyInput.DEBUG_LEFT2)) {
			changingSong = true;
			songTitle = Scene.BGM.get((int) (Math.random()*(Scene.BGM.size)));
			nextSong = Gdx.audio.newMusic(new FileHandle("res/music/"+songTitle+".wav"));
		}
		
		if(MyInput.isPressed(MyInput.DEBUG_RIGHT)) {
			if(colorIndex<colors.size() -1) 
				colorIndex++; 
			rayHandler.setAmbientLight(colors.get(colorIndex)); 
			rayHandler.setAmbientLight(light);
			}
		
		if(MyInput.isPressed(MyInput.DEBUG)) rayHandling = !rayHandling ;
		if(MyInput.isPressed(MyInput.DEBUG1)) dbRender = !dbRender ;
		if(MyInput.isPressed(MyInput.DEBUG2)) character.respawn();
//		if(MyInput.isPressed(MyInput.DEBUG2)) character.respawn();
		if(MyInput.isPressed(MyInput.DEBUG3)) debugtxt=!debugtxt;
		
		if (paused){
			if (stateType == PAUSED) {
				if(MyInput.isPressed(MyInput.PAUSE)) unpause();
				if(MyInput.isPressed(MyInput.JUMP)|| MyInput.isPressed(MyInput.ENTER) && !gsm.isFading()){
					try {
						Method m = Play.class.getMethod(Vars.formatMethodName(menuOptions[menuIndex[0]][menuIndex[1]]));
						m.invoke(this);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				if(buttonTime >= DELAY){
					if(MyInput.isDown(MyInput.DOWN)){
						if(menuIndex[1] < menuMaxY){
							menuIndex[1]++;
							//play menu sound
						} else {
							menuIndex[1] = 0;
						}
						buttonTime = 0;
					}
					
					if(MyInput.isDown(MyInput.UP)){
						if(menuIndex[1] > 0){
							menuIndex[1]--;
							//play menu sound
						} else {
							menuIndex[1] = menuMaxY;
						}
						buttonTime = 0;
					}
					
					if(MyInput.isDown(MyInput.RIGHT)){
						if(menuIndex[0] < menuMaxX){
							menuIndex[0]++;
							//play menu sound
						} else {
							//play menu invalid sound
						}
						buttonTime = 0;
					}
					
					if(MyInput.isDown(MyInput.LEFT)){
						if(menuIndex[0] > 0){
							menuIndex[0]--;
							//play menu sound
						} else {
							//play menu invalid sound
						}
						buttonTime = 0;
					}
				}
			}
		} else switch (stateType){
		case LOCKED:
			if(MyInput.isPressed(MyInput.PAUSE) && !quitting) pause();
			break;
		case MOVE:
			if(MyInput.isPressed(MyInput.PAUSE) && !quitting) pause();
			if(cam.moving||cam.focusing||warping||quitting||character.dead||waiting) return;
			if(MyInput.isPressed(MyInput.JUMP)) character.jump();
			if(MyInput.isDown(MyInput.UP)) {
				if(character.canWarp && player.isOnGround()) {
					warping = true;
					warped = false;
					warp = character.getWarp();
					nextScene = warp.getNext();
//					scene.saveLevel();
					sb.fade();
				}
				else if(character.canClimb) character.climb();
				else character.lookUp();
			}
			if(MyInput.isDown(MyInput.DOWN)) character.descend();
			if(MyInput.isPressed(MyInput.USE)) partnerFollow();
			if(MyInput.isPressed(MyInput.INTERACT)) {
				currentScript = player.interact();
				if (currentScript != null) {
					for (Entity d : objects)
						if (d instanceof SpeechBubble)
							bodiesToRemove.add(d.getBody()); //remove talking speech bubble
					currentScript.analyze();
					if (currentScript.limitDistance) {
						stateType = MOVELISTEN;
					} else {
						stateType = LOCKED;
						positionPlayer(player.getInteractable());
					}
				}
			}
			
			if(MyInput.isDown(MyInput.LEFT)) character.left();
			if(MyInput.isDown(MyInput.RIGHT)) character.right();
			if(MyInput.isDown(MyInput.ATTACK))player.attack(); 
			break;
		case MOVELISTEN:
			if(MyInput.isPressed(MyInput.PAUSE)) pause();
			if(cam.moving||cam.focusing||warping||quitting||character.dead||waiting) return;
			if(MyInput.isDown(MyInput.UP)) character.climb();
			if(MyInput.isDown(MyInput.DOWN)) character.descend();
			if(MyInput.isDown(MyInput.LEFT)) character.left();
			if(MyInput.isDown(MyInput.RIGHT)) character.right();
			if(MyInput.isPressed(MyInput.JUMP) && !speaking && buttonTime >= DELAY) {
				playSound(player.getPosition(), "ok1");

				if(displayText.isEmpty()) {
					if (currentScript != null) {
						currentScript.paused = currentScript.forcedPause = 
								currentScript.dialog = false;
					}
				} else {
					speakDelay = .2f;
					speakTime = 0;
					speak();
				}
			} else if (MyInput.isPressed(MyInput.JUMP) && speaking){
				buttonTime = 0;
				speaking = hud.fillSpeech(speakText);
			}
			break;
		case LISTEN:
			if(MyInput.isPressed(MyInput.PAUSE) && !quitting) pause();
			if(MyInput.isPressed(MyInput.JUMP) && !speaking && buttonTime >= DELAY) {
				playSound(player.getPosition(), "ok1");
				if(displayText.isEmpty()) {
					if (currentScript != null) {
						currentScript.paused = currentScript.forcedPause =
								currentScript.dialog = false;
					} else {
						hud.hide();
						stateType = MOVE;
					}
				} else {
					speakDelay = .2f;
					speakTime = 0;
					speak();
				}
			} else if (MyInput.isPressed(MyInput.JUMP) && speaking){
				buttonTime = 0;
				speaking = hud.fillSpeech(speakText);
			}
			break;
		case CHOICE:
			int prevIndex = choiceIndex;
			if(MyInput.isPressed(MyInput.PAUSE) && !quitting) pause();
			if (buttonTime >= buttonDelay){
				if(MyInput.isDown(MyInput.LEFT)){
					buttonTime = 0;
					choiceIndex++;
					if (choiceIndex >= choices.length)
						choiceIndex = 0;

					playSound(player.getPosition(), "text1");
					choices[choiceIndex].expand();
					choices[prevIndex].collapse();
				} else if (MyInput.isDown(MyInput.RIGHT)){
					buttonTime = 0;
					choiceIndex--;
					if (choiceIndex < 0)
						choiceIndex = choices.length - 1;

					playSound(player.getPosition(), "text1");
					choices[choiceIndex].expand();
					choices[prevIndex].collapse();
				} else if(MyInput.isPressed(MyInput.ENTER)){
					playSound(player.getPosition(), "ok1");
					wait(1f);

					currentScript.paused = choosing = false;
					currentScript.getChoiceIndex(choices[choiceIndex].getMessage());
					
					//delete speechBubbles from world
					for (SpeechBubble b : choices)
						bodiesToRemove.add(b.getBody()); 
				}
			}
			
			break;
		}
	}
	
	private void partnerFollow(){
		if(player.getPartner()!=null){
			if(player.getPartner().getName() != null){
				if(getMob(player.getPartner().getName())!=null){
					stateType = LISTEN;

					if (player.stopPartnerDisabled) {
						displayText.add(new Pair<>("No, I'm coming with you.", Mob.NORMAL));
						hud.changeFace(player.getPartner());
						player.faceObject(player.getPartner());
						speak();
					} else if(player.getPartner().getState() == NPC.FOLLOWING) {
						displayText.add(new Pair<>("I'll just stay here.", Mob.NORMAL));
						hud.changeFace(player.getPartner());
						speak();
						player.getPartner().stay();
					}
					else {
						if(player.getRelationship()<-2){ 
							displayText.add(new Pair<>("No way. I'm staying here.", Mob.MAD));
							player.faceObject(player.getPartner());
						} else if(player.getPartner().getGender().equals("female")) displayText.add(new Pair<>("Coming!", Mob.HAPPY));
						else displayText.add(new Pair<>("On my way.", Mob.NORMAL));

						hud.changeFace(player.getPartner());
						speak();
						player.getPartner().follow();
					}
				}else{
					String partner;
					if (player.getPartner().getGender().equals(Mob.MALE)) partner = "boyfriend";
					else partner = "girlfriend";
					displayText.add(new Pair<>("Your "+partner+" isn't here at the moment.", Mob.NORMAL));
					hud.changeFace(narrator);
					speak();
					stateType = LISTEN;
				}
			}
		} else {
			displayText.add(new Pair<>("You ain't got nobody to follow you!", Mob.NORMAL));
			hud.changeFace(narrator);
			speak();
			stateType = LISTEN;
		}
	}
	
	public void positionPlayer(Entity obj){
		float min = 20;
		float dx = (player.getPosition().x - obj.getPosition().x) * Vars.PPM;
		float gx = min * dx / Math.abs(dx) - dx ;
		
		if (Math.abs(dx) < min -2){
			player.positioning = true;
			player.setGoal(gx);
		} else {
			player.faceObject(player.getInteractable());
			player.controlled = false;
		}
	}
	
	//show choices in a circle around the player
	public void displayChoice(int[] types, String[] messages){
		final float h = 30 / PPM;
		float x, y, theta;
		int c = types.length;
		choices = new SpeechBubble[c];
		choiceIndex = 0;
		
		for (int i = 0; i < c; i++){
			int positioning;
			theta = (float) (i * 2 * Math.PI / c);
			x = (float) (h * Math.cos(theta) + player.getPosition().x) * PPM;
			y = (float) (h * Math.sin(theta) + player.getPosition().y) * PPM + 15;
			
			if((theta>Math.PI/2-Math.PI/20 && theta<Math.PI/2+Math.PI/20) ||
					(theta>3*Math.PI/2-Math.PI/20 && theta<3*Math.PI/2+Math.PI/20)) 
				positioning = SpeechBubble.CENTERED;
			else if((theta>=0 && theta<Math.PI/2-Math.PI/20) ||
					(theta<=2*Math.PI && theta>3*Math.PI/2+Math.PI/20))
				positioning = SpeechBubble.LEFT_MARGIN;
			else positioning = SpeechBubble.RIGHT_MARGIN;
			choices[i] = new SpeechBubble(player, x, y, types[i], messages[i], positioning);
		}
		choices[0].expand();
	}
	
	public void dispose() { 
		if(song != null)
			song.stop();
	}
	
	public void speak(){
		if (!speaking) {
			speaking = true;
			//stateType = LISTEN;

			Pair<String, Integer> current = displayText.poll();
			String line = current.getKey();
			currentEmotion = current.getValue();
			
			speakText = line.split("/l");
			hud.createSpeech(speakText);

			sx = 0;
			sy = 0;

			if(!hud.raised) 
				hud.show();
		}
		
		//show text per character
		if (hud.moving == 0){
			if (speakTime >= speakDelay) {
				speakTime = 0;
				if(hud.raised) speakDelay = .025f;
				else speakDelay = 2f;

				char c = speakText[sy].charAt(sx);
				if (c == ".".charAt(0) || c == ",".charAt(0)) speakDelay = .25f;
				if(c != "~".charAt(0)){
					hud.addChar(sy, c);
					Gdx.audio.newSound(new FileHandle("res/sounds/text1.wav"))
					.play(Game.soundVolume * .9f, (float) Math.random()*.15f + .9f, 1);
				} else speakDelay = .75f;

				sx++;
				if (sx == speakText[sy].length()) {sx = 0; sy++; }
				if (sy == speakText.length) {
					speaking = false;
					speakText = null;
					
					if (currentScript != null){
						if(currentScript.peek() != null)
							if(currentScript.peek().toLowerCase().equals("choice")
									&& displayText.isEmpty())
								currentScript.readNext();
					}
				}
			}
		}
	}
	
	public void pause(){
		if (song != null) 
			if (song.isPlaying()) 
				song.pause();
		
		prevStateType = stateType;
		stateType = PAUSED;
		
		menuOptions = new String[][] {{"Resume", "Stats", "Save Game", "Load Game", "Options", "Quit to Menu", "Quit"}}; 
		
		paused = true;
		menuMaxY = menuOptions[0].length - 1;
		menuMaxX = 0;
		menuIndex[0] = 0;
		menuIndex[1] = 0;
	}
	
	public void stats(){
		//change menu to display stats
	}
	
	public void saveGame() {
		//save game menu
	}
	
	public void loadGame() {
		//load a saved game menu
		//display option to save before loading, with options: "save; don't save; cancel"
		super.loadGame();
	}
	
	public void quitToMenu(){
		//display option to save before exit, with options: "save and exit; exit; cancel"
		gsm.setState(GameStateManager.TITLE, true);
	}
	
	public void quit() {
		//display option to save before exit, with options: "save and exit; exit; cancel"
		super.quit();
	}
	
	public void unpause(){
		stateType = prevStateType;
		paused = false;
		
		if (song != null) 
				song.play();
	}
	
	public void warp(){
		if(warped) return;
		
		destroyBodies();
		scene = nextScene;
		scene.create();
		initEntities();
		createPlayer(new Vector2(warp.getLink().x, warp.getLink().y+player.rh));
		cam.setBounds(Vars.TILE_SIZE*4, (scene.width-Vars.TILE_SIZE*4), 0, scene.height);
		b2dCam.setBounds((Vars.TILE_SIZE*4)/PPM, (scene.width-Vars.TILE_SIZE*4)/PPM, 0, scene.height/PPM);
		if(scene.newSong)
			setSong(scene.DEFAULT_SONG);
		warped = true;
	}
	
	public void wait(float time){
		if(busy()) return;
		totalWait = time;
		waiting = true;
		waitTime = 0;
//		if(hud.raised) hud.clearSpeech();
	}
	
	public boolean busy(){
		return false;
	}
	
	public void setDispText(ArrayDeque<Pair<String, Integer>> dispText) { displayText = dispText;}
	public void setStateType(int type) {stateType = type; }
	public int getStateType(){ return stateType; }
	public SpeechBubble[] getChoices(){ return choices; }

	public void addSound(PositionalAudio s){ sounds.add(s); }
	public void addObject(Entity d){ objects.add(d); }
	public void addBodyToRemove(Body b){ bodiesToRemove.add(b); }
	
	public void createPlayer(Vector2 location){
		player.setPlayState(this);
		player.setPosition(location);
		player.create();
		character = player;
		cam.setCharacter(character); 
		b2dCam.setCharacter(character);
		objects.add(player);
		cam.locate(Vars.DT);
		sortObjects();
	}
	
	//create all enntites in scene
	private void initEntities() {
		getColors();
		removeAllObjects();
		objects.addAll(scene.getInitEntities());
		if(player.getPartner()!=null)
			if (player.getPartner().getName() != null) objects.add(player.getPartner());

		for (Entity d : objects){
			if(d instanceof Player) continue;
			d.setPlayState(this);
			d.create();
		}
		
		sortObjects();
	}
	
	public Mob getMob(String name) {
		for (Entity e : objects)
			if(e instanceof Mob){
				Mob m = (Mob) e;
				if(m.getName().equals(name))
					return m;
			}
		return null;
	}
	
	public void destroyBodies(){
		Array<Body> tmp = new Array<Body>();
		world.getBodies(tmp);
		for(Body b : tmp)
			world.destroyBody(b);
	}
	
	public void removeAllObjects(){
		for(PositionalAudio s : sounds)
			s.sound.stop();
		sounds = new ArrayList<PositionalAudio>();
		objects = new ArrayList<Entity>();
	}
	
	public Scene getScene(){ return scene; }
	public World getWorld(){ return world; }
	public HUD getHud() { return hud; }
	public Camera getCam(){ return cam; }

	public int count(Entity c){
		int i = 0;
		for (Entity d : objects) if (d.getClass().equals(c.getClass())) i++;
		return i;
	}

	//sort by objects' layer
	public void sortObjects(){
		Collections.sort(objects, new Comparator<Entity>(){
			public int compare(Entity o1, Entity o2) {
				return o1.compareTo(o2);
			}
		});
//		printObjects();
	}
	
	public void printObjects() {
		for(Entity e:objects){
			debugText+="/l"+e.ID;
		}
	}
	
	public void addSong(Music song){
		
	}

	public ArrayList<Entity> getObjects(){ return objects;	}
	
	public void getColors() {
		colors.add(Color.BLACK);
		colors.add(Color.BLUE);
		colors.add(Color.RED);
		colors.add(Color.GREEN);
		colors.add(Color.YELLOW);
		colors.add(Color.PINK);
		colors.add(Color.GRAY);
		colors.add(Color.CLEAR);
		colors.add(Color.CYAN);
		colors.add(Color.DARK_GRAY);
		colors.add(Color.MAGENTA);
		colors.add(Color.MAROON);
		colors.add(Color.NAVY);
		colors.add(Color.OLIVE);
		colors.add(Color.ORANGE);
		colors.add(Color.PURPLE);
		colors.add(Color.TEAL);
		colors.add(Color.WHITE);
	}
	
	public Color getAmbient(){
		// should be dependant on time;
		return new Color(2,2,2,Vars.ALPHA);
	}
	
	//class that contains minor handling for all events and flags
	public class History {

		private Array<Pair<String, String>> eventList;
		private Array<Pair<String, Boolean>> flagList;
		
		public History(){
			eventList = new Array<>();
			flagList = new Array<>();
		}
		
		public History(String loadedData){
			
		}
		
		public boolean getFlag(String flag){ 
			for(Pair<String, Boolean> p : flagList)
				if (p.getKey().equals(flag))
					return p.getValue();
			return false;
		}
	
		public void setFlag(String flag, boolean val){
			for(Pair<String, Boolean> p : flagList)
				if(p.getKey().equals(flag))
					p.setValue(val);
		}
		public void addFlag(String flag, boolean val){ flagList.add(new Pair<>(flag, val)); }
		public void setEvent(String event, String description){ eventList.add(new Pair<>(event, description)); }
		public boolean findEvent(String event){ 
			for(Pair<String, String> p : eventList)
				if (p.getKey().equals(event))
					return true;
			return false;
		}
		
		//for use in the thing
		public Texture getEventIcon(String event, String descriptor){
			switch(event){
			case "BrokeAnOldLadyCurse":
				return Game.res.getTexture("girlfriend1");
			default:
				return null;
			}
		}
	}
}



