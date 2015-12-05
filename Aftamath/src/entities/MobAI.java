package entities;

import static handlers.Vars.PPM;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import entities.Mob2.Anim;
import handlers.FadingSpriteBatch;
import handlers.Vars;
import main.Main;

public class MobAI {
	public static enum AIType {
		STATIONARY, IDLEWALK, FOLLOWING, FACEPLAYER, FACEOBJECT, FIGHTING,
		EVADING, EVADING_ALL, DANCING, BLOCKPATH, TIMEDFIGHT, PATH, PATH_PAUSE,
		// merged
		MOVE, RUN, JUMP, FLY, SLEEPING, FLAIL, IDLE, AIM, LOSEAIM, PUNCH, ATTACK, HUG, 
		KISS, SNOOZE, DANCE, SPECIAL1, SPECIAL2, SPECIAL3, STOP
	}

	public static enum ResetType {
		ON_ANIM_END, ON_SCRIPT_END, ON_AI_COMPLETE, ON_LEVEL_CHANGE, NEVER;
	}

	public final Mob2 owner;
	public final AIType type;
	public final ResetType resetType;
	public final float resetTime;
	public boolean finished = false;
	public Path path;

	private final Main main; 

	private Vector2 goalPosition, prevLocation;
	private float inactiveWait, inactiveTime, doTime, doDelay, time;
	private boolean reached, locked, attacked;

	private static final int IDLE_LIMIT = 500;

	public MobAI(Mob2 owner, AIType type, ResetType resetType) {
		this(owner, type, resetType, -1);
	}

	public MobAI(Mob2 owner, AIType type, ResetType resetType, float resetTime) {
		this.owner = owner;
		this.type = type;
		this.resetType = resetType;
		this.resetTime = resetTime;
		this.main = owner.main;
	}

