package entities;

import static handlers.Vars.PPM;
import handlers.Animation.LoopBehavior;
import handlers.FadingSpriteBatch;
import handlers.Vars;
import main.Main;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.utils.Array;

import entities.Entity.DamageType;
import entities.Mob.Anim;

public class MobAI {
	public static enum AIType {
		AIM, ATTACK, BLOCKPATH, DANCING, DUCK, IDLE, IDLEWALK, EVADING, EVADING_ALL, 
		FACELEFT, FACEOBJECT, FACEPLAYER, FACERIGHT, FIGHTING, FLAIL, FLY, FOLLOWING, 
		HUG, JUMP, KISS, KNOCKOUT, LOSEAIM, LOOK_UP, MOVE, RUN, RECOVER, PATH, PATH_PAUSE, PUNCH, SHOOT, SLEEPING,
		SNOOZE, SPECIAL1, SPECIAL2, SPECIAL3, STATIONARY, STOP
	}

	public static enum ResetType {
		ON_ANIM_END, ON_SCRIPT_END, ON_AI_COMPLETE, ON_LEVEL_CHANGE, ON_TIME, NEVER;
	}
	public static final Array<AIType> technical_types = new Array<>();
	static{
		technical_types.add(AIType.LOSEAIM);
		technical_types.add(AIType.STOP);
		technical_types.add(AIType.RECOVER);
	}

	public final Mob owner;
	public final ResetType resetType;
	public final float resetTime;
	public Entity focus;
	public AIType type;
	public boolean finished = false;
	public Path path;
	
	private int repeat = -1;
	private Main main; 
	private Vector2 goalPosition, retLoc;
	private float inactiveWait, inactiveTime, doTime, doDelay, time;
	private boolean reached, attacked, AIPhase2, canPosition;
	private Object data;

	private static final float IDLE_DURATION = 8.33f; //maximum idle time
	private static final float IDLE_RANGE = 3.5f; //maximum idlewalk distance
	private static final float FLAIL_RANGE = 8f; //maximum flail distance
	private static final float DEFAULT_DURATION = 3; //length of time between events
	private static final float POS_RANGE = 8; //maximum distance from return location before repositioning is active
	
	public MobAI(Mob owner, AIType type, Entity focus){
		this(owner, type, ResetType.ON_AI_COMPLETE, focus);
	}

	//initializing timed stuff
	public MobAI(Mob owner, AIType type, float resetTime, Entity focus) {
		this.owner = owner;
		this.type = type;
		this.resetType = ResetType.ON_TIME;
		this.resetTime = resetTime;
		this.main = owner.main;
		this.focus = focus;
		begin();
	}

	//initializing nontimed AIs
	public MobAI(Mob owner, AIType type, ResetType resetType, Entity focus) {
		this.owner = owner;
		this.type = type;
		this.resetType = resetType;
		this.resetTime = -1;
		this.main = owner.main;
		this.focus = focus;
		begin();
	}
	
	public void update(float dt){
		if(!resetType.equals(ResetType.NEVER)) 
			time+=dt;
		position();
		
		if(resetType.equals(ResetType.ON_TIME) && time>=resetTime){
			owner.resetState();
		} else {
			if(!finished){
				if(main!=null)
					act(dt);
				else
					this.main = owner.main;
			}else
				owner.resetState();
		}
	}

