package ch.fhnw.tvver.pitchdetection;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;

/**
 * This class does a Pitch Detection with the Yin-Algorithm.<br>
 * <br>
 * Sources:
 * <ul>
 * <li>https://github.com/JorenSix/TarsosDSP/blob/master/src/core/be/tarsos/dsp/pitch/Yin.java</li>
 * <li>http://recherche.ircam.fr/equipes/pcm/cheveign/ps/2002_JASA_YIN_proof.pdf</li>
 * </ul>
 */
public final class YinPitchDetection extends AbstractRenderCommand<IAudioRenderTarget> implements PitchDetection {

	private static final double THRESHOLD = 0.20;
	private static final int AUDIO_BUFFER_SIZE = 2048;
	private float sampleRate;
	private float[] audioBuffer;
	private PitchDetectionResult result;
	private boolean recording = false;
	private int recordCounter = 0;

	public YinPitchDetection() {
		this.audioBuffer = new float[AUDIO_BUFFER_SIZE];
	}

	@Override
	protected void run(IAudioRenderTarget target) throws RenderCommandException {
		this.sampleRate = target.getSampleRate();
		float[] incomingSamples = target.getFrame().getMonoSamples();
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

	@Override
	public void detectPitch() {
		this.recording = true;
	}

	private void runDetection() {
		this.result = new PitchDetectionResult();
		float[] difference = this.difference(this.audioBuffer);
		float[] normalizedDifference = this.cumulativeMeanNormalizedDifference(difference);
		int index = this.absoluteThreshold(normalizedDifference);
		if (index >= 0) {
			float correctedIndex = this.parabolicInterpolation(normalizedDifference, index);
			float freq = this.sampleRate / correctedIndex;
			this.result.setFreq(freq);
			this.result.setMidiNote(this.calcMidiNote(freq));
		} else {
			this.result.setPitched(false);
		}
	}

	private int calcMidiNote(float freq) {
		return (int) Math.ceil(69 + 12 * (Math.log(freq / 440) / Math.log(2)));
	}

	private float[] difference(final float[] audioBuffer) {
		float[] difference = new float[AUDIO_BUFFER_SIZE / 2];
		for (int offset = 1; offset < difference.length; offset++) {
			for (int index = 0; index < difference.length; index++) {
				float delta = audioBuffer[index] - audioBuffer[index + offset];
				difference[offset] += delta * delta;
			}
		}
		return difference;
	}

	private float[] cumulativeMeanNormalizedDifference(float[] difference) {
		difference[0] = 1;
		float sum = 0;
		for (int index = 1; index < difference.length; index++) {
			sum += difference[index];
			difference[index] *= index / sum;
		}
		return difference;
	}

	private int absoluteThreshold(float[] normalizedDifference) {
		int index = 2;
		for (; index < normalizedDifference.length; index++) {
			if (normalizedDifference[index] < THRESHOLD) {
				while (index + 1 < normalizedDifference.length
						&& normalizedDifference[index + 1] < normalizedDifference[index]) {
					index++;
				}
				break;
			}
		}
		if (index == normalizedDifference.length || normalizedDifference[index] >= THRESHOLD) {
			this.result.setPitched(false);
			return -1;
		} else {
			this.result.setPitched(true);
			return index;
		}
	}

	private float parabolicInterpolation(float[] normalizedDifference, int index) {
		float correctedIndex;
		int x0;
		int x2;
		if (index < 1) {
			x0 = index;
		} else {
			x0 = index - 1;
		}
		if (index + 1 < normalizedDifference.length) {
			x2 = index + 1;
		} else {
			x2 = index;
		}
		if (x0 == index) {
			if (normalizedDifference[index] <= normalizedDifference[x2]) {
				correctedIndex = index;
			} else {
				correctedIndex = x2;
			}
		} else if (x2 == index) {
			if (normalizedDifference[index] <= normalizedDifference[x0]) {
				correctedIndex = index;
			} else {
				correctedIndex = x0;
			}
		} else {
			float d0 = normalizedDifference[x0];
			float d1 = normalizedDifference[index];
			float d2 = normalizedDifference[x2];
			correctedIndex = index + (d2 - d0) / (2 * (2 * d1 - d2 - d0));
		}
		return correctedIndex;
	}

	@Override
	public PitchDetectionResult getResult() {
		return this.result;
	}

	@Override
	public void clearResult() {
		this.result = null;
	}

}