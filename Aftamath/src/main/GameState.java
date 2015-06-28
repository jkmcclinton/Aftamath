package main;

import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.Vars;
import main.Main.InputState;
import scenes.Song;

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
	public int choiceIndex, menuMaxY, menuMaxX;
	public int[] menuIndex = new int[2];
	public boolean tempSong, removingTemp;
	public String[][] menuOptions;
	
	protected GameStateManager gsm;
	protected Game game;
	protected Song music, nextSong, prevSong, tmp;
	protected static TextureRegion[] font;
	
	protected FadingSpriteBatch sb;
	protected Camera cam;
	protected Camera b2dCam;
	protected OrthographicCamera hudCam;
	protected float buttonTime, buttonDelay = .2f;
	protected boolean quitting = false, changingSong;
	
	protected static final float DELAY = .2f;
	protected static final int PERIODX = 7;
	protected static final int PERIODY = 9;
	
	static { 
		font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text3.png")), 7, 9 )[0];
	}
	
	protected GameState(GameStateManager gsm) {
		this.gsm = gsm;
		game = gsm.game();
		sb = game.getSpriteBatch();
		cam = game.getCamera();
		b2dCam = game.getB2DCamera();
		hudCam = game.getHudCamera();
	}
	
	public void update(float dt){
		music.update(dt);
		if(tempSong) prevSong.update(dt);
		if(removingTemp){
			tmp.update(dt);
			if(!prevSong.fading){
				tmp=null;
				removingTemp = false;
			}
		}
	}
	
	public void pause() {
		if (music!=null)
			music.fadeOut(false);
	}

	public void resume() {
		if(this instanceof Main){
			if (((Main)this).stateType != InputState.PAUSED)
				if (music!=null)
					music.fadeIn(music.prevVolume);
		} else if (music!=null)  
			music.fadeIn(music.prevVolume);
	}
	
	public void setSong(String src){
		setSong(new Song(src));
	}
	
	public void setSong(Song song){
		setSong(song, false);
	}
	
	public void setSong(Song song, boolean fade){
//		if(music!=null) this.music.stop();
		music = song.copy();
		
		game.setSong(music);
//		if(gsm.getStates().isEmpty())
			if(fade) {
				this.music.setVolume(0f);
				this.music.fadeIn();
			} else this.music.play();
//		else if (gsm.getStates().peekFirst().equals(this)){
//			song.setVolume(0);
//			song.play();
//	    }
	}
	
	public void addTempSong(Song song){
		music.fadeOut(false, Song.FAST);
		
		prevSong = music;
		setSong(song);
		tempSong = true;
		
		music.play();
	}
	
	public void removeTempSong(){
		tmp = music;
		tmp.fadeOut();
		tempSong = false;
		removingTemp = true;
		music = prevSong;
		
		music.fadeIn();
	}
	
	//UI sound
	public void playSound(String src){
		try{
			Music sound = Gdx.audio.newMusic(new FileHandle("res/sounds/"+src+".wav"));
			sound.play();
		} catch (Exception e){
			System.out.println("Sound file \""+src+"\" not found.");
		}
	}
	
	public void playSound(Vector2 position, String src){
		try{
			Music sound = Gdx.audio.newMusic(new FileHandle("res/sounds/"+src+".wav"));
			playSound(position, sound);
		} catch (Exception e){
			System.out.println("Sound file \""+src+"\" not found.");
		}
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
		
		sound.setPan(pan, volume);
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
	public abstract void render();
	public abstract void dispose();
	public abstract void create();
	public Song getSong() { return music; }
	public Song getPrevSong() {return prevSong;}
	public FadingSpriteBatch getSpriteBatch() { return sb; }
	public Camera getB2dCam(){ return b2dCam; }
	public GameStateManager getGSM(){return gsm; }
}
