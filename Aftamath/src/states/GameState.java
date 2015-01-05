package states;

import handlers.BoundedCamera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.Vars;
import main.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public abstract class GameState {

	public static String debugText = "";
	
	protected GameStateManager gsm;
	protected Game game;
	protected Music song;
	protected static TextureRegion[] font;
	
	static { 
		font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text3.png")), 7, 9 )[0];
	}
	
	protected FadingSpriteBatch sb;
	protected BoundedCamera cam;
	protected BoundedCamera b2dCam;
	protected OrthographicCamera hudCam;
	
	public int choiceIndex, menuMaxY, menuMaxX;
	public int[] menuIndex = new int[2];
	public String[][] menuOptions;
	protected float buttonTime, buttonDelay = .2f;
	protected boolean quitting = false;
	
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
		if (song != null)  
			song.play();
	}
	
	public void setSong(Music song){
		setSong(song, Game.volume);
	}
	
	public void setSong(Music song, float volume){
		this.song = song;
		game.setSong(song);
		
		song.setLooping(true);
		if(gsm.getStates().isEmpty())
			playSong(song, volume);
		else if (gsm.getStates().peekFirst().equals(this))
			playSong(song, 0);
	}
	
	private void playSong(Music song, float volume){
		song.setVolume(volume);
		song.play();
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
	
	public static void drawString(SpriteBatch sb, String text, int x, int y) {
		drawString(sb, font, font[0].getRegionWidth(), text, x, y);
	}
	
	//draw entire string at location
	public static void drawString(SpriteBatch sb, TextureRegion[] font, int px, String text, int x, int y) {
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

	
}
