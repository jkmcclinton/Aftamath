package handlers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import box2dLight.Light;
import box2dLight.RayHandler;
import entities.Entity;
import entities.LightObj;
import main.GameState;
import main.Main;

public class FadingSpriteBatch extends SpriteBatch {

	public boolean fading;
	
	private RayHandler rh;
	private Color overlay;
	private boolean overlayDraw;
	private float fadeTime = 0, fadeInterval;
	private int fadeType;
	private GameState gs;

	public static final int FADE_OUT = -1;
	public static final int FADE_IN = 1;
	
	public FadingSpriteBatch(){
		super();
		setOverlayDraw(false);
	}
	
	//updates fade if fading;
	//returns true if it has completely faded in or out
	public boolean update(float dt) {
		boolean faded = false;
		float end = 2f / (fadeInterval);
		
		if (fading) {
			fadeTime += dt;
			float t;
			if(fadeType == FADE_IN)
				t = fadeTime;
			else
				t = 1 - fadeTime * fadeInterval;
			if (t > 1) 
				t = 1;
			if (t < 0)
				t = 0;

			if(fadeType == FADE_OUT){
				if(overlayDraw)
					setColor(Vars.blendColors(fadeTime, 0, end, overlay, Color.BLACK));
				else
					setColor(Vars.blendColors(fadeTime, 0, end, Vars.DAY_OVERLAY, Color.BLACK));
				
				//fade out entity lights as well
				if(gs instanceof Main){
					Main m = (Main) gs;
					for(Entity e : m.getObjects()){
						Light l = e.getLight();
						if(l!=null){
							Color c = new Color(Color.BLACK);
							c.a = l.getColor().a;
							l.setColor(Vars.blendColors(l.getColor(), c));
						}
					}
				}
			}else
				if(overlayDraw)
					setColor(Vars.blendColors(fadeTime, 0, end, Color.BLACK, overlay));
				else
					setColor(Vars.blendColors(fadeTime, 0, end, Color.BLACK, Vars.DAY_OVERLAY));
			
			if(rh!=null){
				if(gs instanceof Main){
					Main m = (Main) gs;
					for(LightObj l : m.getLights()){
						l.fade(fadeTime, end, fadeType);
					}
				}
			}
				
			if(fadeTime >= end){
				fadeTime = 0;
				faded = true;
				if(fadeType == FADE_OUT)
					fadeType = FADE_IN;
				else  {
					fading = false;
					if(overlayDraw) setColor(overlay);
					else setColor(Vars.DAY_OVERLAY);
					
					if(gs instanceof Main){
						Main m = (Main) gs;
						for(LightObj l : m.getLights()){
							l.resetColor();
						}
					}
				}
			}

		} else if(overlay!=null){
			setColor(overlay);
		}
		
		return faded;
	}
	
	public void fade() {
		fade(2f);
	}
	
	public void fade(float fadeInterval) { 
		this.fadeInterval = fadeInterval;
		fading = true; 
		fadeType = FADE_OUT;
		fadeTime = 0;
	}
	
	public void fastFade() {
		fade(4f);
	}
	
	public void setGameState(GameState gs){ this.gs = gs; }
	public void setRayHandler(RayHandler rh){ this.rh = rh; }
	public void setOverlay(Color overlay){ this.overlay = overlay; }
	public Color getOverlay(){return overlay; }
	public boolean isDrawingOverlay(){ return overlayDraw; }
	public void setOverlayDraw(boolean val){ 
		overlayDraw = val; 
		if(val) setColor(overlay);
		else setColor(Color.WHITE);
	}
	
	public int getFadeType() { return fadeType; }
	public float getFadeTime() { return fadeTime; }
	public boolean isFading() { return fading; }
}
