package entities;

import com.badlogic.gdx.graphics.Color;

import box2dLight.Light;
import handlers.FadingSpriteBatch;
import handlers.Vars;

public class LightObj {
	private Light light;
	private String type;
	private boolean on, flickering, scheduled;
	private Color color;
	
	public LightObj(String type, Light light, boolean scheduled){
		this.light = light;
		this.type = type;
		this.scheduled = scheduled;
		color = new Color(light.getColor());
		on = true;
	}
	
	public void update(float dt){
		//TODO flicker handling
		if(flickering)
			flickering = false;
	}
	
	public void turnOff(){
//		light.setActive(false);
		light.setColor(Color.BLACK);
		on = false;
	}
	
	public void turnOn(){
//		light.setActive(true);
		on = true;
		switch(type){
		case "street":
			flicker();
		default:
			light.setColor(color);
			break;
		}
	}
	
	public boolean isOn() { return on; }
	public boolean isScheduled() { return scheduled; }
	public String getType(){ return type; }
	
	private void flicker(){
		flickering = true;
	}
	
	/**
	 * interpolates the color of the light with a fade
	 * @param t time of the fade
	 * @param endT the final time of the fade
	 * @param fadeType wheter the game is fading in or out; should be -1 or 1
	 */
	public void fade(float t, float endT, int fadeType){
		Color c = new Color(Color.BLACK);
		c.a = color.a;
		if(fadeType == FadingSpriteBatch.FADE_OUT)
			light.setColor(Vars.blendColors(t, 0, endT, color, c));
		else
			light.setColor(Vars.blendColors(t, 0, endT, c, color));
	}
	
	/**
	 * resets the color of light to default
	 */
	public void resetColor(){ light.setColor(color); }
	
	public String toString(){ return type+": "+on+"\tScheduled: "+scheduled; }

}
