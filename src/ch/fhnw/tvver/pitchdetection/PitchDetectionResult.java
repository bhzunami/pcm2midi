package ch.fhnw.tvver.pitchdetection;

public class PitchDetectionResult {

	private MidiNote midiNote;
	private boolean isPitched;

	public MidiNote getMidiNote() {
		return this.midiNote;
	}

	public void setMidiNote(MidiNote midiNote) {
		this.midiNote = midiNote;
	}

	public boolean isPitched() {
		return this.isPitched;
	}

	public void setPitched(boolean isPitched) {
		this.isPitched = isPitched;
	}

}
