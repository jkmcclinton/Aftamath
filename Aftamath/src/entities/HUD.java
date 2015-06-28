package entities;

import handlers.Animation;
import handlers.Vars;

import java.text.NumberFormat;

import main.Game;
import main.GameState;
import main.Main;
import main.Main.InputState;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class HUD {
	
	public TextureRegion[] font, font2;
	public int moving;
	public boolean raised, showStats;
	
	private boolean top;
	private OrthographicCamera cam;
	private Mob character, face;
	private Player player;
	private Main play;
	private Texture textures;
	private TextureRegion[] hearts, cubeFrames, btnHighFrames;
	private TextureRegion cash, /*faceHud,*/ textHud, pauseHud;
	private Animation cube, buttonHigh;
	private String[] speakText; //amount of text already displaying
//	private float rotf;
//	private boolean flipping;
	
	public static final int HUDX = 82;
	public static final int HUDY = -16;
	public static final int PERIODX = 6;
	public static final int PERIODY = -12;
	 
	//positions to control dialog
	public int x;
	public int y;
	private int offset = 0;
	
	public HUD(Player player, OrthographicCamera cam) {
		this.cam = cam;
		play = player.getGameState();
		this.character = play.character;
		
		//font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text.png")), 8, 11)[0];
		//font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text2.png")), 7, 12)[0];
		font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text3.png")), 7, 9 )[0];
		font2 = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text3.png")), 7, 9 )[1];
		textures = Game.res.getTexture("hudTextures");
		
		hearts = new TextureRegion[9];
		for (int i = 0; i < hearts.length; i++){
			hearts[i] = new TextureRegion(textures, i * 19, 78, 19, 19);
		}
		
		cubeFrames = new TextureRegion[10];
		for (int i = 0; i < cubeFrames.length; i++)
			cubeFrames[i] = new TextureRegion(textures, i * 11 + 192, 78, 11, 11);
		cube = new Animation();
		cube.setFrames(cubeFrames, .09f, false);
		
		btnHighFrames = new TextureRegion[9];
		for (int i = 0; i < btnHighFrames.length; i++)
			btnHighFrames[i] = new TextureRegion(textures, 0, i * 18 + 361, 238, 11);
		buttonHigh = new Animation();
		buttonHigh.setFrames(btnHighFrames, .1f, false);
		
		cash = new TextureRegion(textures, 9 * 19, 78, 21, 19);
//		faceHud = new TextureRegion(textures, 0, 0, 72, 78);
//		textHud = new TextureRegion(textures, 72, 0, 361, 78);
		textHud = new TextureRegion(textures, 0, 0, 433, 78);
		pauseHud = new TextureRegion(textures, 96, 166, 240, 139);
		hide();
	}
	
	public void update(float dt){
		if (moving > 0) moveDialog();
		cube.update(dt);
		buttonHigh.update(dt);
	}
	
	public void render(SpriteBatch sb, int emotion) {
		sb.begin();
			sb.setProjectionMatrix(cam.combined);
			drawDialog(sb, emotion);
			if(showStats) drawStats(sb);
			
			if (play.paused && play.getStateType() == InputState.PAUSED)
				drawPauseMenu(sb);
		sb.end();
	}
	
	public void drawPauseMenu(SpriteBatch sb) {
		Vector2 temp = getCenter(pauseHud);
		sb.draw(pauseHud, temp.x, temp.y);
		int x, y;
		
		for (int j = 0; j <= play.menuMaxY; j++)
			for (int i = 0; i <= play.menuMaxX; i++) {
				x = (Game.width/2 - play.menuOptions[i][j].length() * PERIODX -PERIODX/2)/2 ;
				y = (Game.height/2 + pauseHud.getRegionY())/2 + (j + play.menuMaxY) * PERIODY + 12;
				
				if (i == play.menuIndex[0] && j == play.menuIndex[1])
					sb.draw(buttonHigh.getFrame(), (Game.width/2 - pauseHud.getRegionWidth())/2 - PERIODX + 1, y - 1); // draw menu button highlight
				
				GameState.drawString(sb, play.menuOptions[i][j], x, y); // draw menu buttons
			}
	}
	
	//draw dialog graphics
	public void drawDialog(SpriteBatch sb, int emotion) {
//		sb.draw(textHud, x + 71, y - 76);
		sb.draw(textHud, x, y - 76);
		
		drawString(sb);
		if (face != null) if (face.name != null) {
//			sb.draw(faceHud, x, y - 76);
			sb.draw(face.getFace(emotion), x + 7, y - 69);
		}
		if (canShowContinue())
			sb.draw(cube.getFrame(), Game.width/2 - 10 - cubeFrames[0].getRegionWidth(), 2 + cubeFrames[0].getRegionHeight());
	}
	
	public void drawStats(SpriteBatch sb) {
		sb.draw(hearts[(int)(character.getHealth()/3)], Game.width/2 - 24, Game.height/2 - 24 * 2);
		sb.draw(cash, Game.width/2 - 24, Game.height/2 - 24);
		
		String money = NumberFormat.getCurrencyInstance().format(player.getMoney());
		if (money.startsWith("(")) money = "-" + money.substring(1, money.indexOf(")"));
		
//		if (money.split(".")[1].length()<2) money += "0";
		GameState.drawString(sb, money, Game.width/2 - 10 - cash.getRegionWidth() -
				(PERIODX + money.length() * PERIODX), Game.height/2 - 20);
	}
	
	public void show(){
		moving = 1;
		moveDialog();
		
		//Gdx.audio.newSound(new FileHandle("res/sounds/slideup.wav"));
	}
	
	public void hide(){
		moving = 2;
		moveDialog();
		raised = false;
		
		//Gdx.audio.newSound(new FileHandle("res/sounds/slidedown.wav"));
	}
	
	public void showStats(){
		
	}
	
	public void hideStats(){
		
	}
	
	public void moveStats(){
		
	}
	
	public void moveDialog(){
		final int speed = 4;
		
		if(moving == 1){ //bring into focus
			if (top) y -= speed;
			else y += speed;
			
			if(y >= 75 + offset) { moving = 0; raised = true; }
		} else {  //bring out of focus
			if (top) y += speed;
			else y -= speed;
			
			if(y <= -20 || y >= Game.height/2) moving = 0; 
		}
	}
	
	public void setPosition(boolean top){ 
		this.top = top; 
		if (top) {
			offset = 100;
			y = Game.height/2;
		} else {
			offset = 0;
			y = 0;
		}
	}
	
	public void changeFace(Mob face){
		this.face = face;
	}
	
	public void removeFace(){
		face = null;
		hide();
	}
	
	public Entity getFace(){ return face; }
	
	//draw entire dialog text to screen
	public void drawString(SpriteBatch sb){
		if (speakText == null) return;
		
		for (int i = 0; i < speakText.length; i++){
			if (speakText[i] == null) {System.out.println(i+ "\n" + true);return;}
			for (int j = 0; j < speakText[i].length(); j++){
				char c = speakText[i].charAt(j);
				if (c != " ".charAt(0) && c+Vars.FONT_OFFSET<font2.length) sb.draw(font2[c + Vars.FONT_OFFSET], HUDX + j * PERIODX, HUDY + y + i * PERIODY);
			} 
		}
	}
	
	public void addChar(int sy, char c){
		speakText[sy + 1] += c;
	}
	
	public void createSpeech(String[] s){
		speakText = new String[s.length+1];
		
		if (face != null) {
			if (face.getName() != null) speakText[0] = "  " + face.getName() + ":";
		} else speakText[0] = "";
		
		for (int i = 1; i < speakText.length; i++) speakText[i] = "";
	}
	
	//write all of dialog text to be drawn
	public boolean fillSpeech(String[] s){
		for (int i = 1; i < speakText.length; i++){
			speakText[i] = s[i - 1];
			if (speakText[i].contains("~")) speakText[i] = speakText[i].substring(0, speakText[i].indexOf("~")) + 
					speakText[i].substring(speakText[i].indexOf("~") + 1);
		}
		return false;
	}
	
	public void clearSpeech() { speakText = null; }
	
	private boolean canShowContinue(){
		if (raised){
			if (play.paused && play.getStateType() == InputState.PAUSED){
				//System.out.println("paused");
				return false;}
			if (play.speaking){
				//System.out.println("speaking");
				return false;}
			if(play.choosing){
				//System.out.println("choosing");
//			if(play.currentScript != null)
				return false;}
//				if(play.currentScript.getActiveObject() != null)
//					if(play.currentScript.getActiveObject().controlled){
//						//System.out.println("controlled");
//						return false;}
			if(play.getCam().moving){
//				System.out.println("moving");
				return false;}
			if(play.currentScript != null)
				if(play.currentScript.waitTime > 0){
					//System.out.println("waiting");
					return false;}
			if(play.getStateType() == InputState.LOCKED){
				//System.out.println("locked");
				return false;}
			return true;
		}
		
		return false;
	}
	
	public static Vector2 getCenter(TextureRegion t){
		float height = t.getRegionWidth();
		float width = t.getRegionHeight();
		
		return new Vector2((Game.height - width)/4, (Game.height - height)/4);
	}
	
	public static float getCenterW(TextureRegion t){
		return Game.height/2 - t.getRegionWidth()/4;
	}
}
