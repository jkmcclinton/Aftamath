package main;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import entities.MenuObj;
import handlers.FadingSpriteBatch;
import handlers.Pair;
import handlers.Vars;

public class JournalEntry extends MenuObj{

	public static final float PERIODX = 83;
	
	private float time;
	private MenuObj entry;
	private Menu parent;
	private TextureRegion backing;
	private Vector2 startDim, endDim;
	
	// visual constants
	private static final float MARGINX_MENU = 6;
	private static final float MARGINY_IMAGE = 5;
	private static final float TIME = .24f;
	private static final int MIN_HEIGHT = 75;
	private static final int MAX_HEIGHT = 138;
	private static final int MIN_WIDTH = 75;
	private static final int MAX_WIDTH = 142;
	
	public JournalEntry(String eventName, int index, Menu parent, GameState gs) {
		this.text = eventName;
		this.type = SourceType.IMAGE;
		this.parent = parent;
		this .gs = gs;
		this.time = TIME;
		
		width = MIN_WIDTH;
		height = MIN_HEIGHT;
		
		x = parent.x0 + index*PERIODX + 2*MARGINX_MENU + width/2;
		y = parent.y0 - parent.height/2;
		backing = new TextureRegion(Game.res.getTexture("file"));
		
		Pair<String, Vector2> p = Game.EVENT_TO_TEXTURE.get(text);
		Texture src = Game.res.getTexture(p.getKey());
		int offx = (int) p.getValue().x, offy = (int) p.getValue().y;
		
		if(offx>src.getWidth()) offx = src.getWidth()-1; 
		if(offy>src.getHeight()) offy = src.getHeight()-1;
		
		image = new TextureRegion(src, offx, offy, 64, 64);
		entry = new MenuObj(((Main)gs).history.getDescription(text),
				SourceType.TEXT, x, y, 17, 8, gs);
	}
	
	public void update(float dt){
		if(time < TIME) zoom(dt);
	}
	
	public void activate(){
		time = 0; 
		startDim = new Vector2(MIN_WIDTH, MIN_HEIGHT);
		endDim = new Vector2(MAX_WIDTH, MAX_HEIGHT);
	}
	
	public void deacvtivate(){
		time = 0; 
		startDim = new Vector2(MAX_WIDTH, MAX_HEIGHT);
		endDim = new Vector2(MIN_WIDTH, MIN_HEIGHT);
	}
	
	/**
	 *  increase/decrease dimensions depending on time since (de)activation
	 */
	private void zoom(float dt){
		time += dt;
		
		// parabolic interpolation
		width = (int) Vars.easingFunction(time, TIME, startDim.x, endDim.x);
		height = (int) Vars.easingFunction(time, TIME, startDim.y, endDim.y);
		
		if(time >= TIME){
			width = (int) endDim.x;
			height = (int) endDim.y;
		}
	}
	
	public void render(FadingSpriteBatch sb){
		// relocation of draw loc according to zoom
		float x0 = x - width*(width/MIN_WIDTH)/2 - parent.getScrollOff().x;
		float y0 = y - height*(height/MIN_HEIGHT)/2 - parent.getScrollOff().y;
		
		// location bounds checking; only valid for highlighted objs
//		if(width!=MIN_WIDTH){
//			if(x0 < parent.x0 + MARGINX_MENU) 
//				x0 = parent.x0 + MARGINX_MENU;
//			if(x0 > parent.x0 + parent.width - MARGINX_MENU) 
//				x0 = parent.x0 + parent.width - MARGINX_MENU;
//		}
		
		sb.draw(backing, x0, y0, width, height);
		sb.draw(image, x0 + width/2 - image.getRegionWidth()/2,
				y0+height-MARGINY_IMAGE-image.getRegionHeight());
		 
		// entry is highlighted and should be visible
		if(entry!=null && width==MAX_WIDTH) {
			entry.x = x0+(width - 2*MARGINY_IMAGE)/2 - entry.width/2;
			entry.y = y0+height - 2*MARGINY_IMAGE - 64 - entry.getImage().getRegionHeight();
			entry.render(sb);
		}
	}
	
	public String toString(){ return text+";\t"+time;}
}
