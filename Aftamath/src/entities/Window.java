package entities;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Window {

	public ArrayList<MenuObject> buttons;
	public String header;
	public int width;
	public int height;
	
	private Texture image;
	
	public Window(String header, int width, int height) {
		this.header = header;
		this.width = width;
		this.height = height;
		
		buttons = new ArrayList<MenuObject>();
	}
	
	public void draw(SpriteBatch sb){
		sb.draw(image, 0, 0);
	}
	
	public void addButtons(String[] buttonTitle) {
		for(String s : buttonTitle){
			buttons.add(new MenuObject(s, 0, 0));
		}
	}
}
