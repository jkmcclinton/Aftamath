package entities;

import static handlers.Vars.PPM;
import handlers.Vars;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;

public class NPC extends Mob {
	
	//these variables determine what the NPC is doing
	protected int state, defaultState;
	public static final int STATIONARY = 0; 
	public static final int IDLEWALK = 1;
	public static final int FOLLOWING = 2;
	public static final int FACEPLAYER = 3;
	public static final int FACEOBJECT = 4;
	
	protected Vector2 goalPosition;
	protected float idleWait;
	protected float idleTime;
	protected float time;
	protected boolean reached, locked;
	protected Entity focus;
	
	protected static final int RANGE = 500;
	
	public NPC(){
		super(null, "", 0, 0, 10, 25, Vars.BIT_LAYER3);
		gender = "n/a";
	}
	
	public NPC(String name, String ID, int sceneID, float x, float y, short layer){
		super(name, ID, x, y, getWidth(ID), getHeight(ID), layer);
		health = MAX_HEALTH = DEFAULT_MAX_HEALTH;
		determineGender();
		
		this.sceneID = sceneID;
		goalPosition = new Vector2((float) (((Math.random() * 21)+x)/PPM), y);
		idleWait = (float)(Math.random() *(RANGE)+100);
		time = 0;
	}
	
	public void setState(int state){ 
		if (state == FOLLOWING) follow(player);
		else this.state = state;
	}
	
	public void setState(int state, Entity focus){ 
		if (state == FOLLOWING) follow(focus);
		else this.state = state;
		
		this.focus = focus;
	}
	
	public void setDefaultState(int state){
		this.state = defaultState = state;
	}
	
	public void resetState(){ state = defaultState; }
	public int getState(){ return state; }
	
	public void update(float dt){
		time += dt;
		if (state != STATIONARY && isOnGround() && !locked && !controlled) act();
		if (controlled) doAction(controlledAction);
		
		if (action == IDLE) animation.removeAction();
		if (!isOnGround()) {
			setAnimation(FALLING);
		}
		
		animation.update(dt);
		
		//stepping sound
		if (time >= MOVE_DELAY && isOnGround() && Math.abs(body.getLinearVelocity().x) >= MAX_VELOCITY / 2){
			gs.playSound(getPosition(), "step1");
			time = 0;
		}
		
		if (action == WALKING) action = IDLE;
	}
	
	public void act(){
		float dx/*, dy*/;
		switch (state){
			case IDLEWALK:
				if(reached) idleTime++;
				if(idleTime >= idleWait && reached) reached = false;
				else if (!reached){
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
			case FACEPLAYER:
				facePlayer();
				break;
			case FACEOBJECT:
				faceObject(focus);
				break;
			case FOLLOWING:
				facePlayer();
				dx = player.getPosition().x - body.getPosition().x;
				
				float m = MAX_DISTANCE * player.getFollowerIndex(this);
				
				if(dx > m/PPM) right();
				else if (dx < -1 * m/PPM) left();
				break;
		
		}
	}
	
	public boolean mustJump(){
		// determine if object is in way
		return false;
	}
	
	public void follow(Entity focus){
		state = FOLLOWING;
		focus.addFollower(this);
	}
	
	public void stay(){
		state = FACEPLAYER;
		player.removeFollower(this);
	}
	
	public void lock() { locked = true; }
	public void unlock() { locked = false; }
	
	private void determineGender(){
		Array<String> males = new Array<String>(new String[] {"narrator2","gangster1","gangster2","boyfriend1","boyfriend2","boyfriend3","boyfriend4",
				"kid1","kid2","richguy","burly1","burly2","reaper","magician","oldman1","oldman2",
				"bballer","boss1","boss2","cashier","hero1","hero2", "villain1", "villain2","biker1","bot1","policeman1",
				"policeman2","civilian1","civilian2","civilian3","civilian4"});
		if (males.contains(ID, true))
			gender = MALE;
		else gender = FEMALE;
	}
	
	public String getGender(){return gender;}
	
	private static int getWidth(String ID){
		try{
			Texture src = new Texture(Gdx.files.internal("res/images/entities/mobs/"+ID+"base.png"));
			return src.getWidth();
		} catch(Exception e) {
			return DEFAULT_WIDTH;
		}
	}

	private static int getHeight(String ID){
		try{
			Texture src = new Texture(Gdx.files.internal("res/images/entities/mobs/"+ID+"base.png"));
			return src.getHeight();
		} catch(Exception e) {
			return DEFAULT_HEIGHT;
		}
	}
	
	public NPC copy(){
		NPC n = new NPC(name, ID, sceneID, 0, 0, layer);
		
		n.resetHealth(health, MAX_HEALTH);
		n.setDefaultState(defaultState);
		
		return n;
	}
	
	public void spawn(Vector2 location){
		setPosition(location);
		create();
		gs.addObject(this);
	}
	
	public void create(){
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw-4)/Vars.PPM, (rh)/Vars.PPM);
		
		bdef.position.set(x/Vars.PPM, y/Vars.PPM);
		bdef.type = BodyType.DynamicBody;
		fdef.shape = shape;
		
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(ID));
		body.setFixedRotation(true);
		
		createFootSensor();
		createInteractSensor();
		createCenter();
	}
}
