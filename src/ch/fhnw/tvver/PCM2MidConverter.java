package ch.fhnw.tvver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.SynchronousQueue;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;

import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
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

        // program.addLast(new Distort());
        program.addLast(new DCRemove());
        program.addLast(new AutoGain());
        program.addLast(fft);

        program.addLast(new Converter(fft));
        new JFrame().setVisible(true);
    }

    private class Converter extends AbstractRenderCommand<IAudioRenderTarget> {

        private FFT fft;
        int idx = 0;
        private float max = 0f;

        private float[] spectrum = new float[1024 / 2 + 1];
        private float[] last_spectrum = new float[1024 / 2 + 1];
        List<Float> spectralFlux = new ArrayList<Float>( );

        public Converter(FFT fft) {
            this.fft = fft;
        }

        @Override
        protected void init(IAudioRenderTarget target) throws RenderCommandException {
            // do nothing
        }

        @Override
        protected void run(IAudioRenderTarget target) throws RenderCommandException {
            this.idx += 1;
            float[] new_spectrum = this.fft.power().clone();
            float[] samples = new float[target.getFrame().samples.length];

            System.arraycopy(spectrum, 0, last_spectrum, 0, spectrum.length);
            System.arraycopy(new_spectrum, 0, spectrum, 0, spectrum.length);
 
            // Wir brauchen von der FFT das Spectrum
            // The spectrum tells us for each frequency how much the frequency contributes to the
            // original time domain audio signal.
            // When we transform 1024 samples we get 513 frequency bins e
            //  http://www.badlogicgames.com/wordpress/?cat=18&paged=1
            // http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.332.989&rep=rep1&type=pdf

            // spectral flux.
            float flux = 0;
            for (int i = 0; i < spectrum.length; i++) {
                float value = (spectrum[i] - last_spectrum[i]);
                flux += value < 0? 0: value;
            }
            spectralFlux.add(flux);
            
            if(flux < spectralFlux.get(--idx))
                flux = 0;
            if(flux > 80 ) {
                
                double tone = 69 + 12 * (Math.log(flux / 440) / Math.log(2));
                System.out.println(tone);
            }

            // Stereo to mono
            if (target.getNumChannels() > 1) {
                System.out.println("Should be converted to mono");
                samples = target.getFrame().getMonoSamples();
            } else {
                samples = target.getFrame().samples;
            }

            float time = 10;
            float samplingrate = target.getSampleRate();
            int index = (int) (time / samplingrate);

            float max = 0f;

            for (int i = 0; i < spectrum.length; i++) {
                if (spectrum[i] > max) {
                    max = spectrum[i];
                }
            }

            if (this.max != max) {
                this.max = max;
                double tone = 69 + (Math.log(12) / Math.log(2)) * (this.max / 440);
            }

            // AudioUtilities.multiplyHarmonics(transformed, 2);
            // final BitSet peaks = AudioUtilities.peaks(transformed, 3, 0.2f);
            // List<Vec2> list = new ArrayList<>();
            // for (int i = peaks.nextSetBit(0); i >= 0; i = peaks.nextSetBit(i + 1)){
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

    }

}
