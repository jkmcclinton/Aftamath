package entities;

import box2dLight.PointLight;

public class Particle  {

	private PointLight pL;
	
	protected Particle(float x, float y, int w, int h, String ID) {
		// TODO Auto-generated constructor stub
	}
	
	public void kill(){
		if(pL!=null) pL.remove();
	}

}
