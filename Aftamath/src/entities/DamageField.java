package entities;

import static handlers.Vars.PPM;

import java.util.HashMap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;

import box2dLight.Light;
import box2dLight.PointLight;
import handlers.FadingSpriteBatch;
import handlers.PositionalAudio;
import handlers.Vars;

public class DamageField extends Entity {
	
	private float damageStrength, duration, lifeTime;
	private boolean isSensor;
	private Mob owner;
	private DamageType damageType;
	private HashMap<Entity, Float> victims;
	private PositionalAudio damageSound;
	private Light pL;
	
	protected static final float ANIM_RATE = 1/12f;

	public DamageField(float x, float y, int widthOffset, Mob owner){
		this(x, y, widthOffset, owner, DamageType.PHYSICAL);
	}

	public DamageField(float x, float y, int widthOffset, Mob owner, DamageType damageType){
		this(x, y, widthOffset, owner, damageType, getID(damageType));
	}
	
	public DamageField(float x, float y, int widthOffset, Mob owner, DamageType damageType, String image){
		init();
		this.ID = image;
		
		int dx = 1;
		if(owner.isFacingLeft()) {
			facingLeft=true;
			dx=-1;
		}
		if(owner.ducking)
			dx=0;
		
		setDimensions();
		loadSprite();
		
		width += widthOffset;
		this.x = x + dx*rw;
		this.y = y + rh;
		
		if(ID.equals("boulderFist"))
			rw = 20;
		
		this.owner = owner;
		this.damageType = damageType;
		this.animation.setBaseDelay(ANIM_RATE);
		this.layer = Vars.BIT_BATTLE;
		this.victims = new HashMap<>();
		
		instantiateType();
		width += widthOffset;
	}
	
	public void update(float dt){
		lifeTime+=dt;
		animation.update(dt);
		
		if((ID.equals("boulderFist") && lifeTime>=1.5f*ANIM_RATE && 
				lifeTime<=3*ANIM_RATE) || !ID.equals("boulderFist"))
			for(Entity e: victims.keySet()){
				if(victims.get(e)<=0){
					applyDamageEffect(e);
					victims.put(e, (float) (2*e.resistance));
				} else
					victims.put(e, victims.get(e)-dt);
			}
		
		//make body do shit
		switch(ID){
		case"boulderFist":
			if(body.getPosition().y*PPM>=y)
				body.setLinearVelocity(new Vector2(0, 0));
			break;
		default:
			//do nothing special
		}

		if(lifeTime>=duration){
			main.removeBody(body);
			finalize();
		}
	}
	
	public void render(FadingSpriteBatch sb){
		Color overlay = sb.getOverlay();
		if(pL!=null)sb.setColor(Vars.DAY_OVERLAY);
		switch(ID){
		case "boulderFist":	
			sb.draw(animation.getFrame(), x - width/2, y - rh - 2);
			break;
		default:	
			sb.draw(animation.getFrame(), getPixelPosition().x - rw, getPixelPosition().y - rh);
		}
		if(sb.isDrawingOverlay())
			sb.setColor(overlay);
	}
	
	
	//cause damage to the given entity
	public void applyDamageEffect(Entity e){
		if(e instanceof Mob)
			((Mob) e).damage(damageStrength, damageType, owner);
		else
			e.damage(damageStrength, damageType);
		playDamageSound(e);
		
		//apply special effect
		switch(ID){
		case "chillyWind": //instantly freeze the victim
			if(!e.frozen)
				e.freeze();
			break;
		case "fireyField": //instantly burn the victim
			if(!e.burning)
				e.ignite();
			break;
		case "boulderFist": //send flying upward
			if(e.getBody()!=null)
				e.getBody().applyForceToCenter(0f, 240f, true);
			break;
		}
	}
	
	public Mob getOwner(){ return owner; }
	
	//once victim has been added, immediately damage on field's next update call
	public void addVictim(Entity e){
		if(e.destructable && !owner.equals(e))
			victims.put(e, 0f);
	}
	
