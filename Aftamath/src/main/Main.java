package main;

import static handlers.Vars.PPM;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import box2dLight.RayHandler;
import entities.CamBot;
import entities.Entity;
import entities.Ground;
import entities.HUD;
import entities.Mob;
import entities.Mob.AIState;
import entities.Mob.Anim;
import entities.Path;
import entities.SpeechBubble;
import entities.SpeechBubble.PositionType;
import entities.TextBox;
import entities.Warp;
import handlers.Camera;
import handlers.Evaluator;
import handlers.EventTrigger;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.JsonSerializer;
import handlers.MyContactListener;
import handlers.MyInput;
import handlers.MyInput.Input;
import handlers.MyInputProcessor;
import handlers.Pair;
import handlers.PositionalAudio;
import handlers.Vars;
import scenes.Scene;
import scenes.Script;
import scenes.Script.ScriptType;
import scenes.Song;

public class Main extends GameState {

	public Mob character;
	public Player player;
	public Warp warp;
	public Mob narrator;
	public HUD hud;
	public History history;
	public Evaluator evaluator;
	public Script currentScript;
	public InputState stateType, prevStateType;
	public int currentEmotion, dayState;
	public boolean paused, analyzing, choosing, waiting;
	public boolean warping, warped; //for changing between scenes
	public boolean speaking; //are letters currently being drawn individually
	public float dayTime, weatherTime, waitTime, totalWait, clockTime, playTime, keyIndexTime;

	private int sx, sy;
	private float speakTime, speakDelay = .025f;
	private ArrayDeque<Pair<String, Integer>> displayText; //all text pages to display
	private String[] speakText; //current text page displaying
	private String gameFile; //used to load a game
	private World world;
	private Scene scene, nextScene;
	private Color drawOverlay;
	private Array<Body> bodiesToRemove;
	private ArrayList<Entity> objects/*, UIobjects, UItoRemove*/;
	private ArrayList<Path> paths;
	private ArrayList<PositionalAudio> sounds;
	private Hashtable<String, Warp> warps;
	private Box2DDebugRenderer b2dr;
	private InputState beforePause;
	private RayHandler rayHandler;
	private MyContactListener cl = new MyContactListener(this);
	private SpeechBubble[] choices;

	public static enum InputState{
		MOVE, //can move and interact
		MOVELISTEN, //listening to text and can move
		LISTEN, //listening to text only
		CHOICE, //choosing an option
		//LOCKED, //lock all input; it's up to the script to unlock
		PAUSED, //in pause menu
		KEYBOARD, //input text
		GENDERCHOICE, //temporary; used to allow the player to chose their appearance
	}

	public static enum WeatherState{
		CLEAR, CLOUDY, RAINING, STORMY,
	}

	//day/night cycle variables
	private static final int DAY = 0;
	private static final int NOON = 1;
	private static final int NIGHT =2;
	private static final int TRANSITION_INTERVAL = /*60*/ 5;
	public static final float DAY_TIME = 10f*60f; //24min
	public static final float NOON_TIME = DAY_TIME/3f;
	public static final float NIGHT_TIME = 2*DAY_TIME/3f;

	//debug
	//use these for determining hard-coded values
	//please, don't keep them as hard-coded
	//public int debugX;
	public float debugY = Game.height/4, debugX=Game.width/4;

	public boolean dbRender 	= false, 
			rayHandling = false, 
			render 		= true, 
			dbtrender 	= false,
			debugging   = true,
			random;
	//	private float ambient = .5f;
	//	private int colorIndex;
	//	private PointLight tstLight;

	public Main(GameStateManager gsm) {
		this(gsm, "newgame");
	}

	public Main(GameStateManager gsm, String gameFile) {
		super(gsm);
		this.gameFile = gameFile;
		JsonSerializer.gMain = this;
	}

	public void create(){
		bodiesToRemove = new Array<>();
		history = new History();
		currentScript = null;
		evaluator = new Evaluator(this);

		stateType = prevStateType = InputState.MOVE;
		currentEmotion = Mob.NORMAL;
		dayState=DAY;
		paused=analyzing=choosing=waiting=warping=warped=speaking=false;
		waitTime=totalWait=0;

//		sx=sy=0;
//		speakTime=0;
//		speakDelay = .025f;
		speakText = null;
//		dayTime = 3*NOON_TIME/2f;

		objects = new ArrayList<>();
		paths = new ArrayList<>();
		sounds = new ArrayList<>();
		warps = new Hashtable<>();
		world = new World(new Vector2 (0, Vars.GRAVITY), true);
		world.setContactListener(cl);
		b2dr = new Box2DDebugRenderer();
		b2dr.setDrawVelocities(true);
		rayHandler = new RayHandler(world);
//		rayHandler.setAmbientLight(0);

		System.out.println("Don't Panic! The game is just linking levels together!");
		drawString(sb, "Loading...", Game.width/4, Game.height/4);

		cam.reset();
		b2dCam.reset();
		catalogueWarps();
		load();

		speakTime = 0;
		displayText = new ArrayDeque<Pair<String, Integer>>();

		hud = new HUD(this, hudCam);
		handleDayCycle();
		handleWeather();
		hud.showLocation();
	}

