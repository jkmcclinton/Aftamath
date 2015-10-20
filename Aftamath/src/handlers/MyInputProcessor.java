package handlers;

import handlers.MyInput.Input;
import main.Game;

import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.utils.Array;

public class MyInputProcessor extends InputAdapter {
	
	public int mode = 0;
	public void keyboardMode(){ mode = 1; }
	public void gameMode(){ mode = 0; }
	
	public boolean keyDown(int k) {
		if(mode==0){
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
			if(k == Keys.M) MyInput.setKey(Input.DEBUG_CENTER, true);
			if(k == Keys.F1) MyInput.setKey(Input.COLLISION, true);
			if(k == Keys.F2) MyInput.setKey(Input.LIGHTS, true);
			if(k == Keys.F3) MyInput.setKey(Input.DEBUG_TEXT, true);
			if(k == Keys.F4) MyInput.setKey(Input.DEBUG2, true);
			if(k == Keys.PLUS) MyInput.setKey(Input.ZOOM_IN, true);
			if(k == Keys.MINUS) MyInput.setKey(Input.ZOOM_OUT, true);
		} else {
//			System.out.println(k+":"+Keys.toString(k));
			boolean entered = false;
			if(k == Keys.ESCAPE){ entered=true; MyInput.setKey(Input.PAUSE, true); }
			if(k == Keys.SPACE){ entered=true; MyInput.setKey(Input.JUMP, true); }
			if(k == Keys.ENTER){ entered=true; MyInput.setKey(Input.ENTER, true); }
			if(Keys.toString(k).toLowerCase().equals("delete")){ entered=true; MyInput.setKey(Input.DOWN, true); }
			if(k == Keys.LEFT){ entered=true; MyInput.setKey(Input.LEFT, true); }
			if(k == Keys.RIGHT){ entered=true; MyInput.setKey(Input.RIGHT, true); }
			if(k == Keys.HOME){ entered=true; Game.inputIndex = 0;}
			if(k == Keys.END){ entered=true; Game.inputIndex = Game.getInput().length() - 1; }
			if(k == Keys.SHIFT_LEFT || k == Keys.SHIFT_RIGHT){ entered=true; MyInput.setKey(Input.UP, true); }
			
			if(MyInput.isDown(Input.UP)){
				if(k == Keys.NUM_1){ entered = true; Game.addInputChar("!"); }
				if(k == Keys.NUM_2){ entered = true; Game.addInputChar("@"); }
				if(k == Keys.NUM_3){ entered = true; Game.addInputChar("#"); }
				if(k == Keys.NUM_4){ entered = true; Game.addInputChar("$"); }
				if(k == Keys.NUM_5){ entered = true; Game.addInputChar("%"); }
				if(k == Keys.NUM_6){ entered = true; Game.addInputChar("^"); }
				if(k == Keys.NUM_7){ entered = true; Game.addInputChar("&"); }
				if(k == Keys.NUM_8){ entered = true; Game.addInputChar("*"); }
				if(k == Keys.NUM_9){ entered = true; Game.addInputChar("("); }
				if(k == Keys.NUM_0){ entered = true; Game.addInputChar(")"); }
				if(k == Keys.EQUALS){ entered = true; Game.addInputChar("+"); }
				if(k == Keys.MINUS){ entered = true; Game.addInputChar("_"); }
				if(k == Keys.PERIOD){ entered = true; Game.addInputChar(">"); }
				if(k == Keys.COMMA){ entered = true; Game.addInputChar("<"); }
				if(k == Keys.SLASH){ entered = true; Game.addInputChar("?"); }
			}
			
			if(k == Keys.NUMPAD_1){ k = Keys.NUM_1; }
			if(k == Keys.NUMPAD_2){ k = Keys.NUM_2; }
			if(k == Keys.NUMPAD_3){ k = Keys.NUM_3; }
			if(k == Keys.NUMPAD_4){ k = Keys.NUM_4; }
			if(k == Keys.NUMPAD_5){ k = Keys.NUM_5; }
			if(k == Keys.NUMPAD_6){ k = Keys.NUM_6; }
			if(k == Keys.NUMPAD_7){ k = Keys.NUM_7; }
			if(k == Keys.NUMPAD_8){ k = Keys.NUM_8; }
			if(k == Keys.NUMPAD_9){ k = Keys.NUM_9; }
			if(k == Keys.NUMPAD_0){ k = Keys.NUM_0; }
			
			if(!entered && !ignoreKeys.contains(k, false))
				if(MyInput.isDown(Input.UP) || MyInput.isCaps)
					Game.addInputChar(Keys.toString(k));
				else
					Game.addInputChar(Keys.toString(k).toLowerCase());
		}
		
		return true;
	}
	
