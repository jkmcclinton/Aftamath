package main;

import java.util.Stack;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;

import handlers.Camera;
import handlers.FadingSpriteBatch;
import handlers.GameStateManager;
import handlers.JsonSerializer;
import handlers.MyInput;
import handlers.MyInputProcessor;
import handlers.MyInput.Input;
import handlers.Vars;
import main.Main.InputState;
import main.Menu.MenuType;
import scenes.Song;

/*
 * Name: GameState.java
 * Imports: handlers, Main, Song
 * Use: 
 */
public abstract class GameState {

	public static String debugText = "";
	public int choiceIndex;
	public Vector2 cursor = new Vector2(0, 0);
	public boolean tempSong;
	public String[][] menuOptions;
	public String prevLoc;

	protected GameStateManager gsm;
	protected Game game;
	protected Stack<Menu> menus;
	protected Song music, nextSong, prevSong, tmp;
	protected static TextureRegion[] font;

	protected FrameBuffer lightBuffer;
	protected TextureRegion lightBufferRegion;
	protected FadingSpriteBatch sb;
	protected Camera cam;
	protected Camera b2dCam;
	protected OrthographicCamera hudCam;
	protected float buttonTime, buttonDelay = .2f;
	protected boolean quitting = false, changingSong;
	protected MenuType journalTab = MenuType.MAP;
	protected Array<Song> songsToKill;

	protected static final float DELAY = .2f;
	protected static final int PERIODX = 7;
	protected static final int PERIODY = 9;

	static {
		font = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/UI/text3.png")), 7, 9)[0];
	}

	/*
	 * Initialized GameState class with constants listed as below
	 */
	protected GameState(GameStateManager gsm) {
		this.gsm = gsm;
		game = gsm.game();
		sb = game.getSpriteBatch();
		cam = game.getCamera();
		b2dCam = game.getB2DCamera();
		hudCam = game.getHudCamera();
		songsToKill = new Array<>();
		menus = new Stack<>();
	}

	/*
	 * Updates the GameState including: music, restarting songs, songs to
	 * remove/kill
	 */
	public void update(float dt) {
		music.update(dt);
		if (tempSong)
			prevSong.update(dt);

		Array<Song> rmv = new Array<>();
		for (Song s : songsToKill) {
			s.update(dt);
			if (!s.fading)
				rmv.add(s);
		}
		songsToKill.removeAll(rmv, false);
		
		if(!menus.isEmpty())
			menus.peek().update(dt);
	}

	/*
	 * Pauses the GameState by only not fading out the music
	 */
	public void pause() {
		if (music != null)
			music.fadeOut(false);
	}

	/*
	 * Resumes the GameState by fading the music in to the previous volume
	 */
	public void resume() {
		if (this instanceof Main) {
			if (((Main) this).stateType != InputState.PAUSED)
				if (music != null)
					music.fadeIn(music.prevVolume);
		} else if (music != null)
			music.fadeIn(music.prevVolume);
	}

	/*
	 * Sets the song to the parameter (given a string)
	 */
	public void setSong(String src) {
		setSong(new Song(src));
	}

	/*
	 * Sets the song to the parameter (given a song)
	 */
	public void setSong(Song song) {
		setSong(song, false);
	}

	/*
	 * Sets the song to the parameter as well as setting if it will fade
	 */
	public void setSong(Song song, boolean fade) {
		music = song.copy();

		game.setSong(music);
		if (fade) {
			this.music.setVolume(0f);
			this.music.fadeIn();
		} else
			this.music.play();
	}

	/*
	 * adds a temporary song
	 */
	public void addTempSong(Song song) {
		music.fadeOut(false, Song.FAST);

		prevSong = music;
		setSong(song);
		tempSong = true;

		music.play();
	}

	public void removeTempSong() {
		tmp = music;
		tmp.fadeOut();
		songsToKill.add(tmp);
		tempSong = false;
		music = prevSong;

		music.fadeIn();
	}
	
	public void resize(){
		// Fakedlight system (alpha blending)
		// if lightBuffer was created before, dispose, we recreate a new one
		if (lightBuffer!=null) lightBuffer.dispose();
		lightBuffer = new FrameBuffer(Format.RGBA8888, Vars.PowerOf2(Game.width), Vars.PowerOf2(Game.height), false);
		lightBuffer.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		lightBufferRegion = new TextureRegion(lightBuffer.getColorBufferTexture(),0,lightBuffer.getHeight()-Game.height,Game.width,Game.height);
		lightBufferRegion.flip(false, false);
	}

	// UI sound
	public void playSound(String src) {
		try {
			Music sound = Gdx.audio.newMusic(new FileHandle("assets/sounds/" + src + ".wav"));
			sound.setVolume(Game.soundVolume);
			sound.play();
		} catch (Exception e) {
			System.out.println("Sound file \"" + src + "\" not found.");
		}
	}

	public void playSound(Vector2 position, String src) {
		try {
			Music sound = Gdx.audio.newMusic(new FileHandle("assets/sounds/" + src + ".wav"));
			playSound(position, sound);
		} catch (Exception e) {
			System.out.println("Sound file \"" + src + "\" not found.");
		}
	}

