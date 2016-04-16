package main;

import static handlers.Vars.PPM;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.TreeSet;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

import box2dLight.Light;
import box2dLight.RayHandler;
import entities.CamBot;
import entities.Entity;
import entities.Entity.DamageType;
import entities.Ground;
import entities.HUD;
import entities.HUD.MoveType;
import entities.LightObj;
import entities.Mob;
import entities.Mob.Anim;
import entities.MobAI.AIType;
import entities.MobAI.ResetType;
import entities.Particle;
import entities.Path;
import entities.Projectile;
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
import scenes.Script.Option;
import scenes.Script.ScriptType;
import scenes.Song;

public class Main extends GameState {

	public Mob character;
	public Player player;
	public Warp warp;
	public Mob narrator;
	public History history;
	public Evaluator evaluator;
	public Script currentScript, loadScript;
	public InputState stateType, prevStateType;
	public int currentEmotion, dayState;
	public boolean paused, analyzing, loading, choosing, waiting;
	public boolean warping, warped; //for changing between scenes
	public boolean speaking; //are letters currently being drawn individually
	public float dayTime, weatherTime, waitTime, totalWait, clockTime, playTime, keyIndexTime;

	private int sx, sy;
	private float speakTime, speakDelay = .025f;
	private ArrayDeque<Page> displayText; //all text pages to display
	private String[] speakText; //current text page displaying
	private String gameFile; //used to load a game
	private World world;
	private HUD hud;
	private Scene scene, nextScene;
	private Color drawOverlay;
	private Texture pixel;
	private Page currentPage;
	private Array<Body> bodiesToRemove;
	private ArrayList<Entity> objects, objsToAdd/*, UIobjects, UItoRemove*/;
	private ArrayList<Path> paths;
	private ArrayList<Particle> particles, ptclsToAdd, ptclsToRemove;
	private ArrayList<LightObj> lights;
	private ArrayList<PositionalAudio> sounds;
	private Hashtable<String, Warp> warps;
	private HashMap<Entity, Float> healthBars;
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
	private static final int NIGHT = 2;
	private static final int TRANSITION_INTERVAL = /*60*/ 5;
	public static final float DAY_TIME = 6.55f*60f; //10min was the intended
	public static final float NOON_TIME = DAY_TIME/3f;
	public static final float NIGHT_TIME = 1.75f*DAY_TIME/3f;
	
	public static float debugY = 1, debugX=1;
	public static boolean dbRender 	= false, //render physics camera?
						rayHandle   = true, //include lighting?
						render 		= true,  //render world?
						dbtrender 	= false, //render debug text?
						debugging   = true,	 //in debug mode?
						cwarps      = true,	 //create warps?
						document    = false, //document variables?
						random;
	public static String debugLoadLoc = "Church"; //where the player starts
	public static String debugPlayerType = "femaleplayer2"; //what the player looks like in debug mode
//	public static Color ambC = new Color(Vars.NIGHT_LIGHT);

	public Main(GameStateManager gsm) {
		this(gsm, "newgame");
	}

