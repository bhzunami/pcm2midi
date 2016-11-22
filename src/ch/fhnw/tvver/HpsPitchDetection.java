package ch.fhnw.tvver;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.audio.fx.FFT;

/**
 * Pitch Detection with the Harmonic Product Spectrum Algorithm
 */
public class HpsPitchDetection {

	public PitchDetectionResult getPitch(FFT fft, int harmonics) {

		float correctFactor = 0.986f;
		float threshold = 0.2f;
		
		float[] spectrum = fft.power();
		int frameSize = spectrum.length;

		List<List<Float>> downsamples = new ArrayList<>();
		for(int index = 1; index <= harmonics; index++){
			downsamples.add(downsample(spectrum, index));
		}

		List<Float> hps = new ArrayList<>();

		for (int index = 0; index < downsamples.get(harmonics - 1).size() - 1; index++) {
			float product = 1;
			for(int downsampleIdx = 0; downsampleIdx < harmonics; downsampleIdx++){				
				product *= downsamples.get(downsampleIdx).get(index);
			}
			hps.add(product);
		}
		
		float maxProduct = 0;
		float secondMaxProduct = 0.0f;
		int maxIndex = 0;

		for (int index = 0; index < hps.size() - 1; index++) {
			if (hps.get(index) > maxProduct) {
				secondMaxProduct = maxProduct;
				maxProduct = hps.get(index);
				maxIndex = index;
			}
		}

		float fundamentalFreq = fft.idx2f(maxIndex);
		
		// Octave errors are common (detection is sometimes an octave too high).
		// To correct, apply this rule: if the second peak amplitude below
		// initially chosen pitch is approximately 1/2 of the chosen pitch AND
		// the ratio of amplitudes is above a threshold (e.g., 0.2 for 5
		// harmonics), THEN select the lower octave peak as the pitch for the
		// current frame.
		// Due to noise, frequencies below about 50 Hz should not be searched
		// for a pitch.
		
//		if(maxProduct / 2 - secondMaxProduct < 1 ){
//			if(maxProduct / secondMaxProduct > threshold){
//				fundamentalFreq = fft.idx2f(maxIndex) / 2; 
//			}
//		}
		
		boolean isPitched = fundamentalFreq > 50;
		return new PitchDetectionResult(fundamentalFreq, isPitched);

	}

	/**
	 * decreases the sampling rate of x by keeping every nth sample starting
	 * with the first sample.
	 */
	private ArrayList<Float> downsample(float[] samples, int n) {
		ArrayList<Float> downsampled = new ArrayList<>();
		for (int index = 0; index < samples.length; index += n) {
			downsampled.add(samples[index]);
		}
		return downsampled;
	}
	
}
