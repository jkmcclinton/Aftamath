package handlers;

import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.QueryCallback;
import com.badlogic.gdx.utils.Array;

import entities.Entity;

public class MyQueryCallback implements QueryCallback {
    private Array<Entity> foundEntities;
    
    public MyQueryCallback (){
    	foundEntities = new Array<>();
    }
    
    public boolean reportFixture(Fixture fixture) {
    	if(fixture.getBody().getUserData() instanceof Entity)
    		if(((Entity) fixture.getBody().getUserData()).destructable){
    			foundEntities.add((Entity) fixture.getBody().getUserData()); 
    			return true;
    		}
    	
    	return false;
    }
   
    public Pair<Boolean, Entity> found(){
    	if(foundEntities.size>0){
    		return new Pair<>(true, foundEntities.first());
    	}

    	return new Pair<>(false, null);
    }
}
