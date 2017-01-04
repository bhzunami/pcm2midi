package ch.fhnw.tvver.pitchdetection;

public interface PitchDetection {

	void detectPitch();
	
	PitchDetectionResult getResult();
	
	void clearResult();
	
}
