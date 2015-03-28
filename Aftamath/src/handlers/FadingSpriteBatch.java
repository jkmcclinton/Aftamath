package handlers;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class FadingSpriteBatch extends SpriteBatch {

	private float fadeTime = 0, fadeInterval;
	private int fadeType;
	public boolean fading;

	public static final float ALPHA = .99607843f;
	public static final int FADE_OUT = -1;
	public static final int FADE_IN = 1;
	
	public FadingSpriteBatch(){
		super();
	}
	
	
	//updates fade if fading;
	//returns true if it has completely faded in or out
	public boolean update(float dt) {
		boolean faded = false;
		
		if (fading) {
			fadeTime += dt;

			float color;

			if(fadeType == FADE_IN)
				color = fadeTime;
			else
				color = 1 - fadeTime * fadeInterval;
			if (color > 1) 
				color = 1;
			if (color < 0)
				color = 0;

			setColor(color, color, color, ALPHA);

			if(fadeTime >= 2f / (fadeInterval)){
				fadeTime = 0;
				faded = true;
				if(fadeType == FADE_OUT)
					fadeType = FADE_IN;
				else  {
					fading = false;
					setColor(1, 1, 1, ALPHA);
				}
			}
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
	
	public int getFadeType() { return fadeType; }
	public float getFadeTime() { return fadeTime; }
	public boolean isFading() { return fading; }
}
