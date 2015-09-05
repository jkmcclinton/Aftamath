package entities;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import main.Game;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class MenuObject {

	public int width, height, x, y;
	private String name;
	private Texture image;
	private Method function;
	
	public MenuObject() {
		
	}
	
	public MenuObject(String name, int x, int y) {
		this.name = name;
		this.x = x;
		this.y = y;
		
		try {
			image = Game.res.getTexture(name);
			width = image.getWidth();
			height = image.getHeight();
		} catch (Exception e) {
			
		}
	}
	
	public void setMethod(Method m){
		function = m;
	}

	public void draw(SpriteBatch sb, int ObjectState) {
		
	}
	
	public Texture getImage() { return image; }
	public String getName() { return name; }
	
	public void onClick(){
		try {
			function.invoke(this);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
	}
}
