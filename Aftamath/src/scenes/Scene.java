package scenes;

import static handlers.Vars.PPM;
import handlers.FadingSpriteBatch;
import handlers.Vars;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import main.Play;
import box2dLight.PointLight;
import box2dLight.RayHandler;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.MapProperties;
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
import com.badlogic.gdx.utils.Array;

import entities.Entity;
import entities.Ground;
import entities.NPC;
import entities.Player;
import entities.Warp;

public abstract class Scene {
	
	public Script script;
	public int width;
	public int height;
	public String title;
	public Music DEFAULT_SONG;
	public boolean newSong; //tells us whether or not to fade music when loading scene from previous scene
	
	protected Color ambient;
	protected Play play;
	protected FadingSpriteBatch sb;
	protected OrthogonalTiledMapRenderer tmr;
	protected OrthogonalTiledMapRenderer tmr2;
	protected Vector2 spawnpoint, localSpawnpoint;
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
	
	public static final Array<String> BGM = new Array<String>();
	static{
		BGM.add("Silence");
		BGM.add("Bright Days");			//1
		BGM.add("Elevator Music");		//2
		BGM.add("Excitement");			//3
		BGM.add("Explosive");			//4
		BGM.add("Grand");				//5
		BGM.add("Incredible");			//6
		BGM.add("Relax");				//7
		BGM.add("Suspense");			//8
		BGM.add("Title");				//9
		BGM.add("Yesterday");
		BGM.add("Jamming");
		BGM.add("Incinerate");
		BGM.add("Justice");
		BGM.add("Midnight");
		BGM.add("Helpful");
	}
	
	protected Scene(){}
	
	protected Scene(World world, String ID, Play p, Player player) {
		this.world = world;
		this.play = p;
		sb = p.getSpriteBatch();
		
		entities = new ArrayList<>();
		lights = new ArrayList<>();
		width = 1000;
		height = 1000;
		title = ID;
		
		tileMap = new TmxMapLoader().load("res/maps/" + title + ".tmx");
		try {
			MapProperties prop = tileMap.getProperties();
			width = prop.get("width", Integer.class)*Vars.TILE_SIZE;
			height = prop.get("height", Integer.class)*Vars.TILE_SIZE;

			String bgm = "";
			if((bgm = prop.get("bgm", String.class))!=null){
				DEFAULT_SONG = Gdx.audio.newMusic(new FileHandle("res/music/"+
						BGM.get(Integer.parseInt(bgm))+".wav"));
				DEFAULT_SONG.setLooping(true);
				newSong = true;
			}
			
			String light= "";
			if((light=prop.get("ambient", String.class))!=null){
				if(light.equals("daynight".toUpperCase())){
//					outside = true;
					ambient = p.getAmbient();
				}else{
					Field f = Vars.class.getField(light);
					ambient = (Color) f.get(f);
				}
			}
		} catch (Exception e ){
			DEFAULT_SONG = Gdx.audio.newMusic(new FileHandle("res/music/"+
					BGM.get(0)+".wav"));
			ambient = Vars.DAY;
			newSong = true;
		} finally{
			sb.setAmbient(ambient);
		}
		script = new Script(ID, p, player, null);
		
		tmr = new OrthogonalTiledMapRenderer(tileMap, p.getSpriteBatch());
//		background = new Texture(Gdx.files.internal("res/images/scenes/" + ID + "bg.png"));
		background = new Texture(Gdx.files.internal("res/images/scenes/BGtest.png"));
		//foreground = new Texture(Gdx.files.internal(fg));
	}
	
