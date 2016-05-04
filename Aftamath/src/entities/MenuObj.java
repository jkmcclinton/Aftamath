package entities;

import java.lang.reflect.Method;

import com.badlogic.gdx.controllers.PovDirection;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import handlers.FadingSpriteBatch;
import handlers.Vars;
import main.Game;
import main.GameState;

public class MenuObj {

	public int width, height;
	public float x, y;
	public boolean clickable = true, hidden = false, hiLite = true;
	
	protected int marginW, marginH;
	private float rot;
	protected String text;
	protected TextureRegion image;
	protected Method function;
	protected Object args, invokerInstance;
	protected GameState gs;
	protected SourceType type;
	protected Vector2 mapping;
	
	public static enum SourceType{ TEXT, IMAGE, BUTTON }
	
	public MenuObj(){}

	/**
	 * for initializing images and text objects
	 * @param text string related to object
	 * @param type TEXT or IMAGE
	 * @param x obj location
	 * @param y obj location
	 * @param w width
	 * @param h height
	 * @param gs
	 */
	public MenuObj(String text, SourceType type, float x, float y, int w, int h, GameState gs) {
		this.text = text;
		this.x = x;
		this.y = y;
		this.type = type;
		this.gs = gs;
		this.args = null;
		
		if(type==SourceType.IMAGE){
			Texture texture = Game.res.getTexture(text);
		
			if(texture != null){
//				if(x>texture.getWidth()) x = texture.getWidth()-1;
//				if(y>texture.getHeight()) y = texture.getHeight()-1;
//				x = x<0 ? 0 : x;
//				y = y<0 ? 0 : y;
//				w = x+w>texture.getWidth() ? texture.getWidth()-x : w;
//				h = y+h>texture.getHeight() ? texture.getHeight()-y : h;

				width = w;
				height = h;
				
				image = new TextureRegion(texture, 0, 0, w, h);
			} else {
				image = new TextureRegion(Game.res.getTexture("empty"));
				width = image.getRegionWidth();
				height = image.getRegionHeight();
			}
		} else {
			image = TextureRegion.split(Game.res.getTexture("text3"), 7, 9 )[0][0];
			marginW = w;
			marginH = h;
			formatDim();
		}
	}
	
	/**
	 * for initializing button objs
	 * @param text text of button
	 * @param image bg image
	 * @param x
	 * @param y
	 * @param gs
	 */
	public MenuObj(String text, String image, float x, float y, GameState gs){
		this.text = text;
		this.x = x;
		this.y = y;
		this.type = SourceType.BUTTON;
		this.gs = gs;
		this.args = null;
		
		Texture texture = Game.res.getTexture(image);
		if(texture!=null)
			this.image = new TextureRegion(texture);
		else {
			this.image = new TextureRegion(Game.res.getTexture("default_button"));
		}
		
		width = this.image.getRegionWidth();
		height = this.image.getRegionHeight();
	}
	
	/**
	 * for initializing imgs
	 * @param tex
	 * @param x
	 * @param y
	 * @param gs
	 */
	public MenuObj(Texture tex, float x, float y, GameState gs){
		this.text = "img";
		this.type = SourceType.IMAGE;
		this.x = x;
		this.y = y;
		this.gs = gs;
		this.args = null;
		
		if(tex==null)
			tex = Game.res.getTexture("empty");
		this.image = new TextureRegion(tex);
		this.width = tex.getWidth();
		this.height = tex.getHeight();
	}
	
	private void formatDim(){
		height = width = 0;
		String[] s = Vars.formatDialog(text, marginW, marginH).split("/l");
		for(String l : s){
			if(l.length()>width) 
				width = l.length();
			height++;
		}
		width++;
		width *= Vars.TEXT_PERIODX;
		height *= Vars.TEXT_PERIODY;			
	}
	
	public void setMethod(Method m, Object o){
		invokerInstance = o;
		function = m;
	}
	
	public Object getArgs(){ return args; }
	public void setArgs(Object dat){ this.args = dat; }
	public void setMenuMapping(Vector2 mapping){ this.mapping = mapping; }
	public Vector2 getMenuMapping() { return this.mapping; }

	public void update(float dt){ }
	
	public void render(FadingSpriteBatch sb) {
		if(type!=SourceType.TEXT) 
			sb.draw(image, x, y, width/2, height/2, width, height, 1, 1, rot);
		
		if(type==SourceType.BUTTON)
			gs.drawString(sb, Vars.formatDialog(text, width/Vars.TEXT_PERIODX, 
				height/Vars.TEXT_PERIODY), x+width/2 - text.length()*Vars.TEXT_PERIODX/2, y+height/2+Vars.TEXT_PERIODY/2);
		if(type==SourceType.TEXT)
			gs.drawString(sb, Vars.formatDialog(text, width/Vars.TEXT_PERIODX, 
				height/Vars.TEXT_PERIODY), x, y);
	}
	
	public void dispose(){
		image.getTexture().dispose();
	}
	
	public void onClick(){
		if(function==null) return;
		try {
			if(args==null)
				function.invoke(invokerInstance);
			else{
//				System.out.println(args.getClass().getSimpleName() + "; function: "+
//				new Array<>(function.getGenericParameterTypes()));
				function.invoke(invokerInstance, args);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public TextureRegion getImage() { return image; }
	public String getText() { return text; }
	public void setText(String txt){ this.text = txt; formatDim(); }
	public SourceType getType(){ return type; }
	
	public void setRotation(PovDirection dir){
		switch(dir){
		case east:	rot = -90; break;
		case south: rot = 180; break;
		case west: rot = 90; break;
		default: rot = 0; break;
		}
	}
	
	public String toString(){ return type+": "+text +"; "+new Vector2(x, y)+new Vector2(width, height)+"\t"+mapping; }
}
