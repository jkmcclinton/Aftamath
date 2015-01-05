package handlers;

import static handlers.Vars.PPM;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import entities.Mob;

public class BoundedCamera extends OrthographicCamera{
	
	private float minx;
	private float maxx;
	private float miny;
	private float maxy;
	
	public boolean reached, focusing, moving;
	private Vector2 focus;
	private Mob character;
	private boolean flipPlayer;
	
	private static final float y = 15f; //offset camera up
	
	public BoundedCamera(){
		this(0, 0, 0, 0);
	}
	
	public BoundedCamera(float minx, float maxx, float miny, float maxy){
		super();
		setBounds(minx, maxx, miny, maxy);
		setPosition(0, 0);
	}
	
	public void locate() {
		final float min = .5f;
		final float max = 20f;
		
		if(focus == null){
			setPosition(character.getPosition().x * PPM , character.getPosition().y * PPM + y);
		} else {
			if(reached)
				position.set(new Vector3(focus, 0));
			else {
				float dx = focus.x - position.x;
				float dy = focus.y - position.y; 

				if(Math.abs(dx) < .5f && Math.abs(dy) < .5f) {
					if (!focusing) {
						focus = null;
						reached = false;
					} else 
						reached = true;
					moving = false;
				} else {
					float x1 = position.x + (1 / PPM) * dx * 5;
					float y1 = position.y + (1 / PPM) * dy * 5;
					float dx1 = x1 - position.x;
					float dy1 = y1 - position.y;
					
					if (Math.abs(dx) > .5f){
						if (Math.abs(dx1) < min) x1 = position.x + min * (dx1 / Math.abs(dx1));
						if (Math.abs(dx1) > max) x1 = position.x + max * (dx1 / Math.abs(dx1));
					} else x1 = focus.x;
					
					if(Math.abs(dy) > .5f){
						if (Math.abs(dy1) < min) y1 = position.y + min * (dy1 / Math.abs(dy1));
						if (Math.abs(dy1) > max) y1 = position.y + max * (dy1 / Math.abs(dy1));
					} else y1 = focus.y;
					
					position.set(new Vector2(x1, y1), 0);
				}
			}
		}
		update();
	}
	
	public void setFocus(Vector2 f){
		this.focus = new Vector2(f.x * PPM, f.y * PPM + y);
		focusing = moving = true;
		reached = false;
		
		flipPlayer = character.getDirection();
		float dx = focus.x - character.getPosition().x;
		
		if(dx > 0 && flipPlayer) character.changeDirection();
		if(dx < 0 && !flipPlayer) character.changeDirection();
	}
	
	public void removeFocus(){ 
		Vector2 f = character.getPosition(); 
		focus = new Vector2(f.x * PPM, f.y * PPM + y);
		
		reached = focusing = false;
		moving = true;
		
		if (flipPlayer != character.getDirection())
			character.changeDirection();
	}
	
	public Vector2 getFocus() {return focus;}
	
	public void setBounds(float minx, float maxx, float miny, float maxy){
		this.maxx = maxx;
		this.minx = minx;
		this.miny = miny;
		this.maxy = maxy;
	}

	public void setPosition(float x, float y) {
		if(x > maxx - viewportWidth / 2) x = maxx - viewportWidth / 2;
		if(x < minx + viewportWidth / 2) x = minx + viewportWidth / 2;
		if(y > maxy) y = maxy;
		if(y < miny) y = miny;
		position.set(x, y, 0);
	}
	
	public void setPlayer(Mob player) { this.character = player; }
	public void setCharacter(Mob mob) { this.character = mob; }

}
