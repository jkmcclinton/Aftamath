package main;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

public class AftamathDesktop {

public static void main(String[] args) {
		
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();
		cfg.title = Game.TITLE;
		cfg.width = Game.width * Game.scale;
		cfg.height = Game.height * Game.scale;
//		cfg.resizable = false;
		cfg.vSyncEnabled = true;
		//cfg.useGL20 = true;
		//cfg.fullscreen = true;
		
		new LwjglApplication(new Game(), cfg);
		
	}
}
