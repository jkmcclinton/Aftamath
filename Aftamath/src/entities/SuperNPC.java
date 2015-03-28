package entities;

import static handlers.Vars.PPM;
import handlers.Entity;
import handlers.SuperMob;

import com.badlogic.gdx.math.Vector2;

public abstract class SuperNPC extends SuperMob{
	
	protected static final int STATIONARY = 0;
	protected static final int FACEPLAYER = 1;
	protected static final int ATTACK = 2;
	protected static final int IDLEATTACK = 3;
	private static final int RANGE = 250;
	
	protected int state;
	protected float attackTime, attackDelay;
	protected float idleTime, idleWait;
	protected boolean attacked, reached;
	protected Vector2 goalPosition;
	protected Entity focus;
	
	public SuperNPC(String name, String ID, int level, int type, float x, float y, int w, int h, short layer) {
		super(name, ID, level, type, x, y, w, h, layer);
		health = MAX_HEALTH = 30;
	}
	
	
	
	public void act(){
		float dx /*, dy*/;
		
		switch (state){
			case FACEPLAYER:
				facePlayer();
				break;
			case ATTACK:     //attack AI
				if(attacked) attackTime++;
				if(attackTime >= attackDelay && attacked) attacked = false;
				if(!attacked){
					gs.addObject(attack(focus.getPosition().x));
					attacked = true;
					attackDelay = (float) Math.random();
					attackTime = 0;
				}
				
				if(reached) idleWait++;
				if(idleTime >= idleWait && reached) reached = false;
				if(!reached){
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
		}
	}
	
	public void attack(Entity d){
		focus = d;
		state = ATTACK;
		
	}

	public void setDefaultState(int idlewalk) {
		// TODO Auto-generated method stub
		
	}

}
