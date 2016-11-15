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
import ch.fhnw.ether.audio.ButterworthFilter;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;

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
        // Window defines the windowing function in order to have a continuous signal if the sample
        // gets repeated multiple times before and after.
        FFT fft = new FFT(A_SUB_CONTRA_OCTAVE_FREQ, Window.HANN);
        BlockBuffer blockBuffer = new BlockBuffer(1024, true, Window.HANN);
        Plotter plotter = new Plotter("Tone detection", 1000, 1000);
        // plotter.plot();

        // program.addLast(new Distort());
        program.addLast(new DCRemove());
        program.addLast(new AutoGain());
        program.addLast(fft);

        program.addLast(new Converter(fft, blockBuffer, plotter));
        // program.addLast(new Converter(fft, blockBuffer));
        // new JFrame().setVisible(true);
    }

    private class Converter extends AbstractRenderCommand<IAudioRenderTarget> {

    	private Map<Integer, Long> playedNotes = new HashMap<>();
    	private long time = System.currentTimeMillis();
    	
        private FFT fft;
        private float[] samples = new float[1024];
        private BlockBuffer blockBuffer;
        private Plotter plot;
        int idx = 0;
        int buffer = 0;
        private float max = 0f;

        private float[] spectrum = new float[1024];
        private float[] last_spectrum = new float[1024];
        List<Float> spectralFlux = new ArrayList<Float>();

        int tone = 0;

        List<Float> threshold = new ArrayList<>();

        public Converter(FFT fft, BlockBuffer blockBuffer) {
            this.fft = fft;
            this.blockBuffer = blockBuffer;
        }

        public Converter(FFT fft, BlockBuffer blockBuffer, Plotter plot) {
            this.fft = fft;
            this.blockBuffer = blockBuffer;
            this.plot = plot;
        }

        @Override
        protected void init(IAudioRenderTarget target) throws RenderCommandException {
            // do nothing

        }

        @Override
        protected void run(IAudioRenderTarget target) throws RenderCommandException {
            this.buffer += 1;
            if (this.buffer % 9 != 0) {
                int index = ((this.buffer % 9) - 1) * 128;

                for (int i = 0; i < target.getFrame().samples.length; i++) {
                    this.samples[index + i] = target.getFrame().samples[i];
                }
                return;
            }
            this.idx += 1;
            float frequ_calc = (target.getFrame().sRate / 2) / (blockBuffer.size() / 2) / 2; // 22050
                                                                                             // /
                                                                                             // 513
                                                                                             // / 2
            // Copy spectrum to last_spectrum
            System.arraycopy(spectrum, 0, last_spectrum, 0, spectrum.length);

            // Set new spectrum
            System.arraycopy(fft.power().clone(), 0, spectrum, 0, spectrum.length);

            float flux = calculateFlux();
            spectralFlux.add(flux);

            // Now we have to decide if this amplitude is enough high as a
            // new tone.
            float mean = calcualteTreshhold(Math.max(0, this.idx - 150), Math.min(spectralFlux.size() - 1, this.idx + 150));
            threshold.add(mean);

            if (flux > mean) {
               TarsosDspMpm mpm = new TarsosDspMpm(target.getSampleRate(), 1024);
               PitchDetectionResult pitch = mpm.getPitch(samples);
               if(pitch.isPitched()){
            	   float freq = pitch.getPitch();
            	   int note = (int) (69 + 12 * (Math.log(freq / 440) / Math.log(2)));
            	   System.out.println(String.format("%-20s%-10s", freq, note));
               }
              
            }

                // if(tone != calculateTone(fft.getSpectrum(), frequ_calc) &&
                // calculateTone(fft.getSpectrum(), frequ_calc) > 0) {
                // System.out.println("------------------------------");
                // System.out.println("FLUX: " +flux +" treshhold: " +mean * 1.5f);
                // System.out.println("New Tone on: " +(int)calculateTone(fft.getSpectrum(),
                // frequ_calc));
                // tone = (int)calculateTone(fft.getSpectrum(), frequ_calc);
                // }


            // Wir brauchen von der FFT das Spectrum
            // The spectrum tells us for each frequency how much the frequency contributes to the
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

        // Get difference from frequencies
        private float calculateFlux() {
            float flux = 0;
            for (int i = 0; i < spectrum.length; i++) {
                float value = (spectrum[i] - last_spectrum[i]);
                value = (float) ((value + Math.abs(value)) / 2 );
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
