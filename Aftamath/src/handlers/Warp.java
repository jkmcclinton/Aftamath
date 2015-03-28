package handlers;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import main.Play;
import scenes.Scene;

import com.badlogic.gdx.physics.box2d.World;

import entities.Player;

public class Warp extends Entity{

	private Scene link;
	private int width;
	private int height;

	public Warp(String scene, World world, Play play, Player player, float x, float y, int w, int h) {
		Class<?> c;
		
		try {
			c = Class.forName(scene);
			Class<?> C = c.getSuperclass();

			while(C != null) {
				if (C.getName().toLowerCase().equals("scene"))
					break;
				C = C.getSuperclass();
			}
			
			Constructor<?> cr = c.getConstructor(World.class, Play.class, Player.class);
			link = (Scene) cr.newInstance(world, play, player);
			
			ID = "warp";
			this.x = x;
			this.y = y;
			this.width = w/Vars.OBJ_SCALE;
			this.height = w/Vars.OBJ_SCALE;
			
		} catch (ClassNotFoundException e1) {
			System.out.println("Class \"" + scene + "\" not found.");
		} catch (NoSuchMethodException e) {
			System.out.println("No compatible constructor found for \"" + scene);
		} catch (SecurityException e) {
			System.out.println("Security error while trying to construct \"" + scene);
		} catch (InstantiationException e) {
			System.out.println("Could not instantiate \"" + scene);
			//e.printStackTrace();
		} catch (IllegalAccessException e) {
			//e.printStackTrace();
		} catch (IllegalArgumentException e) {
			//e.printStackTrace();
		} catch (InvocationTargetException e) {
			//e.printStackTrace();
		}
	}

	public Scene getLink() { return link; }
	public void setLink(Scene link) { this.link = link; }
	public int getWidth() { return width; }
	public void setWidth(int width) { this.width = width; }
	public int getHeight() { return height; }
	public void setHeight(int height) { this.height = height; }

	public boolean conditionsMet() {
		return false;
	}
}
