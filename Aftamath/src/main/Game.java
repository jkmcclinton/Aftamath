package main;

import static handlers.Vars.PPM;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;

import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.MyInput;
import handlers.MyInputProcessor;
import scenes.Scene;
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
	public static final int scale = 1;
	public static final float STEP = 1 / 60f;
	public static final float DEFAULT_ZOOM = 3f;
	public static final int MAX_INPUT_LENGTH = 20;

	public static Assets res = new Assets();
	public static float musicVolume = 1f;
	public static float soundVolume = .75f;
	public Stack<Song> song;

	private FadingSpriteBatch sb;
	private Camera cam;
	private Camera b2dCam;
	private OrthographicCamera hudCam;
	private static float zoom = 3;
	private static String input = "";
	private GameStateManager gsm;


	public void create() {
//		Texture.setEnforcePotImages(false);
		Gdx.input.setInputProcessor(new MyInputProcessor());

		res.loadTextures();
		res.loadMusic();
		res.loadScriptList(Gdx.files.internal("assets/scripts"));
		res.loadLevelNames();

		cam = new Camera();
		cam.setToOrtho(false, width/zoom, height/zoom);
		hudCam = new OrthographicCamera();
		hudCam.setToOrtho(false, width/2, height/2);
		b2dCam = new Camera();
		b2dCam.setToOrtho(false, width/3/PPM, height/3/PPM);
		sb = new FadingSpriteBatch();
		gsm = new GameStateManager(this);
	}

	public void render() {
		gsm.update(STEP);
		gsm.render();
		MyInput.update();
//		System.out.println("managed textures: "+Texture.getNumManagedTextures());
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

	public static class Assets {

		private HashMap<String, Texture> textures;

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
					if (!Scene.sceneToEntityIds.containsKey(l))
						Scene.sceneToEntityIds.put(l, new HashSet<Integer>());
				}
		}
		
		public void loadScriptList(FileHandle begin){
			FileHandle[] newHandles = begin.list();
			for (FileHandle f : newHandles) {
				if (f.isDirectory())
					loadScriptList(f);
				else {
					if(f.extension().equals("txt"))
						SCRIPT_LIST.put(f.nameWithoutExtension(), f.path());
				}
			}
		}

		public void loadTextures(){
			FileHandle src = Gdx.files.internal("assets/images");
			Array<FileHandle> handles =new Array<>();
			Texture tex;
			String key;

			loadTextures(src, handles);

			for(FileHandle f:handles){
				if(f.extension().equals("png")){
					key = f.nameWithoutExtension();
					tex =  new Texture(Gdx.files.internal(f.path()));
//System.out.println(key);
					textures.put(key, tex);
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
			return textures.get(key);
		}
		
		public String getScript(String key){
			return SCRIPT_LIST.get(key);
		}

		public void disposeTexture(String key){
			Texture tex = textures.get(key);
			if (tex!= null) tex.dispose();
		}
	}
}
