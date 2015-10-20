package entities;

import static handlers.Vars.PPM;
import handlers.Animation;
import handlers.FadingSpriteBatch;
import handlers.Vars;
import main.Game;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.Array;

public class TextBox extends Entity {
	
	public boolean expanded;
	public int sizingState;
	
	private String msgSrc;
	private String[] text;
	private TextureRegion[] font;
	private TextureRegion top, bottom, left, right, center,
		spike, cornerTL, cornerTR, cornerBL, cornerBR;
	private Entity owner;
	private boolean selfKill;
	private int innerWidth, innerHeight, maxWidth, maxHeight, side;
	private float lifetime, speedX, speedY;
	private Array<Entity> contacts;
	
	private static final int COLLAPSING = -1;
	private static final int EXPANDING = 1;
	private static final float SIZE_TIME = 1.25f;

	public TextBox(Entity owner, String message, boolean kill) {
		this.ID = "textBox";
		this.x = owner.getPosition().x*Vars.PPM + owner.rh;
		this.y = owner.getPosition().y*Vars.PPM + owner.rw / 2;
		origLayer = Vars.BIT_LAYER1;
		isAttackable = false;
		selfKill = kill;
		
		animation = new Animation();
		this.msgSrc = Vars.formatDialog(message, false);
		text = msgSrc.split("/l");
		setGameState(owner.getGameState());
		this.owner = owner;
		main.addObject(this);
		contacts = new Array<>();
		
		if(selfKill){
			int n = message.split(" ").length /* *1.5f*/;
			float m = 15, o = 2, a = 2*(m - o), b = 4f/a;
			lifetime = (float) (a/(1+Math.exp(-b*n)) - a/2 + o);
//			System.out.println("("+n+", "+lifetime+")");
		} else
			lifetime = 0;
		double c = (Math.random());
		if(c>.5) side = 1;
		else side = -1;
		
		font = TextureRegion.split(Game.res.getTexture("text3"), 7, 9 )[2];
		top = new TextureRegion(Game.res.getTexture("textBox"), 6, 0, 42, 7);
		bottom = new TextureRegion(Game.res.getTexture("textBox"), 6, 14, 21, 9);
		left = new TextureRegion(Game.res.getTexture("textBox"), 0, 7, 6, 7);
		right = new TextureRegion(Game.res.getTexture("textBox"), 48, 7, 6, 7);
		center = new TextureRegion(Game.res.getTexture("textBox"), 6, 7, 42, 7);
		spike = new TextureRegion(Game.res.getTexture("textBox"), 27, 14, 11, 15);
		cornerTL = new TextureRegion(Game.res.getTexture("textBox"), 6, 7);
		cornerTR = new TextureRegion(Game.res.getTexture("textBox"), 48, 0, 6, 7);
		cornerBL = new TextureRegion(Game.res.getTexture("textBox"), 0, 14, 6, 9);
		cornerBR = new TextureRegion(Game.res.getTexture("textBox"), 48, 14, 6, 9);
			
		rw = rh = 2;
		
		int max = 0;
		for(String s : text)
			if(s.length()>max){
				s = s.trim();
				max = s.length();
			}
		
		maxWidth = max * (font[0].getRegionWidth() + 1);
		maxHeight = text.length * (font[0].getRegionHeight());
		speedX = (5*maxWidth*Vars.DT) / SIZE_TIME;
		speedY = (5*maxHeight*Vars.DT) / SIZE_TIME;
		if(speedX<2)speedX=2;
		if(speedY<2)speedY=2;
		width = cornerTL.getRegionWidth() + maxWidth + cornerTR.getRegionWidth();
		height = cornerTL.getRegionHeight() + maxHeight + cornerBL.getRegionHeight();
		innerWidth = innerHeight = 1;
		expand();
	}
	
