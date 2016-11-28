package ch.fhnw.tvver;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.ether.ui.IPlotable;

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

        OnSetDetection od = new OnSetDetection(fft);

        // program.addLast(new Distort());
        program.addLast(new DCRemove());
        program.addLast(new AutoGain());
        program.addLast(fft);
        program.addLast(od);
        program.addLast(new Converter(fft, od));

    }

    private class Converter extends AbstractRenderCommand<IAudioRenderTarget> implements IPlotable {

        private Map<Integer, Long> playedNotes = new HashMap<>();

        private FFT fft;
        private OnSetDetection od;
        int idx = 0;

        public Converter(FFT fft, OnSetDetection od) {
            fft.addLast(this);
            this.fft = fft;
            this.od = od;
        }

        @Override
        protected void init(IAudioRenderTarget target) throws RenderCommandException {
        }

        @Override
        protected void run(IAudioRenderTarget target) throws RenderCommandException {
            this.idx += 1;

            if (od.tone) {
                System.out.println("New Tone\n");
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
}
