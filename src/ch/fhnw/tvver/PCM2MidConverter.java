package ch.fhnw.tvver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.URLAudioSource;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.tvver.workshop.Distort;
import ch.fhnw.util.math.Vec2;

public class PCM2MidConverter extends AbstractPCM2MIDI {    
	
	private static final float A_SUB_CONTRA_OCTAVE_FREQ = 25.5f;

	public PCM2MidConverter(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException,
			InvalidMidiDataException, RenderCommandException {
		super(track, EnumSet.of(Flags.REPORT, Flags.WAVE));
	}

	@Override
	protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
		// Window defines the windowing function in order to have a continuous signal if the sample 
		// gets repeated multiple times before and after.
		FFT fft = new FFT(A_SUB_CONTRA_OCTAVE_FREQ, Window.HANN);
		
		//program.addLast(new Distort());
		program.addLast(new DCRemove());
		program.addLast(new AutoGain());
		program.addLast(fft);
		
		program.addLast(new Converter(fft));
		new JFrame().setVisible(true);
	}

	private class Converter extends AbstractRenderCommand<IAudioRenderTarget> {
		
		private FFT fft;
		private float max = 0f;
		
		public Converter(FFT fft){
			this.fft = fft;
		}

		@Override
		protected void init(IAudioRenderTarget target) throws RenderCommandException {
			// do nothing
		}

		@Override
		protected void run(IAudioRenderTarget target) throws RenderCommandException {
		    
			float[] transformed = this.fft.power().clone();			
			float max = 0f;
			
			for(int i = 0; i < transformed.length; i++ ) {
			    if(transformed[i] > max) {
			        max = transformed[i];
			    }
			}
			
			if(this.max != max) {
			    this.max = max;
			    double tone = 69 + (Math.log(12) / Math.log(2)) * (this.max/440);
	            System.out.println("Tone:  "+ tone);
			}
			
			
			
			
			
			
			
			
			
//			AudioUtilities.multiplyHarmonics(transformed, 2);
//			final BitSet peaks = AudioUtilities.peaks(transformed, 3, 0.2f);
//			List<Vec2> list = new ArrayList<>();
//			for (int i = peaks.nextSetBit(0); i >= 0; i = peaks.nextSetBit(i + 1)){
//				list.add(new Vec2(transformed[i], fft.idx2f(i)));
//			}
//			Collections.sort(list, (v0, v1) -> Float.compare(v0.x, v1.x));
//			float[] pitch = new float[list.size()];
//			for (int i = 0; i < pitch.length; i++){
//				pitch[i] = list.get(i).y;
//			}
//			if(pitch.length > 0){
//				System.out.println(pitch[0]);
//			}
		}

	}

}
