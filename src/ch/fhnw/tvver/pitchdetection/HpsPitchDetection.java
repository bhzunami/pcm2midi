package ch.fhnw.tvver.pitchdetection;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

/**
 * Pitch Detection with the Harmonic Product Spectrum Algorithm
 */
public class HpsPitchDetection extends AbstractRenderCommand<IAudioRenderTarget> implements PitchDetection {

	private static final int AUDIO_BUFFER_SIZE = 2048;
	private static final int HARMONICS = 2;

	private FFT fft;
	private float[] audioBuffer;
	private PitchDetectionResult lastResult;
	private boolean recording = false;
	private int recordCounter = 0;

	public HpsPitchDetection(FFT fft) {
		this.fft = fft;
		this.audioBuffer = new float[AUDIO_BUFFER_SIZE];
	}

	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		float[] incomingSamples = this.fft.power();
		if (this.recording) {
			int offset = incomingSamples.length * this.recordCounter;
			for (int index = 0; index < incomingSamples.length; index++) {
				this.audioBuffer[index + offset] = incomingSamples[index];
			}
			this.recordCounter++;
		}
		int maxRecords = AUDIO_BUFFER_SIZE / incomingSamples.length;
		if (this.recordCounter == maxRecords) {
			this.recording = false;
			this.recordCounter = 0;
			this.runDetection();
		}
	}

	private void runDetection() {
		List<List<Float>> downsamples = new ArrayList<>();
		for (int index = 1; index <= HARMONICS; index++) {
			downsamples.add(this.downsample(this.audioBuffer, index));
		}

		List<Float> hps = new ArrayList<>();

		for (int index = 0; index < downsamples.get(HARMONICS - 1).size(); index++) {
			float product = 1;
			for (int downsampleIdx = 0; downsampleIdx < HARMONICS; downsampleIdx++) {
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

		float fundamentalFreq = this.fft.idx2f(maxIndex);
		this.lastResult = new PitchDetectionResult();
		this.lastResult.setMidiNote(MidiNoteUtils.getNearestMidiNote(fundamentalFreq));
		this.lastResult.setPitched(true);
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

	
	@Override
	public void detectPitch() {
		this.recording = true;
	}

	
	@Override
	public PitchDetectionResult getResult() {
		return this.lastResult;
	}

	@Override
	public void clearResult() {
		this.lastResult = null;
	}

}
