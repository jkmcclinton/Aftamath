package main;

import handlers.Animation;
import handlers.GameStateManager;
import handlers.MyInput;
import handlers.Vars;

import java.lang.reflect.Method;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class Title extends GameState {

	private Animation title, btnHighlight;
	private Texture bgImage, textures;
	private float[] titleY = new float[2];
	private boolean titleReached;
	private float titleWait, titleTime;
	private TextureRegion[] font;
	private int titleYMax;
	
	public Title(GameStateManager gsm){
		super(gsm);
	}
	
	public void handleInput() {
		if(MyInput.isPressed(MyInput.JUMP)||
				MyInput.isPressed(MyInput.ENTER) && !gsm.isFading()){
			try {
				Method m = Title.class.getMethod(
						Vars.formatMethodName(menuOptions[menuIndex[0]][menuIndex[1]]));
				m.invoke(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if(buttonTime >= DELAY){
			if(MyInput.isDown(MyInput.DOWN)){
				if(menuIndex[1] < menuMaxY){
					menuIndex[1]++;
					//play menu sound
				} else {
					menuIndex[1] = 0;
				}
				buttonTime = 0;
			}

			if(MyInput.isDown(MyInput.UP)){
				if(menuIndex[1] > 0){
					menuIndex[1]--;
					//play menu sound
				} else {
					menuIndex[1] = menuMaxY;
				}
				buttonTime = 0;
			}
		}
	}
	
	public void play() {
		gsm.setState(GameStateManager.PLAY, true);
	}

	public void update(float dt) {
		buttonTime += dt;
		if(!quitting) 
			handleInput();
		
		btnHighlight.update(dt);
		
		title.update(dt);
		titleTime += dt;
		if (titleTime >= titleWait)
			titleReached = false;
		
		debugText = ""+song.getVolume();
		
		if(quitting)
			quit();
	}

	public void render() {
		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		sb.setProjectionMatrix(hudCam.combined);
		
		sb.begin();
			sb.draw(bgImage, (Game.width/2 - bgImage.getWidth())/2, (Game.height/2 - bgImage.getHeight())/2);
			drawTitle(sb);
			drawOptions(sb);
		sb.end();

		updateDebugText();
	}
	
	public void updateDebugText() {
		sb.begin();
			drawString(sb, font, 13, debugText,1,  Game.height/2 - font[0].getRegionHeight() - 2);
			//1drawString(sb, debugText,1,  Game.height/2 - font[0].getRegionHeight()*4 - 2);
		sb.end();
	}

	public void drawTitle(SpriteBatch sb) {
		if(!titleReached) {
			if (titleY[0] == 0)
				if (titleY[1] > titleYMax - 6)
					titleY[1]-=.02f;
				else {
					titleReached = true;
					titleY[0] = 1;
					titleTime = 0;
				}
			else
				if (titleY[1] < titleYMax)
					titleY[1]+=.02f;
				else {
					titleReached = true;
					titleY[0] = 0;
					titleTime = 0;
				}
		}
		
		sb.draw(title.getFrame(), (Game.width/2 - title.getFrame().getRegionWidth())/2, titleY[1]);
	}
	
	public void drawOptions(SpriteBatch sb) {
		int x, y, px = font[0].getRegionWidth()-1, py = font[0].getRegionHeight() + 2;

		for (int j = 0; j <= menuMaxY; j++)
			for (int i = 0; i <= menuMaxX; i++) {
				x = (Game.width/2 - menuOptions[i][j].length() * px - px/2)/2 ;
				y = (Game.height/4 - j * py - 28);
				
				if (i == menuIndex[0] && j == menuIndex[1]){
					sb.draw(btnHighlight.getFrame(), 0, y - 1); // draw menu button highlight
					sb.draw(btnHighlight.getFrame(), (Game.width/4 - PERIODX + 1), y-1);
				}
				
				drawString(sb, font, px, menuOptions[i][j], x, y); // draw menu buttons
			}
	}
	
	public void dispose() { 
		if(song != null)
			song.stop();
		}

	@Override
	public void create() {
		song = Gdx.audio.newMusic(new FileHandle("res/music/Title.wav"));
		setSong(song, gsm.volume);

		font = TextureRegion.split(new Texture(Gdx.files.internal("res/images/text4.png")), 14, 20 )[0];
		
		bgImage = new Texture(Gdx.files.internal("res/images/titleBG.png"));
		title = new Animation(TextureRegion.split(
				new Texture(Gdx.files.internal("res/images/title.png")), 205, 89)[0]);
		titleYMax = Game.height/2 - title.getFrame().getRegionHeight() - 15;
		titleY[1] = titleYMax;
		titleWait = .9f;

		textures = Game.res.getTexture("hudTextures");
		TextureRegion[] btnHighFrames = new TextureRegion[9];
		for (int i = 0; i < btnHighFrames.length; i++)
			btnHighFrames[i] = new TextureRegion(textures, 0, i * 18 + 361, 238, 18);
		btnHighlight = new Animation();
		btnHighlight.setFrames(btnHighFrames, .1f, false);
		
		menuOptions = new String[][] {{"Play", "Load Game", "Options", "Quit"}};
		menuMaxY = menuOptions[0].length - 1;
		menuMaxX = 0;
		menuIndex[0] = 0;
		menuIndex[1] = 0;
	}

}
