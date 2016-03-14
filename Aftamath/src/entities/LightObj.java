package entities;

import com.badlogic.gdx.graphics.Color;

import box2dLight.Light;

public class LightObj {
	private Light light;
	private String type;
	private boolean on, flickering;
	private Color color;
	
	public LightObj(String type, Light light){
		this.light = light;
		this.type = type;
		color = new Color(light.getColor());
	}
	
	public void update(float dt){
		//TODO flicker handling
		if(flickering)
			flickering = false;
	}
	
	public void turnOff(){
		light.setColor(Color.BLACK);
		on = false;
	}
	
	public void turnOn(){
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
	public String getType(){ return type; }
	
	private void flicker(){
		flickering = true;
	}

}
