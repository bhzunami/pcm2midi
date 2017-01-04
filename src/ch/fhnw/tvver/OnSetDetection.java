package ch.fhnw.tvver;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.audio.AudioUtilities;
import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.ui.IPlotable;
import ch.fhnw.util.color.RGB;

public class OnSetDetection extends AbstractRenderCommand<IAudioRenderTarget> implements IPlotable {
   
    public static final float[] bands = { 80, 4000, 4000, 10000, 10000, 16000 };
    private FFT fft;
    
    private RGB[] colors = new RGB[] {RGB.GREEN, RGB.YELLOW, RGB.BLUE};
    
    public boolean tone = false;
    
    private List<Float[]> spectralFlux = new ArrayList<>();
    private float[] spectrum = new float[3];
    private float[] last_spectrum = new float[3];
    private float energy = 0f;
    private float last_energy = 0f;
    int idx = 0;
    
    public OnSetDetection(FFT fft) {
        fft.addLast(this);
        this.fft = fft;
    }

    @Override
    protected void run(IAudioRenderTarget target) throws RenderCommandException {
        this.last_energy = this.energy;
        this.energy = AudioUtilities.energy(target.getFrame().samples);
//        bar(energy, RGB.ORANGE);
       
        for(int i = 0; i < 7; i++) this.clear();
        
        System.arraycopy(this.spectrum, 0, this.last_spectrum, 0, this.spectrum.length);
        for(int i=0; i < bands.length/2; i++) {
            this.spectrum[i] = this.fft.power(bands[i*2], bands[i*2+1]);
        }
        
        // Differenz zwischen diesem FFT und vorherigen
        Float[] flux = this.calculateFlux();
        
        // add to spectralFulx to calculate Treshhold
        this.spectralFlux.add(flux);
        
        // Durchschnittswert für die letzten 5 FFT
        Float[] mean = this.calcualteTreshhold(Math.max(0, this.idx - 5), Math.min(this.spectralFlux.size() - 1, this.idx + 5));
//        System.out.println("Current Flux: " +flux[0] +", " +flux[1] +", " +flux[2]);
//        System.out.println("MEAN " +mean[0] +", " +mean[1] +", " +mean[2]);
//        System.out.println("Energy " +energy);
        
        
        
        
        if( (flux[1] == 0f && flux[2] == 0f && flux[0] - mean[0] < 40) ||
                (flux[0] < 5f)){
//            System.out.println("IGNORE SAMPLES");
        } else {
            
//       
//      if ( flux[0] > mean[0] && flux[0] > 1 && flux[1] > mean[1] && flux[1] > 1||
//           flux[0] > mean[0] && flux[0] > 1 && flux[2] > mean[2] && flux[2] > 1|| 
//           flux[1] > mean[1] && flux[1] > 1 && flux[2] > mean[2] && flux[2] > 1) {
            
            
            
            /**
             * ||
             (flux[0] >= mean[0] && flux[2] >= mean[2] && energyRising())|| 
             (flux[1] >= mean[1] && flux[2] >= mean[2] && energyRising())) 
             */
        if ( flux[0] > mean[0]  && flux[1] >= mean[1] && flux[2] >= mean[2] && this.energyRising()) {
            this.tone = true;
//            System.out.println("Play tone");

            
            this.bar(1, RGB.RED);
        } else {
            this.tone = false;
        }
        }
      
        this.idx++;
        this.clear();
//        System.out.println("----------------");
//        System.out.println();
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
            //point(mean[i], colors[i]);
            mean[i] *= 2f;          
        }
        return mean;
        
    }
    
    private boolean energyRising() {
        
        return this.last_energy + 0.08 < this.energy;
        
    }

}
