package ch.fhnw.tvver.pitchdetection;

public class MidiNoteUtils {
	
	public static MidiNote getNearestMidiNote(float frequency){
		MidiNote lower = null;
		MidiNote upper = null;
		for(MidiNote note : MidiNote.values()){
			if(Float.compare(frequency, note.getFrequency()) == 0){
				return note;
			} else if(frequency > note.getFrequency()){
				lower = note;
			} else if(frequency < note.getFrequency()){
				upper = note;
				break;
			}
		}
		if(lower == null){
			return upper;
		} else if (upper == null){
			return lower;
		} else {
			float diff1 = frequency - lower.getFrequency();
			float diff2 = upper.getFrequency() - frequency;
			if(diff1 <= diff2){
				return lower;
			} else 
				return upper;
		}
	}
	
}
