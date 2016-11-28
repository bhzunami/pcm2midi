package ch.fhnw.tvver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.BlockBuffer;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.ui.PlotWindow;
import ch.fhnw.util.color.RGB;

public class PCM2MidConverter extends AbstractPCM2MIDI {
	// Attack herunter schrauben
	// suspend a decand 2 s
	// attack 0.015

	private static final float A_SUB_CONTRA_OCTAVE_FREQ = 25.5f;

	public PCM2MidConverter(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException,
			InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.REPORT, Flags.WAVE));
	}

	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		// Window defines the windowing function in order to have a continuous
		// signal if the sample
		// gets repeated multiple times before and after.
		FFT fft = new FFT(A_SUB_CONTRA_OCTAVE_FREQ, Window.HANN);
		BlockBuffer blockBuffer = new BlockBuffer(1024, true, Window.HANN);
		HpsPitchDetection hps = new HpsPitchDetection(fft, 5);
		// Plotter plotter = new Plotter("Tone detection", 1000, 1000);
		// plotter.plot();

		// program.addLast(new Distort());
		program.addLast(new DCRemove());
		program.addLast(new AutoGain());
		program.addLast(fft);

		program.addLast(hps);

		program.addLast(new Converter(fft, blockBuffer, hps));

		PlotWindow window = new PlotWindow(program);
		// program.addLast(new Converter(fft, blockBuffer));
		// new JFrame().setVisible(true);
	}

	private class Converter extends AbstractRenderCommand<IAudioRenderTarget> {

		private Map<Integer, Long> playedNotes = new HashMap<>();
		private long time = System.currentTimeMillis();

		private FFT fft;
		// private float[] samples = new float[1024];
		private BlockBuffer blockBuffer;
		private Plotter plot;
		int idx = 0;
		int buffer = 0;

		// bar(list, RGB.GRAY)
		// point(correction, 0, 15, RGB.RED) value

		private float[] spectrum = new float[1024];
		private float[] last_spectrum = new float[1024];
		List<Float> spectralFlux = new ArrayList<Float>();

		List<Float> threshold = new ArrayList<>();

		private HpsPitchDetection hps;

		public Converter(FFT fft, BlockBuffer blockBuffer, HpsPitchDetection hps) {
			fft.addLast(this);
			this.fft = fft;
			this.blockBuffer = blockBuffer;
			this.hps = hps;
		}

		public Converter(FFT fft, BlockBuffer blockBuffer, Plotter plot) {
			fft.addLast(this);
			this.fft = fft;
			this.blockBuffer = blockBuffer;
			this.plot = plot;
		}

		@Override
		protected void init(IAudioRenderTarget target) throws RenderCommandException {
		}

		@Override
		protected void run(IAudioRenderTarget target) throws RenderCommandException {
			this.idx += 1;
			// float frequ_calc = (target.getFrame().sRate / 2) /
			// (blockBuffer.size() / 2) / 2; // 22050
			// /
			// 513
			// / 2
			// Copy spectrum to last_spectrum
			System.arraycopy(spectrum, 0, last_spectrum, 0, spectrum.length);

			// Set new spectrum
			System.arraycopy(fft.power().clone(), 0, spectrum, 0, spectrum.length);

			// float flux = fft.power(25, 16000); //
			float flux = calculateFlux();
			spectralFlux.add(flux);

			// Now we have to decide if this amplitude is enough high as a
			// new tone.
			float mean = calcualteTreshhold(Math.max(0, this.idx - 150),
					Math.min(spectralFlux.size() - 1, this.idx + 150));
			threshold.add(mean);

			if (flux > mean) {
				int[] actual_tone = getVelocities();
				int maxIndex = 0;
				for (int i = 0; i < actual_tone.length; i++) {
					int newVelo = actual_tone[i];
					if (newVelo > actual_tone[maxIndex]) {
						maxIndex = i;
					}
				}

				PitchDetectionResult pitch = hps.getPitch();
				if (pitch.isPitched()) {
					Long then = this.playedNotes.getOrDefault(pitch.getMidiNote(), 0L);
					Long now = System.currentTimeMillis();
					if (now - then > 0) {
						this.playedNotes.put(pitch.getMidiNote(), now);
						System.out.println(String.format("%4d%4d%20d", maxIndex, pitch.getMidiNote(), now - then));
						noteOn(pitch.getMidiNote(), 1);
						noteOff(pitch.getMidiNote(), 1);
					}
				}

				clear();
				bars(new float[] { 1, 2, 3, 4, 5, 6, 7 }, RGB.MAGENTA);
				bars(new float[] { 1, 2, 3, 4, 5, 6, 7 }, RGB.MAGENTA);
				bars(new float[] { 1, 2, 3, 4, 5, 6, 7 }, RGB.MAGENTA);
				bars(new float[] { 1, 2, 3, 4, 5, 6, 7 }, RGB.MAGENTA);
				bars(new float[] { 1, 2, 3, 4, 5, 6, 7 }, RGB.MAGENTA);
				bars(new float[] { 1, 2, 3, 4, 5, 6, 7 }, RGB.MAGENTA);

				// System.out.println("");
				// System.out.println("TONE! Mean: " + mean + " current flux: "
				// + flux);
				// System.out.println("--------------------------------");

			}

			// if(tone != calculateTone(fft.getSpectrum(), frequ_calc) &&
			// calculateTone(fft.getSpectrum(), frequ_calc) > 0) {
			// System.out.println("------------------------------");
			// System.out.println("FLUX: " +flux +" treshhold: " +mean * 1.5f);
			// System.out.println("New Tone on: "
			// +(int)calculateTone(fft.getSpectrum(),
			// frequ_calc));
			// tone = (int)calculateTone(fft.getSpectrum(), frequ_calc);
			// }

			// Wir brauchen von der FFT das Spectrum
			// The spectrum tells us for each frequency how much the frequency
			// contributes to the
			// original time domain audio signal.
			// When we transform 1024 samples we get 513 frequency bins e
			// http://www.badlogicgames.com/wordpress/?cat=18&paged=1
			// http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.332.989&rep=rep1&type=pdf

			// Stereo to mono
			// if (target.getNumChannels() > 1) {
			// System.out.println("Should be converted to mono");
			// samples = target.getFrame().getMonoSamples();
			// } else {
			// samples = target.getFrame().samples;
			// }

			// AudioUtilities.multiplyHarmonics(transformed, 2);
			// final BitSet peaks = AudioUtilities.peaks(transformed, 3, 0.2f);
			// List<Vec2> list = new ArrayList<>();
			// for (int i = peaks.nextSetBit(0); i >= 0; i = peaks.nextSetBit(i
			// + 1)){
			// list.add(new Vec2(transformed[i], fft.idx2f(i)));
			// }
			// Collections.sort(list, (v0, v1) -> Float.compare(v0.x, v1.x));
			// float[] pitch = new float[list.size()];
			// for (int i = 0; i < pitch.length; i++){
			// pitch[i] = list.get(i).y;
			// }
			// if(pitch.length > 0){
			// System.out.println(pitch[0]);
			// }
		}

		// Get difference from frequencies
		private float calculateFlux() {
			float flux = 0;
			for (int i = 0; i < spectrum.length; i++) {
				float value = (spectrum[i] - last_spectrum[i]);
				value = (float) ((value + Math.abs(value)) / (2 * Math.pow(2, i + 1)));
				flux += value < 0 ? 0 : value;
			}

			return flux;
		}

		private float calcualteTreshhold(int start, int end) {
			float mean = 0;
			for (int j = start; j <= end; j++) {
				mean += spectralFlux.get(j);
			}
			mean /= (end - start);
			return mean * 2f;

		}

	}

	private int calculateTone(float[] spectrum, float frequ_calc) {
		float max = 0;
		int idx = 0;
		for (int i = 0; i < spectrum.length; i++) {
			if (max < spectrum[i]) {
				idx = i;
			}
			max = Math.max(max, spectrum[i]);
		}
		Double value = 69 + 12 * (Math.log(idx * frequ_calc / 440) / Math.log(2));
		return value.isInfinite() ? 0 : value.intValue();
	}

}
