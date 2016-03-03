package handlers;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;

import entities.Entity;
import entities.Mob;
import scenes.Scene;

public class Camera extends OrthographicCamera{

	public boolean reached, focusing, moving, zooming, constrained;
	public float offsetY = 20f; //offset camera up

	private float minx, maxx, miny, maxy, amplitude, shakeTime, phaseX, phaseY;
	private float frequency = 150*1000; // in Hertz
	private float tempMinx, tempMaxx, tempMiny, tempMaxy;
	private float defaultZoom, goalZoom, oldZoom, zoomTime, goalTime;
	private boolean locked = true, shaking;
	private Boolean wasCharacterFacingLeft;
	private Entity focus;
	private Vector2 tmpFocus;
	private Mob character;
	private RefocusTrigger trigger;
//	private Camtrack currentTrack;

	public static final float ZOOM_CLOSE = .75f;
	public static final float ZOOM_NORMAL = 1.2f;
	public static final float ZOOM_FAR = 2f;
	public static final float ZOOM_VERYFAR = 3.35f;
	public static final float ZOOM_MAX = 4.25f;
	public static final float DEFAULT_ZOOM_TIME	= 1f;

	public Camera(){
		this(0, 0, 0, 0);
		reset();
	}

	public Camera(float range){
		this(0, 0, 0, 0);
	}

	public Camera(float minx, float maxx, float miny, float maxy){
		super();
		setBounds(minx, maxx, miny, maxy);
	}

	public void reset(){
		reached = focusing = moving = zooming = false;
		goalZoom = oldZoom = ZOOM_NORMAL;
		zoom = defaultZoom;
		zoomTime = goalTime = 0;
		wasCharacterFacingLeft = null;
		focus = null;
		character = null;
		trigger = null;
	}

	private final float min = 1f;
	private final float max = 20f;
	public void locate(float dt) {
//		if(currentTrack!=null)
		if(!moving)
			if(tmpFocus!=null)
				setPosition(tmpFocus);
			else{
				if(focus == null)
					focus = character;
				
				if(focus.getBody()!=null)
					setPosition(focus.getPixelPosition().x, focus.getPixelPosition().y + offsetY);
			}
		else {
			float dx, dy;
			if(tmpFocus!=null){
				dx = tmpFocus.x - position.x;
				dy = tmpFocus.y - position.y;
			} else {
				dx = focus.getPixelPosition().x - position.x;
				dy = focus.getPixelPosition().y - position.y + offsetY;
			}

			if(Math.abs(dx) < .5f && Math.abs(dy) < .5f) {
				moving = false;
				if(character.getGameState().currentScript!=null)
					if(character.getGameState().currentScript.getActiveObject().equals(this))
						character.getGameState().currentScript.setActiveObj(new Entity());
			} else { 
				float x1 = position.x + (dx * 5 / Vars.PPM);
				float y1 = position.y + (dy * 5 / Vars.PPM);
				float dx1 = x1 - position.x;
				float dy1 = y1 - position.y;

				if (Math.abs(dx) > .5f){
					if (Math.abs(dx1) < min) x1 = position.x + min * (dx1 / Math.abs(dx1));
					if (Math.abs(dx1) > max) x1 = position.x + max * (dx1 / Math.abs(dx1));
				} else x1 = dx + position.x;

				if(Math.abs(dy) > .5f){
					if (Math.abs(dy1) < min) y1 = position.y + min * (dy1 / Math.abs(dy1));
					if (Math.abs(dy1) > max) y1 = position.y + max * (dy1 / Math.abs(dy1));
				} else y1 = dy + position.y;

				//bounds limit
				if(x1 > maxx - viewportWidth * zoom / 2) x1 = maxx - viewportWidth * zoom / 2;
				if(x1 < minx + viewportWidth * zoom / 2) x1 = minx + viewportWidth * zoom / 2;
				if(y1 > maxy - viewportHeight * zoom / 2) y1 = maxy - viewportHeight * zoom / 2;
				if(y1 < miny + viewportHeight * zoom / 2) y1 = miny + viewportHeight * zoom / 2;

				//if focus is out of bounds, hence no camera movement
				if(position.x-x1==0 && position.y-y1==0){
					moving = false;
				} else
					position.set(new Vector2(x1, y1), 0);
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

		if(shaking && !character.getGameState().paused) applyShake(dt);
		update();
	}
	
	//give new offset for camera
	private void applyShake(float dt){
		shakeTime +=dt;
		float offX = (float) (amplitude * Math.sin(frequency * shakeTime + phaseX));
		float offY = (float) (amplitude * Math.cos(frequency * shakeTime + phaseY));
		position.set(position.x+offX, position.y+offY, 0);
		
		amplitude-=2*dt;
		if(amplitude<=0)
			shaking = false;
	}
	
	public void shake(){
		shake(3);
	}
	
	public void shake(float maxAmp){
		if(amplitude >= maxAmp) return;
		amplitude = maxAmp;
		shakeTime = 0;
		shaking = true;
		
		phaseX = (float) Math.random()*1.2f;
		phaseY = (float) Math.random()*1.2f;
	}

	public void resetZoom(){ zoom(defaultZoom, DEFAULT_ZOOM_TIME); }
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
			offsetY = 0;
		} else if (zoom>=1.2f){ 
			offsetY = 36;
		} else {
			offsetY = 92.5f*zoom-74;
		}
	}

