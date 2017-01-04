package ch.fhnw.tvver.pitchdetection;

public class MidiNoteUtils {
	
	public static int getNearestMidiNote(float frequency){
		MidiNote lower = null;
		MidiNote upper = null;
		for(MidiNote note : MidiNote.values()){
			if(Float.compare(frequency, note.getFrequency()) == 0){
				return note.getMidiNote();
			} else if(frequency > note.getFrequency()){
				lower = note;
			} else if(frequency < note.getFrequency()){
				upper = note;
				break;
			}
		}
		if(lower == null){
			return upper.getMidiNote();
		} else if (upper == null){
			return lower.getMidiNote();
		} else {
			float diff1 = frequency - lower.getFrequency();
			float diff2 = upper.getFrequency() - frequency;
			if(diff1 <= diff2){
				return lower.getMidiNote();
			} else 
				return upper.getMidiNote();
		}
	}
	
}
