package handlers;

import handlers.MyInput.Input;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;

public class MyInputProcessor extends InputAdapter {
	
	public boolean keyDown(int k) {
		if(k == Keys.W || k == Keys.UP) MyInput.setKey(Input.UP, true);
		if(k == Keys.A || k == Keys.LEFT) MyInput.setKey(Input.LEFT, true);
		if(k == Keys.S || k == Keys.DOWN) MyInput.setKey(Input.DOWN, true);
		if(k == Keys.D || k == Keys.RIGHT) MyInput.setKey(Input.RIGHT, true);
		if(k == Keys.SPACE) MyInput.setKey(Input.JUMP, true);
		if(k == Keys.Q || k == Keys.CONTROL_RIGHT) MyInput.setKey(Input.INTERACT, true);
		if(k == Keys.SHIFT_LEFT || k == Keys.NUMPAD_0) MyInput.setKey(Input.ATTACK, true);
		if(k == Keys.CONTROL_LEFT || k == Keys.Z) MyInput.setKey(Input.RUN, true);
		if(k == Keys.R || k == Keys.NUMPAD_1) MyInput.setKey(Input.USE, true);
		if(k == Keys.ESCAPE) MyInput.setKey(Input.PAUSE, true);
		if(k == Keys.ENTER) MyInput.setKey(Input.ENTER, true);
		if(k == Keys.TAB) MyInput.setKey(Input.DEBUG, true);
		if(k == Keys.NUMPAD_8) MyInput.setKey(Input.DEBUG_UP, true);
		if(k == Keys.NUMPAD_2) MyInput.setKey(Input.DEBUG_DOWN, true);
		if(k == Keys.NUMPAD_4) MyInput.setKey(Input.DEBUG_LEFT, true);
		if(k == Keys.NUMPAD_6) MyInput.setKey(Input.DEBUG_RIGHT, true);
		if(k == Keys.NUMPAD_7) MyInput.setKey(Input.DEBUG_LEFT2, true);
		if(k == Keys.NUMPAD_9) MyInput.setKey(Input.DEBUG_RIGHT2, true);
		if(k == Keys.NUMPAD_5) MyInput.setKey(Input.DEBUG_CENTER, true);
		if(k == Keys.F1) MyInput.setKey(Input.COLLISION, true);
		if(k == Keys.F2) MyInput.setKey(Input.LIGHTS, true);
		if(k == Keys.F3) MyInput.setKey(Input.DEBUG_TEXT, true);
		if(k == Keys.F4) MyInput.setKey(Input.DEBUG2, true);
		if(k == Keys.PLUS) MyInput.setKey(Input.ZOOM_IN, true);
		if(k == Keys.MINUS) MyInput.setKey(Input.ZOOM_OUT, true);
		return true; 
	}
	
	public boolean keyUp(int k) {
		if(k == Keys.W || k == Keys.UP) MyInput.setKey(Input.UP, false);
		if(k == Keys.A || k == Keys.LEFT) MyInput.setKey(Input.LEFT, false);
		if(k == Keys.S || k == Keys.DOWN) MyInput.setKey(Input.DOWN, false);
		if(k == Keys.D || k == Keys.RIGHT) MyInput.setKey(Input.RIGHT, false);
		if(k == Keys.SPACE) MyInput.setKey(Input.JUMP, false);
		if(k == Keys.Q || k == Keys.CONTROL_RIGHT) MyInput.setKey(Input.INTERACT, false);
		if(k == Keys.SHIFT_LEFT || k == Keys.NUMPAD_0) MyInput.setKey(Input.ATTACK, false);
		if(k == Keys.CONTROL_LEFT || k == Keys.Z) MyInput.setKey(Input.RUN, false);
		if(k == Keys.R || k == Keys.NUMPAD_1) MyInput.setKey(Input.USE, false);
		if(k == Keys.ESCAPE) MyInput.setKey(Input.PAUSE, false);
		if(k == Keys.ENTER) MyInput.setKey(Input.ENTER, false);
		if(k == Keys.TAB) MyInput.setKey(Input.DEBUG, false);
		if(k == Keys.NUMPAD_8) MyInput.setKey(Input.DEBUG_UP, false);
		if(k == Keys.NUMPAD_2) MyInput.setKey(Input.DEBUG_DOWN, false);
		if(k == Keys.NUMPAD_4) MyInput.setKey(Input.DEBUG_LEFT, false);
		if(k == Keys.NUMPAD_6) MyInput.setKey(Input.DEBUG_RIGHT, false);
		if(k == Keys.NUMPAD_7) MyInput.setKey(Input.DEBUG_LEFT2, false);
		if(k == Keys.NUMPAD_9) MyInput.setKey(Input.DEBUG_RIGHT2, false);
		if(k == Keys.NUMPAD_5) MyInput.setKey(Input.DEBUG_CENTER, false);
		if(k == Keys.F1) MyInput.setKey(Input.COLLISION, false);
		if(k == Keys.F2) MyInput.setKey(Input.LIGHTS, false);
		if(k == Keys.F3) MyInput.setKey(Input.DEBUG_TEXT, false);
		if(k == Keys.F4) MyInput.setKey(Input.DEBUG2, false);
		if(k == Keys.PLUS) MyInput.setKey(Input.ZOOM_IN, false);
		if(k == Keys.MINUS) MyInput.setKey(Input.ZOOM_OUT, false);
		
		return true;
	}

}
