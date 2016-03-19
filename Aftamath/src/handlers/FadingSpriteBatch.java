package handlers;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import box2dLight.RayHandler;

public class FadingSpriteBatch extends SpriteBatch {

	public boolean fading;
	
	private RayHandler rh;
	private Color overlay;
	private boolean overlayDraw;
	private float fadeTime = 0, fadeInterval;
	private int fadeType;

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

			if(fadeType == FADE_OUT)
				if(overlayDraw)
					setColor(Vars.blendColors(fadeTime, 0, end, overlay, Color.BLACK));
				else
					setColor(Vars.blendColors(fadeTime, 0, end, Vars.DAY_OVERLAY, Color.BLACK));
			else
				if(overlayDraw)
					setColor(Vars.blendColors(fadeTime, 0, end, Color.BLACK, overlay));
				else
					setColor(Vars.blendColors(fadeTime, 0, end, Color.BLACK, Vars.DAY_OVERLAY));
			
			
			//fade rayHandler as well?
			//to be perfected
			if(rh!=null){
//				rh.setAmbientLight(rhAmbient.r-t, rhAmbient.g-t, 
//						rhAmbient.b-t, Vars.ALPHA);
//				for(Light l :rh.lightList){
//					l.setColor(Vars.blendColors(l.getColor(), Color.BLACK));
//				}
			}
				
			if(fadeTime >= end){
				fadeTime = 0;
				faded = true;
				if(fadeType == FADE_OUT)
					fadeType = FADE_IN;
				else  {
					fading = false;
					setColor(1, 1, 1, Vars.ALPHA);
				}
			}
		} else
			setColor(overlay);
		
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
