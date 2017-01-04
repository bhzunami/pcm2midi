package ch.fhnw.tvver.pitchdetection;

public class PitchDetectionResult {
	
	private float freq;
	private float amplitude;
	private int midiNote;
	private boolean isPitched;
	
	public PitchDetectionResult() {}
	
	
	public PitchDetectionResult(float freq, float amplitude, int midiNote, boolean isPitched) {
		this.freq = freq;
		this.amplitude = amplitude;
		this.midiNote = midiNote;
		this.isPitched = isPitched;
	}
	
	public void setFreq(float freq) {
		this.freq = freq;
	}

	public void setAmplitude(float amplitude) {
		this.amplitude = amplitude;
	}

	public void setMidiNote(int midiNote) {
		this.midiNote = midiNote;
	}

	public void setPitched(boolean isPitched) {
		this.isPitched = isPitched;
	}

	public float getFreq() {
		return this.freq;
	}
	
	public float getAmplitude(){
		return this.amplitude;
	}
	
	public int getMidiNote(){
		return this.midiNote;
	}

	public boolean isPitched() {
		return this.isPitched;
	}
	
}
