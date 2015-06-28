package main;

import static handlers.Vars.PPM;
import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.MyInput;
import handlers.MyInputProcessor;

import java.util.HashMap;
import java.util.Stack;

import scenes.Song;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;

public class Game implements ApplicationListener {

	public static final String TITLE = "Aftamath";
	public static final int width = 864;
	public static final int height = 500;
	public static final int scale = 1;
	public static final float STEP = 1 / 60f;
	public static final float defaultZoom = 3f;

	public static float musicVolume = 1f;
	public static float soundVolume = .75f;
	public Stack<Song> song;

	private FadingSpriteBatch sb;
	private Camera cam;
	private Camera b2dCam;
	private OrthographicCamera hudCam;
	private static float zoom = 3;

	private GameStateManager gsm;

	public static Assets res = new Assets();

	public void create() {
		//Texture.setEnforcePotImages(false);
		Gdx.input.setInputProcessor(new MyInputProcessor());

		res.loadTextures();
		res.loadMusic();

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
	public static final Array<String> SONG_LIST = new Array<String>();

	public static class Assets {

		private HashMap<String, Texture> textures;
		
		public Assets() {
			textures = new HashMap<>();
		}
		
		public void loadMusic(){
			SONG_LIST.add("Silence");
			FileHandle [] songs = Gdx.files.internal("res/music").list();
			for(FileHandle f:songs)
				if(f.extension().equals("wav")&&!f.name().contains("Intro"))
					SONG_LIST.add(f.nameWithoutExtension());
		}
		
		public void loadTextures(){
			FileHandle src = Gdx.files.internal("res/images");
			Array<FileHandle> handles =new Array<>();
			Texture tex;
			String key;
			
			loadTextures(src, handles);
			
			for(FileHandle f:handles){
				if(f.extension().equals("png")){
					key = f.nameWithoutExtension();
					tex =  new Texture(Gdx.files.internal(f.path()));
					
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
		
		public void disposeTexture(String key){
			Texture tex = textures.get(key);
			if (tex!= null) tex.dispose();
		}
	}
}
