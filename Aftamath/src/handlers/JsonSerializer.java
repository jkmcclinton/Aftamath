package handlers;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import main.Main;
import main.Player;
import main.History;
import scenes.Scene;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import entities.Entity;
import entities.Mob;

public class JsonSerializer {

	public static Main gMain;
	
	// The point of these reference lists/methods is to only initialize references
	// after the objects have been created - otherwise the references may not exist
	private static List<MobRef> mobRefLst;
	private static List<EntityRef> entityRefLst;
	private static List<PlayerRef> playerRefLst;	//this one is not actually necessary, using it for consistency
	
	static {
		mobRefLst = new LinkedList<MobRef>();
		entityRefLst = new LinkedList<EntityRef>();
		playerRefLst = new LinkedList<PlayerRef>();
	}
	
	public static void saveGameState(String filename) {
		try {
			PrintWriter fout = new PrintWriter(filename);
			Json writer = new Json();
			writer.setWriter(fout);
			writer.writeObjectStart();
			writer.writeValue("sceneToEntityIds", Scene.sceneToEntityIds);
			writer.writeValue("idToEntity", Entity.idToEntity);
			writer.writeValue("playerData", gMain.player);
			writer.writeValue("history", gMain.history);
			writer.writeObjectEnd();
			fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadGameState(String filename) {
		try {
			JsonValue root = new JsonReader().parse(new FileHandle(filename));
			Json reader = new Json();

			// build the associative arrays through iteration
			Scene.sceneToEntityIds.clear();
			for (JsonValue child = root.get("sceneToEntityIds").child(); child != null; child = child.next()) {
				Set<Integer> entityIds = new HashSet<Integer>();
				for (JsonValue child2 = child.child(); child2 != null; child2 = child2.next()) {
					entityIds.add(child2.getInt("value"));
				}
				Scene.sceneToEntityIds.put(child.name(), entityIds);
			}
			
			Entity.idToEntity.clear();
			for (JsonValue child = root.get("idToEntity").child(); child != null; child = child.next()) {
				Entity e = reader.fromJson(Entity.class, child.toString());
				Entity.idToEntity.put(Integer.parseInt(child.name()), e);
			}
			
			if (gMain != null) {
				gMain.player = reader.fromJson(Player.class, root.get("playerData").toString());
				gMain.player.setMainRef(gMain);
				gMain.history = reader.fromJson(History.class, root.get("history").toString());	
			} else {
				System.out.println("JsonSerializer has no reference to instance of Main");
			}
			initReferences();
		} catch (Exception e) {
			e.printStackTrace();
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
				ref.orig.setInteractable(Entity.idToEntity.get(ref.interactable));
			}
		}
		mobRefLst.clear();

		for (EntityRef ref : entityRefLst) {
			for (int i : ref.followers) {
				if (i == -1)
					continue;
				ref.orig.addFollower((Mob)Entity.idToEntity.get(i));
			}
		}
		entityRefLst.clear();
		
		for (PlayerRef ref : playerRefLst) {
			if (ref.partner > -1) {
				ref.orig.setPartner((Mob)Entity.idToEntity.get(ref.partner));
			}
		}
		playerRefLst.clear();
	}
	
	public static void pushMobRef(Mob orig, int attackFocus, int aiFocus, int interactable) {
		MobRef ref = new MobRef();
		ref.orig = orig;
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
}
