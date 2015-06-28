package entities;

import static handlers.Vars.PPM;

import com.badlogic.gdx.math.Vector2;

public class Bird extends NPC {

	public Bird(String name, int ID, float x, float y, short layer) {
		super(name, "bird" + ID,0, x, y, layer);

		setState(AIState.IDLEWALK);
	}

	public void act() {
		float dx/*, dy*/;

		switch (state){
		case IDLEWALK:
			if(reached) idleTime++;
			if(idleTime >= idleWait && reached) reached = false;
			else if (!reached){
				if (!canMove()) {
					goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
					idleWait = (float)(Math.random() *(IDLE_LIMIT) + 100);
					idleTime = 0;
					reached = true;
				}
				else {
					dx = (goalPosition.x - body.getPosition().x) * PPM ;
					if(dx < 1 && dx > -1){
						goalPosition = new Vector2((float) (((Math.random() * 6)+x)/PPM), y);
						idleWait = (float)(Math.random() *(IDLE_LIMIT) + 100);
						idleTime = 0;
						reached = true;
					} else {
						if(dx < 1) left();
						if(dx > -1) right();
					}
				}
			}
			break;
		default:
			break;
		}
	}
}