package main;

import java.lang.reflect.Field;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import entities.MenuObj;
import handlers.FadingSpriteBatch;

public class Slider extends MenuObj {
	
	private TextureRegion image2;
	private float maxValue;
	private Field value;
	
	public Slider(Field value, float maxValue, float x, float y, GameState gs){
		this.gs = gs;
		this.x = x;
		this.y = y;
		this.value = value;
		this.maxValue = maxValue;
		
		image = new TextureRegion(Game.res.getTexture("slider_filled"));
		image2 = new TextureRegion(Game.res.getTexture("slider_empty"));
		width = 10*(image.getRegionWidth() - 1)+1;
		height = image.getRegionHeight();
	}
	
	public void render(FadingSpriteBatch sb){
		try{
			float v = value.getFloat(value);
			int m = (int)(v/ (maxValue)*10);
			for(int i = 0; i < m; i++)
				sb.draw(image, x+(i)*(image.getRegionWidth() - 1), y);
			for(int i = m; i <= 9; i++)
				sb.draw(image2, x+(i)*(image.getRegionWidth() - 1), y);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void changeVal(float val){
		try{
			value.set(value.getDeclaringClass(), val);
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public float getValue(){
		try{
			return value.getFloat(value);
		} catch(Exception e){
			e.printStackTrace();
			return -1;
		}
	}
	
	public float getMaxValue(){ return maxValue; }
}
