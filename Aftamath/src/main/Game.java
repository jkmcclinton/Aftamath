package main;

import static handlers.Vars.PPM;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Stack;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import box2dLight.Light;
import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.MyInput;
import handlers.MyInputProcessor;
import handlers.Pair;
import scenes.Song;

/*
 * Name: Game.java
 * Imports: handlers, Song
 * Use: 
 */
public class Game implements ApplicationListener {

	public static int inputIndex = 0;
	public static final String TITLE = "Aftamath";
	public static final int width = 864;
	public static final int height = 500;
	public static final int scale = 3;
	public static final float STEP = 1 / 60f;
	public static final int MAX_INPUT_LENGTH = 20;
	

	public static Assets res = new Assets();
	public static boolean hasControllers, fullscreen;
	public static final float maxVolume = 1;
	public static float musicVolume = maxVolume;
	public static float soundVolume = .7f;
	public Stack<Song> song;

	private FadingSpriteBatch sb;
	private Camera cam;
	private Camera b2dCam;
	private OrthographicCamera hudCam;
	private static String input = "";
	private GameStateManager gsm;


	public void create() {
//		Texture.setEnforcePotImages(false);
		MyInputProcessor iP = new MyInputProcessor();
		Gdx.input.setInputProcessor(iP);
		Controllers.addListener(iP);

		res.loadTextures();
		res.loadMusic();
		res.loadScriptList(Gdx.files.internal("assets/scripts"));
		res.loadLevelNames();
		res.loadEventToTexture();

		cam = new Camera();
		cam.setToOrtho(false, width/scale, height/scale);
		hudCam = new OrthographicCamera();
		hudCam.setToOrtho(false, width/2, height/2);
		b2dCam = new Camera();
		b2dCam.setToOrtho(false, width/scale/PPM, height/scale/PPM);
		sb = new FadingSpriteBatch();
		gsm = new GameStateManager(this);
		
        if(Controllers.getControllers().size == 0)
            hasControllers = false;
        else hasControllers = true;
	}

	public void render() {
		gsm.update(STEP);
		gsm.render();
		MyInput.update();
//		System.out.println("managed textures: "+Texture.getNumManagedTextures());
		
		 if (Gdx.input.isKeyPressed(Input.Keys.F5)) {
	            fullscreen = !fullscreen;
	            DisplayMode currentMode = Gdx.graphics.getDesktopDisplayMode();
	            if(fullscreen)
	            	Gdx.graphics.setDisplayMode(currentMode.width, currentMode.height, fullscreen);
	            else
	            	Gdx.graphics.setDisplayMode(width, height, fullscreen);
		 }
	}

	public void update() {}
	public void dispose() {}
	public void resize(int w, int h) {
		System.out.println("w: " + w + "\th: " + h);
	}

	public void pause() {
//		GameState gs = ((GameState) gsm.getStates().peek());
//		gs.pause();
	}

	public void resume() {
//		GameState gs = ((GameState) gsm.getStates().peek());
//		gs.resume();
	}

	public static String getInput(){ return input; }
	public static void resetInput(){ input = ""; inputIndex = 0; }
	public static void addInputChar(String s){
		if(input.length()<MAX_INPUT_LENGTH){
			if(inputIndex>0)
				if(inputIndex < input.length())
					input = input.substring(0, inputIndex) + s + input.substring(inputIndex);
				else
					input = input.substring(0, inputIndex) + s;

			else
				input = s + input;
			inputIndex++;
		} else {
//			Gdx.audio.newSound(new FileHandle("assets/sounds/invalid.wav"));
		}
	}

	public static void removeInputChar(){
		if(inputIndex>0){
			input = input.substring(0, inputIndex - 1) + input.substring(inputIndex);
			inputIndex--;
		}
	}
	
	/**
	 * Use this method to immediately halt the program and print out a stack trace
	 */
	@SuppressWarnings("null")
	public static void halt(){
		Light N = null;
		System.out.println(N.getX());
	}

