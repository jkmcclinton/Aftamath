package scenes;

import handlers.Vars;
import main.Play;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import entities.Player;

public class Street extends Scene {
	
	public Street(World world, Play play, Player player){
		super(world, "street", play, player);
		gravity = new Vector2 (0, Vars.GRAVITY);
		
//		NPC guy = new Gangster("Darrius", 1, (int) (Game.width/2 + 56), 400, Vars.BIT_LAYER3);
//		guy.setDefaultState(NPC.IDLEWALK);
//		entities.add(guy);
		
//		SuperNPC grim = new Reaper(550, 125, Vars.BIT_LAYER3);
		//grim.setDefaultState(NPC.IDLEWALK);
//		grim.setScript("speechless");
//		entities.add(grim);
	}
}
