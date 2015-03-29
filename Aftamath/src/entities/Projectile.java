package entities;

import static handlers.Vars.PPM;
import handlers.Vars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class Projectile extends Entity {

	//private Vector2 vector;
	protected float speed = 1/12f;
	protected float damageVal;
	protected float restitution;
	protected BodyType type;
	protected Vector2 initial;
	protected Mob owner;
	
	public Projectile(Mob owner, String ID, float x, float y, int w, int h, Vector2 initial, BodyType type){
		super(x,y,w,h,ID);
		animation.setSpeed(speed);
		layer = Vars.BIT_PROJECTILE;
		this.owner = owner;
		this.initial = initial;
		
		this.type = type;
	}
	
	public float getDamageVal() { return damageVal; }
	public Mob getOwner() { return owner; }
	
	public static int getVelocity(boolean direction){ return 2; }
	
	public void update(float dt){
		if(body.getLinearVelocity().x < 0 && !direction) changeDirection();
		else if(body.getLinearVelocity().x > 0 && direction) changeDirection();
		animation.update(dt);
	}
	
	public void create(){
		Gdx.audio.newSound(new FileHandle("res/sounds/chirp5.wav")).play();
		
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((width-2)/PPM, (height)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = type;
		bdef.bullet = true;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.shape = shape;
		fdef.isSensor = true;
		fdef.restitution = restitution;
		fdef.filter.maskBits = Vars.BIT_GROUND | Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3;
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData("projectile");
		
		body.setLinearVelocity(initial);
	}
	
	public void render(){}
}
