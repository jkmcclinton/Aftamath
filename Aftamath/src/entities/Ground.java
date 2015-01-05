package entities;

import static handlers.Vars.PPM;
import handlers.Entity;
import handlers.Vars;
import states.Play;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.World;

public class Ground extends Entity{
	
	public static final int TILE_SIZE = 16;

	public Ground(World world, String ID, float x, float y){
		super(x, y - 4 / PPM, TILE_SIZE, TILE_SIZE, ID);
		this.world = world;
		create();
	}
	
	public void create(){
		bdef.type = BodyType.StaticBody;
		bdef.position.set(x, y);
		
		ChainShape cs = new ChainShape();
		Vector2[] v = new Vector2[4];
		
		v[0] = new Vector2(-TILE_SIZE / 2 / PPM, -TILE_SIZE / 2 / PPM);
		v[1] = new Vector2(-TILE_SIZE / 2 / PPM, TILE_SIZE / 2 / PPM);
		v[2] = new Vector2(TILE_SIZE / 2 / PPM, TILE_SIZE / 2 / PPM);
		v[3] = new Vector2(TILE_SIZE / 2 / PPM, -TILE_SIZE / 2 / PPM);
		cs.createChain(v);
		
		fdef.friction = .25f;
		fdef.shape = cs;
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3 | Vars.BIT_PROJECTILE;
		fdef.isSensor = false;
		body = world.createBody(bdef);
		body.createFixture(fdef).setUserData("ground");
	}
}