	public void removeVictim(Entity e){
		victims.remove(e);
	}
	
	public DamageType getDamageType(){ return damageType; }
	public Array<Entity> getVictims(){ 
		Array<Entity> arr = new Array<>();
		for(Entity e : victims.keySet())
			arr.add(e);
		return arr;
	}
	
	public void playDamageSound(Entity e){
		if(e.getBody()==null) return;
		
		String sound;
		switch(damageType){
		case DARKMAGIC:
			sound = "horror";
			break;
		case ELECTRO:
			sound = "noise1";
			break;
		case FIRE:
			sound = "explosion1";
			break;
		case ICE:
			sound = "clap1";
			break;
		case ROCK:
			sound = "boulder";
			break;
		default:
			sound = "chirp2";
		}
		main.playSound(e.getPosition(), sound);
	}
	
	//return the default ID of the image used for the field
	//this also determines the render type and ambient sound of the field
	//by default the field doesn't have
	private static String getID(DamageType type){
		switch(type){
		case DARKMAGIC:
			return "mutagenicHorror";
		case ELECTRO:
			return "electricField";
		case FIRE:
			return "fireyField";
		case ICE:
			return "chillyWind";
		case PHYSICAL:
			return "ghostlyPunches?";
		case ROCK:
			return "boulderFist";
		case WIND:
			return "tornado";
//		case MAGIC:
//			return "magicGas";
		default:
			return "damageField";
		}
	}
	
	//inittialize variables specific to the image used
	private void instantiateType(){
		isSensor= true;
		switch(ID){
		case "mutagenicHorror":
		case "fireyField":
		case "chillyWind":
			duration = 3;
			damageStrength = 1;
			break;
		case "electricField":
			duration = 3;
			damageStrength = 1.1f;
			break;
		case "boulderFist":
			//only lasts as long as the animation
//			duration = animation.getFrames().size*animation.getSpeed();
			duration = .45f;
			damageStrength = 1.1f;
			isSensor = false;
			break;
		default:	
			damageStrength = 1;
		}
		
		duration *= (owner.level + 1);
		damageStrength*=owner.strength;
	}
	
	public void create(){
		init = true;
		String sound;
		float yOff=0;

		//initial body shit
		switch(ID){
		case"boulderFist":
			yOff = -1.2f*height;
			break;
		default:
			//do nothing special
		}
		
		//define hitbox physics
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw)/PPM, (rh)/PPM);
		
		bdef.position.set(x/PPM, (y+yOff)/PPM);
		bdef.type = BodyType.KinematicBody;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.shape = shape;
		fdef.isSensor = isSensor;
		fdef.filter.maskBits = (short) Vars.BIT_GROUND | Vars.BIT_LAYER1 | Vars.BIT_PLAYER_LAYER | Vars.BIT_LAYER3;
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData("damageField");
		
		//make body do shit
		//determine sound/light for creation and initial offset
		Color c;
		switch(ID){
		case "electricField":
			sound = "sparking";
			c = new Color(Vars.SUNSET_GOLD); c.a =.5f;
			pL = new PointLight(main.getRayHandler(), Vars.LIGHT_RAYS, c,
					200, x, y);
			break;
		case "fireyField":
			sound = "crackling";
			c = new Color(Vars.SUNSET_ORANGE); c.a =.5f;
			pL = new PointLight(main.getRayHandler(), Vars.LIGHT_RAYS, c,
					150, x, y);
			break;
		case "chillyWind":
			sound = "air1"; 
			break;
		case "boulderFist":
			sound = "boulderFist"; 
			body.setLinearVelocity(new Vector2(0, 2f));
			break;
		default:
			sound = "";
		}
		if(!sound.isEmpty())
			damageSound = new PositionalAudio(getPosition(), sound, main);
	}
	
	public void finalize(){
		try {
			super.finalize();
			if(damageSound!=null){
				damageSound.stop();
				main.removeSound(damageSound);
			}
			if(pL != null)
				pL.remove();
			
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
