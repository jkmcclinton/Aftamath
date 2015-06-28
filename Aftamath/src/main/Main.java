package main;

import static handlers.Vars.PPM;
import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.MyContactListener;
import handlers.MyInput;
import handlers.MyInput.Input;
import handlers.Pair;
import handlers.PositionalAudio;
import handlers.Vars;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

import scenes.Scene;
import scenes.Script;
import scenes.Song;
import box2dLight.PointLight;
//import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
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
import entities.NPC.AIState;
import entities.Player;
import entities.SpeechBubble;
import entities.SpeechBubble.PositionType;
import entities.TextBox;
import entities.Warp;

public class Main extends GameState {
	
	public Mob character;
	public Player player;
	public Warp warp;
	public NPC narrator;
	public HUD hud;
	public History history;
	public Script currentScript;
	public InputState stateType, prevState;
	public int currentEmotion, dayState;
	public boolean paused, analyzing, choosing, waiting;
	public boolean warping, warped; //for changing between scenes
	public boolean speaking; //are letters currently being drawn individually
	public float dayTime, weatherTime, waitTime, totalWait, clockTime, playTime;
	
	private int sx, sy;
	private float speakTime, speakDelay = .025f;
	private ArrayDeque<Pair<String, Integer>> displayText; //all text pages to display
	private String[] speakText; //current text page displaying
	private World world;
	private Scene scene, nextScene;
	private Color drawOverlay;
	private Array<Body> bodiesToRemove;
	private ArrayList<Entity> objects/*, UIobjects, UItoRemove*/;
	private ArrayList<PositionalAudio> sounds;
	private Box2DDebugRenderer b2dr;
	private InputState prevStateType;
	private RayHandler rayHandler;
	private MyContactListener cl = new MyContactListener(this);
	private SpeechBubble[] choices;
	
	public static enum InputState{
		MOVE, //can move and interact
		MOVELISTEN, //listening to text and can move
		LISTEN, //listening to text only
		CHOICE, //choosing an option
		LOCKED, //lock all input; It's up to the script to unlock
		PAUSED, //in pause menu
	}
	
	public static enum WeatherState{
		CLEAR, CLOUDY, RAINING, STORMY,
	}
	
	//day/night cycle variables
	private static final int DAY = 0;
	private static final int NOON = 1;
	private static final int NIGHT =2;
	private static final int TRANSITION_INTERVAL = /*60*/ 5;
	public static final float DAY_TIME = /*24f*60f*/5*60 /*3*(TRANSITION_INTERVAL+5)*/;
	public static final float NOON_TIME = DAY_TIME/3f;
	public static final float NIGHT_TIME = 2*DAY_TIME/3f;
	
	//debug
	//use these for determining hard-coded values
	//please, don't keep them as hard-coded
	//private int debugX;
	public float debugY = 32, debugX=13.399997f;
	
	public boolean dbRender 	= false, 
					rayHandling = false, 
					render 		= true, 
					dbtrender 	= true, 
					random;
//	private float ambient = .5f;
//	private int colorIndex;
	private PointLight tstLight;
	
	public Main(GameStateManager gsm) {
		super(gsm);
	}
	
	public void create(){
		bodiesToRemove = new Array<>();
		history = new History();
		currentScript = null;
		
		stateType = prevStateType=InputState.MOVE;
		currentEmotion = Mob.NORMAL;
		dayState=DAY;
		paused=analyzing=choosing=waiting=warping=warped=speaking=false;
		dayTime = weatherTime= waitTime=totalWait=0;

//		sx=sy=0;
//		speakTime=0;
//		speakDelay = .025f;
		speakText = null;
//		dayTime = 3*NOON_TIME/2f;
		
		objects = new ArrayList<Entity>();
		sounds = new ArrayList<PositionalAudio>();
		world = new World(new Vector2 (0, Vars.GRAVITY), true);
		world.setContactListener(cl);
		b2dr = new Box2DDebugRenderer();
//		b2dr.setDrawVelocities(true);
		rayHandler = new RayHandler(world);
//		rayHandler.setAmbientLight(0);
		player = new Player("TestName", "male", "player1");
		scene= new Scene(world,this,"Street");
//		scene = new Scene(world, this, "room1_1");
		
		cam.reset();
		cam.bind(scene, false);
		b2dCam.bind(scene, true);
		world.setGravity(scene.getGravity());
		
		stateType = InputState.MOVE;
		speakTime = 0;
		displayText = new ArrayDeque<Pair<String, Integer>>();
		
		scene.setRayHandler(rayHandler);
		scene.create();
		
		initEntities();
		createPlayer(scene.getSpawnpoint());
		hud = new HUD(player, hudCam);
		 
		tstLight = new PointLight(rayHandler, Vars.LIGHT_RAYS, Color.RED, 100, 0, 0);
		tstLight.attachToBody(character.getBody(), 0, 0);
		
		handleDayCycle();
		handleWeather();
		setSong(scene.DEFAULT_SONG[dayState]);
	}
	
