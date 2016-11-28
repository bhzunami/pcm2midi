package ch.fhnw.tvver;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

/**
 * Pitch Detection with the Harmonic Product Spectrum Algorithm
 */
public class HpsPitchDetection extends AbstractRenderCommand<IAudioRenderTarget> {
	
	private FFT fft;
	private OnSetDetection osd;
	private int harmonics;
	private LinkedList<Float> spectrum;
	private PitchDetectionResult pitch;
	
	public HpsPitchDetection(FFT fft, OnSetDetection osd, int harmonics){
		fft.addLast(this);
        this.fft = fft;
		this.osd = osd;
		this.harmonics = harmonics;
		spectrum = new LinkedList<>();
	}
	
	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		float[] power = fft.power();
		this.spectrum.clear();
		for(float f : power){
			this.spectrum.addLast(f);
		}
		if(this.osd.tone){
			this.pitch = this.detectPitch(this.spectrum, this.harmonics);
		}
	}

	private PitchDetectionResult detectPitch(List<Float> samples, int harmonics) {
	
		List<List<Float>> downsamples = new ArrayList<>();
		for(int index = 1; index <= harmonics; index++){
			downsamples.add(downsample(samples, index));
		}

		List<Float> hps = new ArrayList<>();

		for (int index = 0; index < downsamples.get(harmonics - 1).size(); index++) {
			float product = 1;
			for(int downsampleIdx = 0; downsampleIdx < harmonics; downsampleIdx++){				
				product *= downsamples.get(downsampleIdx).get(index);
			}
			hps.add(product);
		}
		
		float maxProduct = 0;
		int maxIndex = 0;

		for (int index = 0; index < hps.size() - 1; index++) {
			if (hps.get(index) > maxProduct) {
				maxProduct = hps.get(index);
				maxIndex = index;
			}
		}

		float fundamentalFreq = fft.idx2f(maxIndex);
		
		boolean isPitched = true;
		//isPitched &= fundamentalFreq > 50;
		//isPitched &= amplitudeRatio > 1;
		return new PitchDetectionResult(fundamentalFreq, isPitched);
		
		
		// Octave errors are common (detection is sometimes an octave too high).
		// To correct, apply this rule: if the second peak amplitude below
		// initially chosen pitch is approximately 1/2 of the chosen pitch AND
		// the ratio of amplitudes is above a threshold (e.g., 0.2 for 5
		// harmonics), THEN select the lower octave peak as the pitch for the
		// current frame.
		// Due to noise, frequencies below about 50 Hz should not be searched
		// for a pitch.
		// TODO Octave correction

	}

	/**
	 * decreases the sampling rate of x by keeping every nth sample starting
	 * with the first sample.
	 */
	private ArrayList<Float> downsample(List<Float> samples, int n) {
		ArrayList<Float> downsampled = new ArrayList<>();
		for (int index = n-1; index < samples.size(); index += n) {
			downsampled.add(samples.get(index));
		}
		return downsampled;
	}
	
	public PitchDetectionResult getPitch(){
		return this.pitch;
	}
	
}
