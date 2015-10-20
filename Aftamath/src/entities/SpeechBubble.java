package entities;

import static handlers.Vars.PPM;
import handlers.FadingSpriteBatch;
import handlers.Vars;
import main.Game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;

public class SpeechBubble extends Entity {

	public int sizingState;
	public boolean expanded;

	private Entity owner;
	private Vector2 range = new Vector2(2.5f/PPM, 2.5f/PPM);
	private Vector2 goal;
	private Vector2 center, v, dc;
	private String message;
	private TextureRegion[] font;
	private TextureRegion left, middle, right;
	private boolean reached = true;
	private int idleTime = 30;
	private int time = idleTime;
	private int maxWidth = 16, minWidth, innerWidth;
	private PositionType positioningType;
	
	public static final int DEFAULT_WIDTH = 14;
	public static final int DEFAULT_HEIGHT = 12;
	public static final int EXPANDING = 1;
	public static final int COLLAPSING = -1;
	
	public static enum PositionType{
		LEFT_MARGIN, CENTERED, RIGHT_MARGIN,
	}
	
	/**
	 * Types:
	 * 1 - ...;
	 * 2 - exciting;
	 * 3 - curious;
	 * 4 - cowardly;
	 * 5 - power;
	 * 6 - Yes;
	 * 7 - No
	 */
		
	
	//Standard interaction based bubble, e.g. speech
	public SpeechBubble(Entity d, float x, float y, int ID, String message, PositionType positioningType) {
		super(x, y, "speechBubble");
		setGameState(d.getGameState());
		
		setDimensions(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
		main.addObject(this);
		this.ID += ID;
		this.message = message;
		
		this.positioningType = positioningType;
		owner = d;
		center = new Vector2(x/PPM, y/PPM);
		v = new Vector2(center.x - owner.getPosition().x, center.y - owner.getPosition().y);

		font = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/text5.png")), 7, 9 )[0];
		left = new TextureRegion(Game.res.getTexture("speechBubble"), 10, 96, 3, 12);
		middle = new TextureRegion(Game.res.getTexture("speechBubble"), 14, 96, 1, 12);
		right = new TextureRegion(Game.res.getTexture("speechBubble"), 29, 96, 3, 12);
		
		maxWidth = message.length() * font[0].getRegionWidth();
		TextureRegion[] sprites = TextureRegion.split(Game.res.getTexture("speechBubble"), width, height)[ID];
		setDefaultAnimation(sprites[sprites.length - 1]);
		animation.setAction(sprites, sprites.length, false, 1, Vars.ACTION_ANIMATION_RATE, false);
	}
	
	public SpeechBubble(Entity d, float x, float y, String ID){
		super(x, y, ID);
		setGameState(d.getGameState());

		setDimensions();
		
		main.addObject(this);
		this.message = "";
		owner = d;
		center = new Vector2(x/PPM, y/PPM);
		v = new Vector2(center.x - owner.getPosition().x, center.y - owner.getPosition().y);

		font = TextureRegion.split(new Texture(Gdx.files.internal("assets/images/text5.png")), 7, 9 )[0];
		TextureRegion[] sprites = TextureRegion.split(Game.res.getTexture(ID), width, height)[0];
		setDefaultAnimation(sprites, Vars.ACTION_ANIMATION_RATE*2);
		animation.setAction(TextureRegion.split(texture, width, height)[1], determineLength(ID), 
				false, 1, Vars.ACTION_ANIMATION_RATE/2, false);
	}

	public void update(float dt){
		animation.update(dt);
		
		//calculations for hovering
		if (body != null) {
			reposition();
			Vector2 tmp = center.cpy();
			center = new Vector2(owner.getPosition().x + v.x, owner.getPosition().y + v.y);
			dc = new Vector2(center.x - tmp.x, center.y - tmp.y);
			if(goal != null) goal = new Vector2(goal.x + dc.x, goal.y + dc.y);
		}
		
		//show or hide internal message
		int diff = maxWidth/8;
		if(sizingState == EXPANDING){
			if(innerWidth >= maxWidth){ 
				sizingState = 0;
				expanded = true;
				innerWidth = maxWidth;
			} else 
				innerWidth+=diff;
		} if (sizingState == COLLAPSING){
			if(innerWidth <= minWidth)
				sizingState = 0;
			else
				innerWidth-=diff;
		}
		
		//destroy object if interaction has lost contact
		if(body != null && main.character.getInteractable() != owner && ID.equals("speechBubble0")) 
			main.addBodyToRemove(getBody());
	}
	
	public void render(FadingSpriteBatch sb){
		boolean bool=false;
		if(sb.isDrawingOverlay()){
			sb.setOverlayDraw(false);
			bool = true;
		}
		
		if(sizingState!=0||expanded){
			if(getBody()==null)
				create();
			drawBacking(innerWidth, sb);
//			switch(positioningType){
//				case LEFT_MARGIN:
//					sb.draw(left, getPosition().x*Vars.PPM - rw, getPosition().y*Vars.PPM-rh);
//					sb.draw(right, getPosition().x*Vars.PPM - rw + innerWidth + 3, getPosition().y*Vars.PPM-rh);
//					sb.draw(middle, getPosition().x*Vars.PPM - rw + 3, getPosition().y*Vars.PPM-rh, innerWidth, middle.getRegionHeight());
//					break;
//				case CENTERED:
//					sb.draw(left, getPosition().x*Vars.PPM - rw - innerWidth/2 + 1, getPosition().y*Vars.PPM-rh);
//					sb.draw(right, getPosition().x*Vars.PPM - rw + innerWidth/2 +3, getPosition().y*Vars.PPM-rh);
//					sb.draw(middle, getPosition().x*Vars.PPM - rw + 3, getPosition().y*Vars.PPM-rh, innerWidth/2, middle.getRegionHeight());
//					sb.draw(middle, getPosition().x*Vars.PPM - rw - innerWidth/2 + 3, getPosition().y*Vars.PPM-rh, innerWidth/2, middle.getRegionHeight());
//					break;
//				case RIGHT_MARGIN:
//					sb.draw(left, getPosition().x*Vars.PPM + rw - innerWidth - 5, getPosition().y*Vars.PPM-rh);
//					sb.draw(right, getPosition().x*Vars.PPM + rw - 3, getPosition().y*Vars.PPM-rh);
//					sb.draw(middle, getPosition().x*Vars.PPM + rw - innerWidth - 3, getPosition().y*Vars.PPM-rh, innerWidth, middle.getRegionHeight());
//					break;
//				}
			if(expanded){
				float x = 0;
				if (positioningType==PositionType.RIGHT_MARGIN) 
					x = message.length()*font[0].getRegionWidth()- font[0].getRegionWidth()-2;
				else if (positioningType==PositionType.CENTERED)
					x = message.length()*font[0].getRegionWidth()/2f-1;
				main.drawString(sb, font, font[0].getRegionWidth(), message, 
						getPosition().x*Vars.PPM-rw-x+3, getPosition().y*Vars.PPM-rh+2f);
			}
		} else{
			if(ID.contains("ble6")){
				drawBacking(8, sb);
				float x = 0;
				if (positioningType==PositionType.RIGHT_MARGIN) 
					x = font[0].getRegionWidth()- font[0].getRegionWidth()-2;
				else if (positioningType==PositionType.CENTERED)
					x = font[0].getRegionWidth()/2f-1;
				main.drawString(sb, font, font[0].getRegionWidth(), message.substring(0, 1), 
						getPosition().x*Vars.PPM-rw-x+3, getPosition().y*Vars.PPM-rh+2f);
			} else
			super.render(sb);
		}
		if(bool)
			sb.setOverlayDraw(true);
	}
	
	private void drawBacking(int w, FadingSpriteBatch sb){
		switch(positioningType){
		case LEFT_MARGIN:
			sb.draw(left, getPosition().x*Vars.PPM - rw, getPosition().y*Vars.PPM-rh);
			sb.draw(right, getPosition().x*Vars.PPM - rw + w +3, getPosition().y*Vars.PPM-rh);
			sb.draw(middle, getPosition().x*Vars.PPM - rw + 3, getPosition().y*Vars.PPM-rh, w, middle.getRegionHeight());
			break;
		case CENTERED:
			sb.draw(left, getPosition().x*Vars.PPM - rw - w/2 + 1, getPosition().y*Vars.PPM-rh);
			sb.draw(right, getPosition().x*Vars.PPM - rw + w/2 +3, getPosition().y*Vars.PPM-rh);
			sb.draw(middle, getPosition().x*Vars.PPM - rw + 3, getPosition().y*Vars.PPM-rh, w/2, middle.getRegionHeight());
			sb.draw(middle, getPosition().x*Vars.PPM - rw - w/2 + 3, getPosition().y*Vars.PPM-rh, w/2, middle.getRegionHeight());
			break;
		case RIGHT_MARGIN:
			sb.draw(left, getPosition().x*Vars.PPM + rw - w - 5, getPosition().y*Vars.PPM-rh);
			sb.draw(right, getPosition().x*Vars.PPM + rw - 3, getPosition().y*Vars.PPM-rh);
			sb.draw(middle, getPosition().x*Vars.PPM + rw - w - 3, getPosition().y*Vars.PPM-rh, w, middle.getRegionHeight());
			break;
		}
	}
	
	public void expand(){ 
		if(!ID.startsWith("speechBubble")) return;
		sizingState = EXPANDING; 
	}
	
	public void collapse(){ 
		if(!ID.startsWith("speechBubble")) return;
		sizingState = COLLAPSING; 
		expanded = false;
	}
	
	//floating arround algorithm
	public void reposition() {
		time++;
		if (reached && time >= idleTime){
			reached = false;

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
				body.setLinearVelocity(dx * 1.5f, dy * 1.5f);
			}
		}
	}
	
	public void create(){
		init = true;
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
		body.createFixture(fdef).setUserData(Vars.trimNumbers(ID));
	}
	
	protected void setDimensions(){
		setDimensions(getWidth(ID), getHeight(ID));	
	}
	
	protected void setDimensions(int width, int height){
		this.width = width;
		this.height = height;
		rw = width/2;
		rh = height/2;
	}
	
	protected static int getWidth(String ID){
		try{
			Texture src = Game.res.getTexture(ID+"base");
			return src.getWidth();
		} catch(Exception e) {
			return DEFAULT_WIDTH;
		}
	}

	protected static int getHeight(String ID){
		try{
			Texture src = Game.res.getTexture(ID+"base");
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
	public String getMessage(){ return message; }
	public void setMessage(String message){ this.message = message; }
}
