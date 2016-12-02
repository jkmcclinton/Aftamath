package scenes;

import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Vector2;

import entities.Path;
import main.Main;

public class DynamicTile {
	
	private Cell cell;
//	private Main main;
	private Path path;
	private boolean moving;
	private TiledMapTile tile;
	private Vector2 goalPosition;
	
	DynamicTile(Cell cell, Main main){
		this.cell = cell;
//		this.main = main;
		tile = this.cell.getTile();
	}
	
	public void update(float dt){
		if(moving){
			if(path!=null)
				goalPosition = path.getCurrent();
			
			if(goalPosition!=null) {
				//offset Tile image according to speed?
				tile.setOffsetX(tile.getOffsetX() + dt);
				
				//finished moving?
				if(true){
					if(path!=null){
						path.stepIndex();
						if(path.completed){
							moving = false;
							goalPosition = null;
							path = null;
						}
					} else {
						moving = false;
						goalPosition = null;
					}
				}
			}
		}
	}
	
	public void move(Path path){
		if(path ==null)return;
		moving = true;
		this.path = path;
	}
	
	public void move(Vector2 goal){
		if(goal==null) return;
		moving = true;
		this.goalPosition = goal;
	}

}
