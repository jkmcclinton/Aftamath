package main;

import com.badlogic.gdx.Files;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
/*
 * Name: AftamathDesktop.java
 * Imports: None
 * Use: Starting the game, sets the game width and height, run this code to start the game
 */
public class AftamathDesktop {

public static void main(String[] args) {
		LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration(); //Configuration file in LibGDX used to start the game
		cfg.title = Game.TITLE; //Sets the title of the game
		cfg.width = Game.width * Game.scale; //Sets the width of the game screen
		cfg.height = Game.height * Game.scale; //Sets the height of the game screen
//		cfg.resizable = false; // Allows the game screen to be resizable
		cfg.vSyncEnabled = true; // vSync makes frames load better
//		cfg.useGL20 = true;  
//		cfg.fullscreen = true; // Forces the game screen into fullscreen
      cfg.addIcon("assets/images/Icon.png", Files.FileType.Internal); 
		
		new LwjglApplication(new Game(), cfg); //Creates and launches the game file using the previous settings
		
	}
}