	public void renderBG(SpriteBatch sb){
		sb.disableBlending();
		sb.begin();
		if(background!=null)
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
	public Vector2 getGravity() { return gravity; }
	public TiledMap getTileMap(){ return tileMap; }
	public Vector2 getSpawnpoint(){ return spawnpoint; }
	public void setSpawnpoint(Vector2 newSpawn){ spawnpoint = newSpawn; }
	public Vector2 getLocalSpawnpoint(){ return localSpawnpoint; }

	public void create() {
		TiledMapTileLayer ground = (TiledMapTileLayer) tileMap.getLayers().get("ground");
//		TiledMapTileLayer fg = (TiledMapTileLayer) tileMap.getLayers().get("fg");
//		TiledMapTileLayer bg1 = (TiledMapTileLayer) tileMap.getLayers().get("bg1");
//		TiledMapTileLayer bg2 = (TiledMapTileLayer) tileMap.getLayers().get("bg2");
		
		for (int y = 0; y < ground.getHeight(); y++)
			for(int x = 0; x < ground.getWidth(); x++){
				Cell cell = ground.getCell(x, y);
//				Vector2 location = new Vector2((x+.5f) * Vars.TILE_SIZE / Vars.PPM,
//						y * Vars.TILE_SIZE / Vars.PPM);
				if (cell == null) continue;
				if (cell.getTile() == null) continue;

				new Ground(world, "ground", (x * Vars.TILE_SIZE +7.1f) / PPM,
						(y * Vars.TILE_SIZE+Vars.TILE_SIZE/1.8f) / PPM);
				
//				cell = fg.getCell(x, y);
//				if (cell != null) {
//					if (cell.getTile() != null) {
//						Object b = cell.getTile().getProperties().get("sound");
//						if(b!=null){
//							String src = cell.getTile().getProperties().get("sound", String.class);
//							if(src!=null) new PositionalAudio(new Vector2(location.x, location.y), src, p);
//						}
//					}
//				}
//				
//				cell = bg1.getCell(x, y);
//				if (cell != null) {
//					if (cell.getTile() != null) {
//						Object b = cell.getTile().getProperties().get("sound");
//						if(b!=null){
//							String src = cell.getTile().getProperties().get("sound", String.class);
//							if(src!=null) new PositionalAudio(new Vector2(location.x, location.y), src, p);
//						}
//					}
//				}
			}
		
		//lights
		int distance = 10000;
		Color color = Color.YELLOW;
		lights.add(new PointLight(rayHandler, Vars.LIGHT_RAYS, color, distance, 0, 0 ));
		
		MapObjects objects = tileMap.getLayers().get("objects").getObjects();
		for(MapObject object : objects) {
		    if (object instanceof RectangleMapObject) {
		        Rectangle rect = ((RectangleMapObject) object).getRectangle();
		        	if(object.getProperties().get("warp")!=null)System.out.println(new Vector2(rect.x,rect.y));
		        rect.set(rect.x+7.1f, rect.y-Vars.TILE_SIZE*(9f/80f), rect.width, rect.height);
//		        System.out.println(new Vector2(rect.x, rect.y));
		        
		        entities.add(new Entity(rect.x,rect.y,(int)rect.width,(int)rect.height,"TiledObject"));
				
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
							NPC e = new NPC(name, ID, Integer.parseInt(sceneID), rect.x, rect.y, lyr);
							e.setScript(script);
							e.setDefaultState(z);
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
		        if(o!=null) {
		        	String next = object.getProperties().get("next", String.class);
		        	String nextWarp = object.getProperties().get("nextWarp", String.class);
		        	String id = object.getProperties().get("ID", String.class);
		        	int warpID = Integer.parseInt(id);
		        	int nextID = Integer.parseInt(nextWarp);
		        	entities.add(new Warp(this, next, warpID, nextID, rect.x, 
		        			rect.y+rect.height/2, rect.width, rect.height));
		        }

		        o = object.getProperties().get("spawn");
		        if(o!=null) {
		        	spawnpoint = new Vector2(rect.x, rect.y);
		        	localSpawnpoint = new Vector2(rect.x, rect.y);
		        }
		    }
		}
		
		//left wall
		PolygonShape shape = new PolygonShape();
		shape.setAsBox(Vars.TILE_SIZE / PPM, height / PPM);
		bdef.position.set(0, 0);
		Body body = world.createBody(bdef);
		fdef.shape = shape;
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3 | Vars.BIT_PROJECTILE;
		body.createFixture(fdef).setUserData("wall");

		//right wall
		bdef.position.set((width) / PPM, 0);
		body = world.createBody(bdef);
		fdef.filter.categoryBits = Vars.BIT_GROUND;
		fdef.filter.maskBits = Vars.BIT_LAYER1 | Vars.BIT_LAYER2 | Vars.BIT_LAYER3 | Vars.BIT_PROJECTILE;
		body.createFixture(fdef).setUserData("wall");
		
	}

	public String getType() {
		return null;
	}

	public void setRayHandler(RayHandler rh) {
		sb.setRayHandler(rh);
		this.rayHandler = rh;
	}
	
	public Scene levelFromID(String ID, Warp warp, int warpID){
		if (ID.startsWith("room")){
			Scene s = new Room(world, play, ID, this);

			if(warp!=null)
				if(!warp.owner.newSong) s.newSong = false;
			
			Vector2 nextWarp = s.findWarpLoc(warpID);
			s.setRayHandler(rayHandler);
			if(nextWarp!=null) warp.setLink(nextWarp);
			else warp.setLink(s.spawnpoint);
			
			return s;
		} else {
			//for places like: Street, Underground, Outskirts, House
			Class<?> c;
			try {
				ID = ID.substring(0,1).toUpperCase() + ID.substring(1);
				c = Class.forName("scenes."+ID);
				Constructor<?> cr = c.getConstructor(World.class, Play.class, Player.class);
				Object o = cr.newInstance(world, play, play.player);
				if(warp!=null)
					if(!warp.owner.newSong) ((Scene) o).newSong = false;
				
				Vector2 nextWarp = ((Scene)o).findWarpLoc(warpID);
				((Scene)o).setRayHandler(rayHandler);
				if(nextWarp!=null) warp.setLink(nextWarp);
				else warp.setLink(((Scene)o).spawnpoint);
					
				return (Scene) o;

			} catch (NoSuchMethodException | SecurityException e) {
				System.out.println("no such constructor");
			} catch (InstantiationException e) {
				System.out.println("cannot instantiate object");
			} catch (IllegalAccessException e) {
				System.out.println("cannot access object");
				e.printStackTrace();
			} catch (IllegalArgumentException e) {
				System.out.println("illegal argument");
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.out.println("cannot invoke target");
				e.printStackTrace();
			} catch (ClassNotFoundException e1) {
				System.out.println("Class \"" + ID + "\" not found.");
			}

			return null;
		}
	}
	
	public Vector2 findWarpLoc(int warpID){
		MapObjects objects = tileMap.getLayers().get("objects").getObjects();
		for(MapObject object : objects) {
			if (object instanceof RectangleMapObject) {
				Rectangle rect = ((RectangleMapObject) object).getRectangle();
		        rect.set(rect.x+7.1f*2, rect.y-Vars.TILE_SIZE*(9f/80f), rect.width, rect.height);

				Object o = object.getProperties().get("warp");
				if(o!=null) {
		        	String id = object.getProperties().get("ID", String.class);
		        	int warp = Integer.parseInt(id);
					if(warp==warpID)
			        	return new Vector2(rect.x, rect.y-rect.height/2+10);
				}
			}
		}
		return null;
	}
}