	public void act(float dt) {

		float dx, dy;
		switch (type){
		//walk to random locations within a radius, then wait a random amount of time
		case IDLEWALK:
			if(reached) inactiveTime++;
			if(inactiveTime >= inactiveWait && reached) reached = false;
			else if (!reached){
				if (!owner.canMove()) {
					goalPosition = new Vector2((float)(((Math.random()*6)+owner.x)/PPM), owner.y);
					inactiveWait = (float)(Math.random() * IDLE_LIMIT + 100);
					inactiveTime = 0;
					reached = true;
				}
				else {
					dx = (goalPosition.x - owner.body.getPosition().x) * PPM ;
					if(dx < 1 && dx > -1){
						goalPosition = new Vector2((float)(((Math.random()*6)+owner.x)/PPM), owner.y);
						inactiveWait = (float)(Math.random() * IDLE_LIMIT + 100);
						inactiveTime = 0;
						reached = true;
					} else {
						if(dx <= -1) owner.left();
						if(dx >= 1) owner.right();
					}
				}
			}
			break;
		case FACEPLAYER:
			owner.facePlayer();
			break;
		case FACEOBJECT:
			owner.faceObject(owner.focus);
			break;
			//stay close behind the target
		case FOLLOWING:
			owner.facePlayer();
			dx = main.character.getPosition().x - owner.body.getPosition().x;

			float m = Entity.MAX_DISTANCE * (main.character.getFollowerIndex(owner)+1);
			
			if(dx > m/PPM) owner.right();
			else if (dx < -1 * m/PPM) owner.left();
			break;
			//attack focus once, then return to previous action
//		case ATTACKING:
//			if(!attacked){
//				if(attackFocus!=null){
//					dx = attackFocus.getPosition().x - getPosition().x;
//					dy = attackFocus.getPosition().x - getPosition().x;
//					float d = (float) Math.sqrt(dx*dx + dy+dy);
//
//					if(Math.abs(d)>attackRange){
//						if(dx <= -1) left();
//						if(dx >= 1) right();
//					} else{
//						attack();
//						attacked = true;
//					}
//				} else {
//					attack();
//					attacked = true;
//					state = defaultState;
//				}
//			}
//			break;

		case PATH:
			if (!isReachable()) {
				path.stepIndex();
				if(path.completed){
					path = null;
					owner.setState("STATIONARY");
				} else {
					goalPosition = path.getCurrent();
				}
			}
			else {
				dx = (goalPosition.x - owner.body.getPosition().x*PPM) ;
				if(dx < 1 && dx > -1){
					path.stepIndex();
					if(path.completed){
						path = null;
						owner.setState("STATIONARY");
					} else {
						goalPosition = path.getCurrent();
						reached = true;
					}
				} else {
					if(dx <= -1) owner.left();
					if(dx >= 1) owner.right();
				}
			}
			break;
		case FIGHTING:
			owner.fightAI();
			break;

			// evade target if target is spotted
			// if target not spotted, search vicinity
		case EVADING_ALL:
		case EVADING:
			if(!reached){
				dx = (goalPosition.x - owner.body.getPosition().x) * PPM ;
				if(dx < 1 && dx > -1){
					reached = true;
				} else {
					if(dx <= -1) owner.left();
					if(dx >= 1) owner.right();
				}
			} else {
				boolean found = false; dx =0;

				if(type == AIType.EVADING_ALL) {
					if(owner.discovered.size>0){
						dx = (owner.discovered.get(0).getPixelPosition().x - owner.getPixelPosition().x);
						found = true;
					}
				} else if(type == AIType.EVADING)
					if(owner.discovered.contains(owner.focus, true)){
						dx = (owner.focus.getPixelPosition().x - owner.getPixelPosition().x);
					}

				if(found)
					if(Math.abs(dx) <= owner.visionRange){
						float gx = -1*(dx/Math.abs(dx))*(owner.visionRange*3)/PPM + owner.getPosition().x;
						goalPosition = new Vector2(gx, owner.y);
						reached = false;
					}
					else {
						//idle
					}
			}
			break;
			// stand around and look in random directions
		case STATIONARY:
			if(doTime >= doDelay){
				doTime = 0;
				doDelay = (float)(Math.random()*6) + 1;
				
				owner.changeDirection();
			}
			break;
		case BLOCKPATH:
			owner.facePlayer();
			break;
		case DANCING:
			if(owner.getAnimationAction()!=Anim.DANCE)
				owner.setAnimation(Anim.DANCE, true);
			break;
		case TIMEDFIGHT:
			time-=Vars.DT;
			if(time>=0)
				owner.fightAI();
			else
				owner.resetState();
			break;
		default:
			break;
		}

		// doAction
		Anim remove=null;
		switch(type){
		case RUN:
			owner.run();
		case MOVE:
			if(goalPosition==null && path==null)
				return;

			if(isReachable()){
				dx = (goalPosition.x - owner.getPosition().x)* PPM ;

				if(Math.abs(dx) > 1){
					if (dx > 0) owner.right();
					else owner.left();
				} else {
					// handling when the mob engages conversation
					if (owner.positioning) {
						owner.positioning = false;
						if(owner.positioningFocus!=null)
							owner.faceObject(owner.positioningFocus);
						finish();
					} else if(path!=null){
						path.stepIndex();
						if (path.completed)
							path = null;
						else
							goalPosition = path.getCurrent();
					}
				}
			} else {
				//path cannot be completed
				if(path!=null) {
					path = null;
				}
				finish();
			}
			break;
		// go to the focus object and hug it
		// if the object is a mob, make it hug back
		// wait for the animation to loop for 2 seconds before making both objects release
		case HUG:
			if(owner.AIfocus==null)
				finish();
			else {
				if(owner.getAnimationAction()!=Anim.HUGGING && owner.getAnimationAction()!=Anim.EMBRACE){
					dx = (owner.AIfocus.getPosition().x - owner.getPosition().x) * PPM;
				
					if(Math.abs(dx) > 1){
						if (dx > 0) owner.right();
						else owner.left();
					} else {
						owner.AIfocus.faceObject(owner);
						if(owner.AIfocus instanceof Mob)
							((Mob)owner.AIfocus).setTransAnimation(Anim.EMBRACE, Anim.HUGGING, true);
						owner.setTransAnimation(Anim.EMBRACE, Anim.HUGGING, true);
					}
				} else if (owner.getAnimationAction()==Anim.HUGGING){
					if(owner.animation.getSpeed() * owner.animation.getTimesPlayed() >= 2){
						if(owner.AIfocus instanceof Mob)
							((Mob)owner.AIfocus).setAnimation(true, Anim.EMBRACE);
						owner.setAnimation(true, Anim.EMBRACE);
						owner.controlledPT2 =true;
					}
				} else if (owner.controlledPT2){
					if(owner.getAnimationAction()!=Anim.EMBRACE)
						finish();
				}
			}
			break;
		case KISS:
			if(owner.AIfocus==null)
				finish();
			else {
				if(owner.getAnimationAction()!=Anim.KISSING && owner.getAnimationAction()!=Anim.ENGAGE_KISS){
					dx = (owner.AIfocus.getPosition().x - owner.getPosition().x) * PPM;
					
					if(Math.abs(dx) > 1){
						if (dx > 0) owner.right();
						else owner.left();
					} else {
						owner.AIfocus.faceObject(owner);
						owner.setTransAnimation(Anim.ENGAGE_KISS, Anim.KISSING, true);
					}
				} else if (owner.getAnimationAction()==Anim.KISSING){
					if(owner.animation.getSpeed() * owner.animation.getTimesPlayed() >= 2){
						owner.setAnimation(true, Anim.ENGAGE_KISS);
						owner.controlledPT2 =true;
					}
				} else if (owner.controlledPT2){
					if(owner.getAnimationAction()!=Anim.ENGAGE_KISS)
						finish();
				}
			}
			break;
		case JUMP:
			if(owner.isOnGround())
				finish();
			break;
		// run around back and forth flailing for a few seconds
		// repeated a random # of times
		case FLAIL:
			owner.maxSpeed = Mob2.RUN_SPEED;
			
			if(owner.ctrlRepeat==-1)
				owner.ctrlRepeat = (int) (Math.random()*2) + 2;
			
			if(owner.ctrlReached){
				goalPosition = new Vector2(owner.getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
						Vars.TILE_SIZE, owner.getPosition().y);
				float d = 1;
				if(!owner.isFacingLeft()) d = -1;
				
				goalPosition = new Vector2(d * goalPosition.x, goalPosition.y);
				owner.ctrlReached = false;
				owner.ctrlRepeat--;
			} else {
				if(isReachable()){
					if(goalPosition==null)
						goalPosition = new Vector2(owner.getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
								Vars.TILE_SIZE, owner.getPosition().y);
					dx = (goalPosition.x - owner.getPosition().x)* PPM ;

					if(Math.abs(dx) > 1){
						if (dx > 0) owner.right();
						else owner.left();
					} else 
						owner.ctrlReached = true;
				} else 
					owner.ctrlReached = true;
			}
			
			if(owner.ctrlRepeat == 0){
				finish();
				owner.animation.removeAction();
			}
			break;
		case FLY:
			break;
		case SLEEPING:
			owner.heal(1, false);
			if(main.character.equals(owner)){
				if(main.getSpriteBatch().getFadeType()==FadingSpriteBatch.FADE_IN &&
						!owner.controlledPT2){
//					if(!sleepSpell)
					main.playSound("slept");
					main.dayTime = (main.dayTime + Main.NOON_TIME) % Main.DAY_TIME;
				}
			}
			
			if(owner.health == owner.maxHealth && !owner.controlledPT2){
				owner.controlledPT2=true;
				owner.setAnimation(true, Anim.LIE_DOWN);
			}
			
			if(owner.controlledPT2 && owner.getAnimationAction()!=Anim.LIE_DOWN){
				finish();
				if(main.character.equals(this)){
					//trigger any events
				}
			}
			break;
		case SNOOZE:
			if(!owner.controlledPT2)
				if(owner.getAnimationAction()!=Anim.SNOOZING && owner.getAnimationAction()!=Anim.GET_DOWN)
					if(owner.animation.getTimesPlayed() * owner.animation.getSpeed() >= 3){
						owner.controlledPT2 = true;
						owner.setAnimation(true, Anim.GET_DOWN);
					}
			break;
		case LOSEAIM:
		case AIM:
			remove = Anim.AIM_TRANS;
			break;
		case IDLE:
			remove = Anim.IDLE;
			break;
		case DANCE:
			remove = Anim.DANCE;
			break;
		case SPECIAL1:
			remove = Anim.SPECIAL1;
			break;
		case SPECIAL2:
			remove = Anim.SPECIAL2;
			break;
		case SPECIAL3:
			remove = Anim.SPECIAL3;
			break;
		case ATTACK:
			remove = Anim.ATTACKING;
			break;
		case PUNCH:
			remove = Anim.PUNCHING;
			break;
		default:
			break;
		}
		
		if(remove!=null)
			if(owner.getAnimationAction()!=remove)
				finish();


// doTimedAction
		Anim anim = null;
		switch(type){
		case AIM:
			if(owner.actionTime<=Vars.DT){
				owner.aiming = false;
				owner.setAnimation(true, Anim.AIM_TRANS);
			}else if(!owner.aiming){
				owner.aiming = true;
				owner.setTransAnimation(Anim.AIM_TRANS, Anim.AIMING, true);
			}
			break;
		case ATTACK:
			anim = Anim.ATTACKING;
			break;
		case DANCE:
			anim = Anim.DANCE;
			break;
		case FLAIL:
			if(!owner.controlledPT2){
				owner.setAnimation(Anim.ON_FIRE, true);
				owner.controlledPT2 = true;
			} else {
				if(owner.ctrlReached){
					goalPosition = new Vector2(owner.getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
							Vars.TILE_SIZE, owner.getPosition().y);
					float d = 1;
					if(!owner.facingLeft) d = -1;

					goalPosition = new Vector2(d * goalPosition.x, goalPosition.y);
					owner.ctrlReached = false;
				} else {
					if(isReachable()){
						dx = (goalPosition.x - owner.getPosition().x)* PPM ;

						if(Math.abs(dx) > 1){
							if (dx > 0) owner.right();
							else owner.left();
						} else 
							owner.ctrlReached = true;
					} else 
						owner.ctrlReached = true;
				}

				if(owner.actionTime<=Vars.DT)
					owner.animation.removeAction();
			}
			break;
		case FLY:
			break;
		case HUG:
			if(owner.actionTime<=Vars.DT)
				owner.setAnimation(true, Anim.EMBRACE);
			else if(!owner.controlledPT2){
				owner.controlledPT2 = true;
				owner.setTransAnimation(Anim.EMBRACE, Anim.HUGGING, true);
			}
			break;
		case IDLE:
			anim = Anim.IDLE;
			break;
		case JUMP:
			owner.jump();
			break;
		case KISS:
			if(owner.actionTime<=Vars.DT)
				owner.setAnimation(true, Anim.ENGAGE_KISS);
			else if(!owner.controlledPT2){
				owner.controlledPT2 = true;
				owner.setTransAnimation(Anim.ENGAGE_KISS, Anim.KISSING, true);
			}
			break;
		case PUNCH:
			anim = Anim.PUNCHING;
			break;
		case SNOOZE:
			if(owner.actionTime<=Vars.DT)
				owner.setAnimation(true, Anim.GET_DOWN);
			else if(!owner.controlledPT2){
				owner.controlledPT2 = true;
				owner.setTransAnimation(Anim.GET_DOWN, Anim.SNOOZING);			
			}
			break;
		case SPECIAL1:
			anim = Anim.SPECIAL1;
			break;
		case SPECIAL2:
			anim = Anim.SPECIAL2;
			break;
		case SPECIAL3:
			if(owner.actionTime<=Vars.DT)
				owner.setAnimation(true, Anim.SPECIAL3);
			else if(!owner.controlledPT2){
				owner.controlledPT2 = true;
				owner.setTransAnimation(Anim.SPECIAL3, Anim.HUGGING, true);
			}
			break;
		default:
			break;
		}
		
		if(anim!=null)
			owner.setAnimation(anim);
		
		if(owner.actionTime<=Vars.DT)
			finish();
// end doTimedAction

		checkFinished();
	}

