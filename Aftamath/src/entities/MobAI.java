package entities;

import static handlers.Vars.PPM;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import entities.Mob2.Anim;
import handlers.Anim2.LoopBehavior;
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
		ON_ANIM_END, ON_SCRIPT_END, ON_AI_COMPLETE, ON_LEVEL_CHANGE, ON_TIME, NEVER;
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
	private float actionTime;
	private boolean reached, locked, attacked;

	private static final int IDLE_LIMIT = 500;
	
	public MobAI(Mob2 owner, AIType type){
		this(owner, type, ResetType.ON_AI_COMPLETE);
	}

	//initializing timed stuff
	public MobAI(Mob2 owner, AIType type, float resetTime) {
		this.owner = owner;
		this.type = type;
		this.resetType = ResetType.ON_TIME;
		this.resetTime = resetTime;
		this.main = owner.main;
	}

	//initializing nontimed AIs
	public MobAI(Mob2 owner, AIType type, ResetType resetType) {
		this.owner = owner;
		this.type = type;
		this.resetType = resetType;
		this.resetTime = -1;
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
			fightAI();
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
				owner.setAnimation(Anim.DANCE, LoopBehavior.CONTINUOUS);
			break;
		case TIMEDFIGHT:
			time-=Vars.DT;
			if(time>=0)
				fightAI();
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
							((Mob2)owner.AIfocus).setTransAnimation(Anim.EMBRACE, Vars.ACTION_ANIMATION_RATE,
									Anim.HUGGING, Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
						owner.setTransAnimation(Anim.EMBRACE, Vars.ACTION_ANIMATION_RATE,
								Anim.HUGGING, Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
					}
				} else if (owner.getAnimationAction()==Anim.HUGGING){
					if(owner.animation.getPrimarySpeed() * owner.animation.getTimesPlayed() >= 2){
						if(owner.AIfocus instanceof Mob2)
							((Mob2)owner.AIfocus).setAnimation(Anim.EMBRACE, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
						owner.setAnimation(Anim.EMBRACE, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
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
						owner.setTransAnimation(Anim.ENGAGE_KISS, Anim.KISSING, LoopBehavior.CONTINUOUS);
					}
				} else if (owner.getAnimationAction()==Anim.KISSING){
					if(owner.animation.getPrimarySpeed() * owner.animation.getTimesPlayed() >= 2){
						owner.setAnimation(Anim.ENGAGE_KISS, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
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
				owner.setAnimation(Anim.LIE_DOWN, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
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
					if(owner.animation.getTimesPlayed() * owner.animation.getPrimarySpeed() >= 3){
						owner.controlledPT2 = true;
						owner.wake();
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
			if(actionTime<=Vars.DT)
				owner.unAim();
			else if(!owner.aiming)
				aim();
			break;
		case ATTACK:
			anim = Anim.ATTACKING;
			break;
		case DANCE:
			anim = Anim.DANCE;
			break;
		case FLAIL:
			if(!owner.controlledPT2){
				owner.setAnimation(Anim.ON_FIRE, LoopBehavior.CONTINUOUS);
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

				if(actionTime<=Vars.DT)
					owner.animation.removeAction();
			}
			break;
		case FLY:
			break;
		case HUG:
			if(actionTime<=Vars.DT)
				owner.setAnimation(Anim.EMBRACE, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
			else if(!owner.controlledPT2){
				owner.controlledPT2 = true;
				owner.setTransAnimation(Anim.EMBRACE, Anim.HUGGING, LoopBehavior.CONTINUOUS);
			}
			break;
		case IDLE:
			anim = Anim.IDLE;
			break;
		case JUMP:
			owner.jump();
			break;
		case KISS:
			if(actionTime<=Vars.DT)
				owner.setAnimation(Anim.ENGAGE_KISS, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
			else if(!owner.controlledPT2){
				owner.controlledPT2 = true;
				owner.setTransAnimation(Anim.ENGAGE_KISS, Anim.KISSING, LoopBehavior.CONTINUOUS);
			}
			break;
		case PUNCH:
			anim = Anim.PUNCHING;
			break;
		case SNOOZE:
			if(actionTime<=Vars.DT)
				owner.setAnimation(Anim.GET_DOWN, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
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
			if(actionTime<=Vars.DT)
				owner.setAnimation(Anim.SPECIAL3, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
			else if(!owner.controlledPT2){
				owner.controlledPT2 = true;
				owner.setTransAnimation(Anim.SPECIAL3, Anim.HUGGING, LoopBehavior.CONTINUOUS);
			}
			break;
		default:
			break;
		}
		
		if(anim!=null)
			owner.setAnimation(anim, LoopBehavior.ONCE);
		
		if(actionTime<=Vars.DT)
			finish();
// end doTimedAction

		checkFinished();
	}
	// attack focus if in range
	// if not in range, get close to object
	// otherwise evade hostile mobs or projectiles
	// if no hostile mobs or projectiles in sight, search vicinity
	public void fightAI(){
//		System.out.println("FIGHTING");
		float dx, dy;
		if(!attacked){
//			System.out.println("!attacked");
			if(owner.attackFocus!=null && doTime>=doDelay){
				//System.out.println("attackFocus!=null && doTime>=doDelay");
				dx = owner.attackFocus.getPosition().x - owner.getPosition().x;
				dy = owner.attackFocus.getPosition().x - owner.getPosition().x;
				float d = (float) Math.sqrt(dx*dx + dy+dy);

				if(Math.abs(d)>owner.attackRange){
					if(dx-1>0) owner.right();
					if(dx+1<0) owner.left();
				} else {
					owner.attack();
					attacked = true;
					doDelay = (float) (Math.random()*3);
					doTime = 0;
				}
			} /*else
				System.out.println(doTime+":"+doDelay);*/
		} else {
			if(reached) inactiveWait++;
			if(inactiveTime >= inactiveWait && reached) {
				reached = false;
				attacked = false;
			}
			if(!reached){
				if (!owner.canMove()) {
					goalPosition = new Vector2((float) (((Math.random() * 6)+owner.x)/PPM), owner.y);
					inactiveWait = (float)(Math.random() *(owner.attackRange) + 100);
					inactiveTime = 0;
					reached = true;
				}
				else {
					dx = (goalPosition.x - owner.getPosition().x) * PPM ;
					if(dx < 1 && dx > -1){
						goalPosition = new Vector2((float) (((Math.random() * 6)+owner.x)/PPM), owner.y);
						inactiveWait = (float)(Math.random() *(owner.attackRange) + 100);
						inactiveTime = 0;
						reached = true;
					} else {
						if(dx < 1) owner.left();
						if(dx > -1) owner.right();
					}
				}
			}
		}
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
			owner.unAim();
			break;
		case ATTACK:
			owner.setAnimation(Anim.ATTACKING, LoopBehavior.ONCE);
			break;
		case PUNCH:
			owner.setAnimation(Anim.PUNCHING, LoopBehavior.ONCE);
			break;
		case DANCE:
			if(actionTime>0)
				owner.setAnimation(Anim.DANCE, LoopBehavior.CONTINUOUS);
			else owner.setAnimation(Anim.DANCE, LoopBehavior.ONCE);
			break;
		case FLAIL:
			owner.setAnimation(Anim.ON_FIRE, LoopBehavior.CONTINUOUS);
			break;
		case FLY:
			//flying related mechanics
			
			//flying = true;
			owner.body.setType(BodyType.KinematicBody);
			owner.setAnimation(Anim.SWIMMING, LoopBehavior.CONTINUOUS);
			break;
		case IDLE:
			if(actionTime>0)
				owner.setAnimation(Anim.IDLE, LoopBehavior.CONTINUOUS);
			else owner.setAnimation(Anim.IDLE, LoopBehavior.ONCE);
			break;
		case SLEEPING:
			owner.setTransAnimation(Anim.LIE_DOWN, Anim.SLEEPING, LoopBehavior.CONTINUOUS);
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


	//conclude any controlled actions for mob
	private void finish(){
		goalPosition = null;
		owner.controlled = false;
//		controlledPT2 = false;
//		controlledAction = null;
		actionTime = 0;
		ctrlRepeat = -1;
	}

	public void checkFinished() {
		switch (resetType) {
		case ON_ANIM_END:
			break;
		case NEVER:
			break;
		case ON_AI_COMPLETE:
			break;
		case ON_LEVEL_CHANGE:
			break;
		case ON_SCRIPT_END:
			break;
		case ON_TIME:
			break;
		default:
			break;
			
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