	public void setFocus(float x, float y){
		setFocus(new Vector2(x, y));
	}

	public void setFocus(Vector2 f){
//		tmpFocus = new Vector2(f.x * Vars.PPM, f.y * Vars.PPM);
		tmpFocus = new Vector2(f);
		focusing = moving = true;
		reached = false;

		wasCharacterFacingLeft = character.getDirection();
		float dx = tmpFocus.x - tmpFocus.x;

		if(dx > 0 && wasCharacterFacingLeft) character.changeDirection();
		else if(dx < 0 && !wasCharacterFacingLeft) character.changeDirection();
	}

	public void setFocus(Entity focus){
		focusing = moving = true;
		this.focus = focus;

		wasCharacterFacingLeft = character.getDirection();
		float dx = focus.getPosition().x - character.getPosition().x;

		if(dx > 0 && wasCharacterFacingLeft) character.changeDirection();
		else if(dx < 0 && !wasCharacterFacingLeft) character.changeDirection();
	}

	public void removeFocus(){ 
		if(tmpFocus==null && focus==character) return;
		tmpFocus = null;
		focus = character;
		focusing = false;
		moving = true;

		if(wasCharacterFacingLeft!=null)
			if (wasCharacterFacingLeft != character.getDirection()){
				character.changeDirection();
				wasCharacterFacingLeft = null;
			}
	}

	public Entity getFocus() {return focus;}
	public void setVerticalOffset(float y){ offsetY = y; }
	public void setPosition(Vector2 v){ setPosition(v.x, v.y); }
	public void setPosition(float x, float y) {
		Vector2 v = new Vector2(x, y);

		if(locked){
			if(constrained) v = adjust(tempMinx, tempMaxx, tempMiny, tempMaxy, v);
			else v = adjust(minx, maxx, miny, maxy, v);
		}
		
		position.set(v, 0);
	}

	public void setPosition(Entity focus) {
		this.focus = focus;

		if(locked){
			Vector2 v;
			if(constrained) v = adjust(tempMinx, tempMaxx, tempMiny, tempMaxy, focus.getPosition());
			else v = adjust(minx, maxx, miny, maxy, focus.getPosition());
			position.set(v, 0);
		} else 
			position.set(focus.getPosition(), 0);
	}

	private Vector2 adjust(float minx, float maxx, float miny, float maxy, Vector2 v){
		if(v.x > maxx - viewportWidth * zoom / 2) v.x = maxx - viewportWidth * zoom / 2;
		if(v.x < minx + viewportWidth * zoom / 2) v.x = minx + viewportWidth * zoom / 2;
		if(v.y > maxy - viewportHeight * zoom / 2) v.y = maxy - viewportHeight * zoom / 2;
		if(v.y < miny + viewportHeight * zoom / 2) v.y = miny + viewportHeight * zoom / 2;
		return v;
	}

	public void bind(Scene s, boolean b2d){
		float rate = 1;
		if (b2d) rate = Vars.PPM;

		bind(Vars.TILE_SIZE*4/rate, (s.width-Vars.TILE_SIZE*4)/rate,
				0, (s.height + 25*Vars.TILE_SIZE)/rate);
	}

	public void bind(float minx, float maxx, float miny, float maxy){
		this.maxx = maxx;
		this.minx = minx;
		this.miny = miny;
		this.maxy = maxy;
		constrained = false;
	}

	public void setBounds(float minx, float maxx, float miny, float maxy){
		if(maxx < this.maxx) tempMaxx = maxx;
		else tempMaxx = maxx;
		if(minx > this.minx) tempMinx = minx;
		else tempMinx = minx;
		if(maxy < this.maxy) tempMaxy = maxy;
		else tempMaxy = maxy;
		if(miny > this.miny) tempMiny = miny;
		else tempMiny = miny;

		constrained = true;
	}

	public void resetBounds(){
		constrained = false;
	}

	public void setLock(boolean val){ locked = val; }
	public void setDefaultZoom(float zoom){ defaultZoom = zoom; }
	public void setTrigger(RefocusTrigger trigger){ this.trigger = trigger; }
	public RefocusTrigger getTrigger(){ return trigger; }
	public void setCharacter(Mob character) { this.character = character; }  //set camera focus to the character
	public Mob getCharacter(){ return character; }

	public float getDefaultZoom() { return defaultZoom; }
}
