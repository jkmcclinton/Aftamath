package handlers;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;

public class MyInputProcessor extends InputAdapter {
	
	public boolean keyDown(int k) {
		if(k == Keys.W || k == Keys.UP) MyInput.setKey(MyInput.UP, true);
		if(k == Keys.A || k == Keys.LEFT) MyInput.setKey(MyInput.LEFT, true);
		if(k == Keys.S || k == Keys.DOWN) MyInput.setKey(MyInput.DOWN, true);
		if(k == Keys.D || k == Keys.RIGHT) MyInput.setKey(MyInput.RIGHT, true);
		if(k == Keys.SPACE) MyInput.setKey(MyInput.JUMP, true);
		if(k == Keys.Q || k == Keys.CONTROL_RIGHT) MyInput.setKey(MyInput.INTERACT, true);
//		if(k == Keys.SHIFT_LEFT || k == Keys.NUMPAD_0) MyInput.setKey(MyInput.ATTACK, true);
		if(k == Keys.R || k == Keys.NUMPAD_1) MyInput.setKey(MyInput.USE, true);
		if(k == Keys.ESCAPE) MyInput.setKey(MyInput.PAUSE, true);
		if(k == Keys.ENTER) MyInput.setKey(MyInput.ENTER, true);
		if(k == Keys.NUMPAD_8) MyInput.setKey(MyInput.DEBUG_UP, true);
		if(k == Keys.NUMPAD_2) MyInput.setKey(MyInput.DEBUG_DOWN, true);
		if(k == Keys.NUMPAD_4) MyInput.setKey(MyInput.DEBUG_LEFT, true);
		if(k == Keys.NUMPAD_6) MyInput.setKey(MyInput.DEBUG_RIGHT, true);
		if(k == Keys.L) MyInput.setKey(MyInput.DEBUG, true);
		if(k == Keys.P) MyInput.setKey(MyInput.DEBUG1, true);
		if(k == Keys.NUMPAD_7) MyInput.setKey(MyInput.DEBUG_LEFT2, true);
		if(k == Keys.NUMPAD_9) MyInput.setKey(MyInput.DEBUG_RIGHT2, true);
		if(k == Keys.NUMPAD_5) MyInput.setKey(MyInput.DEBUG_CENTER, true);
		if(k == Keys.L) MyInput.setKey(MyInput.DEBUG, true);
		if(k == Keys.P) MyInput.setKey(MyInput.DEBUG1, true);
		if(k == Keys.TAB) MyInput.setKey(MyInput.DEBUG2, true);
		if(k == Keys.F3) MyInput.setKey(MyInput.DEBUG3, true);
		return true; 
	}
	
	public boolean keyUp(int k) {
		if(k == Keys.W || k == Keys.UP) MyInput.setKey(MyInput.UP, false);
		if(k == Keys.A || k == Keys.LEFT) MyInput.setKey(MyInput.LEFT, false);
		if(k == Keys.S || k == Keys.DOWN) MyInput.setKey(MyInput.DOWN, false);
		if(k == Keys.D || k == Keys.RIGHT) MyInput.setKey(MyInput.RIGHT, false);
		if(k == Keys.SPACE) MyInput.setKey(MyInput.JUMP, false);
		if(k == Keys.Q || k == Keys.CONTROL_RIGHT) MyInput.setKey(MyInput.INTERACT, false);
//		if(k == Keys.SHIFT_LEFT || k == Keys.NUMPAD_0) MyInput.setKey(MyInput.ATTACK, false);
		if(k == Keys.R || k == Keys.NUMPAD_1) MyInput.setKey(MyInput.USE, false);
		if(k == Keys.ESCAPE) MyInput.setKey(MyInput.PAUSE, false);
		if(k == Keys.ENTER) MyInput.setKey(MyInput.ENTER, false);
		if(k == Keys.NUMPAD_8) MyInput.setKey(MyInput.DEBUG_UP, false);
		if(k == Keys.NUMPAD_2) MyInput.setKey(MyInput.DEBUG_DOWN, false);
		if(k == Keys.NUMPAD_4) MyInput.setKey(MyInput.DEBUG_LEFT, false);
		if(k == Keys.NUMPAD_6) MyInput.setKey(MyInput.DEBUG_RIGHT, false);
		if(k == Keys.L) MyInput.setKey(MyInput.DEBUG, false);
		if(k == Keys.P) MyInput.setKey(MyInput.DEBUG1, false);
		if(k == Keys.NUMPAD_7) MyInput.setKey(MyInput.DEBUG_LEFT2, false);
		if(k == Keys.NUMPAD_9) MyInput.setKey(MyInput.DEBUG_RIGHT2, false);
		if(k == Keys.NUMPAD_5) MyInput.setKey(MyInput.DEBUG_CENTER, false);
		if(k == Keys.L) MyInput.setKey(MyInput.DEBUG, false);
		if(k == Keys.P) MyInput.setKey(MyInput.DEBUG1, false);
		if(k == Keys.TAB) MyInput.setKey(MyInput.DEBUG2, false);
		if(k == Keys.F3) MyInput.setKey(MyInput.DEBUG3, false);
		
		return true;
	}

}