	public void update(float dt){
		constrainPosition(dt);
		
		if(selfKill){
			lifetime-=dt;
			if(lifetime<=0){
				if(sizingState!=COLLAPSING && expanded)
					kill();
			}
		} else
			lifetime+=dt;
		
		if(sizingState!=0){
			if(sizingState==EXPANDING){
				if(innerWidth<maxWidth) innerWidth += speedX;
				if(innerWidth>maxWidth) innerWidth = maxWidth;
				if(innerHeight<maxHeight) innerHeight += speedY;
				if(innerHeight>maxHeight) innerHeight = maxHeight;
				
				if(innerWidth==maxWidth && innerHeight==maxHeight){
					expanded = true;
					sizingState = 0;
				}
			} else {
				if(innerWidth>1) innerWidth -= speedX;
				if(innerWidth<1) innerWidth = 1;
				if(innerHeight>1) innerHeight -= speedY;
				if(innerHeight<1) innerHeight = 1;
				
				if(innerWidth==1 && innerWidth==1){
					main.addBodyToRemove(getBody());
					sizingState = 0;
				}
			}
		}
	}
	
	//limit where the textbox is given the position of owner with respect to the Camera
	//and other textboxes
	public void constrainPosition(float dt){
//		for(Entity e : contacts){
//			
//		}
	}
	
	public void render(FadingSpriteBatch sb){
		int marginL = cornerTL.getRegionWidth(), marginD = cornerBL.getRegionHeight();
		int d = innerWidth * (side - 1)/2, o = 10;
//		float z = main.getCam().zoom / Camera.ZOOM_NORMAL;
		float x = 2*(float) Math.cos(lifetime);
		float y = 2*(float) Math.sin(lifetime);
		Vector2 v = new Vector2(owner.getPosition().x * Vars.PPM + o * side + x,
				owner.getPosition().y*Vars.PPM + y + owner.height + 1);
		float x1 = (2 * side / 9f) * (2 * marginL + innerWidth - spike.getRegionWidth()) 
				+ v.x - marginL;
		float y1 = v.y - spike.getRegionHeight() ;
		
		boolean bool = false;
		if(sb.isDrawingOverlay()){
			sb.setOverlayDraw(false);
			bool = true;
		}
		
//		System.out.println("("+speedX+", "+speedY+")\t("+innerWidth+", "+innerHeight+")\t("+maxWidth+", "+maxHeight+")");
		
		//draw all parts
		sb.draw(top, v.x + d, v.y + innerHeight, innerWidth, top.getRegionHeight());
		sb.draw(left, v.x - marginL + d, v.y, marginL, innerHeight);
		sb.draw(center, v.x + d, v.y, innerWidth, innerHeight);
		sb.draw(right, v.x + d + innerWidth, v.y, marginL, innerHeight);
		sb.draw(bottom, v.x + d, v.y - marginD, innerWidth, marginD);
		sb.draw(cornerTL, v.x - marginL + d, v.y + innerHeight);
		sb.draw(cornerTR, v.x + innerWidth + d, v.y + innerHeight);
		sb.draw(cornerBL, v.x + d - marginL, v.y - marginD);
		sb.draw(cornerBR, v.x + d + innerWidth, v.y - marginD);
		sb.draw(spike, x1, y1);
		
//		String msg = msgSrc.substring(0, shown);
		// draw text centered in textbox
		if(expanded){
			float w;
			for(int i = 0; i<text.length; i++){
				w = (maxWidth - (font[0].getRegionWidth()+1) * text[i].length()) / 2f;
				main.drawString(sb, font, font[0].getRegionWidth(), text[i], 
						v.x + w + d , v.y + maxHeight - (i + 1) * font[0].getRegionHeight());
			}
		}
		
		if(bool)
			sb.setOverlayDraw(true);
	}
	
	public void expand(){
		sizingState = EXPANDING;
	}
	
	public void kill(){
		sizingState = COLLAPSING; 
		expanded = false;
		if (!selfKill && owner instanceof Mob)
			((Mob)owner).resetState();
	}
	
	public void add(Entity t){
		contacts.add(t);
	}
	
	public void remove(Entity t){
		contacts.removeValue(t, false);
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
		body.createFixture(fdef).setUserData(ID);
	}
}
