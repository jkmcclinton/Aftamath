package entities;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

public class Fireball extends Projectile {
	
	//return initial tra
	public static int getVelocity(boolean direction){
		if (direction) return -3;
		else return 3;
	}
	
	public Fireball(Mob owner, float x, float y, Vector2 initial){
		super(owner, "fireball", x, y, 10, 5, initial, BodyType.KinematicBody);
		damageVal = 1;
		restitution = 0.25f;
	}

}