	public void playSound(Vector2 position, String src, float pitch) {
		try {
			Sound sound = Gdx.audio.newSound(new FileHandle("assets/sounds/" + src + ".wav"));
			playSound(position, sound, pitch);
		} catch (Exception e) {
			System.out.println("Sound file \"" + src + "\" not found.");
		}
	}

	/**units in meters*/
	public void playSound(float x, float y, Music s) {
		playSound(new Vector2(x, y), s);
	}

	/**@param position units in meters*/
	public void playSound(Vector2 position, Music sound) {
		updateSound(position, sound);
		sound.play();
	}

	/**@param position units in meters*/
	public void playSound(Vector2 position, Sound sound, float pitch) {
		position = new Vector2(position.x * Vars.PPM, position.y * Vars.PPM);
		float dx = cam.position.x - position.x;
		float dy = cam.position.y - cam.yOff - position.y;
		float distance = (float) Math.sqrt(dx * dx + dy * dy);

		float pan;
		float s = 21400, a = .3183f, f = 5;
		float volume = (float) (Math.exp(-1 * (distance * distance) / (2 * s)) / // Gaussian
																				 // curve
				(Math.sqrt((a * Math.PI) / (Game.soundVolume * Game.soundVolume))));
		if (Math.abs(dx) > f)
			pan = (float) -(2 / (1 + Math.exp(-(dx - f * dx / Math.abs(dx)) / 3.5)) - 1); // Logaritmic
																						  // curve
		else
			pan = 0;

		sound.play(volume, pitch, pan);
	}

	public void updateSound(Vector2 position, Music sound) {
		position = new Vector2(position.x * Vars.PPM, position.y * Vars.PPM);
		float dx = cam.position.x - position.x;
		float dy = cam.position.y - cam.yOff - position.y;
		float distance = (float) Math.sqrt(dx * dx + dy * dy);

		float pan;
		float s = 21400, a = .3183f, f = 5;
		float volume = (float) (Math.exp(-1 * (distance * distance) / (2 * s)) / // Gaussian
																				 // curve
				(Math.sqrt((a * Math.PI) / (Game.soundVolume * Game.soundVolume))));
		if (Math.abs(dx) > f)
			pan = (float) -(2 / (1 + Math.exp(-(dx - f * dx / Math.abs(dx)) / 3.5)) - 1); // Logaritmic
																						  // curve
		else
			pan = 0;

		sound.setPan(pan, volume);
	}

	public void drawString(SpriteBatch sb, String text, float x, float y) {
		drawString(sb, font, font[0].getRegionWidth(), text, x, y);
	}

	/**
	 * draw entire string at location
	 * 
	 * @param sb  object necessary to draw to screen
	 * @param font array of font images
	 * @param px  size of character inerval
	 * @param text  the text to be displaed
	 * @param x location in x direction
	 * @param y location in y direction
	 */
	public void drawString(SpriteBatch sb, TextureRegion[] font, int px, String text, float x, float y) {
		String[] lines = text.split("/l");

		for (int j = 0; j < lines.length; j++) {
//			int o = 0;
			for (int i = 0; i < lines[j].length(); i++) {
				char c = lines[j].charAt(i);
				try {
//					if(c == 'i') o -=3;
//					if(c == 'l' || c == 'r' || c == 'T' || c == 'l' || c == 'I') o -=1;
					if (c != " ".charAt(0))
						sb.draw(font[c + Vars.FONT_OFFSET], x + 0 + i * px, y + j * -font[0].getRegionHeight());
				} catch (Exception e) { }
			}
		}
	}

	/**
	 * draw entire string at location with a scaling factor
	 * 
	 * @param sb  object necessary to draw to screen
	 * @param font  array of font images
	 * @param px size of character inerval
	 * @param scale  scaling size of the font;
	 * @param text the text to be displaed
	 * @param x location in x direction
	 * @param y location in y direction
	 */
	public void drawString(SpriteBatch sb, TextureRegion[] font, int px, float scale, String text, float x, float y) {
		String[] lines = text.split("/l");
		int width = font[0].getRegionWidth();
		int height = font[0].getRegionHeight();
		px++;

		for (int j = 0; j < lines.length; j++) {
			for (int i = 0; i < lines[j].length(); i++) {
				char c = lines[j].charAt(i);
				try {
					if (c != " ".charAt(0))
						sb.draw(font[c + Vars.FONT_OFFSET], x + i * px * scale, y + j * -height * scale, width * scale,
								height * scale);
				} catch (Exception e) {
				}
			}
		}
	}

	public abstract void handleInput();
	public abstract void render();
	public abstract void dispose();
	public void renderLighting(FadingSpriteBatch sb){}
	public void create(){ 
		sb.setGameState(this);
	}
	
