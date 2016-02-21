package entities;

import java.text.NumberFormat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import handlers.Animation;
import handlers.Vars;
import main.Game;
import main.Main;
import main.Main.InputState;

public class HUD {
	
	public TextureRegion[] font, font2, font4;
	public int moving;
	public boolean raised, showStats;
	
	private float locationTime;
	private boolean top;
	private OrthographicCamera cam;
	private Mob character;
	private Mob speaker;
	private Main main;
	private Texture textures;
	private TextureRegion[] hearts, cubeFrames, btnHighFrames, emotions;
	private TextureRegion cash, /*faceHud,*/ textHud, pauseHud, inputBGLeft, inputBGMid, inputBGRight;
	private Animation cube, buttonHigh;
	private String[] speakText; //amount of text already displaying
	private Splash splash;
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
	
	public HUD(Main main, OrthographicCamera cam) {
		this.cam = cam;
		this.main = main;
		this.character = main.character;
		
		//font = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/text.png")), 8, 11)[0];
		//font = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/text2.png")), 7, 12)[0];
		font = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/text3.png")), 7, 9 )[0];
		font2 = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/text3.png")), 7, 9 )[1];
		font4 = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/text4.png")), 14, 20 )[0];
		textures = Game.res.getTexture("hudTextures");
		
		hearts = new TextureRegion[9];
		for (int i = 0; i < hearts.length; i++){
			hearts[i] = new TextureRegion(textures, i * 19, 78, 19, 19);
		}
		
		cubeFrames = new TextureRegion[10];
		for (int i = 0; i < cubeFrames.length; i++)
			cubeFrames[i] = new TextureRegion(textures, i * 11 + 192, 78, 11, 11);
		cube = new Animation(null);
		cube.initFrames(cubeFrames, .09f, false);
		
		btnHighFrames = new TextureRegion[9];
		for (int i = 0; i < btnHighFrames.length; i++)
			btnHighFrames[i] = new TextureRegion(textures, 0, i * 18 + 361, 238, 11);
		buttonHigh = new Animation(null);
		buttonHigh.initFrames(btnHighFrames, .1f, false);
		
		inputBGLeft = new TextureRegion(textures, 302, 78, 6, 18);
		inputBGMid = new TextureRegion(textures, 308, 78, 1, 18);
		inputBGRight = new TextureRegion(textures, 309, 78, 6, 18);
		
		
		Texture emote = Game.res.getTexture("emotion");
		if (emote != null) emotions = TextureRegion.split(emote, 64, 64)[0];
		
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
		
		if(splash!=null)
			splash.time-=dt;
		
		if(locationTime>0)
			locationTime-=dt;
	}
	
	public void render(SpriteBatch sb, int emotion) {
		sb.begin();

		if(splash!=null)
			if (splash.time>0){
				if(splash.overlay !=null)
					sb.draw(splash.overlay, 0, 0, Game.width, Game.height);
			}else 
				splash = null;
			
			sb.setProjectionMatrix(cam.combined);
			drawDialog(sb, emotion);
			drawStats(sb);
			
			if (splash!=null)
				splash.render(sb);
			if(locationTime>0)
				main.drawString(sb, main.getScene().title, 5, Game.height/2 - 5 - font[0].getRegionHeight());
			
			if (main.paused && main.getStateType() == InputState.PAUSED)
				drawPauseMenu(sb);
		sb.end();
	}
	
	public void drawPauseMenu(SpriteBatch sb) {
		Vector2 temp = getCenter(pauseHud);
		sb.draw(pauseHud, temp.x, temp.y);
		int x, y;
		
		for (int j = 0; j <= main.menuMaxY; j++)
			for (int i = 0; i <= main.menuMaxX; i++) {
				x = (Game.width/2 - main.menuOptions[i][j].length() * PERIODX -PERIODX/2)/2 ;
				y = (Game.height/2 + pauseHud.getRegionY())/2 + (j + main.menuMaxY) * PERIODY + 12;
				
				if (i == main.menuIndex[0] && j == main.menuIndex[1])
					sb.draw(buttonHigh.getFrame(), (Game.width/2 - pauseHud.getRegionWidth())/2 - PERIODX + 1, y - 1); // draw menu button highlight
				
				main.drawString(sb, main.menuOptions[i][j], x, y); // draw menu buttons
			}
	}
	
	//draw dialog graphics
	public void drawDialog(SpriteBatch sb, int emotion) {
//		sb.draw(textHud, x + 71, y - 76);
		sb.draw(textHud, x, y - 76);
		
		drawString(sb);
		if (speaker != null) if (speaker.nickName != null) {
//			sb.draw(faceHud, x, y - 76);
			if(speaker.getFace(0)!=null){
				sb.draw(emotions[emotion],  x + 7, y - 69);
				sb.draw(speaker.getFace(emotion), x + 7, y - 69);
			}
		}
		if (canShowContinue())
			sb.draw(cube.getFrame(), Game.width/2 - 10 - cubeFrames[0].getRegionWidth(), 2 + cubeFrames[0].getRegionHeight());
	}
	
