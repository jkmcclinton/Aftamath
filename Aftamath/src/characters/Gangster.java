package characters;

import static handlers.Vars.PPM;
import handlers.Vars;
import states.Play;

import com.badlogic.gdx.graphics.g2d.TextureRegion;

import entities.Projectile;

public class Gangster extends NPC {
	
	public static final int PULLGUN = 8;
	public static final int HOLDGUN = 9;
	public static final int LOWERGUN = 10; 
	public static final int BOAST = 11; 

	public Gangster(String name, int ID, int x, int y, short layer) {
		super(name, "gangster" + String.valueOf(ID), x, y, 32, 50, layer);
		
		 int[] tmp = {0, 4, 1, 2, 2, 4, 2, 2, 3, 4, 3, 4};
		 actionLengths = tmp;
		 actionTypes = tmp.length;
	}
	
	//animations
	 public void pullGun() {
		 TextureRegion[] sprites = TextureRegion.split(texture, width*2, height*2)[HOLDGUN];
		 animation.setFrames(sprites, Vars.ANIMATION_RATE, direction);
		 action = PULLGUN;
		 sprites = TextureRegion.split(texture, width*2, height*2)[PULLGUN];
		 animation.setAction(sprites, actionLengths[PULLGUN], direction, PULLGUN);
	 }
	 
	 public void lowerGun() {
		 TextureRegion[] sprites = TextureRegion.split(texture, width*2, height*2)[0];
		 animation.setFrames(sprites, Vars.ANIMATION_RATE, direction);
		 action = LOWERGUN;
		 sprites = TextureRegion.split(texture, width*2, height*2)[LOWERGUN];
		 animation.setAction(sprites, actionLengths[LOWERGUN], direction, LOWERGUN);
	 }
	  
	 public void shootGun(Play play) {
		 TextureRegion[] sprites = TextureRegion.split(texture, width*2, height*2)[ATTACKING];
		 animation.setAction(sprites, actionLengths[ATTACKING], direction, ATTACKING);
	 }
	 
	 
	 public void boast() {}
	 
	 public void doAction(int action){
			if (action > actionTypes) {
				controlled = false;
				controlledAction = 0;
				return;
			}
			
			if(!controlled){
				controlled = true;
				controlledAction = action;
			}
			
			switch(controlledAction){
			case WALKING:
				float dx = goal.x - getPosition().x;

				if(Math.abs(dx)*PPM > 1){
					if (dx>0) right();
					else left();
				} else controlled = false;
				break;
			case PULLGUN:
				if(this.action != PULLGUN)
					pullGun();
				if(animation.actionID != PULLGUN)
					controlled = false;
				break;
			case LOWERGUN:
				if(this.action != LOWERGUN)
					lowerGun();
				if(animation.actionID != LOWERGUN)
					controlled = false;
				break;
				default:
					controlled = false;
			}
		}
}