	public Main(GameStateManager gsm, String gameFile) {
		super(gsm);
		this.gameFile = gameFile;
		JsonSerializer.gMain = this;
	}
	
	
	public void create(){
		super.create();
		ptclsToRemove = new ArrayList<>();
		displayText = new ArrayDeque<>();
		bodiesToRemove = new Array<>();
		ptclsToAdd = new ArrayList<>();
		particles = new ArrayList<>();
		objsToAdd = new ArrayList<>();
		healthBars = new HashMap<>();
		objects = new ArrayList<>();
		lights = new ArrayList<>();
		sounds = new ArrayList<>();
		paths = new ArrayList<>();
		warps = new Hashtable<>();
		history = new History();
		evaluator = new Evaluator(this);
		world = new World(new Vector2 (0, Vars.GRAVITY), true);
		b2dr = new Box2DDebugRenderer();
		rayHandler = new RayHandler(world);
		lightBuffer = new FrameBuffer(Format.RGBA8888, Vars.PowerOf2(Game.width), 
				Vars.PowerOf2(Game.height), false);
		//lightBufferRegion = new TextureRegion(Game.res.getTexture("lightMap"));
		
		Scene.clearEntityMapping();
		Entity.clearMapping();
		speakText = null;
		currentScript = null;
		pixel = Game.res.getTexture("pixel");
		stateType = prevStateType = InputState.MOVE;
		currentEmotion = Mob.NORMAL;
		paused=analyzing=choosing=waiting=warping=warped=speaking=false;
		waitTime=totalWait=speakTime=0;
		
//		if(debugging){
//			dayTime = NIGHT_TIME;
//			dayState = NIGHT;
//		} else {
			dayTime = DAY_TIME+TRANSITION_INTERVAL;
			dayState = DAY;
//		}
		
		cam.reset();
		b2dCam.reset();
		rayHandler.setShadows(false);
		rayHandler.setBlur(false);
		RayHandler.setGammaCorrection(true);
		RayHandler.useDiffuseLight(false);
		rayHandler.setAmbientLight(1);
		world.setContactListener(cl);
		b2dr.setDrawVelocities(true);

		if(cwarps)
			System.out.println("Don't Panic! The game is just linking levels together!");
		//drawString(sb, "Loading...", Game.width/4, Game.height/4);

		if(document) documentVariables();
		if(cwarps) catalogueWarps();
		load();
		hud = new HUD(this, hudCam);
		handleDayCycle(Vars.DT);
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
		
		//update the level specific loading script
		if(loadScript!=null &&loading)
			loadScript.update();

		if (!paused){
			speakTime += dt;
			clockTime = (dayTime+DAY_TIME/6f)%DAY_TIME;
			playTime+=dt;
			debugText = "";
			player.updateMoney();

			handleDayCycle(dt);
			if(!random && !tempSong && !scene.DEFAULT_SONG[dayState].equals(music))
				changeSong(scene.DEFAULT_SONG[dayState]);
			if(scene.outside) {
				weatherTime+=dt;
				handleWeather();
			}
			
			if(currentScript != null &&analyzing && !waiting &&!warping) 
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

			//update positional sounds
			for(PositionalAudio s : sounds)
				updateSound(s.location, s.sound);

			if(currentScript != null && currentScript.getOwner()!=null){
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
			
			//update health bars and their times
			Array<Entity> toRemove = new Array<>();
			for(Entity e: healthBars.keySet()){
				float time = healthBars.get(e);
				if(time-dt<=0 || !exists(e))
					toRemove.add(e);
				else
					healthBars.put(e, time-dt);
			}
			
			for(Entity e: toRemove)
				healthBars.remove(e);

			for (Entity e : objects){
				if (!(e instanceof Ground)) {
					if(!e.init && e.getBody()==null) {
						e.create();					
					}
					e.update(dt);

					//find bodies that are out of bounds
					if (e.getPixelPosition().x > scene.width + 50 || e.getPixelPosition().x < -50 || 
							e.getPixelPosition().y > scene.height + 100 || e.getPixelPosition().y < -50) {
						if (e instanceof Projectile)
							((Projectile) e).kill();
					}
				}
			}
			
			//add new objects into world
			objects.addAll(objsToAdd);
			objsToAdd.clear();
			sortObjects();

			//apply removal of deleted bodies
			for (Body b : bodiesToRemove){
				if (b != null) {
					if (b.getUserData() != null){
						Entity e = (Entity) b.getUserData();
						if(cam.getFocus().equals(e))
							cam.removeFocus();
						objects.remove(e);
						//ensures player still has access to the character
						//after hitting "repsawn"
						if(e.equals(character)) 
							addObject((Entity) e);
						world.destroyBody(b);
					}
				}
			}

			bodiesToRemove.clear();
			
			//particle stuff
			particles.addAll(ptclsToAdd);
			ptclsToAdd.clear();
			for(Particle p : particles)
				p.update(dt);
			
			particles.removeAll(ptclsToRemove);
			ptclsToRemove.clear();

			//apply steps to transport to next level
			if(warping){
				if (sb.getFadeType() == FadingSpriteBatch.FADE_IN)
					warp();
				if (!sb.fading && scene.equals(nextScene)){
					warping = false;
					nextScene = null;
				}
			}

			//update song cross-fade
			if(changingSong){
				if(!music.fading){
					music.dispose();
					setSong(nextSong);
					nextSong=null;
					changingSong = false;
				}
			}

			//if the temporary fanfare/event music is finished, go to default song
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

	// TODO for handling random rain
	public void handleWeather(){
		if(((int)(playTime%DAY_TIME))%2!=0) return;

	}

	// modify colors and sounds according to day time
	public void handleDayCycle(float dt){
		dayTime+=dt;
		//Day state boundaries
		if(dayTime>NOON_TIME){
			dayState = NOON;
		}if(dayTime>NIGHT_TIME){
			dayState = NIGHT;
		}if(dayTime>DAY_TIME){
			dayState = DAY;
			dayTime=0;

			for(LightObj l : lights)
				if(l.isScheduled() && l.isOn())
					l.turnOff();
		} if(dayTime>NIGHT_TIME-TRANSITION_INTERVAL){
			for(LightObj l : lights)
				if(l.isScheduled() && !l.isOn())
					l.turnOn();
		}

		//transition ambient light an color overlay
		Color ambient = scene.ambientLight;
		drawOverlay = scene.ambientOverlay;
		if(scene.outside){
			ambient = getAmbientLight();
			getColorOverlay();
			
			float t=dayTime,i;
			i = DAY_TIME/12f;
			switch(dayState){
			case DAY:
					sb.setOverlay(Vars.DAY_OVERLAY);
					if(sb.isDrawingOverlay()) 
						sb.setOverlayDraw(false);
				break;
			case NOON:
				if(t>NIGHT_TIME-i){
						sb.setOverlay(drawOverlay);
						if(!sb.isDrawingOverlay())
							sb.setOverlayDraw(true);
				}

				break;
			case NIGHT:
					sb.setOverlay(drawOverlay);
					if(!sb.isDrawingOverlay())
						sb.setOverlayDraw(true);
				break;
			}
			
		} else 
			sb.setOverlay(drawOverlay);
		if(!rayHandle) sb.setOverlayDraw(false);
		rayHandler.setAmbientLight(ambient);
	}
	
	/**
	 * produces spritebatch color overlay relative to day time
	 * @return
	 */
	public Color getColorOverlay(){
		float t=dayTime,i;
		switch(dayState){
		case DAY:
			drawOverlay = Vars.DAY_OVERLAY;
			break;
		case NOON:
			i = DAY_TIME/12f;
			drawOverlay = Vars.DAY_OVERLAY;
			//complicated color overlay for sunset
			if(t>NIGHT_TIME-i){
				if(t>NIGHT_TIME-i)
					drawOverlay = Vars.blendColors(t, NIGHT_TIME-i, NIGHT_TIME-i+i/4f, 
							Vars.DAY_OVERLAY, Vars.SUNSET_GOLD);
				if(t>NIGHT_TIME-i+i/4f)
					drawOverlay = Vars.blendColors(t, NIGHT_TIME-i+i/4f, NIGHT_TIME-i+2*i/4f, 
							Vars.SUNSET_GOLD, Vars.SUNSET_ORANGE);
				if(t>NIGHT_TIME-i+2*i/4f)
					drawOverlay = Vars.blendColors(t, NIGHT_TIME-i+2*i/4f, NIGHT_TIME-i+3*i/4f, 
							Vars.SUNSET_ORANGE, Vars.SUNSET_MAGENTA);
				if(t>NIGHT_TIME-i+3*i/4f)
					drawOverlay = Vars.blendColors(t, NIGHT_TIME-i+3*i/4f, NIGHT_TIME, 
							Vars.SUNSET_MAGENTA, Vars.NIGHT_OVERLAY);
			}

			break;
		case NIGHT:
			i = TRANSITION_INTERVAL;
			if(t>DAY_TIME-i){
				if(t>DAY_TIME-i)
					drawOverlay = Vars.blendColors(t, DAY_TIME-i, DAY_TIME-i/2f, 
							Vars.NIGHT_OVERLAY, Vars.SUNRISE);
				if(t>DAY_TIME-i/2f)
					drawOverlay = Vars.blendColors(t, DAY_TIME-i/2f, DAY_TIME, 
							Vars.SUNRISE, Vars.DAY_OVERLAY);
			} else
				drawOverlay = Vars.NIGHT_OVERLAY;
			break;
		}
		return drawOverlay;
	}
	
/**
 * return 
 * @return day time associated ambient lighting
 */
	public Color getAmbientLight(){
		Color ambient = null;
		float t=dayTime,i;
		switch(dayState){
		case DAY:
			ambient = Vars.DAY_LIGHT;
			break;
		case NOON:
			i = DAY_TIME/12f;
			drawOverlay = Vars.DAY_OVERLAY;
			ambient = Vars.blendColors(t, NIGHT_TIME-i, NIGHT_TIME, 
					Vars.DAY_LIGHT, Vars.NIGHT_LIGHT);
			break;
		case NIGHT:
			i = TRANSITION_INTERVAL;
			ambient = Vars.blendColors(t, DAY_TIME-i, DAY_TIME, 
					Vars.NIGHT_LIGHT, Vars.DAY_LIGHT);
			break;
		}
		return ambient;
	}

	public void render() {
//		Gdx.gl20.glClearColor(skyColor.r, skyColor.b, skyColor.g, skyColor.a);
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

		sb.setProjectionMatrix(cam.combined);
		rayHandler.setCombinedMatrix(cam.combined);
		scene.renderBG(sb);

		if(render){
			scene.renderEnvironment(cam, sb);
			sb.begin();

			//render every object on-screen, 
			//stopping at textboxes and speech bubbles
			int i = -1;
			Entity e;
			for (int x = 0; x<objects.size(); x++){
				e = objects.get(x);
				if(e instanceof SpeechBubble || e instanceof TextBox){
					i = x;
					break;
				}
				if(e.getLayer()!=Vars.BIT_LAYERSPECIAL)
					e.render(sb);
			}
			
			drawHealthBars(sb);
			for(Particle p : particles)
				p.render(sb);
			sb.end();

			scene.renderFG(sb);

			//speech bubbles get rendered on top of everything else
			if(i>=0){
				sb.begin();
				for (int x = i; x<objects.size(); x++)
					objects.get(x).render(sb);
				sb.end();
			}
		}

		if (speaking) speak();
		if (rayHandle) {
			rayHandler.updateAndRender();
//			renderLighting(sb);
		}

		boolean o = sb.isDrawingOverlay();
		if(o) sb.setOverlayDraw(false);

		b2dCam.setPosition(character.getPosition().x, character.getPosition().y + 15 / PPM);
		b2dCam.update();
		if (dbRender) b2dr.render(world, b2dCam.combined);
		hud.render(sb, currentEmotion);

		//draw text input string
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
	
	public void renderLighting(FadingSpriteBatch sb){
//TODO
		//start rendering to the lightBuffer
		lightBuffer.begin();

		//setup the right blending
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
		Gdx.gl.glEnable(GL20.GL_BLEND);

		//set the ambient color values, this is the "global" light of your scene
		//imagine it being the sun.  Usually the alpha value is just 1, and you change the darkness/brightness with the Red, Green and Blue values for best effect

		Gdx.gl.glClearColor(0.3f,0.38f,0.4f,1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		//start rendering the lights to our spriteBatch
		sb.begin();

		//set the color of your light (red,green,blue,alpha values)
		sb.setColor(0.9f, 0.4f, 0f, 1f);

		//tx and ty contain the center of the light source
		float tx= (Game.width/2);
		float ty= (Game.height/2);

		//tw will be the size of the light source based on the "distance"
		//(the light image is 128x128)
		//and 96 is the "distance"  
		//Experiment with this value between based on your game resolution 
		//my lights are 8 up to 128 in distance
		float tw=(128/100f)*96;

		//make sure the center is still the center based on the "distance"
		tx=tx-(tw/2);
		ty=ty-(tw/2);

		//and render the sprite
//		batch.draw(sprite, tx,ty,tw,tw,0,0,128,128,false,true);
		sb.end();
		lightBuffer.end();

		//now we render the lightBuffer to the default "frame buffer"
		//with the right blending !
		Gdx.gl.glBlendFunc(GL20.GL_DST_COLOR, GL20.GL_ZERO);
		sb.begin();
		sb.draw(lightBufferRegion, 0, 0,Game.width,Game.height);               
		sb.end();

		//post light-rendering
		//you might want to render your statusbar stuff here
	}

	//everything that needs to be displayed constantly for debug tracking is
	//right here
	public void updateDebugText() {
		Color ambC = Vars.NIGHT_LIGHT;
		debugText="ambient color: ("+ambC.r+", "+ambC.g+", "+ambC.b+" :: "+ambC.a+")";
		debugText+= "/l"+Vars.formatDayTime(clockTime, false)+"    Play Time: "+Vars.formatTime(playTime);
		debugText += "/lLevel: " + scene.title;
		debugText += "/lSong: " + music;
		debugText += "/lcontacts: " + character.contacts;

		debugText +="/l/l"+ character.getName() + " x: " + (int) (character.getPosition().x*PPM  /*/Vars.TILE_SIZE*/) + 
				"    y: " + ((int) (character.getPosition().y*PPM) - character.height);
		debugText+="/lstate: "+character.getCurrentState();
		
		if(currentScript!=null){
			debugText+= "/l"+currentScript+": "+(currentScript.index);
			debugText+= "/lanalyzing: "+(analyzing) +"   waiting: "+waiting;
			debugText+= "/lActiveObj: "+(currentScript.getActiveObject());
			debugText+= "/lPaused: "+(currentScript.paused)+"  ForcedP: "+(currentScript.forcedPause);
		}
		
		float t = ((int)(character.aimTime*100))/100f;
		debugText+="/l/la: "+character.aiming()+"    s: "+character.aimSounded()+"    t: "+t;
		t = ((int)(character.powerCoolDown*100))/100f;
		debugText+="/lC: "+t;
		
		sb.begin();
		drawString(sb, debugText, 2, Game.height/2 - font[0].getRegionHeight() - 2);
		sb.end();
		debugText="";
	}

	public void handleInput() {
		DamageType[] dm = {DamageType.ELECTRO, DamageType.FIRE, DamageType.DARKMAGIC, DamageType.ICE, DamageType.ROCK};
		float r = .005f;
		
		if(debugging){
			if(MyInput.isDown(Input.DEBUG_UP)) {
//			player.addFunds(100d);
//			Vars.NIGHT_LIGHT.r+=r;
//			rayHandler.setAmbientLight(Vars.NIGHT_LIGHT);
				character.damage(-1);
			} if(MyInput.isDown(Input.DEBUG_DOWN)){
//			player.addFunds(-100d);
//			Vars.NIGHT_LIGHT.r-=r;
//			rayHandler.setAmbientLight(Vars.NIGHT_LIGHT);
				character.damage(1);
			} if(MyInput.isDown(Input.DEBUG_LEFT)) {
				Vars.NIGHT_LIGHT.b-=r;
				rayHandler.setAmbientLight(Vars.NIGHT_LIGHT);
			} if(MyInput.isDown(Input.DEBUG_RIGHT)) {
				Vars.NIGHT_LIGHT.b+=r;
				rayHandler.setAmbientLight(Vars.NIGHT_LIGHT);
			} if (MyInput.isPressed(Input.DEBUG_LEFT2)) {
				debugX = debugX>0 ? debugX - 1 : dm.length-1;
				character.setPowerType(dm[(int) debugX]);
				System.out.println(debugX+"\t"+dm[(int) debugX]);
//			Vars.NIGHT_LIGHT.g-=r;
//			rayHandler.setAmbientLight(Vars.NIGHT_LIGHT);;
			} if (MyInput.isPressed(Input.DEBUG_RIGHT2)) {
				debugX = debugX<dm.length-1 ? debugX + 1 : 0;
				character.setPowerType(dm[(int) debugX]);
				System.out.println(debugX+"\t"+dm[(int) debugX]);
//			Vars.NIGHT_LIGHT.g+=r;
//			rayHandler.setAmbientLight(Vars.NIGHT_LIGHT);
			} if(MyInput.isPressed(Input.DEBUG_CENTER)) {
				random=true;
				int current = (Game.SONG_LIST.indexOf(music.title, false) +1)%Game.SONG_LIST.size;
				changeSong(new Song(Game.SONG_LIST.get(current)));
			} if(MyInput.isDown(Input.ZOOM_OUT /*|| Gdx.input.getInputProcessor().scrolled(-1)*/)) {
				cam.zoom+=.01;
				b2dCam.zoom+=.01;
			} if(MyInput.isDown(Input.ZOOM_IN /*|| Gdx.input.getInputProcessor().scrolled(1)*/)) {
				cam.zoom-=.01;
				b2dCam.zoom-=.01;
			}
			if(MyInput.isPressed(Input.LIGHTS)) {rayHandle = !rayHandle ; sb.setOverlayDraw(rayHandle); }
			if(MyInput.isPressed(Input.COLLISION)) dbRender = !dbRender ;
			if(MyInput.isPressed(Input.RENDER)) render=!render;
			if(MyInput.isPressed(Input.DEBUG_TEXT)) dbtrender=!dbtrender;
			if(MyInput.isPressed(Input.RESPAWN)) character.respawn();
		}

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

				//following four functions navigate through the pause menu
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
			boolean canWarp = false;
			if(character.getWarp()!=null)
				canWarp = character.getWarp().conditionsMet();
			
			switch (stateType){
			case KEYBOARD:
				//allows the player to input text 
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
				//handling for changing what the player looks like
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				int index = (int) currentScript.getVariable("playertype");

				if(MyInput.isPressed(Input.LEFT)){
					int cap = 2;
					if(character.getGender().equals("male"))
						cap = 4;
					if(index>1)index--;
					else index = cap;

					character.ID = character.getGender() + "player" + index;
					character.loadSprite();
					currentScript.setVariable("playertype", index);
				}

				if(MyInput.isPressed(Input.RIGHT)){
					int cap = 2;
					if(character.getGender().equals("male"))
						cap = 4;
					if(index<cap) index++;
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
			case MOVE:
				//button input handling for general gameplay
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				if(/*cam.focusing||*/warping||quitting||waiting||character.dead||character.frozen) return;
				if(MyInput.isPressed(Input.JUMP)) character.jump();
				if(MyInput.isDown(Input.UP)) {
					if(canWarp && character.isOnGround() && !character.snoozing){ 
						if(!character.getWarp().instant) {
							initWarp(character.getWarp());
							character.killVelocity();
						}
					} else if(character.canClimb) character.climb();
					else {
						Vector2 f = new Vector2(character.getPixelPosition().x, 
								character.getPixelPosition().y + 4.5f*Vars.TILE_SIZE*cam.zoom + cam.yOff);
						cam.setFocus(f);
						b2dCam.setFocus(f);
						character.lookUp();
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
				if(MyInput.isDown(Input.RUN)) character.run();
				
				if(MyInput.isDown(Input.SPECIAL)) {
					character.aim();
					if(MyInput.isDown(Input.LEFT)&&!character.isFacingLeft()) 
						character.changeDirection();
					else if(MyInput.isDown(Input.RIGHT)&&character.isFacingLeft()) 
						character.changeDirection();
					
				}
				if(MyInput.isPressed(Input.ATTACK)) {
					if(MyInput.isDown(Input.SPECIAL) || (MyInput.isDown(Input.DOWN)
							&& MyInput.isDown(Input.SPECIAL))){
						if(character.seesSomething()){
//							player.doRandomPower(character.target());
							character.powerAttack(character.target());
						} else {
//							player.doRandomPower();
							character.powerAttack(character.target());
						}
					} else
						character.punch();
				}

				if(MyInput.isUp(Input.SPECIAL) && (character.getAction().equals(Anim.AIMING) ||
						character.getAction().equals(Anim.AIM_TRANS) || character.getAction().equals(Anim.ATTACKING)))
					character.unAim();
					
				if(MyInput.isUp(Input.UP) && (character.getAction().equals(Anim.LOOKING_UP)
						|| character.getAction().equals(Anim.LOOK_UP))){
					character.unLookUp();
					cam.removeFocus();
					b2dCam.removeFocus();
				}

				if(MyInput.isUp(Input.DOWN) && (character.getAction().equals(Anim.DUCKING)
						|| character.getAction().equals(Anim.DUCK))){
					character.unDuck();
				}

				break;
			case MOVELISTEN:
				if(MyInput.isPressed(Input.PAUSE)) pause();
				if(cam.moving/*||cam.focusing*/||warping||quitting||character.dead/*||waiting*/||character.frozen) return;
				if(MyInput.isDown(Input.UP)) {
					if(canWarp && character.isOnGround() && !character.snoozing && 
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
				if((MyInput.isPressed(Input.JUMP) || MyInput.isPressed(Input.ENTER)) && !speaking && buttonTime >= DELAY) {
					if(hud.raised) playSound(character.getPosition(), "ok1");

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
					if(canWarp && character.isOnGround() && !character.snoozing && 
							!character.getWarp().instant) {
					}
					else if(character.canClimb) character.climb();
					else {
						Vector2 f = new Vector2(character.getPixelPosition().x, 
								character.getPixelPosition().y + 4.5f*Vars.TILE_SIZE*cam.zoom + cam.yOff);
						cam.setFocus(f);
						b2dCam.setFocus(f);
						character.lookUp();
					}
				}

				if(MyInput.isPressed(Input.UP)) {
					if(canWarp && character.isOnGround() && !character.snoozing && 
							!character.getWarp().instant) {
						initWarp(character.getWarp());
						character.killVelocity();
					}
				}

				if(MyInput.isUp(Input.UP) && (character.getAction().equals(Anim.LOOKING_UP)
						|| character.getAction().equals(Anim.LOOK_UP))){
					character.unLookUp();
					cam.removeFocus();
					b2dCam.removeFocus();
				}
				if(MyInput.isUp(Input.DOWN) && (character.getAction().equals(Anim.DUCKING)
						|| character.getAction().equals(Anim.DUCK))){
					character.unDuck();
				}
				break;
			case LISTEN:
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();

				if(currentScript!=null)
					if(/*currentScript.dialog &&*/ (MyInput.isPressed(Input.JUMP) || MyInput.isPressed(Input.ENTER))){
						if(!speaking && buttonTime >= DELAY){
							if(hud.raised) playSound(character.getPosition(), "ok1");
							if(currentScript.getActiveObject() instanceof TextBox){
								TextBox t = (TextBox) currentScript.getActiveObject();
								t.kill();
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
						if(choices.length!=1){
							choiceIndex = choiceIndex < choices.length -1 ? choiceIndex + 1: 0;

							playSound("text1");
							choices[choiceIndex].expand();
							choices[prevIndex].collapse();
						}
					} else if (MyInput.isDown(Input.RIGHT)){
						buttonTime = 0;
						if(choices.length!=1){
							choiceIndex = choiceIndex == 0 ? choices.length - 1 : choiceIndex - 1;

							playSound("text1");
							choices[choiceIndex].expand();
							choices[prevIndex].collapse();
						}
					} else if(MyInput.isPressed(Input.JUMP) || MyInput.isPressed(Input.ENTER)){
						playSound("ok1");

						currentScript.paused = choosing = false;
						currentScript.getChoiceIndex(choices[choiceIndex].getMessage());

						//delete speechBubbles from world
						for (SpeechBubble b : choices)
							removeBody(b.getBody()); 
					}
				}

				break;
			default:
				break;
			}
		}
	}

	
	//trigger the necessary scripts for making the partner follow
	private void partnerFollow(){
		if(player.getPartner()!=null){
			if(player.getPartner().getName() != null)
				if(getMob(player.getPartner().getName())!=null){
					setStateType(InputState.LISTEN);
					if (player.stopPartnerDisabled)
						triggerScript("toggleFollowDisabled");
					else if(player.getPartner().getState().type == AIType.FOLLOWING) {
						triggerScript("stopFollower");
						player.getPartner().stay();
					} else {
						triggerScript("suggestFollow");
//						player.getPartner().follow(character);
					}
				} else
					triggerScript("noPartnerPresent");
		} else 
			triggerScript("noPartner");
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
		float dx = (character.getPixelPosition().x - obj.getPixelPosition().x);
		float gx = obj.getPixelPosition().x + min * dx / Math.abs(dx);
		
		if (Math.abs(dx) < min - 2){
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
	public void displayChoice(Array<Option> options){
		final float h = 30 / PPM;
		float x, y, theta;
		int c = options.size;
		choices = new SpeechBubble[c];
		choiceIndex = 0;

		Option o;
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
			startSpeak();
			if(!hud.raised) 
				hud.show();
			music.fadeOut(Game.musicVolume*2f/3f);
		}

		//show text per character
		if (hud.moving == MoveType.NOT){
			if (speakTime >= speakDelay) {
				speakTime = 0;
				if(hud.raised) speakDelay = .020f;
				else speakDelay = 2f;

//				System.out.println(currentPage.skip);
				if(sy>=speakText.length){
					endSpeak();
				} else if(speakText[sy].length()>0){
					char c = speakText[sy].charAt(sx);
					//apply character specific delays
					if (c == ".".charAt(0) || c == ",".charAt(0)|| c == "!".charAt(0)|| c == "?".charAt(0)) 
						speakDelay = .25f;
					if(c != "~".charAt(0)){
						hud.addChar(sy, c);
						float voice = 0;
						try{ 
							if(hud.getFace() instanceof Mob)
							voice = ((Mob)hud.getFace()).voice; //wtf is null here
						} catch(Exception e){} 
						
						Gdx.audio.newSound(new FileHandle("assets/sounds/text1.wav"))
						.play(Game.soundVolume * .9f, (float) Math.random()*.15f + .9f + voice, 1);
					} else speakDelay = .75f;

					sx++;
					//reached end of line
					if (sx == speakText[sy].length()) {sx = 0; sy++; }
					//reached end of page
					if (sy == speakText.length)
						//move onto the next page without pausing if possible
						if(currentPage.skip){
							wait(.3f);
							if(!displayText.isEmpty()) startSpeak();
							else {
								endSpeak();
								currentScript.paused = currentScript.forcedPause = false;
							}
						} else endSpeak();
				} else
					sy++;
			}
		}
	}
	
	private void startSpeak(){
		speaking = true;

		currentPage = displayText.poll();
		String line = currentPage.text;
		currentEmotion = currentPage.emotion;

		speakText = line.split("/l");
		hud.createSpeech(speakText);
		sx = sy = 0;
	}

	private void endSpeak(){
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

	//initialize level transition using a warp
	public void initWarp(Warp warp){
		if (warp.getLink()==null) return;
		warping = true;
		warped = false;
		this.warp = warp;
		nextScene = warp.getNextScene();

		sb.fade();
		if(!nextScene.DEFAULT_SONG[dayState].title.equals(music.title))
			changeSong(nextScene.DEFAULT_SONG[dayState]);
	}
	
	private Vector2 location;
	//initalize level transition using teleportation
	public void initTeleport(Vector2 loc, String level){
		if(Game.LEVEL_NAMES.contains(level, false)){
			warping = true;
			warped = false;
			warp = null;

			location = loc;
			nextScene = new Scene(world, this, level);
			nextScene.setRayHandler(rayHandler);

			sb.fade();
			if(!nextScene.DEFAULT_SONG[dayState].title.equals(music.title))
				changeSong(nextScene.DEFAULT_SONG[dayState]);
		} else {
			System.out.println("\""+level+"\" is an invalid level name");
		}
	}

	public void warp(){
		if(warped) return;

		random = false;
		destroyBodies();
		scene = nextScene;
		scene.create();
		
		if(warp!=null){
			Vector2 w = warp.getLink().getWarpLoc();
			createPlayer(w);
			if(warp.getLink().warpID==1 && !character.isFacingLeft()) character.setDirection(true);
			if(warp.getLink().warpID==0 && !character.isFacingLeft()) character.setDirection(false);
		} else {
			location.y-=1.5f*character.height;
			createPlayer(location);
			location = null;
		}

		initEntities();
		cam.setBounds(Vars.TILE_SIZE*4, (scene.width-Vars.TILE_SIZE*4), 0, scene.height);
		b2dCam.setBounds((Vars.TILE_SIZE*4)/PPM, (scene.width-Vars.TILE_SIZE*4)/PPM, 0, scene.height/PPM);
		cam.removeFocus();
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

	public void setDispText(ArrayDeque<Page> dispText) { displayText = dispText;}
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
	public void removeSound(PositionalAudio s){ sounds.remove(s); }
	
	public void setCharacter(Mob e){
		character = e;
		cam.setCharacter(e);
		b2dCam.setCharacter(e);
	}
	
	public void addObject(Entity e){ 
		if(e==null) return;
		if(!exists(e)){
			objsToAdd.add(e); 
			e.setGameState(this);
		}
	}
	
	public void addParticle(Particle p){ 
		if(p==null) return;
			ptclsToAdd.add(p); 
	}
	
	public void removeParticle(Particle p){ ptclsToRemove.add(p); }

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

	public void removeBody(Body b){
		bodiesToRemove.add(b); 
		if(b!=null)
			if(b.getUserData()!=null){
				Light l = ((Entity) b.getUserData()).getLight();
				if(l != null)
					rayHandler.lightList.removeValue(l, true);
			}
	}
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

			narrator = (Mob)Entity.getMapping(Vars.NARRATOR_SCENE_ID);
			character = (Mob)Entity.getMapping(Vars.PLAYER_SCENE_ID);
			createPlayer(character.getPixelPosition().add(new Vector2(0, -character.rh)));	//TODO normalize dealing with height offset
		} else {
			//TODO normalize narrator reference (should exist regardless of what level the player's on)
			if(debugging) scene= new Scene(world,this, debugLoadLoc);
			else scene= new Scene(world,this,"Residential District N");
			setSong(scene.DEFAULT_SONG[dayState]);
			scene.setRayHandler(rayHandler);
			scene.create();

			narrator = new Mob("Narrator", "narrator1", Vars.NARRATOR_SCENE_ID, 0, 0, Vars.BIT_LAYER1);
			if(debugging){
//				character = new Mob("'Normal' person with a name (YOU)", "maleplayer2", Vars.PLAYER_SCENE_ID, scene.getSpawnPoint() , Vars.BIT_PLAYER_LAYER);
				character = new Mob("You", debugPlayerType, Vars.PLAYER_SCENE_ID, scene.getSpawnPoint() , Vars.BIT_PLAYER_LAYER);
				createPlayer(scene.getSpawnPoint());
//				createEmptyPlayer(scene.getSpawnPoint());
				DamageType[] dm = {DamageType.ELECTRO, DamageType.FIRE, DamageType.DARKMAGIC, DamageType.ICE, DamageType.ROCK};
				int j = (int)(Math.random() * ((dm.length-1) + 1));
				character.setPowerType(dm[j]);
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
		Scene s;
		Array<Warp> w;
		// create warps from each level and add them to the hash
		for(String l : Game.LEVEL_NAMES){	
			s = new Scene(l);
			w = s.createWarps();
			for(Warp i : w)
				warps.put(l+i.warpID, i);
			
		}

		//link all warps together
		Enumeration<Warp> e = warps.elements();
		while(e.hasMoreElements()){
			Warp i = e.nextElement();
			i.setLink(warps.get(i.next + i.getLinkID()));
		}
	}

	public void createPlayer(Vector2 location){
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
		if(currentScript!=null || analyzing){
			System.out.println("Main already has script with ID: "+currentScript.ID);
			return;
		}
		
		if (script != null) 
			if(script.source!=null){
				if(character.aiming()) character.unAim();
				currentScript = script;
				analyzing = true;
				for (Entity e : objects)
					if (e instanceof SpeechBubble)
						removeBody(e.getBody()); //remove talking speech bubble
				script.analyze();
				if(currentScript==null)return;
				if (currentScript.limitDistance) { //script can somehow become null at this point (threading issue?)
					setStateType(InputState.MOVELISTEN);
				} else {
					if(character!=null)
						if(character.getInteractable()!=null)
							if(tg==null){
//								positionPlayer(character.getInteractable());
							}else if (tg.getHalt(currentScript.ID)) 
								System.out.println("not positioning");
//						do something
				}
			}
	}
	
	public void doLoadScript(Script script){
		loadScript = script;
		loading = true;
		loadScript.analyze();
	}

	//add all of the scene's entities on init and create them
	private void initEntities() {
		removeAllObjects();
		objects.addAll(scene.getInitEntities());
		lights.addAll(scene.getLights());
		objects.add(character);
		paths.addAll(scene.getInitPaths());
		scene.applyRefs();

		//pull permanent followers into the current level
		HashMap<Mob, Boolean> f = character.getFollowers();
		Array<Entity> toRemove = new Array<>();
		for(Mob m : f.keySet()){
			if(f.get(m)){
				Scene.switchEntityMapping(m.getCurrentScene().ID, scene.ID, m.getSceneID());
				m.setPosition(character.getPixelPosition());
				objects.add(m);
			} else
				toRemove.add(m);
		}
		for(Entity m : toRemove)
			f.remove(m);
		toRemove.clear();
		
		//remove null references?
		for(int i =0; i<objects.size();i++)
			if(objects.get(i)==null)
				toRemove.add(objects.get(i));
		for(Entity r: toRemove)
			objects.remove(r);
		
		for (Entity d : objects){
			d.setGameState(this);
			if(!d.equals(character)){
				d.create();
			}
		}

		sortObjects();
//		printObjects();
		if(scene.loadScript.source!=null) 
			doLoadScript(scene.loadScript);
		
		//turn off loaded lights if necessary
		if(dayState!=NIGHT){
			for(LightObj l : lights){
				if(l.isScheduled() && l.isOn())
					l.turnOff();
			}
		}
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
		rayHandler.removeAll();
		//record last position of entity so it can be saved
		for (Entity e : objects) {
			if(e==null) continue;
			if(e.getBody()==null) continue;
			Vector2 lastPos = e.getPixelPosition();
			if (e instanceof Mob) {
				lastPos.y -= ((Mob)e).rh;	//this offset allows entity to be spawned from right location
				if(((Mob)e).getCurrentState().resetType.equals(ResetType.ON_LEVEL_CHANGE))
					((Mob)e).resetState();
			}
			e.setPosition(lastPos);
			e.addLight(null);
		}

		//destroy all the bodies
		Array<Body> tmp = new Array<Body>();
		world.getBodies(tmp);
		for(Body b : tmp) {
			world.destroyBody(b);
			//invalidate references to the destroyed bodies
			if(b!=null)
				if(b.getUserData() instanceof Entity)
					((Entity) b.getUserData()).setBody(null);
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
	
	public RayHandler getRayHandler(){return rayHandler; }
	public void setScene(Scene s){ 
		if (s == null)
			return;
		scene = s; 
		System.out.println("set Scene: "+scene.ID); 
	}
	public Scene getScene(){ return scene; }
	public World getWorld(){ return world; }
	public HUD getHud() { return hud; }
	public Camera getCam(){ return cam; }
	public MyContactListener getContactListener(){ return this.cl; }

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
		for(Entity e: objects){
			if(e.equals(obj))
				return true;
		}
		return false;
	}

	public ArrayList<LightObj> getLights(){ return lights; }
	public ArrayList<Entity> getObjects(){ return objects;	}
	public void printObjects() {
		for(Entity e:objects){
//			debugText+="/l"+e.ID;
			System.out.println(e+": "+e.getLayer());
		}
	}
	
	public void drawHealthBars(SpriteBatch sb){
		Color tint = sb.getColor();
		for(Entity e: healthBars.keySet()){
			for(int i = 0; i < e.getMaxHealth(); i++){
				if(i<=e.getHealth())
					if(e.frozen)
						sb.setColor(Color.CYAN);
					else
						sb.setColor(Color.RED);
				else
					sb.setColor(Color.GRAY);
				sb.draw(pixel, e.getPixelPosition().x + i - (int)(e.getMaxHealth()/2f), e.getPixelPosition().y + e.rh + 5);
			}

			sb.setColor(tint);
		}
	}
	
	public void addHealthBar(Entity e){
		if(!e.equals(character) || !e.destructable)
			healthBars.put(e, 3f);
	}
	
	
	
	//create a file listing all used scene IDs from game
	//create a file listing all events names
	//create a file listing all used variable names
	public void documentVariables(){
		TreeSet<String> IDs = new TreeSet<>();
		TreeSet<String> variables = new TreeSet<>();
		TreeSet<String> events = new TreeSet<>();
		Array<Integer> used = new Array<>();
		Array<Pair<String, String>> entities = new Array<>(); //used for determining duplication

		//load each level and collect data about NPCs and other Entities
		for(String level : Game.LEVEL_NAMES){
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
		}

		//define variables and events from scripts, as well as spawned entities
		for(String script : Game.SCRIPT_LIST.keySet()){
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
		}
		
		//document unused sceneIDs
		for(int i = 0; i<500; i++)
			if(!used.contains(i, false)){
				String s = " ";
				s = Vars.formatHundreds(s, String.valueOf(i).length());
				IDs.add(s+i+ " - ");
			}

		//collect default global variables
		HashMap<String, Object> varList = history.getVarlist();
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
}

