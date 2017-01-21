package ch.fhnw.tvver.onsetdetection;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.ui.IPlotable;
import ch.fhnw.tvver.pitchdetection.PitchDetection;
import ch.fhnw.util.color.RGB;


/**
 * This class does a on set detection.<br>
 * <br>
 * Sources:
 * <ul>
 * <li>http://www.badlogicgames.com/wordpress/?p=187</li>
 * <li>http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.332.989&rep=rep1&type=pdf</li>
 * </ul>
 */
public class OnSetDetection extends AbstractRenderCommand<IAudioRenderTarget> implements IPlotable {
   
    public static final float[] bands = { 80, 4000, 4000, 10000, 10000, 16000 };
    private FFT fft;
    private PitchDetection pitchDetection;
        
    public boolean tone = false;
    
    private List<Float[]> spectralFlux = new ArrayList<>();
    private float[] spectrum = new float[3];
    private float[] last_spectrum = new float[3];
    private float energy = 0f;
    private float last_energy = 0f;
    int idx = 0;
    
    public OnSetDetection(FFT fft, PitchDetection pitchDetection) {
        fft.addLast(this);
        this.fft = fft;
        this.pitchDetection = pitchDetection;
    }

    @Override
    protected void run(IAudioRenderTarget target) throws RenderCommandException {
        this.last_energy = this.energy;
        this.energy = AudioUtilities.energy(target.getFrame().samples);
       
        for(int i = 0; i < 7; i++) this.clear();
        
        // Copy last spectrum to get a history for comparing
        System.arraycopy(this.spectrum, 0, this.last_spectrum, 0, this.spectrum.length);
        for(int i=0; i < bands.length/2; i++) {
            this.spectrum[i] = this.fft.power(bands[i*2], bands[i*2+1]);
        }
        
        // Differenz zwischen diesem FFT und vorherigen
        Float[] flux = this.calculateFlux();
        
        // add to spectralFulx to calculate Treshhold
        this.spectralFlux.add(flux);

        // Durchschnittswert für die letzten 5 FFT
        Float[] mean = this.calcualteTreshhold(Math.max(0, this.idx - 5),
                Math.min(this.spectralFlux.size() - 1, this.idx + 5));

        if ((flux[1] == 0f && flux[2] == 0f && flux[0] - mean[0] < 40) || (flux[0] < 5f)) {
            // TODO: Check if ignore is the best part
            // System.out.println("IGNORE SAMPLES");
        } else {
            if (flux[0] > mean[0] && flux[1] >= mean[1] && flux[2] >= mean[2] && this.energyRising()) {
                this.tone = true;
                this.pitchDetection.detectPitch();
                this.bar(1, RGB.RED);
            } else {
                this.tone = false;
            }
        }

        this.idx++;
        this.clear();

    }

    private Float[] calculateFlux() {
        Float[] flux = new Float[] {0f, 0f, 0f};
        for (int i = 0; i < 3; i++) {
            float value = this.spectrum[i] - this.last_spectrum[i];
            flux[i] = (float) (value < 0 ? 0 : Math.round(value*10.0)/10.0);
        }
        return flux;
    }
    
    private Float[] calcualteTreshhold(int start, int end) {
        Float[] mean = new Float[] {0f, 0f, 0f};
      
        // Go over all spectralflux entries => run called
        for (int i = start; i <= end; i++) {
            // Go over the diffrent fft sizes
            mean[0] += this.spectralFlux.get(i)[0];
            mean[1] += this.spectralFlux.get(i)[1];
            mean[2] += this.spectralFlux.get(i)[2];
        }
                
        for(int i=0; i < 3; i++) {
            float value = mean[i] / (end - start);
            mean[i] = (float) (Math.round(value*10.0)/10.0);
            mean[i] *= 2f;          
        }
        return mean;
        
    }
    
    private boolean energyRising() {
        return this.last_energy + 0.08 < this.energy; 
    }

}
