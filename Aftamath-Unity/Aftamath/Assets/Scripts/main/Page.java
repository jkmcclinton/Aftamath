package main;

public class Page{
	public String text;
	public int emotion;
	public boolean skip;
	
	public Page(String text, int emotion, boolean skip){
		this.text = text;
		this.emotion = emotion;
		this.skip = skip;
	}
}
