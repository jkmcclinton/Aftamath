package entities;

import static handlers.Vars.PPM;
import handlers.Vars;
import main.Game;
import scenes.Script;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public abstract class Mob extends Entity{
	
	public int numContacts;
	public Sound voice;
	public boolean dead, positioning, canWarp, canClimb;

	protected Vector2 respawnPoint;
	protected String name;
	protected double health, MAX_HEALTH;
	protected boolean invulnerable;
	protected int invulnerableTime, action;
	protected float time;
	protected TextureRegion[] face;
	protected Warp warp;

	private Entity interactable;
	
	//emotion indicies
	public static final int NORMAL = 0;
	public static final int HAPPY = 1;
	public static final int SAD = 2;
	public static final int MAD = 3;	
	public static final int FLIRTY = 4;
	
	//characteristic constants
	public static final String MALE = "male";
	public static final String FEMALE = "female";
	protected static final float MAX_VELOCITY = .75f;
	protected static final float MOVE_DELAY = Vars.ANIMATION_RATE * 2;
	protected static final int DEFAULT_WIDTH = 20;
	protected static final int DEFAULT_HEIGHT = 50;
	protected static final double DEFAULT_MAX_HEALTH = 20;
	protected static final int INTERACTION_SPACE = 10;
	
	//action animation indicies
	public static final int IDLE = 0;
	public static final int WALKING = 1;
	public static final int JUMPING = 2;
	public static final int FALLING = 3;
	public static final int FLINCHING = 4;
	public static final int DYING = 5;
	public static final int POWERING = 6;
	public static final int ATTACKING = 7;
	public static final int SPECIAL1 = 8;
	public static final int SPECIAL2 = 9;
	protected static int[] actionLengths = {0, 4, 1, 2, 2, 4, 2, 2};
	protected static int[] actionPriorities = {0,1,2,3,4,5,4,4,4,4}; // determines what animation gets played first
	
	protected Mob(String name, String ID, float x, float y, int w, int h, short layer) {
		super(x, y+h/2f, w, h, ID);
		this.name = name;
		this.layer = layer;
		actionTypes = 7;
		this.health = MAX_HEALTH = 25;
		
		Texture texture = Game.res.getTexture(ID + "face");
		if (texture != null) face = TextureRegion.split(texture, 64, 64)[0];
	}
	
	public void update(float dt){
		if (invulnerable && invulnerableTime > 0) invulnerableTime--;
		else if (invulnerable && invulnerableTime == 0) invulnerable = false;

		if(controlled)
			doAction(controlledAction);
		if (action == IDLE) 
			animation.removeAction();
		
		if (!isOnGround()) 
			setAnimation(FALLING);
		animation.update(dt);
		
		//stepping sound
		time += dt;
		if (time >= MOVE_DELAY && isOnGround() && Math.abs(body.getLinearVelocity().x) >= MAX_VELOCITY / 2){
			gs.playSound(getPosition(), "step1");
			time = 0;
		}
		
//		if (action == WALKING) action = IDLE;
		
		if (animation.actionID < 4 && animation.actionID != JUMPING && animation.actionID != FLINCHING) {
			action = IDLE;
		} 
	}
	
	//must only be used for copying data
	public void resetHealth(double health, double maxHealth){
		this.health = health;
		this.MAX_HEALTH = maxHealth;
	}
	
	public void setPosition(Vector2 location){
		x=location.x;
		y=location.y+rh;
		setRespawnpoint(location);
	}
	
	protected void setAnimation(int action){
		setAnimation(action, false);
	}
	
	protected void setAnimation(int action, boolean repeat){
		setAnimation(action, Vars.ANIMATION_RATE, repeat);
	}
	
	protected void setAnimation(int action, float delay){
		setAnimation(action, delay, false);
	}

	protected void setAnimation(int action, float delay, boolean repeat) {
//		idleTime = 0;
		if (actionPriorities[action] < actionPriorities[animation.actionID])
			return;
		this.action = action;
		
		try{
			TextureRegion[] sprites = TextureRegion.split(texture, width, height)[action];
			animation.setAction(sprites, actionLengths[action], direction, action/*,repeat*/);
		} catch(ArrayIndexOutOfBoundsException e) {
			
		}
	}

	public double getHealth(){
		return health;
	}
	
	public String getName(){
		return name;
	}
	
	public void damage(double damageVal){
		if (!dead) {
			if (!invulnerable) 
				health -= damageVal;
			setAnimation(FLINCHING, Vars.ACTION_ANIMATION_RATE);
		}
		if (health <= 0 && !dead) 
			die();
		//System.out.println(ID + ": " + health);
	}
	
	public void restore(){
		heal(MAX_HEALTH);
		makeInvulnerable(30);
	}
	
	public void makeInvulnerable(int t){
		invulnerableTime = t;
		invulnerable = true;
	}
	
	public void heal(double healVal){
		health += healVal;
		if (health > MAX_HEALTH) 
			health = MAX_HEALTH;
	}
	
	public void die(){
//		Play.debugText = getID() + ": DIE!!!";
		System.out.println(ID + ": DIE!!!");
		dead = true;
		
//		if(this instanceof Player)
//			//do death event
//			Gdx.app.exit();
	}
	
	public void respawn(){
		if (respawnPoint!=null){
			System.out.println("respawning");
			setPosition(respawnPoint);
			dead = false;
			animation.removeAction();
//			gs.getCam().zoom(Camera.ZOOM_NORMAL);
//			gs.getB2dCam().zoom(Camera.ZOOM_NORMAL);
			gs.addBodyToRemove(body);
			create();
		}
	}
	
	public void setRespawnpoint(Vector2 location){ respawnPoint = location.cpy(); }
	
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

			if(Math.abs(dx) * PPM > 1){
				if (dx > 0) right();
				else left();
			} else {
				if (positioning) {
					positioning = false;
					faceObject(interactable);
					goal = null;
				}
				controlled = false;
			}
			break;
		default:
			controlled = false;
		}
	}
	
	public void lookUp(){
		
	}
	
	public void jump() {
		if (isOnGround()) {
			action = JUMPING;
			gs.playSound(getPosition(), "jump1");
			body.applyForceToCenter(0f, 160f, true);
			
			setAnimation(JUMPING);
		}
	}
	
	// determine if object is in way
	public boolean mustJump(){
		return false;
	}
	
	public void changeDirection(){
		int offset = -1;
		if (direction) {direction = false; offset = 1; }
		else direction = true;
		try{
		//change position of interaction space
		PolygonShape shape = (PolygonShape) body.getFixtureList().get(2).getShape();
		shape.setAsBox((rw + INTERACTION_SPACE/2)/Vars.PPM, rh/Vars.PPM, new Vector2(INTERACTION_SPACE*offset/Vars.PPM/2, 0), 0);

		animation.flip(direction);
		} catch(Exception e){
			
		}
	}
	
	//determine if destination is reachable
	public boolean canMove() {
		return true;
	}
	
	public void left(){
		if (!canMove()) return;
		
		if (!direction) changeDirection();
		if (Math.abs(body.getLinearVelocity().x) < MAX_VELOCITY) body.applyForceToCenter(-5f, 0, true);
		setAnimation(WALKING);
		
		if (!(this instanceof Player) && mustJump()){
			jump();
		}
	}
	
	public void right(){
		if (animation.actionID != JUMPING) action = WALKING;
		if (direction) changeDirection();
		if (Math.abs(body.getLinearVelocity().x) < MAX_VELOCITY) body.applyForceToCenter(5f, 0, true);
		setAnimation(WALKING);
		
		if (!(this instanceof Player) && mustJump()){
			jump();
		}
	}
	
	public void climb(){ }
	
	public void descend(){ }
	
	public Script interact(){
		if (interactable == null) return null;
		return interactable.getScript();
	}
	
	public TextureRegion getFace(int face){
		if (this.face != null) return this.face[face];
		return null;
	}
	
	public boolean isOnGround(){
		if (numContacts > 0) return true;
		else return false;
	}
	
	public Entity getInteractable(){ return interactable; }
	public void setInteractable( Entity d) { interactable = d; }
	
	public Warp getWarp(){ return warp; }
	public void setWarp(Warp warp){ this.warp = warp; }

	public void create(){;
		bdef = new BodyDef();
		fdef = new FixtureDef();
		//hitbox
		direction = false;
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw-4)/Vars.PPM, (rh)/Vars.PPM);
		
		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.DynamicBody;
		fdef.shape = shape;
		
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(ID));
		body.setFixedRotation(true);
		
		createFootSensor();
		createInteractSensor();
		createCenter();
	}
	
	protected void createFootSensor(){
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw - 5)/Vars.PPM, 2/Vars.PPM, new Vector2(0, -1 * rh/Vars.PPM), 0);
		fdef.shape = shape;
		
		fdef.isSensor = true;
		fdef.filter.categoryBits = (short) (layer);
		fdef.filter.maskBits = (short) (Vars.BIT_HALFGROUND | Vars.BIT_GROUND);
		body.createFixture(fdef).setUserData("foot");
	}
	
	protected void createInteractSensor(){
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw + INTERACTION_SPACE/2)/Vars.PPM, rh/Vars.PPM, new Vector2(INTERACTION_SPACE/Vars.PPM/2, 0), 0);
		fdef.shape = shape;

		fdef.isSensor = true;
		fdef.filter.categoryBits = Vars.BIT_GROUND | Vars.BIT_HALFGROUND | Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3;
		body.createFixture(fdef).setUserData("interact");
	}
	
}
