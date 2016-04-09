package main;

import java.lang.reflect.Method;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import handlers.Animation;
import handlers.GameStateManager;
import handlers.MyInput;
import handlers.MyInput.Input;
import handlers.Vars;
import scenes.Song;

public class Title extends GameState {

	private Animation title, btnHighlight;
	private Texture bgImage, textures;
	private float[] titleY = new float[2];
	private boolean titleReached;
	private float titleWait, titleTime;
	private TextureRegion[] font;
	private int titleYMax;
	
	private boolean dbtrender = true;
	
	public Title(GameStateManager gsm){
		super(gsm);
	}
	
	public void handleInput() {
		if(MyInput.isPressed(Input.DEBUG_TEXT))
			dbtrender=!dbtrender;
			
		if(gsm.isFading()) return;
		if(MyInput.isPressed(Input.JUMP)||
				MyInput.isPressed(Input.ENTER) && !gsm.isFading()){
			try {
				Method m = Title.class.getMethod(
						Vars.formatMethodName(menuOptions[menuIndex[0]][menuIndex[1]]));
				m.invoke(this);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if(buttonTime >= DELAY){
			if(MyInput.isDown(Input.DOWN)){
				if(menuIndex[1] < menuMaxY){
					menuIndex[1]++;
					//play menu sound
				} else {
					menuIndex[1] = 0;
				}
				buttonTime = 0;
			}

			if(MyInput.isDown(Input.UP)){
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
		gsm.setState(GameStateManager.MAIN, true);
	}

	public void update(float dt) {
		super.update(dt);
		buttonTime += dt;
		if(!quitting) 
			handleInput();
		
		btnHighlight.update(dt);
		
		title.update(dt);
		titleTime += dt;
		if (titleTime >= titleWait)
			titleReached = false;
		
		debugText = "";
		
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

		if(dbtrender) updateDebugText();
	}
	
	public void updateDebugText() {
		sb.begin();
			drawString(sb, font, 13, debugText, 1,  Game.height/2 - font[0].getRegionHeight() - 2);
			//drawString(sb, debugText,1,  Game.height/2 - font[0].getRegionHeight()*4 - 2);
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
		if(this.music!=null)
			music.dispose();
		}

	@Override
	public void create() {
		super.create();
		Song song = new Song("Title");
		setSong(song, false);
		sb.setOverlay(Color.WHITE);
		sb.setOverlayDraw(false);

		font = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/text4.png")), 14, 20 )[0];
		
		bgImage = new Texture(Gdx.files.internal("assets/images/titleBG.png"));
		title = new Animation(null);
		title.initFrames(TextureRegion.split(new Texture(Gdx.files.internal("assets/images/title.png")), 205, 89)[0]
				, Vars.ANIMATION_RATE, false);
		titleYMax = Game.height/2 - title.getFrame().getRegionHeight() - 15;
		titleY[1] = titleYMax;
		titleWait = .9f;

		textures = Game.res.getTexture("hudTextures");
		TextureRegion[] btnHighFrames = new TextureRegion[9];
		for (int i = 0; i < btnHighFrames.length; i++)
			btnHighFrames[i] = new TextureRegion(textures, 0, i * 18 + 361, 238, 18);
		btnHighlight = new Animation(null);
		btnHighlight.initFrames(btnHighFrames, .1f, false);
		
		menuOptions = new String[][] {{"Play", "Load Game", "Options", "Quit"}};
		menuMaxY = menuOptions[0].length - 1;
		menuMaxX = 0;
		menuIndex[0] = 0;
		menuIndex[1] = 0;
	}

}