	public void update(float dt) {
		super.update(dt);
		buttonTime += dt;
		hud.update(dt);

		if(stateType == InputState.KEYBOARD){
			keyIndexTime+=dt;
			if(keyIndexTime>=1)
				keyIndexTime = 0;
		}

		if (!paused){
			speakTime += dt;
			dayTime+=dt;
			clockTime = (dayTime+DAY_TIME/6f)%DAY_TIME;
			playTime+=dt;
			debugText = "";
			player.updateMoney();

//			handleDayCycle();
			if(scene.outside) {
				weatherTime+=dt;
				handleWeather();
			}

			if(currentScript != null &&analyzing && !waiting) 
				currentScript.update();

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

			if(currentScript != null){
				float dx = character.getPosition().x - currentScript.getOwner().getPosition().x;

				//if player gets too far from whatever they're talking to
				if(Math.abs(dx) > 50/PPM && currentScript.limitDistance){
					currentScript = null;
					character.setInteractable(null);
					speaking = false;
					hud.hide();
					setStateType(InputState.MOVE);
				}
			}

			for (Entity e : objects){
				if (!(e instanceof Ground)) {
					if(!e.init && e.getBody()==null)
						e.create();
					e.update(dt);

					//kill object
					if (e.getBody() == null) {e.create();}
					if (e.getPixelPosition().x > scene.width + 50 || e.getPixelPosition().x < -50 || 
							e.getPixelPosition().y > scene.height + 100 || e.getPixelPosition().y < -50) {
						if (e.equals(character)){
							//Game Over
							//System.out.println("die"); 
						} else {
							bodiesToRemove.add(e.getBody());
						}
					}
				}
			}

			for (Body b : bodiesToRemove){
				if (b != null) 
					if (b.getUserData() != null){
						Object e = b.getUserData();
						if(cam.getFocus().equals(e))
							cam.removeFocus();
						objects.remove(e);
						if(e.equals(character))
							addObject((Entity) e);
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
				}
			}

			if(changingSong){
				if(!music.fading){
					music.dispose();
					setSong(nextSong);
					nextSong=null;
					changingSong = false;
				}
			}

			if(tempSong)
				if(!music.isPlaying())
					removeTempSong();

// moving point light
//			tstLight.setPosition(character.getPosition());
//			rayHandler.setLightMapRendering(false);
		} else {
			if (!quitting)
				handleInput();
		}

		cam.locate(dt);
		if(quitting)
			quit();
	}

	public void handleWeather(){

	}

	// modify colors and sounds according to day time
	public void handleDayCycle(){
		if(dayTime>NOON_TIME){
			dayState = NOON;
		}if(dayTime>NIGHT_TIME){
			dayState = NIGHT;
		}if(dayTime>DAY_TIME){
			dayState = DAY;
			dayTime=0;

			if (scene.outside);
			//if (street lights are on)
			//turn off lights 
		} if(dayTime>NIGHT_TIME-TRANSITION_INTERVAL && scene.outside){
			//if(street lights are off)
			//turn on lights
		}

		float t=dayTime,i;
		if(scene.outside){
			switch(dayState){
			case DAY:
				drawOverlay = Vars.DAYLIGHT;
				if(sb.isDrawingOverlay()) sb.setOverlayDraw(false);
				break;
			case NOON:
				i = DAY_TIME/12f;
				drawOverlay = Vars.DAYLIGHT;

				if(t>NIGHT_TIME-i){
					if(t>NIGHT_TIME-i)
						drawOverlay = Vars.blendColors(t, NIGHT_TIME-i, NIGHT_TIME-i+i/4f, 
								Vars.DAYLIGHT, Vars.SUNSET_GOLD);
					if(t>NIGHT_TIME-i+i/4f)
						drawOverlay = Vars.blendColors(t, NIGHT_TIME-i+i/4f, NIGHT_TIME-i+2*i/4f, 
								Vars.SUNSET_GOLD, Vars.SUNSET_ORANGE);
					if(t>NIGHT_TIME-i+2*i/4f)
						drawOverlay = Vars.blendColors(t, NIGHT_TIME-i+2*i/4f, NIGHT_TIME-i+3*i/4f, 
								Vars.SUNSET_ORANGE, Vars.SUNSET_MAGENTA);
					if(t>NIGHT_TIME-i+3*i/4f)
						drawOverlay = Vars.blendColors(t, NIGHT_TIME-i+3*i/4f, NIGHT_TIME, 
								Vars.SUNSET_MAGENTA, Vars.NIGHT);
					sb.setOverlay(drawOverlay);
					if(!sb.isDrawingOverlay())
						sb.setOverlayDraw(true);
				}

				break;
			case NIGHT:
				i = TRANSITION_INTERVAL;

				if(t>DAY_TIME-i){
					if(t>DAY_TIME-i)
						drawOverlay = Vars.blendColors(t, DAY_TIME-i, DAY_TIME-i/2f, 
								Vars.NIGHT, Vars.SUNRISE);
					if(t>DAY_TIME-i/2f)
						drawOverlay = Vars.blendColors(t, DAY_TIME-i/2f, DAY_TIME, 
								Vars.SUNRISE, Vars.DAYLIGHT);
				} else
					drawOverlay = Vars.NIGHT;

				sb.setOverlay(drawOverlay);
				if(!sb.isDrawingOverlay())
					sb.setOverlayDraw(true);
				break;
			}

//			rayHandler.setAmbientLight(ambient);
		}

		if(!random)
			if(!tempSong)
				if(!scene.DEFAULT_SONG[dayState].equals(music))
					changeSong(scene.DEFAULT_SONG[dayState]);
	}

