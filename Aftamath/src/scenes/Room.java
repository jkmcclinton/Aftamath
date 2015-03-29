package scenes;

import java.lang.reflect.Field;
import java.util.ArrayList;

import handlers.Camera;
import handlers.Vars;
import main.Play;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

public class Room extends Scene {

	private Scene owner;

	public Room(World world, Play p, String ID, Scene owner) {
		this.world = world;
		gravity = new Vector2 (0, Vars.GRAVITY);
		this.play = p;
		this.title = ID;
		this.owner = owner;
		sb = p.getSpriteBatch();

		entities = new ArrayList<>();
		lights = new ArrayList<>();

		tileMap = new TmxMapLoader().load("res/maps/"+ID+".tmx");
		tmr = new OrthogonalTiledMapRenderer(tileMap, p.getSpriteBatch());
		MapProperties prop = tileMap.getProperties();
		width = prop.get("width", Integer.class);
		height = prop.get("height", Integer.class);

		try{
			String bgm ="";
			if((bgm =prop.get("bgm", String.class))!=null){
				bgm = BGM.get(Integer.parseInt(bgm));
				if (!bgm.equals(owner.DEFAULT_SONG)){
					DEFAULT_SONG = Gdx.audio.newMusic(new FileHandle("res/music/"+
							bgm+".wav"));
					DEFAULT_SONG.setLooping(true);
					newSong = true;
				}
			}

			String light= "";
			if((light=prop.get("ambient", String.class))!=null){
				if(light.equals("daynight".toUpperCase())){
					//				outside = true;
					ambient = p.getAmbient();
				}else{
					Field f = Vars.class.getField(light);
					ambient = (Color) f.get(f);
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public void renderBG(SpriteBatch sb, Camera cam, Camera hudCam) {

	}

	public Scene getOwner(){return owner;}
}
