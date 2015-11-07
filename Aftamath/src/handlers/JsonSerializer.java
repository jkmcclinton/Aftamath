package handlers;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import main.Main;
import scenes.Scene;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import entities.Entity;

public class JsonSerializer {

	public static Main gMain;
	
	public static void saveGameState(String filename) {
		try {
			PrintWriter fout = new PrintWriter(filename);
			Json writer = new Json();
			writer.setWriter(fout);
			writer.writeObjectStart();
			//writer.writeObjectStart("sceneToEntityIdsObj");
			writer.writeValue("sceneToEntityIds", Scene.sceneToEntityIds);
			//writer.writeObjectEnd();
			//writer.writeObjectStart("idToEntityObj");
			writer.writeValue("idToEntity", Entity.idToEntity);
			//writer.writeObjectEnd();
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

			// create the associative arrays through iteration
			for (JsonValue child = root.get("sceneToEntityIds").child(); child != null; child = child.next()) {
				Set<Integer> entityIds = new HashSet<Integer>();
				for (JsonValue child2 = child.child(); child2 != null; child2 = child2.next()) {
					entityIds.add(child2.getInt("value"));
				}
				Scene.sceneToEntityIds.put(child.name(), entityIds);
			}
			//System.out.println(Scene.sceneToEntityIds);
			
			for (JsonValue child = root.get("idToEntity").child(); child != null; child = child.next()) {
				//System.out.println(child.name() + ": " + child.toString());
				Entity e = reader.fromJson(Entity.class, child.toString());
				Entity.idToEntity.put(Integer.parseInt(child.name()), e);
			}
			
			gMain.player = reader.fromJson(gMain.player.getClass(), root.getString("playerData"));
			gMain.history = reader.fromJson(gMain.history.getClass(), root.getString("history"));			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
