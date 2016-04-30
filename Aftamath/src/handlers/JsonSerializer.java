package handlers;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import entities.Entity;
import entities.Mob;
import entities.MobAI;
import main.GameState;
import main.History;
import main.Main;
import main.Player;
import scenes.Scene;

public class JsonSerializer {

	public static Main gMain;
	
	// The point of these reference lists/methods is to only initialize references
	// after the objects have been created - otherwise the references may not exist
	private static List<MobRef> mobRefLst;
	private static List<EntityRef> entityRefLst;
	private static List<PlayerRef> playerRefLst;	//this one is not actually necessary, using it for consistency
	private static List<MobAIRef> mobAiRefLst;
	
	static {
		mobRefLst = new LinkedList<MobRef>();
		entityRefLst = new LinkedList<EntityRef>();
		playerRefLst = new LinkedList<PlayerRef>();
		mobAiRefLst = new LinkedList<MobAIRef>();
	}
	
	public static void saveGameState(String filename) {
		try {
			PrintWriter fout = new PrintWriter("saves/" + filename + ".txt");
			Json writer = new Json();
			writer.setWriter(fout);
			writer.writeObjectStart();
			writer.writeValue("levelID", gMain.getScene().ID);
			writer.writeValue("sceneToEntityIds", Scene.getSceneToEntityMapping());
			writer.writeValue("idToEntity", Entity.getIDToEntityMapping());
			writer.writeValue("playerData", gMain.player);
			writer.writeValue("history", gMain.history);
			writer.writeValue("prevLoc", GameState.prevLoc);
			writer.writeValue("summary", gMain.character.ID + "base/l"+ gMain.character.getName()
			+"/l"+gMain.getScene()+"/l"+gMain.history.playTime);
			writer.writeObjectEnd();
			
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadGameState(String filename) {
		try {
			JsonValue root = new JsonReader().parse(new FileHandle("saves/"+filename+".txt"));
			Json reader = new Json();
			
			// build the associative arrays through iteration
			Scene.clearEntityMapping();
			for (JsonValue child = root.get("sceneToEntityIds").child(); child != null; child = child.next()) {
				for (JsonValue child2 = child.child(); child2 != null; child2 = child2.next())
					Scene.addEntityMapping(child.name(), child2.getInt("value"));
			}
			
			Entity.clearMapping();
			for (JsonValue child = root.get("idToEntity").child(); child != null; child = child.next()) {
				Entity e = reader.fromJson(Entity.class, child.toString());
				Entity.addMapping(Integer.parseInt(child.name()), e);
			}
			
			if (gMain != null) {
				gMain.player = reader.fromJson(Player.class, root.get("playerData").toString());
				gMain.player.setMainRef(gMain);
				gMain.history = reader.fromJson(History.class, root.get("history").toString());	
			} else {
				System.out.println("JsonSerializer has no reference to instance of Main");
			}
			initReferences();
			
			String level = root.getString("levelID");
			Scene scene = new Scene(gMain.getWorld(), gMain, level);
			gMain.setScene(scene);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// return save state of format
	/**
	 * find savegame file; if file at index exists, preload data and store it into a parseable string
	 * @param path loafus cramwell of slurmpville, how do I door?
	 * @return string of format "playerType/lname/llocation/lplayTime"
	 */
	public static String getSummary(FileHandle path){
		try{
			return new JsonReader().parse(path).getString("summary");
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}

	private static void initReferences() {
		for (MobRef ref : mobRefLst) {
			if (ref.attackFocus > -1) {
				//TODO: use fight() or timedFight()?
			}
			if (ref.aiFocus > -1) {
				//TODO: use doTimedAction()?
			}
			if (ref.interactable > -1) {
				ref.orig.setInteractable(Entity.getMapping(ref.interactable));
			}
		}
		mobRefLst.clear();

		for (EntityRef ref : entityRefLst) {
			for (int i : ref.followers) {
				if (i == -1)
					continue;
				ref.orig.addFollower((Mob)Entity.getMapping(i));
			}
		}
		entityRefLst.clear();
		
		for (PlayerRef ref : playerRefLst) {
			if (ref.partner > -1) {
				ref.orig.setPartner((Mob)Entity.getMapping(ref.partner));
			}
		}
		playerRefLst.clear();
		
		for (MobAIRef ref : mobAiRefLst) {
			if (ref.focus > -1) {
				ref.orig.focus = Entity.getMapping(ref.focus);
			}
		}
		mobAiRefLst.clear();
	}
	
	public static void pushMobRef(Mob mob2, int attackFocus, int aiFocus, int interactable) {
		MobRef ref = new MobRef();
		ref.orig = mob2;
		ref.attackFocus = attackFocus;
		ref.aiFocus = aiFocus;
		ref.interactable = interactable;
		mobRefLst.add(ref);
	}
	public static void pushEntityRef(Entity orig, Array<Integer> followers) {
		EntityRef ref = new EntityRef();
		ref.orig = orig;
		ref.followers = followers;
		entityRefLst.add(ref);
	}
	public static void pushPlayerRef(Player orig, int partner) {
		PlayerRef ref = new PlayerRef();
		ref.orig = orig;
		ref.partner = partner;
		playerRefLst.add(ref);
	}
	public static void pushMobAiRef(MobAI mobAi, int focus) {
		MobAIRef ref = new MobAIRef();
		ref.orig = mobAi;
		ref.focus = focus;
		mobAiRefLst.add(ref);
	}
	
	private static class MobRef {
		public Mob orig;
		public int attackFocus;
		public int aiFocus;
		public int interactable;
	}
	private static class EntityRef {
		public Entity orig;
		public Array<Integer> followers;
	}
	private static class PlayerRef {
		public Player orig;
		public int partner;
	}
	private static class MobAIRef {
		public MobAI orig;
		public int focus;
	}
}
