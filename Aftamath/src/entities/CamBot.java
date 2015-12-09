package entities;

import handlers.Vars;

import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class CamBot extends Mob {
	
	public int speed = 4;
	public static int DEFAULT_SPEED = 1;

	public CamBot(float x, float y) {
		super("CamBot", "camBot", 0, x, y, Vars.BIT_LAYER1);
		width = height = 5;
		rh = rw = width/2;
		
		canClimb = true;
	}
	
	public void update(float dt){
		speed = 1;
	}
	
	public void create(){
		init = true;
		bdef = new BodyDef();
		fdef = new FixtureDef();
		//hitbox
		setDirection(false);
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw-4)/Vars.PPM, (rh)/Vars.PPM);
		
		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_BATTLE);
		fdef.filter.categoryBits = layer;
		fdef.isSensor = true;
		body.setBullet(true);
		body.createFixture(fdef).setUserData(Vars.trimNumbers(ID));
		body.setFixedRotation(true);
	}
	
	public void climb(){
		y+=1*speed;
		main.removeBody(body);
		create();
	}
	
	public void descend(){
		y-=1*speed;
		main.removeBody(body);
		create();
	}
	
	public void left(){
		x-=1*speed;
		main.removeBody(body);
		create();
	}
	
	public void right(){
		x+=1*speed;
		main.removeBody(body);
		create();
	}
	
	public void run(){
		speed = 4;
	}

	public void follow(Entity focus) {}
	public void stay() { } 
	public Mob copy() { return new CamBot(0,0); }
}
