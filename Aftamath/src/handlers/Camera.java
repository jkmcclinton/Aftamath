package handlers;

import static handlers.Vars.PPM;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;

import entities.Mob;

public class Camera extends OrthographicCamera{
	
	private float minx;
	private float maxx;
	private float miny;
	private float maxy;
	
	public boolean reached, focusing, moving, zooming;
	private float goalZoom, oldZoom;
	private float zoomTime, goalTime;
	private boolean flipPlayer;
	private Vector2 focus, lastPosition;
	private Mob character;
	private RefocusTrigger trigger;
	
	public float YOFFSET = 15f; //offset camera up
	
	public static final float ZOOM_CLOSE = .75f;
	public static final float ZOOM_NORMAL = 1.2f;
	public static final float ZOOM_FAR = 2f;
	public static final float ZOOM_VERYFAR = 3.35f;
	public static final float DEFAULT_ZOOM_TIME	= 1f;
	
	public Camera(){
		this(0, 0, 0, 0);
	}
	
	public Camera(float range){
		this(0, 0, 0, 0);
	}
	
	public Camera(float minx, float maxx, float miny, float maxy){
		super();
		setBounds(minx, maxx, miny, maxy);
	}
	
	public void locate(float dt) {
		if(focus == null){
			setPosition(character.getPosition().x * Vars.PPM , character.getPosition().y * Vars.PPM + YOFFSET);
		} else {
			
		final float min = 1f;
		final float max = 20f;
			if(reached)
				setPosition(focus);
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
					float x1 = position.x + (1 / Vars.PPM) * dx * 5;
					float y1 = position.y + (1 / Vars.PPM) * dy * 5;
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

					if(x1 > maxx - viewportWidth / 2) x1 = maxx - viewportWidth / 2;
					if(x1 < minx + viewportWidth / 2) x1 = minx + viewportWidth / 2;
					if(y1 > maxy) y1 = maxy;
					if(y1 < miny) y1 = miny;
					
					lastPosition = new Vector2(position.x, position.y);
					position.set(new Vector2(x1, y1), 0);
					
					//if focus is out of bounds, hence no camera movement
					if(position.x-lastPosition.x==0 && position.y-lastPosition.y==0){
						reached = true;
						moving = false;
					}
				}
			}
		}
		
		if(zooming){
			float diff = goalZoom - zoom;
			zoomTime += dt;
			
			if(Math.abs(diff)<=.005f || zoomTime>=goalTime){
				zoom = goalZoom;
				zooming = false;
			} else {
				float a = (float) (-(goalZoom-.95f*oldZoom)/(Math.pow(goalTime,2)));
				float y = (a*(zoomTime-goalTime)*(zoomTime-goalTime) + goalZoom);
				zoom=y;
				fixOffset();
			}
		} 
		update();
	}
	
		
	public void zoom(float newZoom){ zoom(newZoom, DEFAULT_ZOOM_TIME); }
	public void zoom(float newZoom, float goalTime){
		goalZoom = newZoom;
		this.goalTime = goalTime;
		oldZoom = zoom;
		zooming = true;
		zoomTime = 0;
	}
	
	public void instantZoom(float newZoom){
		zoom = newZoom;
		fixOffset();
	}
	
	public void fixOffset(){
		if (zoom<=.8){
			YOFFSET = 0;
		} else if (zoom>=1.2f){ 
			YOFFSET = 37;
		} else {
			YOFFSET = 92.5f*zoom-74;
		}
	}
	
	public void setFocus(Vector2 f){
		this.focus = new Vector2(f.x * Vars.PPM, f.y * Vars.PPM);
		focusing = moving = true;
		reached = false;
		
		flipPlayer = character.getDirection();
		float dx = focus.x - character.getPosition().x;
		
		if(dx > 0 && flipPlayer) character.changeDirection();
		if(dx < 0 && !flipPlayer) character.changeDirection();
	}
	
	public void removeFocus(){ 
		Vector2 f = character.getPosition(); 
		focus = new Vector2(f.x * PPM, f.y * PPM + YOFFSET);
		
		reached = focusing = false;
//		moving = true;
		
		if (flipPlayer != character.getDirection())
			character.changeDirection();
	}
	
	public Vector2 getFocus() {return focus;}
	public void setVerticalOffset(float y){ YOFFSET = y; }

	public void setPosition(Vector2 v){ setPosition(v.x, v.y); }
	public void setPosition(float x, float y) {
		if(x > maxx - viewportWidth / 2) x = maxx - viewportWidth / 2;
		if(x < minx + viewportWidth / 2) x = minx + viewportWidth / 2;
		if(y > maxy) y = maxy;
		if(y < miny) y = miny;
		position.set(x, y, 0);
	}

	public void setBounds(float minx, float maxx, float miny, float maxy){
		this.maxx = maxx;
		this.minx = minx;
		this.miny = miny;
		this.maxy = maxy;
	}
	
	public void setTrigger(RefocusTrigger trigger){ this.trigger = trigger; }
	public RefocusTrigger getTrigger(){ return trigger; }
	public void setCharacter(Mob mob) { this.character = mob; }  //set camera focus to the character
	public Mob getCharacter(){ return character; }
}