	public boolean keyUp(int k) {
		if(mode==0){
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
			if(k == Keys.M) MyInput.setKey(Input.DEBUG_CENTER, false);
			if(k == Keys.F1) MyInput.setKey(Input.COLLISION, false);
			if(k == Keys.F2) MyInput.setKey(Input.LIGHTS, false);
			if(k == Keys.F3) MyInput.setKey(Input.DEBUG_TEXT, false);
			if(k == Keys.F4) MyInput.setKey(Input.DEBUG2, false);
			if(k == Keys.PLUS) MyInput.setKey(Input.ZOOM_IN, false);
			if(k == Keys.MINUS) MyInput.setKey(Input.ZOOM_OUT, false);
		} else {
			if(k == Keys.ESCAPE){ MyInput.setKey(Input.PAUSE, false); }
			if(k == Keys.SPACE){ MyInput.setKey(Input.JUMP, false); }
			if(k == Keys.ENTER){ MyInput.setKey(Input.ENTER, false); }
			if(k == Keys.LEFT){ MyInput.setKey(Input.LEFT, false); }
			if(k == Keys.RIGHT){  MyInput.setKey(Input.RIGHT, false); }
			if(Keys.toString(k).toLowerCase().equals("delete")){ MyInput.setKey(Input.DOWN, false); }
			if(k == Keys.SHIFT_LEFT || k == Keys.SHIFT_RIGHT){ MyInput.setKey(Input.UP, false); }
		}
		return true;
	}
	
	private static Array<Integer> ignoreKeys = new Array<>();
	static{
		ignoreKeys.add(Keys.ALT_LEFT);
		ignoreKeys.add(Keys.ALT_RIGHT);
		ignoreKeys.add(Keys.TAB);
		ignoreKeys.add(Keys.F1);
		ignoreKeys.add(Keys.F2);
		ignoreKeys.add(Keys.F3);
		ignoreKeys.add(Keys.F4);
		ignoreKeys.add(Keys.F5);
		ignoreKeys.add(Keys.F6);
		ignoreKeys.add(Keys.F7);
		ignoreKeys.add(Keys.F8);
		ignoreKeys.add(Keys.F9);
		ignoreKeys.add(Keys.F10);
		ignoreKeys.add(Keys.F11);
		ignoreKeys.add(Keys.F12);
		ignoreKeys.add(Keys.NUM);
		ignoreKeys.add(Keys.PAGE_UP);
		ignoreKeys.add(Keys.PAGE_DOWN);
		ignoreKeys.add(Keys.CONTROL_LEFT);
		ignoreKeys.add(Keys.CONTROL_RIGHT);
		ignoreKeys.add(Keys.HEADSETHOOK);
		ignoreKeys.add(Keys.SEARCH);
		ignoreKeys.add(Keys.SEMICOLON);
		ignoreKeys.add(Keys.SOFT_RIGHT);
		ignoreKeys.add(Keys.SOFT_LEFT);
		ignoreKeys.add(Keys.SWITCH_CHARSET);
		ignoreKeys.add(Keys.CLEAR);
		ignoreKeys.add(Keys.CENTER);
		ignoreKeys.add(Keys.ENDCALL);
//		ignoreKeys.add(Keys.LEFT_BRACKET);
//		ignoreKeys.add(Keys.RIGHT_BRACKET);
		ignoreKeys.add(Keys.MEDIA_STOP);
		ignoreKeys.add(Keys.FORWARD_DEL);
		ignoreKeys.add(Keys.FOCUS);
		ignoreKeys.add(Keys.ENVELOPE);
		ignoreKeys.add(Keys.EXPLORER);
		ignoreKeys.add(Keys.MEDIA_FAST_FORWARD);
		ignoreKeys.add(Keys.MEDIA_NEXT);
		ignoreKeys.add(Keys.MEDIA_PLAY_PAUSE);
		ignoreKeys.add(Keys.MEDIA_PREVIOUS);
		ignoreKeys.add(Keys.MEDIA_REWIND);
		ignoreKeys.add(Keys.MENU);
		ignoreKeys.add(Keys.META_ALT_LEFT_ON);
		ignoreKeys.add(Keys.META_ALT_ON);
		ignoreKeys.add(Keys.META_ALT_RIGHT_ON);
		ignoreKeys.add(Keys.META_SHIFT_LEFT_ON);
		ignoreKeys.add(Keys.META_SHIFT_ON);
		ignoreKeys.add(Keys.META_SHIFT_RIGHT_ON);
		ignoreKeys.add(Keys.META_SYM_ON);
		ignoreKeys.add(Keys.SYM);
		ignoreKeys.add(Keys.MUTE);
		ignoreKeys.add(Keys.NOTIFICATION);
		ignoreKeys.add(Keys.POWER);
		ignoreKeys.add(Keys.PICTSYMBOLS);
		ignoreKeys.add(Keys.UNKNOWN);
		ignoreKeys.add(Keys.VOLUME_DOWN);
		ignoreKeys.add(Keys.VOLUME_UP);
		ignoreKeys.add(Keys.DOWN);
		ignoreKeys.add(Keys.UP);
		ignoreKeys.add(Keys.PLUS);
		ignoreKeys.removeValue(32, false);
		ignoreKeys.removeValue(16, false);
	}

}
