package scenes;

import static handlers.Vars.PPM;
import handlers.Entity;
import handlers.Vars;

import java.lang.reflect.Field;
import java.util.ArrayList;

import main.Game;
import main.Play;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;

import entities.Ground;
import entities.Mob;
import entities.NPC;
import entities.Player;

public abstract class Scene {

	private Scene previous;
	private Scene next;
	public Script script;
	public int width;
	public int height;
	public String title;
	public Music DEFAULT_SONG;
	
	private OrthogonalTiledMapRenderer tmr, tmr2;
	
	protected Vector2 spawnPoint;
	protected ArrayList<Entity> entities;
	protected ArrayList<PointLight> lights;
	protected TiledMap tileMap;
	protected Texture background;
	protected Texture foreground;
	protected World world;
	protected RayHandler rayHandler;
	protected static Vector2 gravity;
	protected static BodyDef bdef = new BodyDef();
	protected static FixtureDef fdef = new FixtureDef();
	
	protected Scene(World world, Scene previous, Scene next, String ID, Play p, Player player, int w, int h) {
		this.world = world;
		this.previous = previous;
		this.next = next;
		
		entities = new ArrayList<>();
		lights = new ArrayList<>();
		title = ID;
		script = new Script(ID, p, player, null);
		width = w;
		height = h;
		
		tileMap = new TmxMapLoader().load("res/maps/street.tmx");
		//tileMap = new TmxMapLoader().load("res/maps/" + title + ".tmx");
		tmr = new OrthogonalTiledMapRenderer(tileMap, p.getSpriteBatch());
//		background = new Texture(Gdx.files.internal("res/images/scenes/" + ID + "bg.png"));
		background = new Texture(Gdx.files.internal("res/images/scenes/BGtest.png"));
		//foreground = new Texture(Gdx.files.internal(fg));
	}
	
	public void renderBG(SpriteBatch sb){
		sb.disableBlending();
		sb.begin();
			sb.draw(background, 0, 0);
		sb.end();
		sb.enableBlending();
	}
	
	public void renderEnvironment(OrthographicCamera cam){
		tmr.setView(cam);
		tmr.render();
	}
	
	public void renderFG(SpriteBatch sb, OrthographicCamera cam){
		sb.begin();
			sb.draw(foreground, 0, 0);
		sb.end();
		
		tmr.setView(cam);
		tmr.render();
	}
	
	public void renderFG(OrthographicCamera cam){
		tmr2.setView(cam);
		tmr2.render();
	}
	
	
	public ArrayList<Entity> getInitEntities() { return entities; }
	
	public Scene getPrev() { return previous; }
	public Scene getNext() { return next; }
	//public void setPrev(Scene p) { previous = p;}
	//public void setNext(Scene n) { next = n; }
	
	public Vector2 getGravity() { return gravity; }
	
	public TiledMap getTileMap(){
		return tileMap;
	}

	@SuppressWarnings("unused")
	public void create(Play p) {
		Ground g;
		TiledMapTileLayer layer = (TiledMapTileLayer) tileMap.getLayers().get("ground");
		
		for (int y = 0; y < layer.getHeight(); y++)
			for(int x = 0; x < layer.getWidth(); x++){
				Cell cell = layer.getCell(x, y);
				
				if (cell == null) continue;
				if (cell.getTile() == null) continue;

				g = new Ground(world, "ground", (x + .5f) * Ground.TILE_SIZE / PPM,
						(y + .5f) * Ground.TILE_SIZE / PPM);
			}
		
		//lights
		int distance = 10000;
		Color color = Color.YELLOW;
		lights.add(new PointLight(rayHandler, Vars.LIGHT_RAYS, color, distance, 0, 0 ));
		
		
		MapObjects objects = tileMap.getLayers().get("objects").getObjects();
		for(MapObject object : objects) {
		    if (object instanceof RectangleMapObject) {
		        Rectangle rect = ((RectangleMapObject) object).getRectangle();
		        
		        Object o = object.getProperties().get("NPC");
		        if(o!=null) {
		        	String l = object.getProperties().get("Layer", String.class);
		        	String ID = object.getProperties().get("NPC", String.class);
		        	String sceneID = object.getProperties().get("ID", String.class);
		        	String name = object.getProperties().get("Name", String.class);
		        	String state = object.getProperties().get("State", String.class);
		        	String script = object.getProperties().get("Script", String.class);
		        	
		        	if (l.toLowerCase().equals("back")) l = "3";
		        	else if(l.toLowerCase().equals("middle")) l = "2";
		        	else l = "1";
		        	
		        	try {
						Field f = NPC.class.getField(state);
						int z = f.getInt(f);
						f = Vars.class.getField("BIT_LAYER"+Integer.parseInt(l));
						short lyr = f.getShort(f);
						ID = ID.toLowerCase();
						
						if (sceneID==null)
							System.out.println("NPC: "+ID+", name: "+name+" not given sceneID");
						else{
							NPC e = new NPC(name, ID, Integer.parseInt(sceneID), rect.x, rect.y+20, lyr);
							e.setScript(script);
							e.setState(z);
							entities.add(e);
						}
		        	} catch (NoSuchFieldException | SecurityException e) {
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}
		        }
		        
		        o = object.getProperties().get("warp");
		        if(o!=null);
		    }
		}
		
		//left wall
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(10 / PPM, Game.height / 2 / PPM);
		bdef.position.set(0, 3 * Game.height / 4 / PPM);
		Body body = world.createBody(bdef);
		fdef.shape = shape;
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3 | Vars.BIT_PROJECTILE;
		body.createFixture(fdef).setUserData("wall");

		//right wall
		bdef.position.set(Game.width / PPM, 3 * Game.height / 4 / PPM);
		body = world.createBody(bdef);
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3 | Vars.BIT_PROJECTILE;
		body.createFixture(fdef).setUserData("wall");
		
		layer = (TiledMapTileLayer) tileMap.getLayers().get("foreground");
		
		for (int y = 0; y < layer.getHeight(); y++)
			for(int x = 0; x < layer.getWidth(); x++){
				Cell cell = layer.getCell(x, y);
				
				if (cell == null) continue;
				if (cell.getTile() == null) continue;
			}
	}

	public String getType() {
		return null;
	}

	public void setRayHandler(RayHandler rayHandler) {
		this.rayHandler = rayHandler;
	}
}