	public void drawStats(SpriteBatch sb) {
		sb.draw(hearts[(int)(character.getHealth()/3)], Game.width/2 - 24, Game.height/2 - 24 * 2);
		sb.draw(cash, Game.width/2 - 24, Game.height/2 - 24);
		
		String money = NumberFormat.getCurrencyInstance().format(main.player.getMoney());
		if (money.startsWith("(")) money = "-" + money.substring(1, money.indexOf(")"));

		//		if (money.split(".")[1].length()<2) money += "0";
		main.drawString(sb, money, Game.width/2 - 10 - cash.getRegionWidth() -
				(PERIODX + money.length() * PERIODX), Game.height/2 - 20);
	}
	
	public void drawInputBG(SpriteBatch sb){
		int x = Game.width/4 - (Game.MAX_INPUT_LENGTH+1)*font[0].getRegionWidth()/2;
		int y = 195-5;
		int max = (Game.MAX_INPUT_LENGTH+1)*font[0].getRegionWidth();

		sb.draw(inputBGLeft, x-inputBGLeft.getRegionWidth(), y);
		for(int i = 0; i < max; i++)
			sb.draw(inputBGMid, x + i, y);
		sb.draw(inputBGRight, x + max, y);
	}
	
	public void show(){
		moving = 1;
		moveDialog();
		
		//Gdx.audio.newSound(new FileHandle("assets/sounds/slideup.wav"));
	}
	
	public void hide(){
		moving = 2;
		moveDialog();
		raised = false;
		
		//Gdx.audio.newSound(new FileHandle("assets/sounds/slidedown.wav"));
	}
	