	//apply AI 
	public void act(float dt) {
		float max;
		float dx = retLoc.x - owner.getPixelPosition().x;
		Anim anim = null; //used for finding the final animation
		Anim strictAnim = null; //used for keeping the animation with the current AI, though only for 1 parters
		
		switch(type){
		case AIM:
			canPosition = false;
			
			if(time>=Vars.ACTION_ANIMATION_RATE * 
					Mob.actionLengths[Mob.animationIndicies.get(Anim.AIM_TRANS)])
				canPosition = true;
			//do aiming animation if outside influences changed the animation
			if(Math.abs(dx) < POS_RANGE && !owner.aiming)
				owner.aim();
			if(focus!=null){
				//watch focus if Mob doesn't need to reposition
				if((canPosition && Math.abs(dx) < POS_RANGE) || !canPosition)
					owner.faceObject(focus);
			}
			
			if (resetType.equals(ResetType.ON_TIME)){
				if(time>=resetTime-1){
					owner.unAim();
					finish();
				}
			} else if(resetType==ResetType.ON_ANIM_END || resetType==ResetType.ON_AI_COMPLETE){
				inactiveTime += dt;
				if(inactiveTime>=inactiveWait){
					owner.unAim();
					finish();
				}
			}
			break;
		case ATTACK: 
			//use super attack, or simply punch
			if(owner.powerType==DamageType.PHYSICAL)
				type = AIType.PUNCH;
			else { 
				switch(resetType){
				case ON_ANIM_END:
					anim = Anim.ATTACKING;
				case ON_AI_COMPLETE:
					if(!reached){
						owner.superAttack();
						reached = true;
					}
					break;
				default:
					//move close enough to mob to effect an attack
					if(focus!=null){
						dx = focus.getPosition().x - owner.getPosition().x;
						float a = (Math.abs(dx)/dx);
						max = 4*Vars.TILE_SIZE/PPM;
						canPosition = moveToLoc(new Vector2(focus.getPosition().x - a*max, 
								focus.getPosition().y));
					} 
					owner.superAttack();
					break;
				}
			}
			break;
		case BLOCKPATH: //stand in place colliding w/ player, always face player
			if(Math.abs(dx) < POS_RANGE)
				owner.facePlayer();
			if (resetType.equals(ResetType.ON_TIME)){
				if(time>=resetTime-.5f)
					finish();
			} else if(resetType!=ResetType.NEVER) {
				inactiveTime+=dt;
				if(inactiveTime>=inactiveWait)
					finish();
			}
			break;
		case DANCING:
			if(!resetType.equals(ResetType.ON_AI_COMPLETE) && 
					!resetType.equals(ResetType.ON_ANIM_END))
				strictAnim = Anim.DANCE;
			anim = Anim.DANCE;
			break;
		case DUCK:
			//do ducking animation if outside influences changed the animation
			if(!owner.ducking)
				owner.duck();
			
			if (resetType.equals(ResetType.ON_TIME)){
				if(time>=resetTime-1){
					owner.unDuck();
					finish();
				}
			} else if(resetType==ResetType.ON_ANIM_END || resetType==ResetType.ON_AI_COMPLETE){
				inactiveTime += dt;
				if(inactiveTime>=inactiveWait){
					owner.unDuck();
					finish();
				}
			}
			break;
		// evade target if target is spotted
		// if target not spotted, search vicinity
		case EVADING:
		case EVADING_ALL:
			boolean found = false; 
			if(!reached){
				canPosition = false;
				dx =0;
				float d = goalPosition.x - owner.getPixelPosition().x;
				//locate the focus of the evasion
				if(type == AIType.EVADING_ALL && owner.discovered.size>0){
					dx = (owner.discovered.get(0).getPixelPosition().x - owner.getPixelPosition().x);
					found = true;
				} else if(type == AIType.EVADING && owner.discovered.contains(focus, true)){
					dx = (focus.getPixelPosition().x - owner.getPixelPosition().x);
					found = true;
				}

				//initiate evasion! (only if the new focus is on the opposite side of the Mob
				//as the previous focus)
				if(found && ((d>0&&dx<=0) || (d<=0&&dx>=0)))
					if(Math.abs(dx) <= owner.visionRange){
						float gx = -dx*1.5f + owner.getPixelPosition().x;
						setGoal(new Vector2(gx, owner.y));
						retLoc = goalPosition;
						reached = false;
					}
				
				//attempt to evade
				reached = moveToLoc(goalPosition);
			} else {
				canPosition = true;
				dx =0;
				//locate the focus of the evasion
				if(type == AIType.EVADING_ALL && owner.discovered.size>0){
					dx = (owner.discovered.get(0).getPixelPosition().x - owner.getPixelPosition().x);
					found = true;
				} else if(type == AIType.EVADING && owner.discovered.contains(focus, true)){
					dx = (focus.getPixelPosition().x - owner.getPixelPosition().x);
					found = true;
				}

				//initiate evasion!
				if(found){
					canPosition = false;
					if(Math.abs(dx) <= owner.visionRange){
						float gx = -dx*1.5f + owner.getPixelPosition().x;
						setGoal(new Vector2(gx, owner.y));
						retLoc = goalPosition;
						reached = false;
					}
				} else {
					if(resetType.equals(ResetType.ON_AI_COMPLETE) || 
							resetType.equals(ResetType.ON_ANIM_END))
						finish();
					canPosition = true;
					inactiveTime+=dt;
					if(inactiveTime >= inactiveWait){
						inactiveTime = 0;
						inactiveWait = (float)(Math.random()*2) + 1;
						if(!Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.IDLE))
							owner.changeDirection();
					}
				}
			}
			break;
		case FACEPLAYER:
			if(main!=null)
				focus = main.character;
		case FACEOBJECT:
			dx = retLoc.x - owner.getPixelPosition().x;
			if((canPosition && Math.abs(dx) < POS_RANGE) || !canPosition)
				owner.faceObject(focus);
			if (resetType==ResetType.ON_ANIM_END || resetType==ResetType.ON_AI_COMPLETE)
				finish();
			break;
		case FACELEFT:
			dx = retLoc.x - owner.getPixelPosition().x;
			if((canPosition && Math.abs(dx) < POS_RANGE) || !canPosition)
				if(!owner.facingLeft)
					owner.changeDirection();
			break;
		case FACERIGHT:
			dx = retLoc.x - owner.getPixelPosition().x;
			if((canPosition && Math.abs(dx) < POS_RANGE) || !canPosition)
				if(owner.facingLeft)
					owner.changeDirection();
			break;
		case FIGHTING:
			fightAI();
			break;
		case FLAIL:
			strictAnim = Anim.ON_FIRE;
			
			owner.run();
			if (!owner.canMove()){ 
				findNewLoc(FLAIL_RANGE, true);
			} else {
				reached = moveToLoc(goalPosition);
				if(reached) {
					AIPhase2 = !AIPhase2;
					repeat--;
					if((resetType==ResetType.ON_AI_COMPLETE || resetType==ResetType.ON_ANIM_END)
						&& repeat<0)
						finish();
					else {
						reached = false;
						findNewLoc(FLAIL_RANGE, true);
					}
				}
			}
			break;
		case FLY:
			System.out.println("Flying is not implemented. Sorry!");
			finish();
			break;
		case FOLLOWING:
			if (resetType.equals(ResetType.ON_TIME)){
				if(time>=resetTime-1){
					focus.removeFollower(owner);
					focus = null;
					finish();
				}
			} 

			owner.faceObject(focus);
			dx = focus.getPosition().x - owner.body.getPosition().x;

			float m = Entity.MAX_DISTANCE * (owner.followIndex);
			if(Math.abs(dx) > (m + 2*Entity.MAX_DISTANCE)/PPM) 	owner.run();
			if(dx > m/PPM) owner.right();
			else if (dx < -1 * m/PPM) owner.left();
			break;
		case IDLE:
			if(!resetType.equals(ResetType.ON_AI_COMPLETE) && 
					!resetType.equals(ResetType.ON_ANIM_END))
				strictAnim = Anim.IDLE;
			anim = Anim.IDLE;
			break;
			// walk around and look around randomly
		case IDLEWALK:
			if(AIPhase2){
				inactiveTime+=dt;
				if(inactiveTime >= inactiveWait)
					//decide what mob will do on next update
					if(Math.random()>=.5d){
						inactiveTime = 0;
						inactiveWait = (float)(Math.random()*6) + 1;
						if(!Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.IDLE))
							owner.changeDirection();
					} else {
						AIPhase2 = false;
						findNewLoc(IDLE_RANGE, false);
						inactiveWait = (float)(Math.random() * IDLE_DURATION + 1);
					}
			} else {
				if(reached) 
					inactiveTime+=dt;
				if(inactiveTime >= inactiveWait && reached){ 
					reached = false;
					if(resetType==ResetType.ON_AI_COMPLETE || resetType==ResetType.ON_ANIM_END)
						finish();
					else{
						//decide what mob will do on next update
						if(Math.random()>=.5d){
							AIPhase2 = true;
							inactiveTime = 0;
							inactiveWait = (float)(Math.random()*6) + 1;
							owner.changeDirection();
						} else {
							findNewLoc(IDLE_RANGE, false);
							inactiveWait = (float)(Math.random() * IDLE_DURATION + 1);
						}
					}
				} else if (!reached){
					if (!owner.canMove()){ 
						findNewLoc(IDLE_RANGE, false);
						inactiveWait = (float)(Math.random() * IDLE_DURATION + 1);
					} else {
						reached = moveToLoc(goalPosition);
						if(reached) canPosition = true;
					}
				} 
			}
			break;
		case JUMP:
			if(resetType==ResetType.ON_AI_COMPLETE || resetType==ResetType.ON_ANIM_END){
				if(owner.isOnGround())
					finish();
			} else {
				inactiveTime+=dt;
				if(owner.isOnGround() && inactiveTime>=1){
					inactiveTime=0;
					owner.jump();
				}
			}
			break;
		// go to the focus object and kiss/hug it
		// if the object is a mob, make it kiss/hug back
	    // if no focus exists, stay at location to do animation
		// wait a while before making both objects release
		case HUG:
		case KISS:
			//move to focus
			if(!AIPhase2){
				reached = moveToLoc(goalPosition);
				if(reached){
					AIPhase2 = true;
					owner.embrace(type);
					// make focus respond to embrace
					if(focus instanceof Mob){
						((Mob) focus).faceObject(owner);
						((Mob) focus).embrace(type);
					}
				}
			} else {
				//forceably reset animation if outside influences changed it
				if(resetType==ResetType.NEVER){
					if(type==AIType.KISS)
						strictAnim = Anim.KISSING;
					else
						strictAnim = Anim.HUGGING;
				}
				
				if(!canPosition){
					inactiveTime +=dt;
					//wait for embrace to finish
					if(((inactiveTime>=inactiveWait && resetType!=ResetType.ON_TIME) ||
							(time>=resetTime-1 && resetType==ResetType.ON_TIME))
							&& resetType!=ResetType.NEVER && reached){
						reached = false;
						owner.release(type);
						if(focus!=null)
							if(focus instanceof Mob)
								((Mob) focus).release(type);
					} else {
						//has release finished?
						if(type==AIType.KISS){
							if(!reached && !Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.KISSING)
									&& !Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.ENGAGE_KISS))
								canPosition = true;
						} else
							if(!reached && !Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.HUGGING)
									&& !Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.EMBRACE))
								canPosition = true;
					}
				//has mob returned to original location?
				} else {
					if(Math.abs(dx) > POS_RANGE && resetType!=ResetType.NEVER
						&& resetType!=ResetType.ON_TIME)
						finish();
				}
			}
			break;
		case KNOCKOUT:
			if (resetType.equals(ResetType.ON_TIME)){
				if(time>=resetTime-1){
					owner.recover();
					finish();
				}
			} else if(resetType==ResetType.ON_ANIM_END || resetType==ResetType.ON_AI_COMPLETE){
				inactiveTime += dt;
				if(inactiveTime>=inactiveWait){
					owner.recover();
					finish();
				}
			}
			break;
		case LOOK_UP:
			canPosition = false;

			if(time>=Vars.ACTION_ANIMATION_RATE * 
					Mob.actionLengths[Mob.animationIndicies.get(Anim.LOOK_UP)])
				canPosition = true;
			//do animation if outside influences changed it
			if(Math.abs(dx) < POS_RANGE && (
					!Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.LOOKING_UP) &&
					!Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.LOOK_UP)))
				owner.setAnimation(Anim.LOOKING_UP, LoopBehavior.CONTINUOUS);
			if(focus!=null){
				//watch focus if Mob doesn't need to reposisition
				if((canPosition && Math.abs(dx) < POS_RANGE) || !canPosition)
					owner.faceObject(focus);
			}

			if (resetType.equals(ResetType.ON_TIME)){
				if(time>=resetTime-1){
					owner.unLookUp();
					finish();
				}
			} else if(resetType!=ResetType.NEVER){
				inactiveTime += dt;
				if(inactiveTime>=inactiveWait){
					owner.unLookUp();
					finish();
				}
			}
			break;
		case LOSEAIM:
			owner.unAim();
			if(owner.defaultState.type==AIType.AIM)
				owner.setDefaultState((MobAI) owner.defaultState.data);
			finish();
			break;
		case RUN:
			owner.run();
		case MOVE:
			if(moveToLoc(goalPosition)){
				owner.respawnPoint = new Vector2(goalPosition);
				finish();
			}
			break;
		case PATH:
			if (!owner.canMove()){ 
				path.stepIndex();
				if(path.completed){
					path = null;
					finish();
				} else {
					setGoal(path.getCurrent());
					reached = false;
				}
			} else {
				reached = moveToLoc(goalPosition);
				if(reached) {
					if(path.completed){
						path = null;
						finish();
					} else {
						setGoal(path.getCurrent());
						reached = false;
					}
				}
			}
			break;
		case PATH_PAUSE: //pause the mob in the middle of walking
			break;
		case PUNCH:
			if(!resetType.equals(ResetType.ON_AI_COMPLETE) && 
					!resetType.equals(ResetType.ON_ANIM_END))
				strictAnim = Anim.PUNCHING;
			anim = Anim.PUNCHING;
			break;
		case RECOVER:
			owner.recover();
			System.out.println("find data from: "+owner.defaultState);
			if(owner.defaultState.type==AIType.KNOCKOUT){
				owner.setDefaultState((MobAI) owner.defaultState.data);
				System.out.println("found data: "+owner.defaultState.data);
			}
			finish();
			break;
		case SHOOT:
			if(!resetType.equals(ResetType.ON_AI_COMPLETE) && 
					!resetType.equals(ResetType.ON_ANIM_END))
				strictAnim = Anim.ATTACKING;
			if(owner.defaultState.type==AIType.AIM && !owner.animation.hasTrans()){
				finish();
			} else
				anim = Anim.ATTACKING;
			break;
		case SLEEPING:
			owner.heal(1, false);
			if(main.character.equals(owner)){
				if(main.getSpriteBatch().getFadeType()==FadingSpriteBatch.FADE_IN &&
						!AIPhase2){
//					if(!sleepSpell)
					main.playSound("slept");
					main.dayTime = (main.dayTime + Main.NOON_TIME) % Main.DAY_TIME;
				}
			}
			
			if(owner.health == owner.maxHealth && !AIPhase2){
				AIPhase2=true;
				//owner.setAnimation(Anim.LIE_DOWN, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
			}
			
			if(AIPhase2 && owner.getAction()!=Anim.LIE_DOWN){
				finish();
				if(main.character.equals(this)){
					//trigger any events
				}
			}
			break;
		case SNOOZE:
			if (resetType.equals(ResetType.ON_TIME)){
				if(time>=resetTime-1){
					owner.wake();
					finish();
				}
			} else if(resetType!=ResetType.NEVER){
				inactiveTime += dt;
				if(inactiveTime>=inactiveWait){
					owner.wake();
					finish();
				}
			}
			break;
		case SPECIAL1:
			if(!resetType.equals(ResetType.ON_AI_COMPLETE) && 
					!resetType.equals(ResetType.ON_ANIM_END))
				strictAnim = Anim.SPECIAL1;
			anim = Anim.SPECIAL1;
			break;
		case SPECIAL2:
			if(!resetType.equals(ResetType.ON_AI_COMPLETE) || 
					!resetType.equals(ResetType.ON_ANIM_END))
				strictAnim = Anim.SPECIAL2;
			anim = Anim.SPECIAL2;
			break;
		case SPECIAL3:
			canPosition = false;
			if(time>=Vars.ACTION_ANIMATION_RATE * 
					Mob.actionLengths[Mob.animationIndicies.get(Anim.SPECIAL3_TRANS)]){
				canPosition = true;

				//do animation if outside influences changed it
				if(Math.abs(dx) < POS_RANGE && (
						!Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.SPECIAL3) &&
						!Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.SPECIAL3_TRANS)))
					owner.setAnimation(Anim.SPECIAL3, LoopBehavior.CONTINUOUS);
			} if(focus!=null){
				//watch focus if Mob doesn't need to reposisition
				if((canPosition && Math.abs(dx) < POS_RANGE) || !canPosition)
					owner.faceObject(focus);
			}
			
			if (resetType.equals(ResetType.ON_TIME)){
				if(time>=resetTime-1){
					owner.setAnimation(Anim.SPECIAL3_TRANS, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
					finish();
				}
			} else if(resetType!=ResetType.NEVER){
				inactiveTime += dt;
				if(inactiveTime>=inactiveWait){
					owner.setAnimation(Anim.SPECIAL3_TRANS, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
					finish();
				}
			}
			break;
			// stand around and look in random directions
		case STATIONARY:
			inactiveTime+=dt;
			if(inactiveTime >= inactiveWait){
				inactiveTime = 0;
				inactiveWait = (float)(Math.random()*6) + 1;
				if(!Mob.getAnimName(owner.animation.getCurrentType()).equals(Anim.IDLE))
					owner.changeDirection();
			}
			break;
		case STOP:
			finish();
			break;
		}
		
		if(anim!=null && !Mob.getAnimName(owner.animation.getCurrentType()).equals(anim))
				finish();
		if(strictAnim!=null && !Mob.getAnimName(owner.animation.getCurrentType()).equals(strictAnim))
			owner.setAnimation(strictAnim, LoopBehavior.CONTINUOUS);
			
	}
	
	//randomly find a new location for walking-type behaviors
	private void findNewLoc(float range, boolean limited){
		float max = (range*Vars.TILE_SIZE);
		float d = (float) Math.random()*(max - (2.25f*Vars.TILE_SIZE));
		float side = 1;
		
		if(limited && AIPhase2)
			side =-1;
		else if(Math.random()>=.5d)
				side = -1;
		d*=side;
		goalPosition = new Vector2((owner.respawnPoint.x+d), owner.respawnPoint.y);
		retLoc = goalPosition;
		canPosition = false;
		inactiveTime = 0;
		reached = false;
	}
	
	//relocate mob to original location
	public void position(){
		if(!canPosition) return;
//		System.out.println("positioning: "+owner);
		float dx = retLoc.x - owner.getPixelPosition().x;
		if(Math.abs(dx)>= POS_RANGE)
			moveToLoc(retLoc);
	}
	
	//moves the owner to position on update, returns true when reached
	//units in pixels
	private boolean moveToLoc(Vector2 loc){
		if(isReachable(loc)){
			float dx = loc.x - owner.getPixelPosition().x;
			if(Math.abs(dx) > 2){
				if (dx > 0) owner.right();
				else owner.left();
			} else {
				Vector2 v = owner.getBody().getLinearVelocity();
				owner.getBody().setLinearVelocity(new Vector2(v.x/2f, v.y));
				return true;
			}
			return false;
		}
		Vector2 v = owner.getBody().getLinearVelocity();
		owner.getBody().setLinearVelocity(new Vector2(v.x/2f, v.y));
		return true;
	}
	
	// attack focus if in range
	// if not in range, get close to object
	// otherwise evade hostile mobs or projectiles
	// if no hostile mobs or projectiles in sight, search vicinity
	
	//TODO
	public void fightAI(){
//		System.out.println(owner.ID+": FIGHTING");
		float dx, dy;
		if(!attacked){
//			System.out.println("!attacked");
			if(owner.attackFocus!=null && doTime>=doDelay){
				//System.out.println("attackFocus!=null && doTime>=doDelay");
				dx = owner.attackFocus.getPixelPosition().x - owner.getPixelPosition().x;
				dy = owner.attackFocus.getPixelPosition().y - owner.getPixelPosition().y;
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
					setGoal(new Vector2((float) (((Math.random() * 6)+owner.x)), owner.y));
					inactiveWait = (float)(Math.random() *(owner.attackRange) + 100);
					inactiveTime = 0;
					reached = true;
				}
				else {
					dx = (goalPosition.x - owner.getPixelPosition().x) ;
					if(dx < 1 && dx > -1){
						setGoal(new Vector2((float) (((Math.random() * 6)+owner.x)), owner.y));
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
		canPosition = true;
//		if(owner.respawnPoint!=null)
//			retLoc = owner.respawnPoint.cpy();
//		else
//			retLoc = new Vector2(owner.x, owner.y);
		
		retLoc = owner.getPixelPosition().cpy();
		Anim anim = null;

		switch(type){
		case AIM:
			data = owner.defaultState;
			if(resetType!=ResetType.ON_AI_COMPLETE && resetType!=ResetType.ON_ANIM_END)
				owner.defaultState = this;
			owner.aim();
			inactiveWait = DEFAULT_DURATION;
			break;
		case ATTACK:
			owner.setAnimation(Anim.ATTACKING, LoopBehavior.ONCE);
			break;
		case BLOCKPATH:
			short layer = Vars.BIT_BATTLE;
			data = owner.layer;
			owner.changeLayer(layer);
			owner.setAnimation(Anim.WALKING, LoopBehavior.ONCE);
			inactiveWait = DEFAULT_DURATION;
			break;
		case DANCING:
			anim = Anim.DANCE;
			break;
		case DUCK:
			owner.duck();
			inactiveWait = DEFAULT_DURATION;
			canPosition = false;
			break;
		case FLAIL:
			owner.setAnimation(Anim.ON_FIRE, LoopBehavior.CONTINUOUS);
			repeat = (int)(Math.random()*((10-2)+1))+2;
			findNewLoc(FLAIL_RANGE, true);
			canPosition = false;
			retLoc = goalPosition;
			break;
		case FLY:
			//flying related mechanics

			//flying = true;
			owner.body.setType(BodyType.KinematicBody);
			owner.setAnimation(Anim.SWIMMING, LoopBehavior.CONTINUOUS);
			break;
		case EVADING:
		case EVADING_ALL:
			reached = true;
			canPosition = true;
			break;
		case FOLLOWING:
			canPosition = false;
			break;
		case IDLE:
			anim = Anim.IDLE;
			break;
		case IDLEWALK:
			findNewLoc(IDLE_RANGE, false);
			inactiveWait = (float)(Math.random() * IDLE_DURATION + 1);
			retLoc = goalPosition;
			break;
		case HUG:
		case KISS:
			canPosition = false;
			inactiveWait = 2;
			if(resetType.equals(ResetType.NEVER)){
				AIPhase2 = true;
				reached = true;
				owner.embrace(type);
			}else{
				if(focus!=null && focus!=owner){
					float dx = focus.getPixelPosition().x - owner.getPixelPosition().x;
					float a = (Math.abs(dx)/dx), d = 1*Vars.TILE_SIZE;
					goalPosition = new Vector2(focus.getPixelPosition().x - a*d, focus.getPixelPosition().y);
				} else {
					AIPhase2 = true;
					reached = true;
					owner.embrace(type);
				}
			}
			break;
		case KNOCKOUT:
			data = owner.defaultState;
			if(resetType!=ResetType.ON_AI_COMPLETE && resetType!=ResetType.ON_ANIM_END)
				owner.defaultState = this;
			owner.knockOut();
			inactiveWait = DEFAULT_DURATION;
			break;
		case JUMP:
			canPosition = false;
			owner.jump();
			break;
		case LOOK_UP:
			owner.lookUp();
			inactiveWait = DEFAULT_DURATION;
			break;
		case LOSEAIM:
			if(!owner.aiming) finish();
			break;
		case RUN:
		case MOVE:
			canPosition = false;
			break;
		case PATH:
			canPosition = false;
			break;
		case PUNCH:
			anim = Anim.PUNCHING;
			break;
		case RECOVER:
			if(!owner.knockedOut) finish();
			break;
		case SHOOT:
			canPosition = false;
			if(owner.defaultState.type==AIType.AIM){
				owner.animation.reset();
				owner.setTransAnimation(Anim.ATTACKING, Vars.ACTION_ANIMATION_RATE, Anim.AIMING, 
						Vars.ANIMATION_RATE,LoopBehavior.CONTINUOUS, -1);
			}else
				anim = Anim.ATTACKING;
			break;
		case SLEEPING:
			canPosition = false;
			owner.setTransAnimation(Anim.LIE_DOWN, Anim.SLEEPING, LoopBehavior.CONTINUOUS);
			if(main.character.equals(this)/* && !sleepSpell*/){
				main.getSpriteBatch().fade();

				main.saveGame();
			}
			break;
		case SNOOZE:
			canPosition = false;
			inactiveWait = DEFAULT_DURATION;
			owner.snooze();
			break;
		case SPECIAL3:
			inactiveWait = DEFAULT_DURATION;
			owner.setTransAnimation(Anim.SPECIAL3, Vars.ACTION_ANIMATION_RATE, Anim.SPECIAL3, 
					Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
			break;
		case STATIONARY:
			inactiveWait = (float)(Math.random()*6) + 1;
			break;
		default:
			break;
		}
		
		if(anim!=null)
			switch(resetType){
			case ON_TIME:
				owner.setAnimation(anim, resetTime);
				break;
			case ON_AI_COMPLETE:
			case ON_ANIM_END:
				owner.setAnimation(anim, LoopBehavior.ONCE);
				break;
			case NEVER:
			case ON_LEVEL_CHANGE:
			case ON_SCRIPT_END:
				owner.setAnimation(anim, LoopBehavior.CONTINUOUS);
				break;
			}
	}
	
	//do special things when the AI finished
	public void close(){
		switch(type){
		case BLOCKPATH:
			owner.changeLayer((short) data);
			owner.setAnimation(Anim.WALKING, LoopBehavior.ONCE);
			break;
		case FIGHTING:
			break;
		case FLAIL:
			owner.animation.reset();
			break;
		case FLY:
			break;
		case FOLLOWING:
			focus.removeFollower(owner);
			break;
		case MOVE:
			if(owner.positioning && owner.positioningFocus!=null){
				owner.faceObject(owner.positioningFocus);
				owner.positioning = false;
				owner.positioningFocus = null;
			}
			break;
		case PATH_PAUSE:
			break;
		default:
			break;
		}
	}

	//conclude any controlled actions for mob
	private void finish(){
		finished = true;
		owner.controlled = false;
		System.out.println("AI finished: "+owner+";\t"+this.type);
		if(main.currentScript!=null)
			if(owner.equals(main.currentScript.getActiveObject()))
				main.currentScript.removeActiveObj();
	}

	//ensure the mob can reach its destination
	//units pixel
	private boolean isReachable(Vector2 loc){
		if(!owner.canMove())
			return false;
		
		//adjust goal if location is outside of level
		if(loc.x>owner.getCurrentScene().width)
			return false;
		if(loc.x<0)
			return false;
		return true;
	}

	public Vector2 getGoal() { return goalPosition; }
	public void setGoal(Vector2 goalPosition) { this.goalPosition = goalPosition; }
	
	public boolean equals(Object o){
		if(o instanceof MobAI){
			MobAI m = (MobAI) o;
			return type.equals(m.type) && resetType.equals(m.resetType) && resetTime==m.resetTime;
		}
		return false;
	}
	
	public String toString(){ return "t: "+type+"    rt: "+resetType+"    t: "+time; }
}