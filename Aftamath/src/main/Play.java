package main;

import static handlers.Vars.PPM;
import handlers.Camera;
import handlers.Entity;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.MyContactListener;
import handlers.MyInput;
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
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
//import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import entities.Ground;
import entities.HUD;
import entities.Mob;
import entities.NPC;
import entities.Player;
import entities.SpeechBubble;

public class Play extends GameState {
	
	public Mob character;
	public Player player;
	public NPC narrator;
	public boolean paused;
	private World world;
	private Scene scene, nextScene;
	public HUD hud;
	private Array<Body> bodies;
	private ArrayList<Entity> objects;
	private ArrayList<PositionalAudio> sounds;
	private Box2DDebugRenderer b2dr;
	private RayHandler rayHandler;
	private MyContactListener cl = new MyContactListener();
	
	public Script currentScript;
	public boolean warping, warpe; //for changing between scenes
	public boolean analyzing;
	public boolean speaking; //are letters currently being drawn individually
	private int sx, sy;
	private float speakTime, speakDelay = .025f;
	private ArrayDeque<String> displayText; //all text pages to display
	private String[] speakText; //current text page displaying
	
	int stateType;
	private int prevStateType;
	private SpeechBubble[] choices;
	public boolean choosing;
	
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
	private boolean dbRender, rayHandling;
	private float light = .5f;
	private static ArrayList<Color> colors = new ArrayList<Color>();
	private int colorIndex;
	
	public Play(GameStateManager gsm) {
		super(gsm);
	}
	
	public void create(){
		bodies = new Array<>();

		objects = new ArrayList<Entity>();
		sounds = new ArrayList<PositionalAudio>();
		world = new World(new Vector2 (0, Vars.GRAVITY), true);
		world.setContactListener(cl);
		b2dr = new Box2DDebugRenderer();
		rayHandler = new RayHandler(world);
		rayHandler.setAmbientLight(0.5f);
		player = new Player("TestName", "male", "player1");
		scene = new Street(world, this, player);
		cam.setBounds(6, scene.width, 0, 220);
		b2dCam.setBounds(6/PPM, (scene.width)/PPM, 0, 220/PPM);
		world.setGravity(scene.getGravity());
		
		stateType = MOVE;
		speakTime = 0;
		displayText = new ArrayDeque<String>();
		
		scene.setRayHandler(rayHandler);
		scene.create(this);
		
		initEntities();
		hud = new HUD(player, hudCam);
		//new PointLight(rayHandler, Vars.LIGHT_RAYS, Color.RED, 1000000000, player.getPosition().x, player.getPosition().y + 10);
		
//		character = getMob("Olivierre");
		character = player;
		cam.setCharacter(character); //set camera focus to the character
		
		setSong(scene.DEFAULT_SONG, Game.volume);
		//displayText.add("This place is extremely boring and actually/lquite pointless.~ Even my words are pointless./lThis is Just an extra line or two to show/loff some zesty letters I got in meh.");
		//speak();
	}
	
	public void update(float dt) {
			buttonTime += dt;
			hud.update(dt);
		
		if (!paused){
			speakTime += dt;
			debugText = "";
			
			if(currentScript != null) currentScript.update();

			handleInput();
			world.step(dt, 6, 2);

			
			for(PositionalAudio s : sounds)
				updateSound(s.location, s.sound);
			bodies = cl.getBodies();
			
			if(currentScript != null){
				float dx = player.getPosition().x - currentScript.getOwner().getPosition().x;
				
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
							bodies.add(d.getBody());
						}
					}
				}
			}

			for (Body b : bodies){
				if (b != null) if (b.getUserData() != null){
					//System.out.println(((Displayable) b.getUserData()).getID());
					objects.remove(b.getUserData());
					world.destroyBody(b);
				}
			}

			bodies.clear();

			if(warping){
				if (sb.getFadeType() == FadingSpriteBatch.FADE_IN)
					warp();
				if (!sb.fading && scene.equals(nextScene)){
					warping = false;
					nextScene = null;
				}
			}
			debugText= ""+song.getVolume();