	public static void setInput(String i){input = i; }

	public FadingSpriteBatch getSpriteBatch() { return sb; }
	public Camera getCamera() { return cam; }
	public OrthographicCamera getHudCamera() { return hudCam; }
	public Camera getB2DCamera() { return b2dCam; }

	public void setSong(Song song) {
		if(this.song==null)
			this.song = new Stack<>();
		this.song.clear();
		this.song.push(song);
	}

	//Contains all the titles for songs
	public static final Array<String> SONG_LIST = new Array<>();
	public static final Array<String> LEVEL_NAMES = new Array<>();
	public static final HashMap<String, String> SCRIPT_LIST = new HashMap<>();
	public static HashMap<String, Pair<String, Vector2>> EVENT_TO_TEXTURE = new HashMap<>();

	public static class Assets {

		private HashMap<String, String> textures;

		public Assets() {
			textures = new HashMap<>();
		}

		public void loadMusic(){
			SONG_LIST.add("Silence");
			FileHandle [] songs = Gdx.files.internal("assets/music").list();
			for(FileHandle f:songs)
				if(f.extension().equals("mp3")&&!f.name().contains("Intro"))
					SONG_LIST.add(f.nameWithoutExtension());
		}
		
		public void loadLevelNames(){
			//collect names for valid levels
			FileHandle [] files = Gdx.files.internal("assets/maps").list();
			for(FileHandle f:files)
				if(f.extension().equals("tmx")){
					String l = f.nameWithoutExtension();
					LEVEL_NAMES.add(l);
				}
		}
		
		public void loadScriptList(FileHandle begin){
			FileHandle[] newHandles = begin.list();
			for (FileHandle f : newHandles) {
				if (f.isDirectory())
					loadScriptList(f);
				else {
					if(f.extension().equals("amsc"))
						SCRIPT_LIST.put(f.nameWithoutExtension(), f.path());
				}
			}
		}

		public void loadTextures(){
			FileHandle src = Gdx.files.internal("assets/images");
			Array<FileHandle> handles =new Array<>();
			String key;

			loadTextures(src, handles);

			for(FileHandle f:handles){
				if(f.extension().equals("png")){
					key = f.nameWithoutExtension();
					textures.put(key, f.path());
				}
			}
		}

		private void loadTextures(FileHandle begin, Array<FileHandle> handles) {
			FileHandle[] newHandles = begin.list();
			for (FileHandle f : newHandles) {
				if (f.isDirectory())
					loadTextures(f, handles);
				else{
					handles.add(f);
				}
			}
		}
		public Texture getTexture(String key){
			String path = textures.get(key);
			if(path!=null){
				if(Thread.currentThread().getName().equals("Loader"))
					return null;
				return new Texture(Gdx.files.internal(path));
			} else {
//				System.out.println("Key \""+key+"\" is not a valid texture key");
				return null;
			}
		}
		
		public String getScript(String key){
			return SCRIPT_LIST.get(key);
		}
		
		/**
		 * for use in the history section in the journal window;
		 * only includes major events;
		 * format from file: "eventName/ltextureName/loffX/loffY"
		 */
		public void loadEventToTexture() {
			try {
				BufferedReader br = new BufferedReader(new FileReader("assets/eventToTexture.txt"));
				String line = br.readLine();
				String[] dat;
				
				while (line != null ) {
					dat = line.split("/l");
					if(getTexture(dat[1])!=null) {
						EVENT_TO_TEXTURE.put(dat[0], new Pair<>(dat[1], 
								new Vector2(Float.parseFloat(dat[2]), Float.parseFloat(dat[3]))));
//						System.out.println("entry: \t"+dat[0]+" :: "+EVENT_TO_TEXTURE.get(dat[0]));
					}
					line = br.readLine();
				}
				
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

//		public void disposeTexture(String key){
//			Texture tex = textures.get(key);
//			if (tex!= null) tex.dispose();
//		}
	}
}