	public void render() {
//		Gdx.gl20.glClearColor(skyColor.r, skyColor.b, skyColor.g, skyColor.a);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

		sb.setProjectionMatrix(cam.combined);
		rayHandler.setCombinedMatrix(cam.combined);
		scene.renderBG(sb);

		if(render){
			scene.renderEnvironment(cam);

			sb.begin();

			int i = -1;
			Entity e;
			for (int x = 0; x<objects.size(); x++){
				e = objects.get(x);
				if(e instanceof SpeechBubble || e instanceof TextBox){
					i = x;
					break;
				}
				e.render(sb);
			}
			sb.end();

			scene.renderFG(sb);

			if(i>=0){
				sb.begin();
				for (int x = i; x<objects.size(); x++)
					objects.get(x).render(sb);
				sb.end();
			}
		}

		if (speaking) speak();
		if (rayHandling) rayHandler.updateAndRender();

		boolean o = sb.isDrawingOverlay();
		if(o) sb.setOverlayDraw(false);

		b2dCam.setPosition(character.getPosition().x, character.getPosition().y + 15 / PPM);
		b2dCam.update();
		if (dbRender) b2dr.render(world, b2dCam.combined);
		hud.render(sb, currentEmotion);

		if(stateType == InputState.KEYBOARD){
			sb.begin();
			hud.drawInputBG(sb);
			drawString(sb, Game.getInput(), Game.width/4 - Game.getInput().length()*font[0].getRegionWidth()/2,
					195);
			if(keyIndexTime<.5f)
				drawString(sb, "_", Game.width/4 - Game.getInput().length()*font[0].getRegionWidth()/2 +
						Game.inputIndex*font[0].getRegionWidth(), 195);
			sb.end();
		}

		if(dbtrender) updateDebugText();

		if(o) sb.setOverlayDraw(true);
	}

	//everything that needs to be displayed constantly for debug tracking is
	//right here
	public void updateDebugText() {
//		debugText= "next: "+nextSong+"     "+music;
		debugText= Vars.formatDayTime(clockTime, false)+"    Play Time: "+Vars.formatTime(playTime);
		debugText += "/lLevel: " + scene.title;
		debugText += "/lSong: " + music;
		debugText+= "/lState: "+(stateType);
//		debugText+="/lATTACKABLES: "+character.getAttackables();

		debugText +="/l/l"+ character.getName() + " x: " + (int) (character.getPosition().x*PPM) + 
				"    y: " + ((int) (character.getPosition().y*PPM) - character.height);
		debugText+="/l"+character.o;
		

		sb.begin();
		drawString(sb, debugText, 2, Game.height/2 - font[0].getRegionHeight() - 2);
		sb.end();
	}

