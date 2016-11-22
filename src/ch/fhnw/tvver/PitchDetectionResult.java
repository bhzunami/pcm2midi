package ch.fhnw.tvver;

public class PitchDetectionResult {
	
	private float freq;
	private boolean isPitched;
	
	public PitchDetectionResult(float freq, boolean isPitched) {
		this.freq = freq;
		this.isPitched = isPitched;
	}

	public float getFreq() {
		return freq;
	}

	public int getMidiNote() {
		return (int) (69 + 12 * (Math.log(this.freq / 440) / Math.log(2)));
	}

	public boolean isPitched() {
		return isPitched;
	}
	
	

}
