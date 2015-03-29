package entities;

import static handlers.Vars.PPM;
import handlers.Vars;
import main.Game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class SpeechBubble extends Entity {

	private Entity owner;
	private Vector2 range = new Vector2(2.5f/PPM, 2.5f/PPM);
	private Vector2 goal;
	private Vector2 center, v, dc;
	private boolean reached = true;
	private int idleTime = 30;
	private int time = idleTime;
	private float px, py;
	
	public static final int DEFAULT_WIDTH = 14;
	public static final int DEFAULT_HEIGHT = 12;
	
	//Standard interaction based bubble, e.g. speech
	public SpeechBubble(Entity d, float x, float y, int ID) {
		super(x, y, 14, 12, "speechBubble");
		setPlayState(d.getPlayState());
		gs.addObject(this);
		this.ID += ID;
		owner = d;
		player = d.getPlayer();
		center = new Vector2(x/PPM, y/PPM);
		v = new Vector2(center.x - owner.getPosition().x, center.y - owner.getPosition().y);
		
		TextureRegion[] sprites = TextureRegion.split(Game.res.getTexture("speechBubble"), width, height)[ID - 1];
		setDefaultAnimation(sprites[sprites.length - 1]);
		animation.setAction(sprites, sprites.length, false, 1, Vars.ACTION_ANIMATION_RATE);
	}
	
	public SpeechBubble(Entity d, float x, float y, String ID){
		super(x, y, getWidth(ID), getHeight(ID), ID);
		setPlayState(d.getPlayState());
		gs.addObject(this);
		owner = d;
		player = d.getPlayer();
		center = new Vector2(x/PPM, y/PPM);
		v = new Vector2(center.x - owner.getPosition().x, center.y - owner.getPosition().y);
		
		TextureRegion[] sprites = TextureRegion.split(Game.res.getTexture(ID), width, height)[0];
		setDefaultAnimation(sprites, Vars.ACTION_ANIMATION_RATE*2);
		animation.setAction(TextureRegion.split(texture, width, height)[1], determineLength(ID), 
				false, 1, Vars.ACTION_ANIMATION_RATE/2);
	}

	public void update(float dt){
		animation.update(dt);
		
		//calculations for hovering
		if (body != null) {
			act();
			Vector2 tmp = center.cpy();
			center = new Vector2(owner.getPosition().x + v.x, owner.getPosition().y + v.y);
			dc = new Vector2(center.x - tmp.x, center.y - tmp.y);
			if(goal != null) goal = new Vector2(goal.x + dc.x, goal.y + dc.y);
		}
		
		if(body != null && player.getInteractable() != owner && ID.equals("speechBubble1")) 
			gs.addBodyToRemove(body);
	}
	
	public void act() {
		time++;
		if (reached && time >= idleTime){
			reached = false;

			// goal position algoritm
			goal = new Vector2((float)((Math.random() * range.x * 2) - range.x + center.x), 
					(float)((Math.random() * range.y * 2) - range.y + center.y));
		}

		if (!reached){
			float dx = goal.x - body.getPosition().x; float dy = goal.y - body.getPosition().y;
			if (Math.abs(dx) <= 0.01 && Math.abs(dy) <= 0.01){
				reached = true;
				time = 0;
			}
			else {
				px = dx * 1.5f; py = dy * 1.5f;
				body.setLinearVelocity(px, py);
			}
		}
	}
	
	public void create(){
		//hitbox
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((rw-2)/PPM, (rh)/PPM);
		
		bdef.position.set(x/PPM, y/PPM);
		bdef.type = BodyType.KinematicBody;
		fdef.shape = shape;
		body = world.createBody(bdef);
		body.setUserData(this);
		fdef.filter.maskBits = (short) (layer | Vars.BIT_GROUND | Vars.BIT_PROJECTILE);
		fdef.filter.categoryBits = layer;
		body.createFixture(fdef).setUserData(Vars.trimNumbers(getID()));
	}
	
	private static int getWidth(String ID){
		try{
			Texture src = new Texture(Gdx.files.internal("res/images/entities/"+ID+"base.png"));
			return src.getWidth();
		} catch(Exception e) {
			return DEFAULT_WIDTH;
		}
	}

	private static int getHeight(String ID){
		try{
			Texture src = new Texture(Gdx.files.internal("res/images/entities/"+ID+"base.png"));
			return src.getHeight();
		} catch(Exception e) {
			return DEFAULT_HEIGHT;
		}
	}
	
	private static int determineLength(String ID){
		switch(ID){
		case "arrow":
			return 3;
		}
		return 1;
	}

	public Entity getOwner() { return owner;}
}
