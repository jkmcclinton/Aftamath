package entities;

import static handlers.Vars.PPM;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;

import box2dLight.PointLight;
import handlers.FadingSpriteBatch;
import handlers.Vars;

public class Projectile extends Entity {

	//private Vector2 vector;
	protected float damageVal, speed, restitution, killTime, totKillTime, gScale;
	protected boolean impacted, isSensor = true;
	protected BodyType bodyType;
	protected ProjectileType type;
	protected KillType killType;
	protected String shape;
	protected Vector2 velocity;
	protected Mob owner;
	protected DamageType damageType;
	protected PointLight pL;
	
	public static enum ProjectileType{
		FIREBALL, ICE_SPIKE, BOULDER, ELECTRO_BALL, SPELL, ITEM
	}
	
	private static enum KillType{
		ON_IMPACT, TIMED, BOUNCE
	}
	
	protected static final float ANIM_RATE = 1/12f;
	
	//for instantiating throwable items
//	public Projectile(Mob owner, Item type, float x, float y, Vector2 target){
//		super(x,y,Item.getID());
//	}
	
	public Projectile(Mob owner, ProjectileType type, float x, float y, Vector2 target){
		super(x,y,getID(type));
		
		this.type = type;
		this.animation.setBaseDelay(ANIM_RATE);
		this.layer = Vars.BIT_BATTLE;
		this.owner = owner;
		this.killType = KillType.ON_IMPACT;
		this.shape = "box";
		bodyType = BodyType.KinematicBody;
		
		setDimensions();
		instantiateType();
		this.velocity = Vars.getVelocity(owner.getPixelPosition(), target, speed);
	}
	
	public float getDamageVal() { return damageVal; }
	public Mob getOwner() { return owner; }
	public Vector2 getVelocity(){ return velocity; }
	
	public void update(float dt){
		totKillTime+=dt;
		if(totKillTime>=killTime && killType.equals(KillType.TIMED))
			main.removeBody(getBody());
		if(pL!=null)
			pL.setPosition(getPixelPosition());
		if(body.getLinearVelocity().x < 0 && !isFacingLeft()) changeDirection();
		else if(body.getLinearVelocity().x > 0 && isFacingLeft()) changeDirection();
		animation.update(dt);
	}
	
	public void render(FadingSpriteBatch sb){
		Color overlay = sb.getOverlay();
		if(pL!=null) sb.setColor(Vars.DAY_OVERLAY);
		switch(damageType){
		case ELECTRO:
		case ROCK:	
			sb.draw(animation.getFrame(), getPixelPosition().x - rw, getPixelPosition().y-rh/4 - 1);
			break;
		default:	
			Vector2 loc = new Vector2(getPixelPosition().x - rw, getPixelPosition().y);
			Vector2 vel = body.getLinearVelocity();
			float angle = (float)(Math.atan(vel.y/vel.x)*180/Math.PI);
			sb.draw(animation.getFrame(), loc.x, loc.y, rw, rh, width, height, 1, 1, angle);
//			 draw(region, x, y, originX, originY,  width,  height, scaleX, scaleY, rotation)
			break;
		
		}
		if(sb.isDrawingOverlay())
			sb.setColor(overlay);
	}

	//handling for when projectile collides with something
	//doesn't necessarily remove it from the world
	public void impact(Entity target) {
		if(target==null) return;
		if(!(target instanceof Mob))

			//forced bounce
			if(!killType.equals(KillType.ON_IMPACT)){
				int x = 1;
				if(facingLeft) x = -1;

				body.setLinearVelocity(speed*x, 4*restitution);
			}

		//conditions for removing projectile
		if(owner.equals(target)) return;
		switch(killType){
		case ON_IMPACT:
			kill();
			break;
		case BOUNCE:
			if(target.destructable){
				kill();
			}
			break;
		default:
			break;
		}

		if(!(target.destructable) && !(target instanceof Ground)) playCollideSound();
		if(impacted || !target.destructable) return;
		
		//apply damage to object
		impacted = true;

		if(target instanceof Mob){
			((Mob) target).damage(damageVal, damageType, owner);
//			main.addHealthBar(target);
			
			if(damageType.equals(DamageType.ROCK) && target.equals(main.character))
				main.getCam().shake();
			
			//apply recoil for sensor type bodies
			if(killType.equals(KillType.ON_IMPACT))
				target.getBody().applyForceToCenter(speed, 0.5f, true);
		} else 
			target.damage(getDamageVal(), damageType);
	}
	
	public void kill(){
		main.removeBody(getBody());
		if(pL!=null)
			main.getRayHandler().lightList.removeValue(pL, true);
	}
	
	// give sound effect to collision based on damage type
	public void playCollideSound(){
		String sound;
		switch(damageType){
		case ELECTRO:
			sound = "noise1";
			break;
		case DARKMAGIC:
		case FIRE:
			sound = "explosion1";
			break;
		case ICE:
			sound = "clap1";
			break;
		case ROCK:
			sound = "shake3";
			break;
		default:
			sound = "chirp2";

		}
		main.playSound(getPosition(), sound);
	}
	
