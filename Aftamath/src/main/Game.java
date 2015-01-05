package main;

import static handlers.Vars.PPM;
import handlers.Assets;
import handlers.BoundedCamera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.MyInput;
import handlers.MyInputProcessor;
import states.GameState;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Game implements ApplicationListener {

	public static final String TITLE = "Aftamath";
	public static final int width = 864;
	public static final int height = 500;
	public static final int scale = 1;
	public static final float STEP = 1 / 60f;
	public static final float defaultZoom = 3f;

	public static float volume = .33f;
	public Music song;

	private FadingSpriteBatch sb;
	private BoundedCamera cam;
	private BoundedCamera b2dCam;
	private OrthographicCamera hudCam;
	private static float zoom = 3;

	private GameStateManager gsm;

	public static Assets res = new Assets();

	public void create() {
		//Texture.setEnforcePotImages(false);
		Gdx.input.setInputProcessor(new MyInputProcessor());

		loadImages();

		cam = new BoundedCamera();
		cam.setToOrtho(false, width/zoom, height/zoom);
		hudCam = new OrthographicCamera();
		hudCam.setToOrtho(false, width/2, height/2);
		b2dCam = new BoundedCamera();
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
		GameState gs = ((GameState) gsm.getStates().peek());
		gs.pause();
	}

	public void resume() {
		GameState gs = ((GameState) gsm.getStates().peek());
		gs.resume();
	}

	public FadingSpriteBatch getSpriteBatch() { return sb; }
	public BoundedCamera getCamera() { return cam; }
	public OrthographicCamera getHudCamera() { return hudCam; }
	public BoundedCamera getB2DCamera() { return b2dCam; }

	public void loadImages() {
		//mobs
		res.loadTexture("res/images/entities/mobs/gangster1.png", "gangster1");
		res.loadTexture("res/images/entities/mobs/gangster1face.png", "gangster1face");
//		res.loadTexture("res/images/entities/mobs/gangster2.png", "gangster2");
//		res.loadTexture("res/images/entities/mobs/boyfriend1.png", "boyfriend1");
//		res.loadTexture("res/images/entities/mobs/boyfriend1face.png", "boyfriend1face");
//		res.loadTexture("res/images/entities/mobs/boyfriend2.png", "boyfriend2");
//		res.loadTexture("res/images/entities/mobs/boyfriend3face.png", "boyfriend3face");
//		res.loadTexture("res/images/entities/mobs/boyfriend3.png", "boyfriend3");
//		res.loadTexture("res/images/entities/mobs/boyfriend3face.png", "boyfriend3face");
		res.loadTexture("res/images/entities/mobs/girlfriend1.png", "girlfriend1");
		res.loadTexture("res/images/entities/mobs/girlfriend1face.png", "girlfriend1face");
//		res.loadTexture("res/images/entities/mobs/girlfriend2.png", "girlfriend2");
//		res.loadTexture("res/images/entities/mobs/girlfriend2face.png", "girlfriend2face");
//		res.loadTexture("res/images/entities/mobs/girlfriend3.png", "girlfriend3");
		res.loadTexture("res/images/entities/mobs/girlfriend3face.png", "girlfriend3face");
		res.loadTexture("res/images/entities/mobs/maleplayer1.png", "maleplayer1");
		res.loadTexture("res/images/entities/mobs/maleplayer1face.png", "maleplayer1face");
//		res.loadTexture("res/images/entities/mobs/maleplayer2.png", "maleplayer2");
//		res.loadTexture("res/images/entities/mobs/maleplayer2face.png", "maleplayer2face");
//		res.loadTexture("res/images/entities/mobs/maleplayer3.png", "maleplayer3");
//		res.loadTexture("res/images/entities/mobs/maleplayer3face.png", "maleplayer3face");
//		res.loadTexture("res/images/entities/mobs/maleplayer4.png", "maleplayer4");
//		res.loadTexture("res/images/entities/mobs/maleplayer4face.png", "maleplayer4face");
//		res.loadTexture("res/images/entities/mobs/femaleplayer1.png", "femaleplayer1");
//		res.loadTexture("res/images/entities/mobs/femaleplayer1face.png", "femaleplayer1face");
//		res.loadTexture("res/images/entities/mobs/femaleplayer2.png", "femaleplayer2");
//		res.loadTexture("res/images/entities/mobs/femaleplayer2face.png", "femaleplayer2face");
//		res.loadTexture("res/images/entities/mobs/femaleplayer3.png", "femaleplayer3");
//		res.loadTexture("res/images/entities/mobs/femaleplayer3face.png", "femaleplayer3face");
//		res.loadTexture("res/images/entities/mobs/femaleplayer4.png", "femaleplayer4");
//		res.loadTexture("res/images/entities/mobs/femaleplayer4face.png", "femaleplayer4face");
		res.loadTexture("res/images/entities/mobs/richguy.png", "richguy");
		res.loadTexture("res/images/entities/mobs/richguyface.png", "richguyface");
		res.loadTexture("res/images/entities/mobs/reaper.png", "reaper");
		res.loadTexture("res/images/entities/mobs/reaperface.png", "reaperface");
//		res.loadTexture("res/images/entities/mobs/bballer.png", "bballer");
		res.loadTexture("res/images/entities/mobs/bballerface.png", "bballerface");
		res.loadTexture("res/images/entities/mobs/boss.png", "boss");
		res.loadTexture("res/images/entities/mobs/bossface.png", "bossface");
		res.loadTexture("res/images/entities/mobs/burly.png", "burly");
		res.loadTexture("res/images/entities/mobs/burlyface.png", "burlyface");
//		res.loadTexture("res/images/entities/mobs/cashier.png", "cashier");
		res.loadTexture("res/images/entities/mobs/cashierface.png", "cashierface");
//		res.loadTexture("res/images/entities/mobs/hippie.png", "hippie");
		res.loadTexture("res/images/entities/mobs/hippieface.png", "hippieface");
//		res.loadTexture("res/images/entities/mobs/logger.png", "logger");
		res.loadTexture("res/images/entities/mobs/loggerface.png", "loggerface");
		res.loadTexture("res/images/entities/mobs/oldlady.png", "oldlady");
		res.loadTexture("res/images/entities/mobs/oldladyface.png", "oldladyface");
//		res.loadTexture("res/images/entities/mobs/quantumguy.png", "quantumguy");
		res.loadTexture("res/images/entities/mobs/quantumguyface.png", "quantumguyface");
//		res.loadTexture("res/images/entities/mobs/witch.png", "witch");
		res.loadTexture("res/images/entities/mobs/witchface.png", "witchface");
//		res.loadTexture("res/images/entities/mobs/magician.png", "magician");
//		res.loadTexture("res/images/entities/mobs/magicianface.png", "magicianface");
//		res.loadTexture("res/images/entities/mobs/oldman.png", "oldman");
//		res.loadTexture("res/images/entities/mobs/oldmanface.png", "oldmanface");
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
//		res.loadTexture("res/images/entities/mobs/kid1.png", "kid1");
//		res.loadTexture("res/images/entities/mobs/kid2.png", "kid2");
//		res.loadTexture("res/images/entities/mobs/kid3.png", "kid3");
//		res.loadTexture("res/images/entities/mobs/kid4.png", "kid4");

		//projectiles
		res.loadTexture("res/images/entities/projectiles/fireball.png", "fireball");

		//particles

		//gui
		res.loadTexture("res/images/hudTextures.png", "hudTextures");
		res.loadTexture("res/images/entities/speechBubbles.png", "speechBubble");
	}

	public void setSong(Music song) {
		this.song = song;
	}

}
