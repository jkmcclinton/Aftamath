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
	protected float damageVal, speed, restitution;
	protected BodyType bodyType;
	protected ProjectileType type;
	protected Vector2 velocity;
	protected Mob owner;
	protected DamageType damageType;
	
	public static enum ProjectileType{
		FIREBALL, ICE_SPIKE, BOULDER, THUNDER_BOLT, SPELL, ITEM
	}
	
	protected static final float ANIM_RATE = 1/12f;
	
	//for instantiating throwable items
//	public Projectile(Mob owner, Item type, float x, float y, Vector2 target){
//		super(x,y,Item.getID());
//	}
	
	public Projectile(Mob owner, ProjectileType type, float x, float y, Vector2 target){
		super(x,y,getID(type));
		
		animation.setSpeed(ANIM_RATE);
		layer = Vars.BIT_BATTLE;
		this.owner = owner;
		this.velocity = Vars.getVelocity(getPosition(), target, speed);
		
		instantiateType();
	}
	
	public float getDamageVal() { return damageVal; }
	public Mob getOwner() { return owner; }
	public Vector2 getVelocity(){ return velocity; }
	
	public void update(float dt){
		if(body.getLinearVelocity().x < 0 && !isFacingLeft()) changeDirection();
		else if(body.getLinearVelocity().x > 0 && isFacingLeft()) changeDirection();
		animation.update(dt);
	}
	
	public void create(){
		init = true;
		Gdx.audio.newSound(new FileHandle("res/sounds/chirp5.wav")).play();
		
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((width-2)/PPM, (height)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = bodyType;
		bdef.bullet = true;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.shape = shape;
		fdef.isSensor = true;
		fdef.restitution = restitution;
		fdef.filter.maskBits = Vars.BIT_GROUND | Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData("projectile");
		
		body.setLinearVelocity(velocity);
	}

	public void impact(Entity target) {
		if (owner.equals(target)) return;
			main.playSound(getPosition(), "chirp2");
		
		if (target instanceof Mob){
			((Mob) target).damage(damageVal, damageType, owner);
			target.getBody().applyForceToCenter(velocity, true);
		} else {
			target.damage(getDamageVal(), damageType);
		}
			
		main.addBodyToRemove(getBody());
	}
	
	public static String getID(ProjectileType type){
		switch(type){
		case BOULDER:
			return "boulder";
		case FIREBALL:
			return "fireball";
		case ICE_SPIKE:
			return "iceSpike";
		case ITEM:
			return "item";
		case SPELL:
			return "fireball";
		case THUNDER_BOLT:
			return "thunderBolt";
		default:
			return "";
		}
	}
	
	private void instantiateType(){
		switch(type){
		case FIREBALL:
			damageType = DamageType.FIRE;
			bodyType = BodyType.KinematicBody;
			damageVal = 1;
			speed = 3;
			restitution = 0.25f;
			break;
		case ICE_SPIKE:
			damageType = DamageType.ICE;
			bodyType = BodyType.KinematicBody;
			damageVal = 1;
			speed = 3;
			restitution = 0.15f;
			break;
		default:
			damageType = DamageType.PHYSICAL;	
			bodyType = BodyType.KinematicBody;
			damageVal = 1;
			speed = 3;
			restitution = 0.25f;
		}
		
		damageVal*=owner.strength;
	}
	
}