	public void handleInput() {
		if(MyInput.isPressed(Input.DEBUG_UP)) {
//			light += .1f; rayHandler.setAmbientLight(light);
			player.addFunds(100d);
		}

		if(MyInput.isPressed(Input.DEBUG_DOWN)){
//			light -= .1f; rayHandler.setAmbientLight(light);
			player.addFunds(-100d);
		}

		if(MyInput.isDown(Input.DEBUG_LEFT)) {
//			if(colorIndex>0) 
//				colorIndex--; 
//			rayHandler.setAmbientLight(Vars.COLORS.get(colorIndex)); 
//			rayHandler.setAmbientLight(ambient);
			debugX-=1;
		}

		if(MyInput.isDown(Input.DEBUG_RIGHT)) {
//			if(colorIndex<Vars.COLORS.size -1) 
//				colorIndex++; 
//			rayHandler.setAmbientLight(Vars.COLORS.get(colorIndex)); 
//			rayHandler.setAmbientLight(ambient);
//			debugX+=.2f;
//			System.out.println(debugX);
			debugX+=1;
		}

		if (MyInput.isDown(Input.DEBUG_LEFT2)) {
			debugY+=1;
		}
		if (MyInput.isDown(Input.DEBUG_RIGHT2)) {
			debugY-=1;
		}

		if(MyInput.isPressed(Input.DEBUG_CENTER)) {
//			random=true;
//			int current = (Game.SONG_LIST.indexOf(music.title, false) +1)%Game.SONG_LIST.size;
//			changeSong(new Song(Game.SONG_LIST.get(current)));
			//			dayTime+=1.5f;
			cam.shake();
		}

		if(MyInput.isDown(Input.ZOOM_OUT /*|| Gdx.input.getInputProcessor().scrolled(-1)*/)) {
			cam.zoom+=.01;
			b2dCam.zoom+=.01;
		}
		if(MyInput.isDown(Input.ZOOM_IN /*|| Gdx.input.getInputProcessor().scrolled(1)*/)) {
			cam.zoom-=.01;
			b2dCam.zoom-=.01;
		}
		if(MyInput.isPressed(Input.LIGHTS)) rayHandling = !rayHandling ;
		if(MyInput.isPressed(Input.COLLISION)) dbRender = !dbRender ;
		if(MyInput.isPressed(Input.DEBUG)) character.respawn();
		if(MyInput.isPressed(Input.DEBUG2)) {
			//render=!render;
		}
		if(MyInput.isPressed(Input.DEBUG_TEXT)) dbtrender=!dbtrender;

		if (paused){
			if (stateType == InputState.PAUSED) {
				if(MyInput.isPressed(Input.PAUSE)) unpause();
				if(MyInput.isPressed(Input.JUMP)|| MyInput.isPressed(Input.ENTER) && !gsm.isFading()){
					try {
						Method m = Main.class.getMethod(Vars.formatMethodName(menuOptions[menuIndex[0]][menuIndex[1]]));
						m.invoke(this);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if(buttonTime >= DELAY){
					if(MyInput.isDown(Input.DOWN)){
						if(menuIndex[1] < menuMaxY){
							menuIndex[1]++;
							//play menu sound
						} else {
							menuIndex[1] = 0;
						}
						buttonTime = 0;
					}

					if(MyInput.isDown(Input.UP)){
						if(menuIndex[1] > 0){
							menuIndex[1]--;
							//play menu sound
						} else {
							menuIndex[1] = menuMaxY;
						}
						buttonTime = 0;
					}

					if(MyInput.isDown(Input.RIGHT)){
						if(menuIndex[0] < menuMaxX){
							menuIndex[0]++;
							//play menu sound
						} else {
							//play menu invalid sound
						}
						buttonTime = 0;
					}

					if(MyInput.isDown(Input.LEFT)){
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
		} else{ 
			switch (stateType){
			case KEYBOARD:
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				if(MyInput.isPressed(Input.CAPS)) MyInput.isCaps = !MyInput.isCaps;
				if(buttonTime>=.15f){
					if(MyInput.isPressed(Input.ENTER)){
						buttonTime=0;
						//isNameAppropiate(Game.input);
						if(!Game.getInput().isEmpty()) currentScript.applyInput();
					}

					if(MyInput.isDown(Input.DOWN)){
						buttonTime=0;
						Game.removeInputChar();
					}

					if(MyInput.isDown(Input.JUMP)){
						buttonTime=0;
						Game.addInputChar(" ");
					}

					if(MyInput.isDown(Input.LEFT)){
						buttonTime=0;
						if(Game.inputIndex>0)
							Game.inputIndex--;
					}

					if(MyInput.isDown(Input.RIGHT)){
						buttonTime=0;
						if(Game.inputIndex<Game.getInput().length()-2)
							Game.inputIndex++;
					}
				}
				break;
			case GENDERCHOICE:
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				int index = (int) currentScript.getVariable("playertype");

				if(MyInput.isPressed(Input.LEFT)){
					if(index>1)index--;
					else index = 4;

					character.ID = character.getGender() + "player" + index;
					character.loadSprite();
					currentScript.setVariable("playertype", index);
				}

				if(MyInput.isPressed(Input.RIGHT)){
					if(index<4) index++;
					else index =1;

					character.ID = character.getGender() + "player" + index;
					character.loadSprite();
					currentScript.setVariable("playertype", index);
				}

				if(MyInput.isPressed(Input.ENTER) || MyInput.isPressed(Input.JUMP)){
					currentScript.paused = false;
					setStateType(prevStateType);
				}

				break;
				//			case LOCKED:
				//				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				//				break;
			case MOVE:
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				if(/*cam.focusing||*/warping||quitting||character.dead||waiting||character.frozen) return;
				if(MyInput.isPressed(Input.JUMP)) character.jump();
				if(MyInput.isDown(Input.UP)) {
					if(character.canWarp && character.isOnGround() && !character.snoozing && 
							!character.getWarp().instant) {
					}
					else if(character.canClimb) character.climb();
					else {
						Vector2 f = new Vector2(character.getPixelPosition().x, 
								character.getPixelPosition().y + 4.5f*Vars.TILE_SIZE*cam.zoom + cam.offsetY);
						cam.setFocus(f);
						b2dCam.setFocus(f);
						character.lookUp();
					}
				}

				if(MyInput.isPressed(Input.UP)) {
					if(character.canWarp && character.isOnGround() && !character.snoozing && 
							!character.getWarp().instant) {
						initWarp(character.getWarp());
						character.killVelocity();
					}
				}


				if(MyInput.isDown(Input.DOWN)) 
					if(character.canClimb)
						character.descend();
					else character.duck();
				if(MyInput.isPressed(Input.USE)) partnerFollow();
				if(MyInput.isPressed(Input.INTERACT)) {
					triggerScript(character.interact());
				}

				if(MyInput.isDown(Input.LEFT)) character.left();
				if(MyInput.isDown(Input.RIGHT)) character.right();
				if(MyInput.isDown(Input.RUN)) {character.run();}
				if(MyInput.isPressed(Input.ATTACK)) {
					character.attack();
				}

				if(MyInput.isUp(Input.UP) && (character.getAnimationAction().equals(Anim.LOOKING_UP)
						|| character.getAnimationAction().equals(Anim.LOOK_UP))){
					character.setAnimation(true, Anim.LOOK_UP);
					cam.removeFocus();
					b2dCam.removeFocus();
				}

				if(MyInput.isUp(Input.DOWN) && (character.getAnimationAction().equals(Anim.DUCKING)
						|| character.getAnimationAction().equals(Anim.DUCK))){
					character.unDuck();
				}

				break;
			case MOVELISTEN:
				if(MyInput.isPressed(Input.PAUSE)) pause();
				if(cam.moving/*||cam.focusing*/||warping||quitting||character.dead/*||waiting*/||character.frozen) return;
				if(MyInput.isDown(Input.UP)) {
					if(character.canWarp && character.isOnGround() && !character.snoozing && 
							!character.getWarp().instant) {
						initWarp(character.getWarp());
						character.killVelocity();
					}
					else if(character.canClimb) character.climb();
					else character.lookUp();
				}
				if(MyInput.isDown(Input.DOWN)) 
					if(character.canClimb)
						character.descend();
					else character.duck();
				if(MyInput.isPressed(Input.USE)) partnerFollow();
				if(MyInput.isPressed(Input.INTERACT)) {
					triggerScript(character.interact());
				}
				if(MyInput.isDown(Input.LEFT)) character.left();
				if(MyInput.isDown(Input.RIGHT)) character.right();
				if(MyInput.isPressed(Input.JUMP) && !speaking && buttonTime >= DELAY) {
					playSound(character.getPosition(), "ok1");

					if(displayText.isEmpty()) {
						if (currentScript != null)
							currentScript.paused = currentScript.forcedPause = false;
					} else {
						speakDelay = .2f;
						speakTime = 0;
						speak();
					}
				} else if (MyInput.isPressed(Input.JUMP) && speaking){
					buttonTime = 0;
					speaking = hud.fillSpeech(speakText);
				}

				if(MyInput.isDown(Input.UP)) {
					if(character.canWarp && character.isOnGround() && !character.snoozing && 
							!character.getWarp().instant) {
					}
					else if(character.canClimb) character.climb();
					else {
						Vector2 f = new Vector2(character.getPixelPosition().x, 
								character.getPixelPosition().y + 4.5f*Vars.TILE_SIZE*cam.zoom + cam.offsetY);
						cam.setFocus(f);
						b2dCam.setFocus(f);
						character.lookUp();
					}
				}

				if(MyInput.isPressed(Input.UP)) {
					if(character.canWarp && character.isOnGround() && !character.snoozing && 
							!character.getWarp().instant) {
						initWarp(character.getWarp());
						character.killVelocity();
					}
				}

				if(MyInput.isUp(Input.UP) && (character.getAnimationAction().equals(Anim.LOOKING_UP)
						|| character.getAnimationAction().equals(Anim.LOOK_UP))){
					character.setAnimation(true, Anim.LOOK_UP);
					cam.removeFocus();
					b2dCam.removeFocus();
				}
				if(MyInput.isUp(Input.DOWN) && (character.getAnimationAction().equals(Anim.DUCKING)
						|| character.getAnimationAction().equals(Anim.DUCK))){
					character.setAnimation(true, Anim.DUCK);
				}
				break;
			case LISTEN:
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();

				if(currentScript!=null)
					if(/*currentScript.dialog &&*/ MyInput.isPressed(Input.JUMP)){
						if(!speaking && buttonTime >= DELAY){
							playSound(character.getPosition(), "ok1");
							if(currentScript.getActiveObject() instanceof TextBox){
								TextBox t = (TextBox) currentScript.getActiveObject();
								t.kill();

								currentScript.paused = currentScript.forcedPause = false;
								currentScript.setActiveObj(new Entity());
								setStateType(prevStateType);
							} else {

								// This line of code is essential; it gives the game just enough time
								// so that the script doesn't skip more than once to the end
								int x12 = 1; if (x12==1) x12=2;

								if(displayText.isEmpty()) {
									if (currentScript != null) {
										currentScript.paused = currentScript.forcedPause = false;
									} else {
										hud.hide();
										setStateType(InputState.MOVE);
									}
								} else {
									speakDelay = .2f;
									speakTime = 0;
									speak();
								}
							}
						} else if (speaking){
							buttonTime = 0;
							speaking = hud.fillSpeech(speakText);
						}	
					}
				break;
			case CHOICE:
				int prevIndex = choiceIndex;
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				if (buttonTime >= buttonDelay){
					if(MyInput.isDown(Input.LEFT)){
						buttonTime = 0;
						choiceIndex++;
						if (choiceIndex >= choices.length)
							choiceIndex = 0;

						playSound("text1");
						choices[choiceIndex].expand();
						choices[prevIndex].collapse();
					} else if (MyInput.isDown(Input.RIGHT)){
						buttonTime = 0;
						choiceIndex--;
						if (choiceIndex < 0)
							choiceIndex = choices.length - 1;

						playSound("text1");
						choices[choiceIndex].expand();
						choices[prevIndex].collapse();
					} else if(MyInput.isPressed(Input.JUMP)){
						//						setStateType(prevStateType);
						playSound("ok1");
						//						wait(.5f);

						currentScript.paused = choosing = false;
						currentScript.getChoiceIndex(choices[choiceIndex].getMessage());

						//delete speechBubbles from world
						for (SpeechBubble b : choices)
							bodiesToRemove.add(b.getBody()); 
					}
				}

				break;
			default:
				break;
			}
		}
	}

	private void partnerFollow(){
		if(player.getPartner()!=null){
			if(player.getPartner().getName() != null){
				if(getMob(player.getPartner().getName())!=null){
					setStateType(InputState.LISTEN);

					if (player.stopPartnerDisabled) {
						triggerScript("toggleFollowDisabled");
					} else if(player.getPartner().getState() == AIState.FOLLOWING) {
						triggerScript("stopFollower");
						player.getPartner().stay();
					} else {
						triggerScript("suggestFollow");
						player.getPartner().follow(character);
					}
				}else{
					triggerScript("noPartnerPresent");
				}
			}
		} else {
			triggerScript("noPartner");
		}
	}

	public void changeSong(Song newSong){
		if(changingSong)return;
		if(music!=null){
			nextSong = newSong;
			music.fadeOut();
			changingSong = true;
		} else
			music = newSong;
	}

	public void positionPlayer(Entity obj){
		float min = 18;
		float dx = (character.getPosition().x - obj.getPosition().x) * Vars.PPM;
		float gx = min * dx / Math.abs(dx) - dx ;

		if (Math.abs(dx) < min - 2){
			//			setStateType(InputState.LOCKED);
			character.positioning = true;
			character.setPositioningFocus(character.getInteractable());
			character.setGoal(gx);
			currentScript.setActiveObj(character);
		} else {
			character.faceObject(character.getInteractable());
			character.controlled = false;
		}
	}

	//show choices in a circle around the player
	public void displayChoice(Array<Script.Option> options){
		final float h = 30 / PPM;
		float x, y, theta;
		int c = options.size;
		choices = new SpeechBubble[c];
		choiceIndex = 0;

		Script.Option o;
		PositionType positioning;
		for (int i = 0; i < c; i++){
			o = options.get(i);
			theta = (float) (i * 2 * Math.PI / c);
			x = (float) (h * Math.cos(theta) + character.getPosition().x) * PPM;
			y = (float) (h * Math.sin(theta) + character.getPosition().y) * PPM + 15;

			if((theta>Math.PI/2-Math.PI/20 && theta<Math.PI/2+Math.PI/20) ||
					(theta>3*Math.PI/2-Math.PI/20 && theta<3*Math.PI/2+Math.PI/20)) 
				positioning = PositionType.CENTERED;
			else if((theta>=0 && theta<Math.PI/2-Math.PI/20) ||
					(theta<=2*Math.PI && theta>3*Math.PI/2+Math.PI/20))
				positioning = PositionType.LEFT_MARGIN;
			else positioning = PositionType.RIGHT_MARGIN;
			choices[i] = new SpeechBubble(character, x, y, o.type, o.message, positioning);
		}
		choices[0].expand();
	}

	public void dispose() { 
		if(music!=null)
			music.dispose();
	}

	public void speak(){
		if (!speaking && !displayText.isEmpty()) {
			speaking = true;

			Pair<String, Integer> current = displayText.poll();
			String line = current.getKey();
			currentEmotion = current.getValue();

			speakText = line.split("/l");
			hud.createSpeech(speakText);

			sx = 0;
			sy = 0;

			if(!hud.raised) 
				hud.show();

			music.fadeOut(Game.musicVolume*2f/3f);
		}

		//show text per character
		if (hud.moving == 0){
			if (speakTime >= speakDelay) {
				speakTime = 0;
				if(hud.raised) speakDelay = .020f;
				else speakDelay = 2f;

				if(sy>=speakText.length){
					speaking = false;
					speakText = null;

					music.fadeIn(Game.musicVolume);

					if (currentScript != null){
						if(currentScript.peek() != null)
							if(currentScript.peek().toLowerCase().equals("choice")
									&& displayText.isEmpty())
								currentScript.readNext();
					}
				} else if(speakText[sy].length()>0){
					char c = speakText[sy].charAt(sx);
					if (c == ".".charAt(0) || c == ",".charAt(0)|| c == "!".charAt(0)|| c == "?".charAt(0)) 
						speakDelay = .25f;
					if(c != "~".charAt(0)){
						hud.addChar(sy, c);
						Gdx.audio.newSound(new FileHandle("assets/sounds/text1.wav"))
						.play(Game.soundVolume * .9f, (float) Math.random()*.15f + .9f, 1);
					} else speakDelay = .75f;

					sx++;
					if (sx == speakText[sy].length()) {sx = 0; sy++; }
					if (sy == speakText.length) {
						speaking = false;
						speakText = null;

						music.fadeIn(Game.musicVolume);

						if (currentScript != null){
							if(currentScript.peek() != null)
								if(currentScript.peek().toLowerCase().equals("choice")
										&& displayText.isEmpty())
									currentScript.readNext();
						}
					}
				}else
					sy++;
			}
		}
	}

	public void pause(){
		if (music!=null) 
			if (music.isPlaying()) 
				music.fadeOut(false);

		beforePause = stateType;
		setStateType(InputState.PAUSED);

		menuOptions = new String[][] {{"Resume", "Journal","Save Game", "Load Game", 
			"Options", "Quit to Menu", "Quit"}}; 

			paused = true;
			menuMaxY = menuOptions[0].length - 1;
			menuMaxX = 0;
			menuIndex[0] = 0;
			menuIndex[1] = 0;
	}

	public void journal(){
		//TODO change menu to display stats
	}

	public void saveGame() {
		//TODO save game menu
		gameFile = "savegame";
		
		//select save file, create new one, or overwrite current save
		System.out.println("saving game to " + gameFile + ".txt");
		JsonSerializer.saveGameState(gameFile);
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
		setStateType(beforePause);
		paused = false;

		if (music!=null) 
			music.fadeIn();
	}

	public void initWarp(Warp warp){
		warping = true;
		warped = false;
		this.warp = warp;
		nextScene = warp.getNextScene();
		//		scene.saveLevel();

		sb.fade();
		if(nextScene.newSong)
			changeSong(nextScene.DEFAULT_SONG[dayState]);
	}

	public void warp(){
		if(warped) return;

		random = false;
		destroyBodies();
		scene = nextScene;
		scene.create();
		Vector2 w = warp.getLink().getWarpLoc();
		w.y+=character.rh;
		createPlayer(w);
		initEntities();

		if(warp.getLink().warpID==1 && !character.isFacingLeft()) character.setDirection(true);
		if(warp.getLink().warpID==0 && !character.isFacingLeft()) character.setDirection(false);
		cam.setBounds(Vars.TILE_SIZE*4, (scene.width-Vars.TILE_SIZE*4), 0, scene.height);
		b2dCam.setBounds((Vars.TILE_SIZE*4)/PPM, (scene.width-Vars.TILE_SIZE*4)/PPM, 0, scene.height/PPM);

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
	public void setStateType(InputState type) {
		if(type == InputState.KEYBOARD)
			((MyInputProcessor) Gdx.input.getInputProcessor()).keyboardMode();
		else
			((MyInputProcessor) Gdx.input.getInputProcessor()).gameMode();
		if(stateType!=InputState.PAUSED)
			prevStateType = stateType;
		stateType = type; 
	}

	public InputState getStateType(){ return stateType; }
	public SpeechBubble[] getChoices(){ return choices; }

	public void addSound(PositionalAudio s){ sounds.add(s); }
	public void addObject(Entity e){ 
		if(!exists(e)){
			objects.add(e); 
			e.setGameState(this);
			sortObjects();
		}
	}

	public Entity findObject(String objectName){
		Entity object = null;

		if(Vars.isNumeric(objectName)){
			for(Entity d : getObjects())
				if (d.getSceneID() == Integer.parseInt(objectName)) return d;
		} else switch(objectName) {
		case "player":
			object = character;
			break;
		case "partner":
			if(player.getPartner().getName() != null)
				object = player.getPartner();
			break;
		case "narrator":
			object = narrator;
			break;
		default:
			for(Entity d : getObjects()){
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

	public void addBodyToRemove(Body b){ bodiesToRemove.add(b); }
	public ArrayList<Path> getPaths() {return paths; }
	public Path getPath(String pathName){
		for(Path p : paths){
			if(p.ID.toLowerCase().equals(pathName.toLowerCase()))
				return p;
		}
		return null;
	}

	public void load()	{
		player = new Player(this);
		
		if(!gameFile.equals("newgame")){
			JsonSerializer.loadGameState(gameFile);

			scene.setRayHandler(rayHandler);
			setSong(scene.DEFAULT_SONG[dayState]);
			music.pause();
			scene.create();

			narrator = (Mob)Entity.idToEntity.get(Vars.NARRATOR_SCENE_ID);
			character = (Mob)Entity.idToEntity.get(Vars.PLAYER_SCENE_ID);
			createPlayer(character.getPixelPosition().add(new Vector2(0, -character.rh)));	//TODO normalize dealing with height offset
		} else {
			//TODO normalize narrator reference (should exist regardless of what level the player's on)
			scene= new Scene(world,this,"Residential District N");
			setSong(scene.DEFAULT_SONG[dayState]);
			scene.setRayHandler(rayHandler);
			scene.create();

			narrator = new Mob("Narrator", "narrator1", Vars.NARRATOR_SCENE_ID, 0, 0, Vars.BIT_LAYER1);
			if(debugging){
				character = new Mob("TestName", "femaleplayer2", Vars.PLAYER_SCENE_ID, scene.getSpawnPoint() , Vars.BIT_PLAYER_LAYER);
				createPlayer(scene.getSpawnPoint());
			} else {
				
				//this spawns a camBot at its special location to take the place of 
				//the player before it has been created by the introduction
				if(scene.getCBSP()!=null)
					createEmptyPlayer(scene.getCBSP());
				else
					createEmptyPlayer(scene.getSpawnPoint());
				
				triggerScript("intro");
			}
		}

		initEntities();
		cam.bind(scene, false);
		b2dCam.bind(scene, true);
		world.setGravity(scene.getGravity());
	}

	//precreates all existing warps across entire game and puts them to hashtable
	public void catalogueWarps(){
		//collect names for valid levels
		Array<String> levels = new Array<>();
		FileHandle [] files = Gdx.files.internal("assets/maps").list();
		for(FileHandle f:files)
			if(f.extension().equals("tmx"))
				levels.add(f.nameWithoutExtension());

		Scene s;
		Array<Warp> w;
		// create warps from each level and add them to the hash
		for(String l : levels){
			s = new Scene(l);
			w = s.createWarps();
			for(Warp i : w) 
				warps.put(l+i.warpID, i);
		}

		//link all warps together
		Enumeration<Warp> e = warps.elements();
		while(e.hasMoreElements()){
			Warp i = e.nextElement();
//			if(i.type==Warp.Type.LINK);
//			System.out.print(i.locTitle+i.warpID+" :->: "+ i.next+i.getLinkID()+":\t");
			i.setLink(warps.get(i.next + i.getLinkID()));
		}
	}

	//basically a numerical representation of an entity
	//sceneIDs should NOT be used to directly access the entity from scripts, 
	//as the number represents chronologically how many mobs have been created 
	//since the game started
	public int createSceneID(){
		//get highest value of sceneID from file
		if(gameFile!=null){
			return 0;
		} else {
			//get Highest Value from entities in the scene currently 
			int max = 0;
			for(Entity e: objects){
				if(max<e.getSceneID())
					max = e.getSceneID();
			}

			return max + 1;
		}
	}

	public void createPlayer(Vector2 location){
//		String gender = character.getGender();
//		character.setGender(gender);

//		cam.setLock(false);
//		b2dCam.setLock(false);
		cam.zoom = Camera.ZOOM_NORMAL;
		b2dCam.zoom = Camera.ZOOM_NORMAL;

		character.setPosition(location);
		character.setGameState(this);
		character.create();
		cam.setCharacter(character); 
		b2dCam.setCharacter(character);
		cam.locate(Vars.DT);
		sortObjects();
	}

	public void createEmptyPlayer(Vector2 location){
		//debug
//		cam.setLock(false);
//		b2dCam.setLock(false);
//		cam.zoom = Camera.ZOOM_FAR;
//		b2dCam.zoom = Camera.ZOOM_FAR;

		character = new CamBot(0, 0);
		cam.zoom = Camera.ZOOM_NORMAL;
		b2dCam.zoom = Camera.ZOOM_NORMAL;

		character.setPosition(location);
		character.setGameState(this);
		character.create();
		cam.setCharacter(character); 
		b2dCam.setCharacter(character);
		cam.locate(Vars.DT);
		sortObjects();
	}

	public void switchCharacter(Mob c){
		character = c;
		cam.setCharacter(character);
		b2dCam.setCharacter(character);
	}

	public void triggerScript(String src){
		triggerScript(src, null);
	}

	public void triggerScript(String src, EventTrigger tg){
		Script s = new Script(src, ScriptType.EVENT, this, character);
		if (s.ID!=null && s.source!=null)
			triggerScript(s, tg);
	}

	public void triggerScript(Script script){
		triggerScript(script, null);
	}

	public void triggerScript(Script script, EventTrigger tg){
		currentScript = script;

		if (analyzing) return;
		if (currentScript != null) {
			analyzing = true;
			for (Entity e : objects)
				if (e instanceof SpeechBubble)
					addBodyToRemove(e.getBody()); //remove talking speech bubble
			script.analyze();
			if (currentScript.limitDistance) { //script can somehow become null at this point (threading issue?)
				setStateType(InputState.MOVELISTEN);
			} else {
				if(character.getInteractable()!=null)
					if(tg==null)
						positionPlayer(character.getInteractable());
					else if (tg.halt) System.out.println("not positioning");
//						do something
			}
		}
	}

	//add all of the scene's entities on init and create them
	private void initEntities() {
		removeAllObjects();
		objects.addAll(scene.getInitEntities());
		objects.add(character);
		paths.addAll(scene.getInitPaths());
		scene.applyPaths();

		//this should be changed to happen when the game checks if the player's
		//partner was last saved on the same level (part of global mobs)
		if(player.getPartner()!=null)
			if (player.getPartner().getName() != null) 
				objects.add(player.getPartner());

		for (Entity d : objects){
			d.setGameState(this);
			if(!d.equals(character))
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
		//record last position of entity so it can be saved
		for (Entity e : objects) {
			Vector2 lastPos = e.getBody().getPosition();
			lastPos.x *= Vars.PPM;
			lastPos.y *= Vars.PPM;
			if (e instanceof Mob) {
				lastPos.y -= ((Mob)e).rh;	//this offset allows entity to be spawned from right location
			}
			e.setPosition(lastPos);
		}

		//destroy all the bodies
		Array<Body> tmp = new Array<Body>();
		world.getBodies(tmp);
		for(Body b : tmp) {
			world.destroyBody(b);
		}
				
		//invalidate references to the destroyed bodies
		for (Entity e : objects) {
			e.setBody(null);
		}
	}

	public void removeAllObjects(){
		for(PositionalAudio s : sounds)
			s.sound.stop();
		sounds = new ArrayList<PositionalAudio>();
		objects = new ArrayList<Entity>();
		paths = new ArrayList<Path>();
	}

	public Warp findWarp(String levelID, int warpID){
		return warps.get(levelID + warpID);

	}
	
	public void setScene(Scene s){ scene = s;
	System.out.println(scene.ID); }
	public Scene getScene(){ return scene; }
	public World getWorld(){ return world; }
	public HUD getHud() { return hud; }
	public Camera getCam(){ return cam; }

	//sort by objects' layer
	public void sortObjects(){
		//		printObjects();
		Collections.sort(objects, new Comparator<Entity>(){
			public int compare(Entity o1, Entity o2) {
				return o1.compareTo(o2);
			}
		});

		//add all UI objects to end of list
		ArrayList<Entity> tmp = new ArrayList<>();
		for(Entity e: objects)
			if(e instanceof SpeechBubble || e instanceof TextBox)
				tmp.add(e);
		objects.removeAll(tmp);
		objects.addAll(tmp);

		//		printObjects();
	}

	public boolean exists(Entity obj){
		for(Entity e: objects)
			if(e.equals(obj))
				return true;
		return false;
	}

	public void printObjects() {
		for(Entity e:objects){
			//			debugText+="/l"+e.ID;
			System.out.println(e);
		}
	}

	public ArrayList<Entity> getObjects(){ return objects;	}

	public Color getColorOverlay(){
		// should be dependant on time;
		return new Color(2,2,2,Vars.ALPHA);
	}
}