	public void traverseMenu(){
		if(MyInput.isPressed(Input.JUMP)|| MyInput.isPressed(Input.ENTER) && !gsm.isFading()){
			menus.peek().onClick(cursor);
		}
		if(MyInput.isPressed(Input.PAUSE)) back();
		if(menus.isEmpty()) return;
		
		// following four functions navigate through the pause menu via keyboard
		if(buttonTime >= DELAY){
			Vector2 prev = cursor;
			if(MyInput.isDown(Input.DOWN)){
				cursor = menus.peek().getNextObj(cursor, 3);
//				System.out.println(menus.peek().getObj(cursor));
				buttonTime = 0;
			}

			if(MyInput.isDown(Input.UP)){
				cursor = menus.peek().getNextObj(cursor, 1);
//				System.out.println(menus.peek().getObj(cursor));
				buttonTime = 0;
			}

			// modify value of slider
			if(menus.isEmpty()) return;
			if(menus.peek().getObj(cursor) instanceof Slider){
				Slider s = (Slider) menus.peek().getObj(cursor);
				if(MyInput.isDown(Input.RIGHT)){
					if(s.getValue() < s.getMaxValue()){
						float i = ((s.getValue()/s.getMaxValue()*10) + 1)/10f;
						s.changeVal(i);
						playSound("menu1");
					} else { playSound("menu2"); }
					buttonTime = 0;
				}

				if(MyInput.isDown(Input.LEFT)){
					if(s.getValue() > 0){
						float i = ((s.getValue()/s.getMaxValue()*10) - 1)/10f;
						s.changeVal(i);
						playSound("menu1");
					} else { playSound("menu2"); }
					buttonTime = 0;
				}
			} else {
				// traverse left and right
				if(MyInput.isDown(Input.RIGHT)){
					cursor = menus.peek().getNextObj(cursor, 2);
//					System.out.println(menus.peek().getObj(cursor));
					buttonTime = 0;
				}

				if(MyInput.isDown(Input.LEFT)){
					cursor = menus.peek().getNextObj(cursor, 4);
//					System.out.println(menus.peek().getObj(cursor));
					buttonTime = 0;
				}
				
				if(prev.y!=cursor.y || prev.x!=cursor.x){
					if(menus.peek().type==MenuType.MAP) menus.peek().updateMap(prev);
					playSound("menu1");
				}
			}
			
			// tab changing
			if(MyInput.isPressed(Input.INTERACT) && menus.peek().tabs != null){
				MenuType leftTab = menus.peek().getLeftTab();
				back();
				addMenu(new Menu(leftTab, this));
				journalTab = leftTab;
			}
			
			if(MyInput.isPressed(Input.ATTACK) && menus.peek().tabs != null){
				MenuType rightTab = menus.peek().getRightTab();
				back();
				addMenu(new Menu(rightTab, this));
				journalTab = rightTab;
			}	
		}
	}

	public void loadGame() {
		// create load Game Menu
		addMenu(new Menu(MenuType.LOAD, this));
	}
	
	public void loadGame(int i){
		//temporary conditioning
		if(Gdx.files.internal("saves/savegame"+i+".txt").exists())
			gsm.setState(GameStateManager.MAIN, true, "savegame"+i);
		else 
			System.out.println("No save file to load");
		
		while(!menus.isEmpty()) back();
		
	}
	
	public void saveGame(int i){
		JsonSerializer.saveGameState("savegame"+1);
	}

	public void options() {
		// display options menu
		addMenu(new Menu(MenuType.OPTIONS, this));
	}

	public void back() {
		// return to previous menu
		menus.pop();
		if(menus.isEmpty()) {
			MyInputProcessor.menu = false;
			cursor = new Vector2(0, 0);
			unpause();
		} else 
			cursor = menus.peek().getStartPoint();
	}
	
	public void unpause(){
		if (music!=null) 
			music.fadeIn();
	}

	public void quit() {
		if (!quitting) {
			quitting = true;
			sb.fade();
		}

		if (sb.getFadeType() == FadingSpriteBatch.FADE_IN)
			Gdx.app.exit();
		
		menus.clear();
	}

	// change menu to display the LAST TAB of the journal (stats/history/map)
	public void journal(){
		addMenu(new Menu(journalTab, this));
	}

	public void saveGame() {
		addMenu(new Menu(MenuType.SAVE, this));
	}

	public void quitToMenu(){
		//display option to save before exit, with options: "save and exit; exit; cancel"
		gsm.setState(GameStateManager.TITLE, true);
		menus.clear();
	}

	public Song getSong() {
		return music;
	}

	public Song getPrevSong() { return prevSong; }
	public FadingSpriteBatch getSpriteBatch() { return sb; }
	public Camera getB2dCam() { return b2dCam; }
	public GameStateManager getGSM() { return gsm; }
	public void addMenu(Menu w) { 
		this.menus.add(w); 
		cursor = w.getStartPoint();
		if(w.type == MenuType.MAP) 
			w.updateMap(new Vector2(0, 1));
		MyInputProcessor.menu = true;
	}
	public void printMenus(){
		System.out.print("Menu: ");
		if(!menus.empty())
			System.out.println(menus.peek());
		else System.out.println("null");
	}
}