	public void begin(){
		switch(type){
		case JUMP:
			owner.jump();
			break;
		case AIM:
			owner.aiming = true;
			owner.setTransAnimation(Anim.AIM_TRANS, Anim.AIMING);
			break;
		case LOSEAIM:
			owner.aiming = false;
			owner.setAnimation(true, Anim.AIM_TRANS);
			break;
		case ATTACK:
			owner.setAnimation(Anim.ATTACKING);
			break;
		case PUNCH:
			owner.setAnimation(Anim.PUNCHING);
			break;
		case DANCE:
			if(owner.actionTime>0)
				owner.setAnimation(Anim.DANCE, true);
			else owner.setAnimation(Anim.DANCE);
			break;
		case FLAIL:
			owner.setAnimation(Anim.ON_FIRE, true);
			break;
		case FLY:
			//flying related mechanics
			
			//flying = true;
			owner.body.setType(BodyType.KinematicBody);
			owner.setAnimation(Anim.SWIMMING);
			break;
		case IDLE:
			if(owner.actionTime>0)
				owner.setAnimation(Anim.IDLE, true);
			else owner.setAnimation(Anim.IDLE);
			break;
		case SLEEPING:
			owner.setTransAnimation(Anim.LIE_DOWN, Anim.SLEEPING, true);
			if(main.character.equals(this)/* && !sleepSpell*/){
				main.getSpriteBatch().fade();
				
				main.saveGame();
			}
			break;
		case SPECIAL3:
			owner.setTransAnimation(Anim.SPECIAL3_TRANS, Anim.SPECIAL3);
			break;
		default:
			break;
		}
	}

	public void finish() {
		
	}

	public void checkFinished() {
		switch (resetType) {
		case ON_ANIM_END:
			
		}
	}

	//ensure the mob can reach its destination
	public boolean isReachable(){
		if(!owner.canMove())
			return false;
		//goalPosisition;
		return true;
	}

}
