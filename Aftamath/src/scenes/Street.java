package scenes;

import handlers.Vars;
import main.Play;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;

import entities.Player;

public class Street extends Scene {
	
	public Street(World world, Play play, Player player){
		super(world, null, null, "street", play, player, 965, 508);
		DEFAULT_SONG = Gdx.audio.newMusic(new FileHandle("res/music/Bright Days.wav"));
		DEFAULT_SONG.setLooping(true);
		gravity = new Vector2 (0, Vars.GRAVITY);
		
//		NPC guy = new Gangster("Darrius", 1, (int) (Game.width/2 + 56), 400, Vars.BIT_LAYER3);
//		guy.setDefaultState(NPC.IDLEWALK);
//		entities.add(guy);
		
//		SuperNPC grim = new Reaper(550, 125, Vars.BIT_LAYER3);
		//grim.setDefaultState(NPC.IDLEWALK);
//		grim.setScript("speechless");
//		entities.add(grim);
		
//		OldLady elder1 = new OldLady("Miss Pancakes", 300, 127, Vars.BIT_LAYER3);
//		entities.add(elder1);
		
//		guy = new RichGuy("Olivierre", (Game.width/2 - 100), 400, Vars.BIT_LAYER3);
//		guy.setDefaultState(NPC.FACEPLAYER);
//		guy.setScript("script1");
//		entities.add(guy);
		
		for (int i = 0; i < entities.size(); i++ )
			entities.get(i).setSceneID(i + 1);
	}
}
