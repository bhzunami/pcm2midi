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

	private static final double THRESHOLD = 0.2;
	private static final int AUDIO_BUFFER_SIZE = 2048;

	private float sampleRate;
	private float[] audioBuffer;
	private PitchDetectionResult pitchResult;
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
		this.pitchResult = new PitchDetectionResult();
		float[] resultBuffer = new float[AUDIO_BUFFER_SIZE / 2];
		this.applyDifferenceFunction(this.audioBuffer, resultBuffer);
		this.applyCumulativeMeanNormalization(resultBuffer);
		int index = this.applyAbsoluteThreshold(resultBuffer);
		if (index >= 0) {
			float correctedIndex = this.applyParabolicInterpolation(resultBuffer, index);
			float freq = this.sampleRate / correctedIndex;
			this.pitchResult.setMidiNote(MidiNoteUtils.getNearestMidiNote(freq));
		} else {
			this.pitchResult.setPitched(false);
		}
	}

	/**
	 * Applies the difference function from the Yin paper.
	 * 
	 * @param audioBuffer
	 *            the input buffer
	 * @param resultBuffer
	 *            the result buffer
	 */
	private void applyDifferenceFunction(float[] audioBuffer, float[] resultBuffer) {
		for (int offset = 1; offset < resultBuffer.length; offset++) {
			for (int index = 0; index < resultBuffer.length; index++) {
				float delta = audioBuffer[index] - audioBuffer[index + offset];
				resultBuffer[offset] += delta * delta;
			}
		}
	}

	/**
	 * Applies the cumulative mean normalization on the given buffer
	 * 
	 * @param resultBuffer
	 *            the buffer where this function should be applied
	 */
	private void applyCumulativeMeanNormalization(float[] resultBuffer) {
		resultBuffer[0] = 1;
		float sum = 0;
		for (int index = 1; index < resultBuffer.length; index++) {
			sum = sum + resultBuffer[index];
			resultBuffer[index] = resultBuffer[index] * index / sum;
		}
	}

	/**
	 * Applies the absolute threshold step from the Yin paper. It searches the
	 * index where the first local minimum exists.
	 * 
	 * @param resultBuffer
	 *            the buffer where to search.
	 * @return the index of the first local minimum, or -1 of none found.
	 */
	private int applyAbsoluteThreshold(float[] resultBuffer) {
		int index;
		for (index = 2; index < resultBuffer.length; index++) {
			if (resultBuffer[index] < THRESHOLD) {
				while (index + 1 < resultBuffer.length && resultBuffer[index + 1] < resultBuffer[index]) {
					index++;
				}
				break;
			}
		}
		if (index == resultBuffer.length || resultBuffer[index] >= THRESHOLD) {
			this.pitchResult.setPitched(false);
			return -1;
		} else {
			this.pitchResult.setPitched(true);
			return index;
		}
	}

	private float applyParabolicInterpolation(float[] resultBuffer, int index) {
		int indexBelow;
		int indexAbove;
		if (index < 1) {
			indexBelow = index;
		} else {
			indexBelow = index - 1;
		}
		if (index + 1 < resultBuffer.length) {
			indexAbove = index + 1;
		} else {
			indexAbove = index;
		}
		if (indexBelow == index) {
			if (resultBuffer[index] <= resultBuffer[indexAbove]) {
				return index;
			} else {
				return indexAbove;
			}
		} else if (indexAbove == index) {
			if (resultBuffer[index] <= resultBuffer[indexBelow]) {
				return index;
			} else {
				return indexBelow;
			}
		} else {
			return index + (resultBuffer[indexAbove] - resultBuffer[indexBelow])
					/ (2 * (2 * resultBuffer[index] - resultBuffer[indexAbove] - resultBuffer[indexBelow]));
		}
	}

	@Override
	public PitchDetectionResult getResult() {
		return this.pitchResult;
	}

	@Override
	public void clearResult() {
		this.pitchResult = null;
	}

}