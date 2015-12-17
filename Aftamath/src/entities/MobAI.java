package entities;

import static handlers.Vars.PPM;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;

import entities.Mob.Anim;
import handlers.Animation.LoopBehavior;
import handlers.FadingSpriteBatch;
import handlers.Vars;
import main.Main;

public class MobAI {
	public static enum AIType {
		STATIONARY, IDLEWALK, FOLLOWING, FACEPLAYER, FACEOBJECT, FIGHTING,
		EVADING, EVADING_ALL, DANCING, BLOCKPATH, /*TIMEDFIGHT,*/ PATH, PATH_PAUSE,
		MOVE, RUN, JUMP, FLY, SLEEPING, FLAIL, IDLE, AIM, LOSEAIM, PUNCH, SHOOT, ATTACK, HUG, 
		KISS, SNOOZE, DANCE, SPECIAL1, SPECIAL2, SPECIAL3, STOP
	}

	public static enum ResetType {
		ON_ANIM_END, ON_SCRIPT_END, ON_AI_COMPLETE, ON_LEVEL_CHANGE, ON_TIME, NEVER;
	}

	public final Mob owner;
	public Entity focus;
	public final AIType type;
	public final ResetType resetType;
	public final float resetTime;
	public boolean finished = false;
	public Path path;
	
	private int ctrlRepeat = -1;
	private final Main main; 
	private Vector2 goalPosition/*, prevLocation*/;
	private float inactiveWait, inactiveTime, doTime, doDelay, time;
	private float actionTime;
	private boolean reached, /*locked,*/ attacked, AIPhase2;

	private static final int IDLE_LIMIT = 500;
	
	public MobAI(Mob owner, AIType type){
		this(owner, type, ResetType.ON_AI_COMPLETE);
	}

	//initializing timed stuff
	public MobAI(Mob owner, AIType type, float resetTime) {
		this.owner = owner;
		this.type = type;
		this.resetType = ResetType.ON_TIME;
		this.resetTime = resetTime;
		this.main = owner.main;
	}

	//initializing nontimed AIs
	public MobAI(Mob owner, AIType type, ResetType resetType) {
		this.owner = owner;
		this.type = type;
		this.resetType = resetType;
		this.resetTime = -1;
		this.main = owner.main;
	}
	
	public void update(float dt){
		time+=dt;
		if(resetType.equals(ResetType.ON_TIME) && time>=resetTime){
			owner.resetState();
		} else if(!finished){
			act(dt);
		}
	}