	public void update(float dt) {
		super.update(dt);
		buttonTime += dt;
		hud.update(dt);
		
		if (!paused){
			speakTime += dt;
			dayTime+=dt;
			clockTime = (dayTime+DAY_TIME/6f)%DAY_TIME;
			playTime+=dt;
			debugText = "";
			
			handleDayCycle();
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
					stateType = InputState.MOVE;
				}
			}
 
			for (Entity e : objects){
				if (!(e instanceof Ground)) {
					e.update(dt);

					//kill object
					if (e.getBody() == null) {e.create();}
					if (e.getPosition().x*PPM > scene.width + 50 || e.getPosition().x*PPM < -50 || 
							e.getPosition().y*PPM > scene.height + 100 || e.getPosition().y*PPM < -50) {
						if (e instanceof Player){
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
			tstLight.setPosition(character.getPosition());
			rayHandler.setLightMapRendering(false);
		} else {
			if (!quitting)
				handleInput();
		}
		
		
//		debugText= "Volume: "+music.getVolume();
//		debugText= "next: "+nextSong+"     "+music;
		debugText= Vars.formatDayTime(clockTime, false)+"    Play Time: "+/*Vars.formatTime(*/playTime/*)*/;
//		debugText+= "/l/l"+drawOverlay.r+"/l"+drawOverlay.g+"/l"+drawOverlay.b;
//		(cam.moving||cam.focusing||warping||quitting||character.dead||waiting||character.frozen)
		debugText+= "/lState: "+(stateType);
		if(currentScript!=null){
			debugText+= "/lfocedPause: "+(currentScript.forcedPause);
			debugText+= "/lmoving: "+(cam.moving);
			debugText+= "/ldialog: "+(currentScript.dialog);
			debugText+= "/lwaitTime: "+(currentScript.time +":"+currentScript.waitTime);
		}
		
		debugText+= "/l/lcontrolled: "+character.controlled;
//		debugText+= "/l/lSpeed: "+character.getBody().getLinearVelocity();
//		debugText +="/l/l"+ character.getName() + " x: " + (int) (character.getPosition().x*PPM) + 
//				"    y: " + ((int) (character.getPosition().y*PPM) - character.height);
//		debugText +="/lCamera" + " x: " + (int) (cam.position.x) + "    y: " + ((int) (cam.position.y));

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
		if(dbtrender) updateDebugText();
		
		if(o) sb.setOverlayDraw(true);
	}
	
	public void updateDebugText() {
		
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
		
		if(MyInput.isPressed(Input.DEBUG_LEFT)) {
//			if(colorIndex>0) 
//				colorIndex--; 
//			rayHandler.setAmbientLight(Vars.COLORS.get(colorIndex)); 
//			rayHandler.setAmbientLight(ambient);
//			debugX-=.2f;
//			System.out.println(debugX);
		}
		
		if (MyInput.isDown(Input.DEBUG_LEFT2)) {
//			debugY-=1;
//			System.out.println(debugY);
		}
		if (MyInput.isDown(Input.DEBUG_RIGHT2)) {
//			debugY+=1;
//			System.out.println(debugY);
		}
		
		if(MyInput.isPressed(Input.DEBUG_CENTER)) {
			random=true;
			changeSong(new Song(Game.SONG_LIST.get((int) (Math.random()*(Game.SONG_LIST.size)))));
//			dayTime+=1.5f;
		}
		
		if(MyInput.isPressed(Input.DEBUG_RIGHT)) {
//			if(colorIndex<Vars.COLORS.size -1) 
//				colorIndex++; 
//			rayHandler.setAmbientLight(Vars.COLORS.get(colorIndex)); 
//			rayHandler.setAmbientLight(ambient);
//			debugX+=.2f;
//			System.out.println(debugX);
			}
		
		if(MyInput.isDown(Input.ZOOM_OUT)) {
			cam.zoom+=.01;
			b2dCam.zoom+=.01;
		}
		if(MyInput.isDown(Input.ZOOM_IN)) {
			cam.zoom-=.01;
			b2dCam.zoom-=.01;
		}
		if(MyInput.isPressed(Input.LIGHTS)) rayHandling = !rayHandling ;
		if(MyInput.isPressed(Input.COLLISION)) dbRender = !dbRender ;
		if(MyInput.isPressed(Input.DEBUG)) character.respawn();
		if(MyInput.isPressed(Input.DEBUG2)) render=!render;
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
			case LOCKED:
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				break;
			case MOVE:
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				if(cam.focusing||warping||quitting||character.dead||waiting||character.frozen) return;
				if(MyInput.isPressed(Input.JUMP)) character.jump();
				if(MyInput.isDown(Input.UP)) {
					if(character.canWarp && character.isOnGround() && !character.snoozing) {
						warping = true;
						warped = false;
						warp = character.getWarp();
						nextScene = warp.getNext();
						//					scene.saveLevel();

						sb.fade();
						if(nextScene.newSong)
							changeSong(nextScene.DEFAULT_SONG[dayState]);
					}
					else if(character.canClimb) character.climb();
					else character.lookUp();
				}
				if(MyInput.isDown(Input.DOWN)) 
					if(character.canClimb)
						character.descend();
					else character.lookDown();
				if(MyInput.isPressed(Input.USE)) partnerFollow();
				if(MyInput.isPressed(Input.INTERACT)) 
					triggerScript(character.interact());

				if(MyInput.isDown(Input.LEFT)) character.left();
				if(MyInput.isDown(Input.RIGHT)) character.right();
				if(MyInput.isDown(Input.RUN)) character.run();
				if(MyInput.isPressed(Input.ATTACK)) {
					character.attack(); 
				}
				break;
			case MOVELISTEN:
				if(MyInput.isPressed(Input.PAUSE)) pause();
				if(cam.moving||cam.focusing||warping||quitting||character.dead||waiting||character.frozen) return;
				if(MyInput.isDown(Input.UP)) character.climb();
				if(MyInput.isDown(Input.DOWN)) character.descend();
				if(MyInput.isDown(Input.LEFT)) character.left();
				if(MyInput.isDown(Input.RIGHT)) character.right();
				if(MyInput.isPressed(Input.JUMP) && !speaking && buttonTime >= DELAY) {
					playSound(player.getPosition(), "ok1");

					if(displayText.isEmpty()) {
						if (currentScript != null)
							currentScript.paused = currentScript.forcedPause = 
							currentScript.dialog = false;
					} else {
						speakDelay = .2f;
						speakTime = 0;
						speak();
					}
				} else if (MyInput.isPressed(Input.JUMP) && speaking){
					buttonTime = 0;
					speaking = hud.fillSpeech(speakText);
				}
				break;
			case LISTEN:
				if(MyInput.isPressed(Input.PAUSE) && !quitting) pause();
				if(currentScript!=null)
					if(MyInput.isPressed(Input.JUMP) && !speaking && buttonTime >= DELAY)
						if(currentScript.getActiveObject() instanceof TextBox){
							TextBox t = (TextBox) currentScript.getActiveObject();
							t.kill();

							currentScript.paused = currentScript.forcedPause = false;
							currentScript.setActiveObj(new Entity());
							stateType = prevState;
						}
				else
				if(MyInput.isPressed(Input.JUMP) && !speaking && buttonTime >= DELAY) {
					playSound(player.getPosition(), "ok1");
					if(displayText.isEmpty()) {
						if (currentScript != null) {
							currentScript.paused = currentScript.forcedPause =
									currentScript.dialog = false;
						} else {
							hud.hide();
							stateType = InputState.MOVE;
						}
					} else {
						speakDelay = .2f;
						speakTime = 0;
						speak();
					}
				} else if (MyInput.isPressed(Input.JUMP) && speaking){
					buttonTime = 0;
					speaking = hud.fillSpeech(speakText);
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
//						stateType = prevStateType;
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
					stateType = InputState.LISTEN;

					if (player.stopPartnerDisabled) {
						displayText.add(new Pair<>("No, I'm coming with you.", Mob.NORMAL));
						hud.changeFace(player.getPartner());
						player.faceObject(player.getPartner());
						speak();
					} else if(player.getPartner().getState() == AIState.FOLLOWING) {
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
						player.getPartner().follow(player);
					}
				}else{
					String partner;
					if (player.getPartner().getGender().equals(Mob.MALE)) partner = "boyfriend";
					else partner = "girlfriend";
					displayText.add(new Pair<>("Your "+partner+" isn't here at the moment.", Mob.NORMAL));
					hud.changeFace(narrator);
					speak();
					stateType = InputState.LISTEN;
				}
			}
		} else {
			displayText.add(new Pair<>("You ain't got nobody to follow you!", Mob.NORMAL));
			hud.changeFace(narrator);
			speak();
			stateType = InputState.LISTEN;
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
			stateType = InputState.LOCKED;
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
	public void displayChoice(int[] types, String[] messages){
		final float h = 30 / PPM;
		float x, y, theta;
		int c = types.length;
		choices = new SpeechBubble[c];
		choiceIndex = 0;
		
		for (int i = 0; i < c; i++){
			PositionType positioning;
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
			choices[i] = new SpeechBubble(character, x, y, types[i], messages[i], positioning);
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
			
			music.fadeOut(Game.musicVolume*2f/3f);
		}
		
		//show text per character
		if (hud.moving == 0){
			if (speakTime >= speakDelay) {
				speakTime = 0;
				if(hud.raised) speakDelay = .020f;
				else speakDelay = 2f;

				if(speakText[sy].length()>0){
					char c = speakText[sy].charAt(sx);
					if (c == ".".charAt(0) || c == ",".charAt(0)|| c == "!".charAt(0)|| c == "?".charAt(0)) 
						speakDelay = .25f;
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
		
		prevStateType = stateType;
		stateType = InputState.PAUSED;
		
		menuOptions = new String[][] {{"Resume", "Stats", "Save Game", "Load Game", 
			"Options", "Quit to Menu", "Quit"}}; 
		
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
		
		if (music!=null) 
			music.fadeIn();
	}
	
	public void warp(){
		if(warped) return;
		
		random=false;
		destroyBodies();
		scene = nextScene;
		scene.create();
		initEntities();
		createPlayer(new Vector2(warp.getLink().x, warp.getLink().y+character.rh));
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
	public void setStateType(InputState type) {stateType = type; }
	public InputState getStateType(){ return stateType; }
	public SpeechBubble[] getChoices(){ return choices; }

	public void addSound(PositionalAudio s){ sounds.add(s); }
	public void addObject(Entity e){ 
		objects.add(e); 
		sortObjects();
	}
	public void addBodyToRemove(Body b){ bodiesToRemove.add(b); }
	
	public void createPlayer(Vector2 location){
		player.setGameState(this);
		player.create();
		character = player;
		
		//debug
//		character = new CamBot(0, 0);
////		cam.setLock(false);
//		b2dCam.setLock(false);
		cam.zoom = Camera.ZOOM_FAR;
		b2dCam.zoom = Camera.ZOOM_FAR;
		cam.zoom = Camera.ZOOM_NORMAL;
		b2dCam.zoom = Camera.ZOOM_NORMAL;
		
		character.setPosition(location);
		character.setGameState(this);
		character.create();
		cam.setCharacter(character); 
		b2dCam.setCharacter(character);
		objects.add(character);
		cam.locate(Vars.DT);
		sortObjects();
	}
	
	public void switchCharacter(){
		switchCharacter(player);
	}
	
	public void switchCharacter(Mob c){
		character = c;
		cam.setCharacter(character);
		b2dCam.setCharacter(character);
	}
	
	public void triggerScript(Script script){
		currentScript = script;
		
		if (analyzing) return;
		if (currentScript != null) {
			analyzing = true;
			for (Entity e : objects)
				if (e instanceof SpeechBubble)
					addBodyToRemove(e.getBody()); //remove talking speech bubble
			script.analyze();
			if (currentScript.limitDistance) {
				stateType = InputState.MOVELISTEN;
			} else {
				positionPlayer(character.getInteractable());
			}
		}
	}
	
	//create all enntites in scene
	private void initEntities() {
		removeAllObjects();
		objects.addAll(scene.getInitEntities());
		if(player.getPartner()!=null)
			if (player.getPartner().getName() != null) 
				objects.add(player.getPartner());

		for (Entity d : objects){
			d.setGameState(this);
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
		for (Entity e : objects) 
			if (e.getClass().equals(c.getClass())) 
				i++;
		return i;
	}

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
	
	public void printObjects() {
		for(Entity e:objects){
			debugText+="/l"+e.ID;
			System.out.println(e);
		}
	}

	public ArrayList<Entity> getObjects(){ return objects;	}
	
	public Color getColorOverlay(){
		// should be dependant on time;
		return new Color(2,2,2,Vars.ALPHA);
	}
	
	//class that contains minor handling for all events and flags
	public class History {

		private HashSet<Pair<String, String>> eventList;
		private HashMap<String, Boolean> flagList;
		private HashMap<String, Object> variableList;
		
		public History(){
			eventList = new HashSet<>();
			flagList = new HashMap<>();
			variableList = new HashMap<>();
			
			flagList.put("true",true);
			flagList.put("false",false);
			variableList.put("male", (Object)"male");
			variableList.put("female", (Object)"female");
		}
		
		public History(String loadedData){
			
		}
		
		public Boolean getFlag(String flag){ 
			for(String p : flagList.keySet())
				if (p.equals(flag))
					return flagList.get(p);
			return null;
		}
	
		//creates the flag if no flag found
		public void setFlag(String flag, boolean val){
			for(String p : flagList.keySet())
				if(p.equals(flag)){
					flagList.put(p, val);
					return;
				}
			addFlag(flag, val);
		}
		
		public boolean addFlag(String flag, boolean val){ return flagList.put(flag, val); 	}
		
		public boolean setEvent(String event, String description){ 
			for(Pair<String, String> p : eventList)
				if (p.getKey().equals(event))
					return false;
			eventList.add(new Pair<>(event, description)); 
			return true;
		}
		
		public boolean findEvent(String event){ 
			for(Pair<String, String> p : eventList)
				if (p.getKey().equals(event))
					return true;
			return false;
		}
		
		public String getDescription(String event){
			for(Pair<String, String> p : eventList)
				if (p.getKey().equals(event))
					return p.getValue();
			return null;
		}
		
		public boolean declareVariable(String variableName, Object value){
			if (value instanceof Boolean) return addFlag(variableName, (Boolean)value);
			for(String p : variableList.keySet())
				if (p.equals(variableName))
					return false;
			if (!(value instanceof String) && !(value instanceof Integer) && !(value instanceof Float))
				return false;
			
			variableList.put(variableName, value);
			return true;
		}
		
		public Object getVariable(String variableName){
			for(String p : variableList.keySet())
				if (p.equals(variableName))
					return variableList.get(p);
			return null;                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        
		}
	
		public void setVariable(String var, Object val){
			for(String p : variableList.keySet())
				if(p.equals(var)){
					String type = variableList.get(p).getClass().getSimpleName();
					if(!var.getClass().getSimpleName().equals(type)){
						try{
							if(type.toLowerCase().equals("float"))
								variableList.put(p, (float) val);
							if(type.toLowerCase().equals("integer"))
								variableList.put(p, (int) val);
							if(type.toLowerCase().equals("string"))
								variableList.put(p, (String) val);
						} catch (Exception e){ }
					} else 
						variableList.put(p, val);
				}
		}
		
		//for use in the history section in the stats window
		//only includes major events
		public Texture getEventIcon(String event/*, String descriptor*/){
			switch(event){
			case "BrokeAnOldLadyCurse": return Game.res.getTexture("girlfriend1face");
			case "MetTheBadWitch": return Game.res.getTexture("witchface");
			case "RobbedTwoGangsters": return Game.res.getTexture("gangster1face");
			case "FellFromNowhere": return Game.res.getTexture(player.ID+"base");
			case "FoundTheNarrator": return Game.res.getTexture("narrator1face");
			default:
				return null;
			}
		}
	}
}



