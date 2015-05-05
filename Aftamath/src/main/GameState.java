package main;

import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.Vars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public abstract class GameState {

	public static String debugText = "";
	
	protected GameStateManager gsm;
	protected Game game;
	protected Music song, nextSong;
	protected static TextureRegion[] font;
	
	static { 
		font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text3.png")), 7, 9 )[0];
	}
	
	protected FadingSpriteBatch sb;
	protected Camera cam;
	protected Camera b2dCam;
	protected OrthographicCamera hudCam;
	
	public int choiceIndex, menuMaxY, menuMaxX;
	public int[] menuIndex = new int[2];
	public String[][] menuOptions;
	protected float buttonTime, buttonDelay = .2f;
	protected boolean quitting = false, changingSong;
	
	protected static final float DELAY = .2f;
	protected static final int PERIODX = 7;
	protected static final int PERIODY = 9;
	
	protected GameState(GameStateManager gsm) {
		this.gsm = gsm;
		game = gsm.game();
		sb = game.getSpriteBatch();
		cam = game.getCamera();
		b2dCam = game.getB2DCamera();
		hudCam = game.getHudCamera();
	}
	
	public void pause() {
		if (song != null) 
			if (song.isPlaying()) 
				song.pause();
	}

	public void resume() {
		if(this instanceof Play){
			if (((Play)this).stateType != Play.PAUSED)
				if (song != null)
				song.play();
		} else if (song != null)  
			song.play();
	}
	
	public void setSong(String src){
		setSong(Gdx.audio.newMusic(new FileHandle("res/music/"+src+".wav")));
	}
	
	public void setSong(Music song){
		setSong(song, Game.musicVolume);
	}
	
	public void setSong(Music song, float volume){
		this.song = song;
		game.setSong(song);
		
		song.setLooping(true);
//		if(gsm.getStates().isEmpty())
			playSong(song, volume);
//		else if (gsm.getStates().peekFirst().equals(this))
//			playSong(song, 0);
	}
	
	private void playSong(Music song, float volume){
		song.setVolume(volume);
		song.play();
	}
	
	//UI sound
	public void playSound(String src){
		Music sound = Gdx.audio.newMusic(new FileHandle("res/sounds/"+src+".wav"));
		sound.play();
	}
	
	public void playSound(Vector2 position, String src){
		Music sound = Gdx.audio.newMusic(new FileHandle("res/sounds/"+src+".wav"));
		playSound(position, sound);
	}

	public void playSound(float x, float y, Music s) {
		playSound(new Vector2(x,y), s);
	}
	
	public void playSound(Vector2 position, Music sound){
		updateSound(position, sound);
		sound.play();
	}
	
	public void updateSound(Vector2 position, Music sound){
		position = new Vector2 (position.x*Vars.PPM, position.y*Vars.PPM);
		float dx = cam.position.x - position.x;
		float dy = cam.position.y - cam.YOFFSET - position.y;
		float distance = (float) Math.sqrt(dx*dx + dy*dy);
		
		float pan;
		float s = 21400, a = .3183f, f = 5;
		float volume = (float) (Math.exp(-1*(distance*distance)/(2*s))/   //Gaussian curve
				(Math.sqrt((a*Math.PI)/(Game.soundVolume*Game.soundVolume))));
		if(Math.abs(dx)>f)
			pan = (float) -(2/(1+Math.exp(-(dx-f*dx/Math.abs(dx))/3.5)) - 1);      		  //Logaritmic curve
		else 
			pan = 0;
		
//		System.out.println("pan "+pan+" : dx "+dx+" : volume "+volume+" : distance "+distance);
		sound.setPan(pan, volume);
	}
	
	public void fadeSong(float dt, boolean spritebatch){
		float volume = song.getVolume();
		volume += dt * sb.getFadeType();
		if (volume > Game.musicVolume)
			volume = Game.musicVolume;
		if (volume < 0){
			volume = 0;
			if(song.isPlaying()) {
				song.stop();
				song.dispose();
			}
		}

		if(song.isPlaying())
			song.setVolume(volume);
	}
	
	public boolean fadeOutSong(float dt){
		float volume = song.getVolume();
		volume -= dt;
		if (volume > Game.musicVolume)
			volume = Game.musicVolume;
		if (volume < 0){
			volume = 0;
			if(song.isPlaying()) song.stop();
			return true;
		}
		
		if(song.isPlaying())
			song.setVolume(volume);
		return false;
	}
	
	public void loadGame(){
		
	}
	
	public void options() {
		//display options menu
	}
	
	public void back() {
		//return to previous menu
		
		//menus.pop();
	}
	
	public void quit() {
		if(!quitting){
			quitting = true;
			sb.fade();
		}
		
		if (sb.getFadeType() == FadingSpriteBatch.FADE_IN)
			Gdx.app.exit();
	}
	
	public static void drawString(SpriteBatch sb, String text, float x, float y) {
		drawString(sb, font, font[0].getRegionWidth(), text, x, y);
	}
	
	//draw entire string at location
	public static void drawString(SpriteBatch sb, TextureRegion[] font, int px, String text, float x, float y) {
		String[] lines = text.split("/l");
		
		for (int j = 0; j < lines.length; j++){
			for (int i = 0; i < lines[j].length(); i++){
				char c = lines[j].charAt(i);
				try{
					if (c != " ".charAt(0)) sb.draw(font[c + Vars.FONT_OFFSET], x + i * px, y + j * -font[0].getRegionHeight());
				} catch(Exception e) {}
			}
		}
	}
	
	public abstract void handleInput();
	public abstract void update(float dt);
	public abstract void render();
	public abstract void dispose();
	public abstract void create();
	public Music getSong() { return song; }
	public FadingSpriteBatch getSpriteBatch() { return sb; }
	public Camera getB2dCam(){ return b2dCam; }
	public GameStateManager getGSM(){return gsm; }
}
