package characters;

import static handlers.Vars.PPM;
import handlers.Entity;
import handlers.Vars;
import main.Game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

import entities.Mob;

public abstract class NPC extends Mob {
	
	//these variables determine what the NPC is doing
	protected int state, defaultState;
	public static final int STATIONARY = 0; 
	public static final int IDLEWALK = 1;
	public static final int FOLLOWING = 2;
	public static final int FACEPLAYER = 3;
	public static final int FACEOBJECT = 4;
	
	protected Vector2 goalPosition;
	protected float idleWait;
	protected float idleTime;
	protected float time;
	protected boolean reached, locked;
	protected Entity focus;
	
	protected static final int RANGE = 250;
	
	public NPC(String name, String ID, float x, float y, int w, int h, short layer){
		super(name, ID, x, y, w, h, layer);
		health = MAX_HEALTH = 20;
		
		goalPosition = new Vector2((float) (((Math.random() * 21)+x)/PPM), y);
		idleWait = (float)(Math.random() *(RANGE)+100);
		time = 0;
	}
	
	public void setState(int state){ 
		if (state == FOLLOWING) follow();
		else this.state = state;
	}
	
	public void setState(int state, Entity focus){ 
		if (state == FOLLOWING) follow();
		else this.state = state;
		
		this.focus = focus;
	}
	
	public void setDefaultState(int state){
		this.state = defaultState = state;
	}
	
	public void resetState(){ state = defaultState; }
	public int getState(){ return state; }
	
	public void update(float dt){
		time += dt;
		if (state != STATIONARY && isOnGround() && !locked && !controlled) act();
		if (controlled) doAction(controlledAction);
		
		if (action == IDLE) animation.removeAction();
		if (!isOnGround()) {
			setAnimation(FALLING);
		}
		
		animation.update(dt);
		
		//stepping sound
		if (time >= MOVE_DELAY && isOnGround() && Math.abs(body.getLinearVelocity().x) >= MAX_VELOCITY / 2){
			Gdx.audio.newSound(new FileHandle("res/sounds/step1.wav")).play(Game.volume);
			time = 0;
		}
		
		if (action == WALKING) action = IDLE;
	}
	
	public void act(){
		float dx/*, dy*/;
		
		switch (state){
			case IDLEWALK:
				if(reached) idleTime++;
				if(idleTime >= idleWait && reached) reached = false;
				else if (!reached){
					if (!canMove()) {
						goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
						idleWait = (float)(Math.random() *(RANGE) + 100);
						idleTime = 0;
						reached = true;
					}
					else {
						dx = (goalPosition.x - body.getPosition().x) * PPM ;
						if(dx < 1 && dx > -1){
							goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
							idleWait = (float)(Math.random() *(RANGE) + 100);
							idleTime = 0;
							reached = true;
						} else {
							if(dx < 1) left();
							if(dx > -1) right();
						}
					}
				}
				break;
			case FACEPLAYER:
				facePlayer();
				break;
			case FACEOBJECT:
				faceObject(focus);
				break;
			case FOLLOWING:
				facePlayer();
				dx = player.getPosition().x - body.getPosition().x;
				
				float m = MAX_DISTANCE * player.getFollowerIndex(this);
				
				if(dx > m/PPM) right();
				else if (dx < -1 * m/PPM) left();
				break;
		
		}
	}
	
	public boolean mustJump(){
		// determine if object is in way
		return false;
	}
	
	public void follow(){
		state = FOLLOWING;
		player.addFollower(this);
	}
	
	public void stay(){
		state = FACEPLAYER;
		player.removeFollower(this);
	}
	
	public void lock() { locked = true; }
	public void unlock() { locked = false; }
	
	public void create(){
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((width-2)/PPM, (height)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.DynamicBody;
		fdef.shape = shape;

		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = Vars.BIT_GROUND | Vars.BIT_PROJECTILE;
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(getID()));
		
		createFootSensor();
		createInteractSensor();
	}

}
