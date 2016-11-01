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

import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.AudioUtilities.Window;
import ch.fhnw.ether.audio.BlockBuffer;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.AutoGain;
import ch.fhnw.ether.audio.fx.DCRemove;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.media.RenderProgram;
import ch.fhnw.util.math.Vec2;
import javafx.application.Application;

public class PCM2MidConverter extends AbstractPCM2MIDI {
    
    

    private static final float A_SUB_CONTRA_OCTAVE_FREQ = 25.5f;

    public PCM2MidConverter(File track) throws UnsupportedAudioFileException, IOException, MidiUnavailableException,
            InvalidMidiDataException, RenderCommandException {
        super(track, EnumSet.of(Flags.REPORT, Flags.WAVE));
        
        System.out.println("File " +track);

    }

    @Override
    protected void initializePipeline(RenderProgram<IAudioRenderTarget> program) {
        // Window defines the windowing function in order to have a continuous signal if the sample
        // gets repeated multiple times before and after.
        FFT fft = new FFT(A_SUB_CONTRA_OCTAVE_FREQ, Window.HANN);
        BlockBuffer blockBuffer = new BlockBuffer(1024, true, Window.HANN);
        Plotter plotter = new Plotter("Tone detection", 1000, 1000);
        plotter.plot();
        
        // program.addLast(new Distort());
        program.addLast(new DCRemove());
        program.addLast(new AutoGain());
        program.addLast(fft);

        program.addLast(new Converter(fft, blockBuffer, plotter));
        //new JFrame().setVisible(true);
    }

    private class Converter extends AbstractRenderCommand<IAudioRenderTarget> {

        private FFT fft;
        private BlockBuffer blockBuffer;
        private Plotter plot;
        int idx = 0;
        private float max = 0f;

        private float[] spectrum = new float[1024 / 2 + 1];
        private float[] last_spectrum = new float[1024 / 2 + 1];
        List<Float> spectralFlux = new ArrayList<Float>( );
        
        

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
            this.idx += 1;

            float[] samples = this.fft.power().clone();
            blockBuffer.add(samples);
            
            float[] new_spectrum = blockBuffer.nextBlock();
            if(new_spectrum == null) {
                System.out.println("Block buffer is empty");
                return;
            }
            
            plot.update(new_spectrum);
            
            //float[] samples = new float[target.getFrame().samples.length];
            
            float frequ_calc = (target.getFrame().sRate / 2) / (blockBuffer.size() / 2) / 2; //22050 / 513 / 2

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
                
                if( value > 20) {
                  final BitSet peaks = AudioUtilities.peaks(new_spectrum, 3, 0.2f);
                  List<Vec2> list = new ArrayList<>();
                  for (int y = peaks.nextSetBit(0); y >= 0; y = peaks.nextSetBit(y + 1)){
                      list.add(new Vec2(new_spectrum[y], fft.idx2f(y)));
                  }
                  Collections.sort(list, (v0, v1) -> Float.compare(v0.x, v1.x));
                  float[] pitch = new float[list.size()];
                  for (int y = 0; y < pitch.length; y++){
                      pitch[y] = list.get(y).y;
                  }
                  if(pitch.length > 0 && i > 0) {
                      //double tone = 69 + 12 * (Math.log(pitch[0] / 440) / Math.log(2));
//                      System.out.println("69 + 12 * Math.log("+i +" " +frequ_calc +" / 440) / log(2))");
                      double tone = 69 + 12 * (Math.log(i*frequ_calc / 440) / Math.log(2));
                      System.out.println(tone);
                      try {
                          noteOn((int)tone, 0);
                          
                      } catch (InvalidMidiDataException e) {
                          // TODO Auto-generated catch block
                          e.printStackTrace();
                      }
                  }
                    
                    
                    
                    
                    
                    
                }
                flux += value < 0? 0: value;
            }
            spectralFlux.add(flux);
            
            if(flux > 100){}
            
            // TODO Find note:
//            final BitSet peaks = AudioUtilities.peaks(new_spectrum, 3, 0.2f);
//            List<Vec2> list = new ArrayList<>();
//            for (int i = peaks.nextSetBit(0); i >= 0; i = peaks.nextSetBit(i + 1)){
//                list.add(new Vec2(new_spectrum[i], fft.idx2f(i)));
//            }
//            Collections.sort(list, (v0, v1) -> Float.compare(v0.x, v1.x));
//            float[] pitch = new float[list.size()];
//            for (int i = 0; i < pitch.length; i++){
//                pitch[i] = list.get(i).y;
//            }
//            if(pitch.length > 0) {
//                double tone = 69 + 12 * (Math.log(pitch[0] / 440) / Math.log(2));
//                System.out.println(tone);
//                try {
//                    noteOn((int)tone, 0);
//                    
//                } catch (InvalidMidiDataException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }

            // Stereo to mono
            if (target.getNumChannels() > 1) {
                System.out.println("Should be converted to mono");
                samples = target.getFrame().getMonoSamples();
            } else {
                samples = target.getFrame().samples;
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
