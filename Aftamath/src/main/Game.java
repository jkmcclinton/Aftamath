package main;

import static handlers.Vars.PPM;
import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.MyInput;
import handlers.MyInputProcessor;

import java.util.HashMap;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;

public class Game implements ApplicationListener {

	public static final String TITLE = "Aftamath";
	public static final int width = 864;
	public static final int height = 500;
	public static final int scale = 1;
	public static final float STEP = 1 / 60f;
	public static final float defaultZoom = 3f;

	public static float musicVolume = 1f;
	public static float soundVolume = .75f;
	public Music song;

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

		loadImages();

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
		GameState gs = ((GameState) gsm.getStates().peek());
		gs.resume();
	}

	public FadingSpriteBatch getSpriteBatch() { return sb; }
	public Camera getCamera() { return cam; }
	public OrthographicCamera getHudCamera() { return hudCam; }
	public Camera getB2DCamera() { return b2dCam; }

	public void loadImages() {
		res.loadTexture("res/images/empty.png", "empty");
		
		//mobs
//		res.loadTexture("res/images/entities/mobs/narrator1.png", "narrator1");
//		res.loadTexture("res/images/entities/mobs/narrator1face.png", "narrator1face");
//		res.loadTexture("res/images/entities/mobs/narrator2.png", "narrator2");
//		res.loadTexture("res/images/entities/mobs/narrator2face.png", "narrator2face");
		res.loadTexture("res/images/entities/mobs/gangster1.png", "gangster1");
		res.loadTexture("res/images/entities/mobs/gangster1face.png", "gangster1face");
//		res.loadTexture("res/images/entities/mobs/gangster2.png", "gangster2");
//		res.loadTexture("res/images/entities/mobs/gangster2face.png", "gangster2face");
//		res.loadTexture("res/images/entities/mobs/gangster3.png", "gangster3");
//		res.loadTexture("res/images/entities/mobs/gangster3face.png", "gangster3face");
//		res.loadTexture("res/images/entities/mobs/gangster4.png", "gangster4");
//		res.loadTexture("res/images/entities/mobs/gangster4face.png", "gangster4face");
//		res.loadTexture("res/images/entities/mobs/boyfriend1.png", "boyfriend1");
//		res.loadTexture("res/images/entities/mobs/boyfriend1face.png", "boyfriend1face");
//		res.loadTexture("res/images/entities/mobs/boyfriend2.png", "boyfriend2");
//		res.loadTexture("res/images/entities/mobs/boyfriend3face.png", "boyfriend3face");
//		res.loadTexture("res/images/entities/mobs/boyfriend3.png", "boyfriend3");
//		res.loadTexture("res/images/entities/mobs/boyfriend3face.png", "boyfriend3face");
		res.loadTexture("res/images/entities/mobs/girlfriend1.png", "girlfriend1");
		res.loadTexture("res/images/entities/mobs/girlfriend1face.png", "girlfriend1face");
		res.loadTexture("res/images/entities/mobs/girlfriend2.png", "girlfriend2");
		res.loadTexture("res/images/entities/mobs/girlfriend2face.png", "girlfriend2face");
		res.loadTexture("res/images/entities/mobs/girlfriend3.png", "girlfriend3");
		res.loadTexture("res/images/entities/mobs/girlfriend3face.png", "girlfriend3face");
		res.loadTexture("res/images/entities/mobs/maleplayer1.png", "maleplayer1");
		res.loadTexture("res/images/entities/mobs/maleplayer1face.png", "maleplayer1face");
//		res.loadTexture("res/images/entities/mobs/maleplayer2.png", "maleplayer2");
//		res.loadTexture("res/images/entities/mobs/maleplayer3.png", "maleplayer3");
//		res.loadTexture("res/images/entities/mobs/maleplayer4.png", "maleplayer4");
//		res.loadTexture("res/images/entities/mobs/femaleplayer1.png", "femaleplayer1");
//		res.loadTexture("res/images/entities/mobs/femaleplayer2.png", "femaleplayer2");
//		res.loadTexture("res/images/entities/mobs/femaleplayer3.png", "femaleplayer3");
//		res.loadTexture("res/images/entities/mobs/femaleplayer4.png", "femaleplayer4");
		res.loadTexture("res/images/entities/mobs/richguy.png", "richguy");
		res.loadTexture("res/images/entities/mobs/richguyface.png", "richguyface");
		res.loadTexture("res/images/entities/mobs/reaper.png", "reaper");
		res.loadTexture("res/images/entities/mobs/reaperface.png", "reaperface");
		res.loadTexture("res/images/entities/mobs/bballer.png", "bballer");
		res.loadTexture("res/images/entities/mobs/bballerface.png", "bballerface");
		res.loadTexture("res/images/entities/mobs/boss1.png", "boss1");
		res.loadTexture("res/images/entities/mobs/boss1face.png", "boss1face");
//		res.loadTexture("res/images/entities/mobs/boss2.png", "boss2");
//		res.loadTexture("res/images/entities/mobs/boss2face.png", "boss2face");
		res.loadTexture("res/images/entities/mobs/burly1.png", "burly1");
		res.loadTexture("res/images/entities/mobs/burly1face.png", "burly1face");
//		res.loadTexture("res/images/entities/mobs/burly2.png", "burly2");
//		res.loadTexture("res/images/entities/mobs/burly2face.png", "burly2face");
		res.loadTexture("res/images/entities/mobs/cashier.png", "cashier");
		res.loadTexture("res/images/entities/mobs/cashierface.png", "cashierface");
//		res.loadTexture("res/images/entities/mobs/barista.png", "barista");
//		res.loadTexture("res/images/entities/mobs/baristaface.png", "baristaface");
		res.loadTexture("res/images/entities/mobs/hippie.png", "hippie");
		res.loadTexture("res/images/entities/mobs/hippieface.png", "hippieface");
		res.loadTexture("res/images/entities/mobs/logger.png", "logger");
		res.loadTexture("res/images/entities/mobs/loggerface.png", "loggerface");
//		res.loadTexture("res/images/entities/mobs/hero1.png", "hero1");
		res.loadTexture("res/images/entities/mobs/hero1face.png", "hero1face");
//		res.loadTexture("res/images/entities/mobs/hero2.png", "hero2");
//		res.loadTexture("res/images/entities/mobs/hero2face.png", "hero2face");
//		res.loadTexture("res/images/entities/mobs/hero3.png", "hero3");
//		res.loadTexture("res/images/entities/mobs/hero3face.png", "hero3face");
//		res.loadTexture("res/images/entities/mobs/hero4.png", "hero4");
//		res.loadTexture("res/images/entities/mobs/hero4face.png", "hero4face");
//		res.loadTexture("res/images/entities/mobs/witch.png", "witch");
		res.loadTexture("res/images/entities/mobs/witchface.png", "witchface");
//		res.loadTexture("res/images/entities/mobs/magician.png", "magician");
//		res.loadTexture("res/images/entities/mobs/magicianface.png", "magicianface");
		res.loadTexture("res/images/entities/mobs/oldlady1.png", "oldlady1");
		res.loadTexture("res/images/entities/mobs/oldlady1face.png", "oldlady1face");
//		res.loadTexture("res/images/entities/mobs/oldlady2.png", "oldlady2");
//		res.loadTexture("res/images/entities/mobs/oldlady2face.png", "oldlady2face");
//		res.loadTexture("res/images/entities/mobs/oldman1.png", "oldman1");
//		res.loadTexture("res/images/entities/mobs/oldman1face.png", "oldman1face");
//		res.loadTexture("res/images/entities/mobs/oldman2.png", "oldman2");
//		res.loadTexture("res/images/entities/mobs/oldman2face.png", "oldman2face");
//		res.loadTexture("res/images/entities/mobs/biker1.png", "biker1");
//		res.loadTexture("res/images/entities/mobs/biker1face.png", "biker1face");
//		res.loadTexture("res/images/entities/mobs/biker2.png", "biker2");
//		res.loadTexture("res/images/entities/mobs/biker2face.png", "biker2face");
//		res.loadTexture("res/images/entities/mobs/policeman1.png", "policeman1");
//		res.loadTexture("res/images/entities/mobs/policeman1face.png", "policeman1face");
//		res.loadTexture("res/images/entities/mobs/policeman2.png", "policeman2");
//		res.loadTexture("res/images/entities/mobs/policeman2face.png", "policeman2face");
//		res.loadTexture("res/images/entities/mobs/policewoman.png", "policewoman");
//		res.loadTexture("res/images/entities/mobs/policewomanface.png", "policewomanface");
//		res.loadTexture("res/images/entities/mobs/bot1.png", "bot1");
//		res.loadTexture("res/images/entities/mobs/bot1.png", "bot1face");
//		res.loadTexture("res/images/entities/mobs/bot2.png", "bot2");
//		res.loadTexture("res/images/entities/mobs/bot2face.png", "bot2face");
//		res.loadTexture("res/images/entities/mobs/civilian1.png", "civilian1");
//		res.loadTexture("res/images/entities/mobs/civilian2.png", "civilian2");
//		res.loadTexture("res/images/entities/mobs/civilian3.png", "civilian3");
//		res.loadTexture("res/images/entities/mobs/civilian4.png", "civilian4");
//		res.loadTexture("res/images/entities/mobs/civilian5.png", "civilian5");
//		res.loadTexture("res/images/entities/mobs/civilian6.png", "civilian6");
//		res.loadTexture("res/images/entities/mobs/civilian7.png", "civilian7");
//		res.loadTexture("res/images/entities/mobs/civilian8.png", "civilian8");
//		res.loadTexture("res/images/entities/mobs/kid1.png", "kid1");
//		res.loadTexture("res/images/entities/mobs/kid2.png", "kid2");
//		res.loadTexture("res/images/entities/mobs/kid3.png", "kid3");
//		res.loadTexture("res/images/entities/mobs/kid4.png", "kid4");

		//projectiles
		res.loadTexture("res/images/entities/projectiles/fireball.png", "fireball");

		//particles
//		res.loadTexture("res/images/entities/particles/.png", "");

		//gui
		res.loadTexture("res/images/hudTextures.png", "hudTextures");
		res.loadTexture("res/images/entities/speechBubbles.png", "speechBubble");
		res.loadTexture("res/images/entities/arrow.png", "arrow");
	}

	public void setSong(Music song) {
		this.song = song;
	}

	public static class Assets {

		private HashMap<String, Texture> textures;
		
		public Assets() {
			textures = new HashMap<>();
		}
		
		public void loadTexture(String path, String key){
			Texture tex = new Texture(Gdx.files.internal(path));
			textures.put(key, tex);
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