	public void act(float dt) {
		float dx;
		Anim remove = null, anim = null;
		switch (type){
		//walk to random locations within a radius, then wait a random amount of time
		case IDLEWALK:
			if(reached) inactiveTime++;
			if(inactiveTime >= inactiveWait && reached) reached = false;
			else if (!reached){
				if (!owner.canMove()) {
					setGoal(new Vector2((float)(((Math.random()*6)+owner.x)/PPM), owner.y));
					inactiveWait = (float)(Math.random() * IDLE_LIMIT + 100);
					inactiveTime = 0;
					reached = true;
				}
				else {
					dx = (getGoal().x - owner.body.getPosition().x) * PPM ;
					if(dx < 1 && dx > -1){
						setGoal(new Vector2((float)(((Math.random()*6)+owner.x)/PPM), owner.y));
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
			owner.faceObject(focus);
			break;
			//stay close behind the target
		case FOLLOWING:
			owner.facePlayer();
			dx = main.character.getPosition().x - owner.body.getPosition().x;

			float m = Entity.MAX_DISTANCE * (main.character.getFollowingIndex(owner)+1);
			
			if(dx > m/PPM) owner.right();
			else if (dx < -1 * m/PPM) owner.left();
			break;
		case PATH:
			if (!isReachable()) {
				path.stepIndex();
				if(path.completed){
					path = null;
					owner.setState("STATIONARY");
				} else {
					setGoal(path.getCurrent());
				}
			}
			else {
				dx = (getGoal().x - owner.body.getPosition().x*PPM) ;
				if(dx < 1 && dx > -1){
					path.stepIndex();
					if(path.completed){
						path = null;
						owner.setState("STATIONARY");
					} else {
						setGoal(path.getCurrent());
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
				dx = (getGoal().x - owner.body.getPosition().x) * PPM ;
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
					if(owner.discovered.contains(focus, true)){
						dx = (focus.getPixelPosition().x - owner.getPixelPosition().x);
					}

				if(found)
					if(Math.abs(dx) <= owner.visionRange){
						float gx = -1*(dx/Math.abs(dx))*(owner.visionRange*3)/PPM + owner.getPosition().x;
						setGoal(new Vector2(gx, owner.y));
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
			if(owner.getAction()!=Anim.DANCE)
				owner.setAnimation(Anim.DANCE, LoopBehavior.CONTINUOUS);
			break;
		case RUN:
			owner.run();
		case MOVE:
			if(getGoal()==null && path==null)
				return;

			if(isReachable()){
				dx = (getGoal().x - owner.getPosition().x)* PPM ;

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
							setGoal(path.getCurrent());
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
			if(owner.AIfocus==null){
				finish();
			} else if (resetType.equals(ResetType.ON_TIME)){
				if(actionTime<=Vars.DT)
					owner.setAnimation(Anim.EMBRACE, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
				else if(!AIPhase2){
					AIPhase2 = true;
					owner.setTransAnimation(Anim.EMBRACE, Anim.HUGGING, LoopBehavior.CONTINUOUS);
				}
			} else {
				if(owner.getAction()!=Anim.HUGGING && owner.getAction()!=Anim.EMBRACE){
					dx = (owner.AIfocus.getPosition().x - owner.getPosition().x) * PPM;
				
					if(Math.abs(dx) > 1){
						if (dx > 0) owner.right();
						else owner.left();
					} else {
						owner.AIfocus.faceObject(owner);
						if(owner.AIfocus instanceof Mob)
							((Mob)owner.AIfocus).setTransAnimation(Anim.EMBRACE, Vars.ACTION_ANIMATION_RATE,
									Anim.HUGGING, Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
						owner.setTransAnimation(Anim.EMBRACE, Vars.ACTION_ANIMATION_RATE,
								Anim.HUGGING, Vars.ANIMATION_RATE, LoopBehavior.CONTINUOUS, -1);
					}
				} else if (owner.getAction()==Anim.HUGGING){
					if(owner.animation.getSpeed() * owner.animation.getTimesPlayed() >= 2){
						if(owner.AIfocus instanceof Mob)
							((Mob)owner.AIfocus).setAnimation(Anim.EMBRACE, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
						owner.setAnimation(Anim.EMBRACE, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
						AIPhase2 =true;
					}
				} else if (AIPhase2){
					if(owner.getAction()!=Anim.EMBRACE)
						finish();
				}
			}
			break;
		case KISS:
			if(resetType.equals(ResetType.ON_TIME)){
				if(actionTime<=Vars.DT)
					owner.setAnimation(Anim.ENGAGE_KISS, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
				else if(!AIPhase2){
					AIPhase2 = true;
					owner.setTransAnimation(Anim.ENGAGE_KISS, Anim.KISSING, LoopBehavior.CONTINUOUS);
				}
			} else {
				if(owner.AIfocus==null)
					finish();
				else {
					if(owner.getAction()!=Anim.KISSING && owner.getAction()!=Anim.ENGAGE_KISS){
						dx = (owner.AIfocus.getPosition().x - owner.getPosition().x) * PPM;

						if(Math.abs(dx) > 1){
							if (dx > 0) owner.right();
							else owner.left();
						} else {
							owner.AIfocus.faceObject(owner);
							owner.setTransAnimation(Anim.ENGAGE_KISS, Anim.KISSING, LoopBehavior.CONTINUOUS);
						}
					} else if (owner.getAction()==Anim.KISSING){
						if(owner.animation.getSpeed() * owner.animation.getTimesPlayed() >= 2){
							owner.setAnimation(Anim.ENGAGE_KISS, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
							AIPhase2 =true;
						}
					} else if (AIPhase2){
						if(owner.getAction()!=Anim.ENGAGE_KISS)
							finish();
					}
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
			owner.maxSpeed = Mob.RUN_SPEED;
			if(resetType.equals(ResetType.ON_TIME)){
				if(!AIPhase2){
					owner.setAnimation(Anim.ON_FIRE, LoopBehavior.CONTINUOUS);
					AIPhase2 = true;
				} else {
					if(owner.ctrlReached){
						setGoal(new Vector2(owner.getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
								Vars.TILE_SIZE, owner.getPosition().y));
						float d = 1;
						if(!owner.facingLeft) d = -1;

						setGoal(new Vector2(d * getGoal().x, getGoal().y));
						owner.ctrlReached = false;
					} else {
						if(isReachable()){
							dx = (getGoal().x - owner.getPosition().x)* PPM ;

							if(Math.abs(dx) > 1){
								if (dx > 0) owner.right();
								else owner.left();
							} else 
								owner.ctrlReached = true;
						} else 
							owner.ctrlReached = true;
					}

					if(actionTime<=Vars.DT)
						owner.animation.reset();
				}
			}else{
				if(ctrlRepeat==-1)
					ctrlRepeat = (int) (Math.random()*2) + 2;

				if(owner.ctrlReached){
					setGoal(new Vector2(owner.getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
							Vars.TILE_SIZE, owner.getPosition().y));
					float d = 1;
					if(!owner.isFacingLeft()) d = -1;

					setGoal(new Vector2(d * getGoal().x, getGoal().y));
					owner.ctrlReached = false;
					ctrlRepeat--;
				} else {
					if(isReachable()){
						if(getGoal()==null)
							setGoal(new Vector2(owner.getPosition().x + (float) Math.random()*(2f*Vars.TILE_SIZE)+
									Vars.TILE_SIZE, owner.getPosition().y));
						dx = (getGoal().x - owner.getPosition().x)* PPM ;

						if(Math.abs(dx) > 1){
							if (dx > 0) owner.right();
							else owner.left();
						} else 
							owner.ctrlReached = true;
					} else 
						owner.ctrlReached = true;
				}

				if(ctrlRepeat == 0){
					finish();
					owner.animation.reset();
				}
			}
			break;
		case FLY:
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
				owner.setAnimation(Anim.LIE_DOWN, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
			}
			
			if(AIPhase2 && owner.getAction()!=Anim.LIE_DOWN){
				finish();
				if(main.character.equals(this)){
					//trigger any events
				}
			}
			break;
		case SNOOZE:
			if(resetType.equals(ResetType.ON_TIME)){
				if(actionTime<=Vars.DT)
					owner.setAnimation(Anim.GET_DOWN, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
				else if(!AIPhase2){
					AIPhase2 = true;
					owner.setTransAnimation(Anim.GET_DOWN, Anim.SNOOZING);			
				}
				break;
			} else {
				if(!AIPhase2)
					if(owner.getAction()!=Anim.SNOOZING && owner.getAction()!=Anim.GET_DOWN)
						if(owner.animation.getTimesPlayed() * owner.animation.getSpeed() >= 3){
							AIPhase2 = true;
							owner.wake();
						}
			}
			break;
		case LOSEAIM:
		case AIM:
			if(resetType.equals(ResetType.ON_TIME)){
				if(actionTime<=Vars.DT)
					owner.unAim();
				else if(!owner.aiming)
					owner.aim();
			} else
				remove = Anim.AIM_TRANS;
			break;
		case IDLE:
			if (resetType.equals(ResetType.ON_TIME)){
				anim = Anim.IDLE;
			} else
				remove = Anim.IDLE;
			break;
		case DANCE:
			if (resetType.equals(ResetType.ON_TIME)){
				anim = Anim.DANCE;
			} else
				remove = Anim.DANCE;
			break;
		case SPECIAL1:
			if (resetType.equals(ResetType.ON_TIME)){
				anim = Anim.SPECIAL1;
			} else
				remove = Anim.SPECIAL1;
			break;
		case SPECIAL2:
			if (resetType.equals(ResetType.ON_TIME)){
				anim = Anim.SPECIAL2;
			} else
				remove = Anim.SPECIAL2;
			break;
		case SPECIAL3:
			if (resetType.equals(ResetType.ON_TIME)){
				if(actionTime<=Vars.DT)
					owner.setAnimation(Anim.SPECIAL3, LoopBehavior.ONCE, Vars.ACTION_ANIMATION_RATE, true);
				else if(!AIPhase2){
					AIPhase2 = true;
					owner.setTransAnimation(Anim.SPECIAL3, Anim.HUGGING, LoopBehavior.CONTINUOUS);
				}
			} else
				remove = Anim.SPECIAL3;
			break;
		case ATTACK:
			if (resetType.equals(ResetType.ON_TIME)){
				anim = Anim.ATTACKING;
			} else
				remove = Anim.ATTACKING;
			break;
		case PUNCH:
			if (resetType.equals(ResetType.ON_TIME)){
				anim = Anim.PUNCHING;
			} else
				remove = Anim.PUNCHING;
			break;
		default:
			break;
		}
		
		if(remove!=null)
			if(owner.getAction()!=remove)
				finish();
		
		if(anim!=null)
			owner.setAnimation(anim, LoopBehavior.ONCE);
		
		if(actionTime<=Vars.DT)
			finish();
	}
	
	// attack focus if in range
	// if not in range, get close to object
	// otherwise evade hostile mobs or projectiles
	// if no hostile mobs or projectiles in sight, search vicinity
	public void fightAI(){
//		System.out.println(owner.ID+": FIGHTING");
		float dx, dy;
		if(!attacked){
//			System.out.println("!attacked");
			if(owner.attackFocus!=null && doTime>=doDelay){
				//System.out.println("attackFocus!=null && doTime>=doDelay");
				dx = owner.attackFocus.getPosition().x - owner.getPosition().x;
				dy = owner.attackFocus.getPosition().y - owner.getPosition().y;
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
					setGoal(new Vector2((float) (((Math.random() * 6)+owner.x)/PPM), owner.y));
					inactiveWait = (float)(Math.random() *(owner.attackRange) + 100);
					inactiveTime = 0;
					reached = true;
				}
				else {
					dx = (getGoal().x - owner.getPosition().x) * PPM ;
					if(dx < 1 && dx > -1){
						setGoal(new Vector2((float) (((Math.random() * 6)+owner.x)/PPM), owner.y));
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
		setGoal(null);
		owner.controlled = false;
		AIPhase2 = false;
		actionTime = 0;
		ctrlRepeat = -1;
//		finished = true;
		
	}

	//ensure the mob can reach its destination
	private boolean isReachable(){
		if(!owner.canMove())
			return false;
		//goalPosisition;
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
	
	public String toString(){
		return "t: "+type+"    rt: "+resetType+"    t: "+time;
	}

}