//			debugText = character.getName() + " x: " + (int) (character.getPosition().x*PPM) + "    y: " + ((int) (character.getPosition().y*PPM) - player.height);
			rayHandler.lightList.first().setPosition(character.getPosition().x, character.getPosition().y - 1000);  // moving point light
			//debugText += "/lLight x: " + (int) (rayHandler.lightList.first().getX()*PPM) + "    y: " + (int) (rayHandler.lightList.first().getY()*PPM);
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

		if (speaking) speak();
		if (rayHandling) rayHandler.updateAndRender();
		hud.render(sb, Mob.NORMAL);
		updateDebugText();
		
		b2dCam.setPosition(character.getPosition().x, character.getPosition().y + 15 / PPM);
		b2dCam.update();
		if (dbRender) b2dr.render(world, b2dCam.combined);

		updateDebugText();
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
		
		if(MyInput.isPressed(MyInput.DEBUG_RIGHT)) {
			if(colorIndex<colors.size() -1) 
				colorIndex++; 
			rayHandler.setAmbientLight(colors.get(colorIndex)); 
			rayHandler.setAmbientLight(light);
			}
		
		if(MyInput.isPressed(MyInput.DEBUG)) rayHandling = !rayHandling ;
		if(MyInput.isPressed(MyInput.DEBUG1)) dbRender = !dbRender ;
		
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
			if(MyInput.isPressed(MyInput.PAUSE)) pause();
			break;
		case MOVE:
			if(MyInput.isPressed(MyInput.PAUSE)) pause();
			if(cam.moving) return;
			if(MyInput.isPressed(MyInput.JUMP)) character.jump();
			if(MyInput.isDown(MyInput.UP)) character.climb();
			if(MyInput.isDown(MyInput.DOWN)) character.descend();
			if(MyInput.isPressed(MyInput.USE)) use();
			if(MyInput.isPressed(MyInput.INTERACT)) {
				currentScript = player.interact();
				if (currentScript != null) {
					for (Entity d : objects)
						if (d instanceof SpeechBubble)
							bodies.add(d.getBody()); //remove talking speech bubble
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
			if(MyInput.isDown(MyInput.UP)) character.climb();
			if(MyInput.isDown(MyInput.DOWN)) character.descend();
			if(MyInput.isDown(MyInput.LEFT)) character.left();
			if(MyInput.isDown(MyInput.RIGHT)) character.right();
			if(MyInput.isPressed(MyInput.PAUSE)) pause();
			if(MyInput.isPressed(MyInput.JUMP) && !speaking && buttonTime >= DELAY) {
				Gdx.audio.newSound(new FileHandle("res/sounds/ok1.wav")).play(Game.volume);

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
			if(MyInput.isPressed(MyInput.PAUSE)) pause();
			if(MyInput.isPressed(MyInput.JUMP) && !speaking && buttonTime >= DELAY) {
				Gdx.audio.newSound(new FileHandle("res/sounds/ok1.wav")).play(Game.volume);
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
			if(MyInput.isPressed(MyInput.PAUSE)) pause();
			if (buttonTime >= buttonDelay){
				if(MyInput.isDown(MyInput.LEFT)){
					buttonTime = 0;
					choiceIndex++;
					if (choiceIndex >= choices.length)
						choiceIndex = 0;

					Gdx.audio.newSound(new FileHandle("res/sounds/text1.wav")).play(Game.volume);
				} else if (MyInput.isDown(MyInput.RIGHT)){
					buttonTime = 0;
					choiceIndex--;
					if (choiceIndex < 0)
						choiceIndex = choices.length - 1;

					Gdx.audio.newSound(new FileHandle("res/sounds/text1.wav")).play(Game.volume);
				} else if(MyInput.isPressed(MyInput.ENTER)){
					for (SpeechBubble b : choices)
						bodies.add(b.getBody()); 

					Gdx.audio.newSound(new FileHandle("res/sounds/ok1.wav")).play(Game.volume);

					currentScript.paused = choosing = false;
					currentScript.getChoiceIndex(choiceIndex);
				}
			}
			
			break;
		}
	}
	
	private void use(){
		if(player.getPartner().getName() != null){
			if(getMob(player.getPartner().getName())!=null){
				stateType = LISTEN;

				if (player.stopPartnerDisabled) {
				    displayText.add("No, I'm coming with you.");
				    hud.changeFace(player.getPartner());
				    player.faceObject(player.getPartner());
				    speak();
				} else if(player.getPartner().getState() == NPC.FOLLOWING) {
					displayText.add("I'll just stay here.");
					hud.changeFace(player.getPartner());
					speak();
					player.getPartner().stay();
				}
				else {
					if(player.getRelationship()<-2){ 
						displayText.add("No way. I'm staying here.");
					    player.faceObject(player.getPartner());
					} else if(player.getPartner().getGender().equals("female")) displayText.add("Coming!");
					else displayText.add("On my way.");

					hud.changeFace(player.getPartner());
					speak();
					player.getPartner().follow();
				}
			}else{
				String partner;
				if (player.getPartner().getGender().equals(Mob.MALE)) partner = "boyfriend";
				else partner = "girlfriend";
				displayText.add("Your "+partner+" isn't here at the moment.");
				hud.changeFace(narrator);
				speak();
				stateType = LISTEN;
			}
		}
	}
	
	public void positionPlayer(Entity obj){
		float min = 20;
		float dx = (player.getPosition().x - obj.getPosition().x) * Vars.PPM;
		float gx = min * dx / Math.abs(dx) - dx ;
		
			System.out.println("Hello");
		if (Math.abs(dx) < min -2){
			player.positioning = true;
			player.setGoal(gx);
		} else {
			player.faceObject(player.getInteractable());
			player.controlled = false;
		}
	}
	
	public void displayChoice(int[] types){
		final float h = 30 / PPM;
		float x, y, theta;
		int c = types.length;
		choices = new SpeechBubble[c];
		choiceIndex = 0;
		
		for (int i = 0; i < c; i++){
			theta = (float) (i * 2 * Math.PI / c);
			x = (float) (h * Math.cos(theta) + player.getPosition().x) * PPM;
			y = (float) (h * Math.sin(theta) + player.getPosition().y) * PPM + 4;
			choices[i] = new SpeechBubble(player, x, y, types[i]);
		}
	}
	
	public void dispose() { 
		if(song != null)
			song.stop();
		}
	
	public void speak(){
		if (!speaking) {
			speaking = true;
			//stateType = LISTEN;

			String line = displayText.poll();
			
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
					.play(Game.volume * .9f, (float) Math.random()*.15f + .9f, 1);
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
		scene.create(this);
		initEntities();
//		createPlayer(warp.getLink());
		createPlayer(scene.getSpawnpoint());
		if(scene.newSong)
			setSong(scene.DEFAULT_SONG);
		warped = true;
	}
	
	public void setDispText(ArrayDeque<String> dispText) { displayText = dispText;}
	public void setStateType(int type) {stateType = type; }
	public int getStateType(){ return stateType; }
	public SpeechBubble[] getChoices(){ return choices; }

	public void addSound(PositionalAudio s){ sounds.add(s); }
	public void addObject(Entity d){ objects.add(d); }
	public void addBodyToRemove(Body b){ bodies.add(b); }
	
	public void createPlayer(Vector2 location){
		player.setPlayState(this);
		player.setPosition(location);
		player.create();
		character = player;
		cam.setCharacter(character);
		b2dCam.setCharacter(character);
		objects.add(player);
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
		
//		createPlayer(scene.getSpawnLocation());
		createPlayer(new Vector2(Game.width/2, 151));
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
			debugText+="/l"+e.getID();
		}
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
}

