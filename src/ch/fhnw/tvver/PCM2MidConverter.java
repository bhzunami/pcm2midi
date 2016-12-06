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
        // Window defines the windowing function in order to have a continuous signal if the sample
        // gets repeated multiple times before and after.
        FFT fft = new FFT(A_SUB_CONTRA_OCTAVE_FREQ, Window.HANN);

        HpsPitchDetection hps = new HpsPitchDetection(fft, HARMONICS);
        OnSetDetection osd = new OnSetDetection(fft, hps);

        program.addLast(new AutoGain());
        program.addLast(fft);
        program.addLast(osd);
        program.addLast(hps);
        program.addLast(new Converter(hps));

    }

    private class Converter extends AbstractRenderCommand<IAudioRenderTarget> implements IPlotable {

        private HpsPitchDetection hps;

        public Converter(HpsPitchDetection hps) {
            this.hps = hps;
        }

        @Override
        protected void init(IAudioRenderTarget target) throws RenderCommandException {
        }

        @Override
        protected void run(IAudioRenderTarget target) throws RenderCommandException {
            PitchDetectionResult lastResult = this.hps.getLastResult();
            if(lastResult != null && lastResult.isPitched()) {
                PCM2MidConverter.this.noteOn(lastResult.getMidiNote(), 64);
                PCM2MidConverter.this.noteOff(lastResult.getMidiNote(), 64);
            }
            this.hps.clearResult();
            
        }
        
    }
}
