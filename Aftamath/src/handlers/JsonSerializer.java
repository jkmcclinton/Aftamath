package handlers;

import java.io.PrintWriter;

import scenes.Scene;

import com.badlogic.gdx.utils.Json;

import entities.Entity;

public class JsonSerializer {

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
		writer.writeObjectEnd();
		fout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void loadGameState(String filename) {
		
	}
}
