package entities;

import handlers.Animation;
import handlers.Vars;

import java.text.NumberFormat;

import main.Game;
import main.GameState;
import main.Play;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

public class HUD {
	
	public TextureRegion[] font, font2;
	private OrthographicCamera cam;
	private Player player;
	private Play play;
	private Mob face;
	private Texture textures;
	private TextureRegion[] hearts, cubeFrames, highFrames, btnHighFrames;
	private TextureRegion cash, faceHud, textHud, pauseHud;
	private Animation cube, highlight, buttonHigh;
	private String[] speakText; //amount of text already displaying
	
	public int moving;
	public boolean raised;
	private boolean top;
	
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
		this.player = player;
		play = player.getPlayState();
		
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
		
		highFrames = new TextureRegion[10];
		for (int i = 0; i < highFrames.length;i++)
			highFrames[i] = new TextureRegion (textures, i * 14, 97, 14, 14);
		highlight = new Animation();
		highlight.setFrames(highFrames, .1f, false);
		
		btnHighFrames = new TextureRegion[9];
		for (int i = 0; i < btnHighFrames.length; i++)
			btnHighFrames[i] = new TextureRegion(textures, 0, i * 18 + 361, 238, 11);
		buttonHigh = new Animation();
		buttonHigh.setFrames(btnHighFrames, .1f, false);
		
		cash = new TextureRegion(textures, 9 * 19, 78, 21, 19);
		faceHud = new TextureRegion(textures, 0, 0, 72, 78);
		textHud = new TextureRegion(textures, 72, 0, 361, 78);
		pauseHud = new TextureRegion(textures, 96, 166, 240, 139);
		hide();
	}
	
	public void update(float dt){
		if (moving > 0) moveDialog();
		cube.update(dt);
		highlight.update(dt);
		buttonHigh.update(dt);
	}
	
	@SuppressWarnings("static-access")
	public void render(SpriteBatch sb, int emotion) {
		sb.begin();
			SpeechBubble b = null;
			if (player.getPlayState().getChoices() != null) {
				b = player.getPlayState().getChoices()[player.getPlayState().choiceIndex];
			}
			if(player.getPlayState().choosing && !player.getPlayState().speaking && b!= null) 
				if (b.getBody() != null) sb.draw(highlight.getFrame(), 
						b.getPosition().x*Vars.PPM - highFrames[0].getRegionWidth()/2, 
						b.getPosition().y*Vars.PPM - highFrames[0].getRegionHeight()/2);
			

			sb.setProjectionMatrix(cam.combined);
			drawDialog(sb, emotion);
			drawStats(sb);
			
			if (play.paused && play.getStateType() == play.PAUSED)
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
		sb.draw(textHud, x + 71, y - 76);
		
		drawString(sb);
		if (face != null) if (face.name != null) {
			sb.draw(faceHud, x, y - 76);
			sb.draw(face.getFace(emotion), x + 7, y - 69);
		}
		if (!busy())
			sb.draw(cube.getFrame(), Game.width/2 - 10 - cubeFrames[0].getRegionWidth(), 2 + cubeFrames[0].getRegionHeight());
	}
	
	public void drawStats(SpriteBatch sb) {
		sb.draw(hearts[(int)(player.getHealth()/3)], Game.width/2 - 24, Game.height/2 - 24 * 2);
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
		hide();
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
				if (c != " ".charAt(0)) sb.draw(font2[c + Vars.FONT_OFFSET], HUDX + j * PERIODX, HUDY + y + i * PERIODY);
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
	
	private boolean busy(){
		if (raised){
			if (play.paused && play.getStateType() != Play.PAUSED){
				//System.out.println("paused");
				return true;}
			if (play.speaking){
				//System.out.println("speaking");
				return true;}
			if(play.choosing){
				//System.out.println("choosing");
				return true;}
			if(play.currentScript != null)
				if(play.currentScript.getActiveObject() != null)
					if(play.currentScript.getActiveObject().controlled){
						//System.out.println("controlled");
						return true;}
			if(play.getCam().moving){
				//System.out.println("moving");
				return true;}
			if(play.currentScript != null)
				if(play.currentScript.waitTime > 0){
					//System.out.println("waiting");
					return true;}
			if(play.getStateType() == Play.LOCKED){
				//System.out.println("locked");
				return true;}
			return false;
		}
		
		return true;
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
