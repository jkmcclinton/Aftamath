package main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;

public class House {
	private Texture texture;
	private Sprite sprite;
	private String type;
	private String address;
	
	public House() {
		//texture = HOMELESS;
		//sprite = new Sprite(HOMELESS);
		type = "Homeless";
		address = "Nowhere";
	}
	
	public House(String newType, String location){
		type= newType;
		address = location;
		
		texture = new Texture(Gdx.files.internal(type + ".png"));
		sprite = new Sprite(texture);
		sprite.getColor();
	}
	
	public String getType(){
		return type;
	}
	
	public String getAddress(){
		return address;
	}
	
	public void setType(String newType){
		type=newType;
	}
}
