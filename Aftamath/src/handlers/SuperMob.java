package handlers;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;

import entities.Fireball;
import entities.Mob;
import entities.Projectile;

public abstract class SuperMob extends Mob {
	
	public static final int FIRE = 0;
	public static final int ICE = 1;
	public static final int ELECTRO = 2;
	public static final int ROCK = 3;
	public static final int FLY = 4;
	
	protected int level;
	protected int type;
	
	//protected Vector2 target;
	
	protected SuperMob(String name, String ID, int level, int type, float x, float y, int w, int h, short layer) {
		super(name, ID, x, y, w, h, layer);
		this.level =  level;
		this.type = type;
	}
	
	public Projectile attack(){
		return attack(0);
	}
	
	public Projectile attack(float focus){
		switch (type){
		case FIRE: return attack(new Vector2(Fireball.getVelocity(direction), focus));
		case ICE: return null;
		default: return null;
		}
	}

	public Projectile attack(Vector2 target){
		switch (type){
			case FIRE:
				float w = width/2	;
				if (direction) w *= -1;
				
				TextureRegion[] sprites = TextureRegion.split(texture, width*2, height*2)[ATTACKING];
				animation.setAction(sprites, 2, direction, ATTACKING);
				
				Fireball p = new Fireball(this, (body.getPosition().x + w/Vars.PPM)*Vars.PPM, body.getPosition().y*Vars.PPM, target);
				p.setPlayState(this.gs);
				p.create();
				return p;
			case ICE:
				return null;
			default: return null;
		}
	}
	
	public void levelUp(){
		if(level < 20) level++;
	}
	
	public int getLevel(){ return level; }
	public int getPowerType(){ return type; }
	public void setPowerType(int type){
		level = 1;
		this.type = type;
	}
}