	public void showLocation(){
		locationTime = 5;
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
	
	public void changeFace(Mob speaker2){
		this.speaker = speaker2;
	}
	
	public void removeFace(){
		speaker = null;
		hide();
	}
	
	public Entity getFace(){ return speaker; }
	public void setSplash(String str){
		splash = new Splash(str);
		splash.time = 3;
	}
	
	public void setSplash(SplashType st){
		splash = new Splash(st);
	}
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
		
		if (speaker != null) {
			if (speaker.getNickName() != null) speakText[0] = "  " + speaker.getNickName() + ":";
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
			if (main.paused && main.getStateType() == InputState.PAUSED){
//				System.out.println("paused");
				return false;}
			if (main.speaking){
//				System.out.println("speaking");
				return false;}
			if(main.choosing){
//				System.out.println("choosing");
//			if(play.currentScript != null)
				return false;}
//			if(main.currentScript.getActiveObject() != null)
//				if(main.currentScript.getActiveObject().controlled){
//					System.out.println("controlled");
//					return false;}
			if(main.getCam().moving){
//				System.out.println("moving");
				return false;}
			if(main.currentScript != null)
				if(main.currentScript.waitTime > 0){
//					System.out.println("waiting");
					return false;}
//			if(main.getStateType() == InputState.LOCKED){
//				System.out.println("locked");
//				return false;}
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


	public static enum SplashType {
		KICK_ASS, FIGHT, DO_BATTLE, TO_DEATH, KILL_IT, ITS_ALIVE, DO_IT, STRING,
		WASTED;
	}
	
	public class Splash{
		public String text;
		public SplashType type;
		public float time;
		public Texture overlay;
		
		private float maxTime;
		private Animation animation;
		
		public Splash(){
			text = "";
			type = SplashType.STRING;
		}
		
		public Splash(String str){
			text = str;
			type = SplashType.STRING;
		}
		
		public Splash(SplashType type){
			this.type = type;
			text = type.toString().replace("_", " ");
			init();
		}
		
		private void init(){
			TextureRegion[] tr = null;
			boolean direction = false;
			Texture t;
			time = 3;
//			System.out.println("init splash: "+type);
			switch(type){
			case DO_BATTLE:
				t = Game.res.getTexture("doBattlebase");
				tr=TextureRegion.split(Game.res.getTexture("doBattle"), t.getWidth(), t.getHeight())[0];
				break;
			case DO_IT:
				t = Game.res.getTexture("doItbase");
				tr=TextureRegion.split(Game.res.getTexture("doIt"), t.getWidth(), t.getHeight())[0];
				break;
			case FIGHT:
				t = Game.res.getTexture("fightbase");
				tr=TextureRegion.split(Game.res.getTexture("fight"), t.getWidth(), t.getHeight())[0];
				break;
			case ITS_ALIVE:
				t = Game.res.getTexture("itsAlivebase");
				tr=TextureRegion.split(Game.res.getTexture("itsAlive"), t.getWidth(), t.getHeight())[0];
				break;
			case KICK_ASS:
				t = Game.res.getTexture("kickAssbase");
				tr=TextureRegion.split(Game.res.getTexture("kickAss"), t.getWidth(), t.getHeight())[0];
				break;
			case KILL_IT:
				time = 2f;
				t = Game.res.getTexture("killItbase");
				tr=TextureRegion.split(Game.res.getTexture("killIt"), t.getWidth(), t.getHeight())[0];
				break;
			case TO_DEATH:
				t = Game.res.getTexture("toDeathbase");
				tr=TextureRegion.split(Game.res.getTexture("toDeath"), t.getWidth(), t.getHeight())[0];
				break;
			case WASTED:
				overlay = Game.res.getTexture("splashOverlay");
			default:
				break;
			}
			
			maxTime = time;
			
			if (tr!=null){
				animation = new Animation(null);
				animation.initFrames(tr, Vars.ANIMATION_RATE, direction);
			}
		}
		
		// YAY MATH~!!!
		public void render(SpriteBatch sb){
			float x = Game.height/4f, y = Game.height/4f, 
					zx=1, zy=1, rot=0, t = maxTime - time;
			float ts1 = maxTime/6, ts2 = 5*maxTime/6f;
			float rx = 0, ry = 0;

			if(animation!=null){
				rx = animation.getFrame().getRegionWidth()/2f;
				ry = animation.getFrame().getRegionHeight()/2f;
				animation.update(Vars.DT);
			}

			switch(type){
			case DO_BATTLE:
				break;
			case DO_IT:
				break;
			case ITS_ALIVE:
				break;
			case KILL_IT: //rotate and zoom in, stretch out
				float az1 = 1.65f;
				float az2 = 900;
				float zm = .8f;
				float oa = 8100f;
				ts1 = .35f; ts2 = 1.5f;
				
				x = Game.width/4f;
				y = 0.65f*Game.height/2f;
				
				if(t<ts1){
					rot = oa*(t-ts1)*(t-ts1) ;
					zx = zy = zm*t/ts1;
				} else if (t<ts2) {
					rot = ((-2)/(ts2-ts1))*(t-ts1)+
							1/(ts1+oa*ts1);
					zx = zy = ((1.25f-zm)/(ts2 - ts1))*(t-ts1)+zm;
				} else {
					rot = ((20-25)/(ts2-ts1))*(ts2-ts1)+
							1/(ts1+oa*ts1);
					float fz1ts2 = ((1.25f-zm)/(ts2 - ts1))*(t-ts1)+zm;
					zy = (fz1ts2 /(ts2 - az1))*(t-ts2) + fz1ts2;
					if(zy<0)zy=0;
					zx = az2*(t-ts2)*(t-ts2)+
							((1.25f-zm)/(ts2 - ts1))*(ts2-ts1)+zm;
				}
				
				break;
			case STRING:
				int width = font4[0].getRegionWidth();
				float scale = 2;
				main.drawString(sb, font4, width, 2, splash.text, 
						Game.width/4 - scale * width * splash.text.length()/2,  Game.height/4);
				break;
			case TO_DEATH:
				break;
			case WASTED:
				break;
				
			default: //slide in from left
				float cx =.5555555f*Game.width/2f;
				float cxw=.1064814f*Game.width/2f;
				float a1 = -13.3f;
				float a0 = -1560;
				float o  = .034f;
				float o0 = .043f;
				float o1 = 53;
				y = 0.65f*Game.height/2f;

//				if(animation!=null)
//					y-=animation.getFrame().getRegionHeight()/2f;
				
				if(t<ts1)
					x = a0*(t-ts1-o)*(t-ts1-o)+cx-cxw;
				else if (t<ts2)
					x = a1*(t-ts2)*(t-ts2)+o1+
							a0*o*o+cx-cxw;
				else
					x = -a0*(t-ts2+o0)*(t-ts2+o0)+
						a1*o0*o0+o1+
						a0*o*o+cx-cxw;
				break;
			}

			if(animation!=null){
				TextureRegion tr = animation.getFrame();
				sb.draw(tr, x-tr.getRegionWidth()/2f, y-tr.getRegionHeight()/2f, rx, ry, 
						tr.getRegionWidth(), tr.getRegionHeight(), zx, zy, rot);
				//sb.draw(tr,x-tr.getRegionWidth()/2f, y-tr.getRegionHeight()/2f);
//				sb.draw(tr, main.debugX, main.debugY);
			}
//			TextureRegion region, float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation)
		}
	}	
}
