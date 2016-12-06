package ch.fhnw.tvver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

/**
 * Pitch Detection with the Harmonic Product Spectrum Algorithm
 */
public class HpsPitchDetection extends AbstractRenderCommand<IAudioRenderTarget> {

	private FFT fft;
	private int harmonics;
	private Map<Integer, Long> pitchHistory;
	private PitchDetectionResult lastResult;

	public HpsPitchDetection(FFT fft, int harmonics) {
		this.fft = fft;
		this.harmonics = harmonics;
		this.pitchHistory = new HashMap<>();
	}

	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
	}

	public void detectPitch() {
		float[] spectrum = this.fft.power();
		List<List<Float>> downsamples = new ArrayList<>();
		for (int index = 1; index <= this.harmonics; index++) {
			downsamples.add(this.downsample(spectrum, index));
		}

		List<Float> hps = new ArrayList<>();

		for (int index = 0; index < downsamples.get(this.harmonics - 1).size(); index++) {
			float product = 1;
			for (int downsampleIdx = 0; downsampleIdx < this.harmonics; downsampleIdx++) {
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

		float amplitude = spectrum[maxIndex];
		float fundamentalFreq = this.fft.idx2f(maxIndex);
		int midiNote = this.calcMidiNote(fundamentalFreq);

		long lastPlayed = this.pitchHistory.getOrDefault(midiNote, 0L);
		this.pitchHistory.put(midiNote, System.currentTimeMillis());
		long now = this.pitchHistory.get(midiNote);
		
		boolean isPitched = true;
		isPitched &= fundamentalFreq > 55.0f;
		isPitched &= now - lastPlayed > 100;
		isPitched &= amplitude > 100;
		this.lastResult =  new PitchDetectionResult(fundamentalFreq, amplitude, midiNote, isPitched);

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
	private ArrayList<Float> downsample(float[] samples, int n) {
		ArrayList<Float> downsampled = new ArrayList<>();
		for (int index = n - 1; index < samples.length; index += n) {
			downsampled.add(samples[index]);
		}
		return downsampled;
	}

	private int calcMidiNote(float freq) {
		return (int) Math.ceil(69 + 12 * (Math.log(freq / 440) / Math.log(2)));
	}

	public PitchDetectionResult getLastResult() {
		return this.lastResult;
	}
	
	public void clearResult() {
		this.lastResult = null;
	}

}