	public static String getID(ProjectileType type){
		switch(type){
		case BOULDER:
			return "boulder";
		case FIREBALL:
			return "fireball";
		case ICE_SPIKE:
			return "icespike";
		case ITEM:
			return "item";
		case SPELL:
			return "greenfire";
		case ELECTRO_BALL:
			return "electroball";
		default:
			return "";
		}
	}
	
	//predict if object will hit anything
//	public Pair<Boolean, Entity> willHit(){
//		MyQueryCallback cb = new MyQueryCallback();
//		Vector2 startingPosition = getPosition();
//		Vector2 startingVelocity = body.getLinearVelocity();
//
//		for (int i = 0; i < 2; i++) {
//			Vector2 trajPos = getTrajectoryPoint( startingPosition, startingVelocity, i );
//
//			if ( i > 0 ) { //avoid degenerate raycast where start and end point are the same
//				main.getWorld().QueryAABB(cb, trajPos.x-1, trajPos.y-1, 
//						trajPos.x+1, trajPos.y+1);
//				if (cb.found().getKey()){
//					return cb.found();
//				}
//			}
//		}
//		return new Pair<>(false, null);
//	}
//
//	Vector2 getTrajectoryPoint(Vector2 startPos, Vector2 startVel, float n )   {
//		//velocity and gravity are given per second but we want time step values here
//		float t = Vars.DT;
//		Vector2 stepVel = new Vector2(t*startVel.x, t*startVel.x); // m/s
//		Vector2 stepGrav = main.getWorld().getGravity(); // m/s/s
//		stepGrav.x*=t*t;
//		stepGrav.y*=t*t;
//
//		Vector2 vec = new Vector2(startPos.x + n * stepVel.x+ 0.5f * (n*n+n) * stepGrav.x,
//				startPos.y + n * stepVel.y + 0.5f * (n*n+n) * stepGrav.y);
//
//		return vec;
//	}
	
	private void instantiateType(){
		gScale = 1;
		switch(type){
		case FIREBALL:
			damageType = DamageType.FIRE;
			damageVal = 1;
			speed = 3;
			restitution = 0.25f;
			break;
		case SPELL:
			damageType = DamageType.DARKMAGIC;
			damageVal = 1;
			speed = 3;
			restitution = 0.25f;
			break;
		case ICE_SPIKE:
			damageType = DamageType.ICE;
			damageVal = 1;
			speed = 2.5f;
			restitution = 0.15f;
			break;
		case ELECTRO_BALL:
			damageType = DamageType.ELECTRO;
			bodyType = BodyType.DynamicBody;
			killType = KillType.BOUNCE;
			shape = "circle";
			damageVal = 1.1f;
			speed = 3.5f;
			restitution = .75f;
			break;
		case BOULDER:
			damageType = DamageType.ROCK;
			bodyType = BodyType.DynamicBody;
			killType = KillType.BOUNCE;
			shape = "circle";
			damageVal = 1.1f;
			restitution = .01f;
			speed = 1.5f;
			gScale = .24f;
			isSensor = false;
			break;
		default:
			damageType = DamageType.PHYSICAL;	
			damageVal = 1;
			speed = 3;
			restitution = 0.25f;
		}
		
		damageVal*=owner.strength;
	}
	
	public void create(){
		init = true;
		
		//hitbox
		if(this.shape.equals("circle")){
			CircleShape shape = new CircleShape();
			shape.setRadius((rw)/PPM);
			fdef.shape = shape;
		}else{
		PolygonShape shape = new PolygonShape();
			shape.setAsBox((rw)/PPM, (rh)/PPM);
			fdef.shape = shape;
		}
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = bodyType;
		bdef.bullet = true;
		bdef.gravityScale = gScale;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.isSensor = isSensor;
		fdef.restitution = restitution;
		fdef.filter.maskBits = (short) Vars.BIT_GROUND | Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData("projectile");
		body.setLinearVelocity(velocity);
		
		String sound;
		Color c;
		switch(damageType){
		case ELECTRO:
			sound = "chirp2"; 
			c = new Color(Vars.SUNSET_GOLD); c.a =.5f;
			pL = new PointLight(main.getRayHandler(), Vars.LIGHT_RAYS, c,
					100, x, y);
			break;
		case DARKMAGIC:
			sound = "spooky";
			c = new Color(Color.GREEN); c.a =.5f;
			pL = new PointLight(main.getRayHandler(), Vars.LIGHT_RAYS, c,
					75, x, y);
			break;
		case FIRE:
			sound = "swish1"; 
			c = new Color(Vars.SUNSET_ORANGE); c.a =.5f;
			pL = new PointLight(main.getRayHandler(), Vars.LIGHT_RAYS, c,
					75, x, y);
			break;
		case ICE:
			sound = "sweep"; 
			c = new Color(Color.CYAN); c.a =.5f;
			pL = new PointLight(main.getRayHandler(), Vars.LIGHT_RAYS, c,
					25, x, y);
			break;
		case ROCK:
			sound = "rockShot"; break;
		default:
			sound = "jump3";
		}
		main.playSound(getPosition(), sound);
	}
}
