package ch.fhnw.tvver;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.ui.IPlotable;
import ch.fhnw.tvver.onsetdetection.OnSetDetection;
import ch.fhnw.tvver.pitchdetection.PitchDetection;
import ch.fhnw.tvver.pitchdetection.PitchDetectionResult;
import ch.fhnw.tvver.pitchdetection.YinPitchDetection;

public class PCM2MidConverter extends AbstractPCM2MIDI {
	// Attack herunter schrauben
	// suspend a decand 2 s
	// attack 0.015

	private static final float A_SUB_CONTRA_OCTAVE_FREQ = 25.5f;
	private static final int HARMONICS = 2;

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

		YinPitchDetection ypd = new YinPitchDetection();
		OnSetDetection osd = new OnSetDetection(fft, ypd);

		program.addLast(new AutoGain());
		program.addLast(fft);
		program.addLast(osd);
		program.addLast(ypd);
		program.addLast(new Converter(ypd));

	}

	private class Converter extends AbstractRenderCommand<IAudioRenderTarget> implements IPlotable {

		private PitchDetection pitchDetection;

		public Converter(PitchDetection pitchDetection) {
			this.pitchDetection = pitchDetection;
		}

		@Override
		protected void init(IAudioRenderTarget target) throws RenderCommandException {
		}

		@Override
		protected void run(IAudioRenderTarget target) throws RenderCommandException {
			PitchDetectionResult result = this.pitchDetection.getResult();
			if(result != null){
				int midiNote = result.getMidiNote();
				System.out.println(String.format("%5s %5s", midiNote, this.getActualNote()));
				PCM2MidConverter.this.noteOn(midiNote, 16);
				this.pitchDetection.clearResult();
			}
		}

		private int getActualNote() {
			int velocity = 0;
			int maxIndex = 0;
			int[] velocities = PCM2MidConverter.this.getVelocities();
			for (int index = 0; index < velocities.length; index++) {
				if (velocities[index] > velocity) {
					velocity = velocities[index];
					maxIndex = index;
				}
			}
			return maxIndex;
		}

	}
}
