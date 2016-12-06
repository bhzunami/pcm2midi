package ch.fhnw.tvver;

import java.util.ArrayList;
import java.util.List;

import ch.fhnw.ether.audio.IAudioRenderTarget;
import ch.fhnw.ether.audio.fx.FFT;
import ch.fhnw.ether.media.AbstractRenderCommand;
import ch.fhnw.ether.media.RenderCommandException;
import ch.fhnw.ether.ui.IPlotable;
import ch.fhnw.util.color.RGB;

public class OnSetDetection extends AbstractRenderCommand<IAudioRenderTarget> implements IPlotable {
   
    public static final float[] bands = { 80, 4000, 4000, 10000, 10000, 16000 };
    private FFT fft;
    private HpsPitchDetection hps;
    
    private RGB[] colors = new RGB[] {RGB.GREEN, RGB.YELLOW, RGB.BLUE};
    
    public boolean tone = false;
    
    private List<Float[]> spectralFlux = new ArrayList<>();
    private float[] spectrum = new float[3];
    private float[] last_spectrum = new float[3];
    int idx = 0;
    
    public OnSetDetection(FFT fft, HpsPitchDetection hps) {
        fft.addLast(this);
        this.fft = fft;
        this.hps = hps;
    }
    
    public OnSetDetection(FFT fft) {
        fft.addLast(this);
        this.fft = fft;
    }

    @Override
    protected void run(IAudioRenderTarget target) throws RenderCommandException {
        for(int i = 0; i < 7; i++) clear();
        
        System.arraycopy(spectrum, 0, last_spectrum, 0, spectrum.length);
        for(int i=0; i < bands.length/2; i++) {
            spectrum[i] = fft.power(bands[i*2], bands[i*2+1]);
        }
        
        // Differenz zwischen diesem FFT und vorherigen
        Float[] flux = calculateFlux();
        
        // add to spectralFulx to calculate Treshhold
        spectralFlux.add(flux);
        
        // Durchschnittswert für die letzten 5 FFT
        Float[] mean = calcualteTreshhold(Math.max(0, this.idx - 5), Math.min(spectralFlux.size() - 1, this.idx + 5));
//        System.out.println("Current Flux: " +flux[0] +", " +flux[1] +", " +flux[2]);
//        System.out.println("MEAN " +mean[0] +", " +mean[1] +", " +mean[2]);
//       
        // Check if all are over treshholds
//        if (spectrum[0] > mean[0] && spectrum[1] > mean[1] ||
//            spectrum[0] > mean[0] && spectrum[2] > mean[2] ||
//            spectrum[1] > mean[1] && spectrum[2] > mean[2]) {
        
//        for(int i = 0; i < 7; i++) { 
//            clear();
//        }
        
//        if(flux[0] > mean[0] && flux[1] > mean[1] && flux[2] > mean[2]) {
      if ( flux[0] > mean[0] && flux[0] > 1 && flux[1] > mean[1] && flux[1] > 1||
           flux[0] > mean[0] && flux[0] > 1 && flux[2] > mean[2] && flux[2] > 1|| 
           flux[1] > mean[1] && flux[1] > 1 && flux[2] > mean[2] && flux[2] > 1) {
            this.tone = true;
            this.hps.detectPitch();
            
            bar(1, RGB.RED);
        } else {
            this.tone = false;
        }
      
        this.idx++;
        clear();
    }

    private Float[] calculateFlux() {
        Float[] flux = new Float[] {0f, 0f, 0f};
        for (int i = 0; i < 3; i++) {
            float value = spectrum[i] - last_spectrum[i];
            flux[i] = (float) (value < 0 ? 0 : Math.round(value*100.0)/100.0);
        }
        return flux;
    }
    
    private Float[] calcualteTreshhold(int start, int end) {
        Float[] mean = new Float[] {0f, 0f, 0f};
      
        // Go over all spectralflux entries => run called
        for (int i = start; i <= end; i++) {
            // Go over the diffrent fft sizes
            mean[0] += spectralFlux.get(i)[0];
            mean[1] += spectralFlux.get(i)[1];
            mean[2] += spectralFlux.get(i)[2];
        }
                
        for(int i=0; i < 3; i++) {
            float value = mean[i] / (end - start);
            mean[i] = (float) (Math.round(value*100.0)/100.0);
            point(mean[i], colors[i]);
            if(i == 0) {
                mean[i] *= 1f;
            } else {
                mean[i] *= 2f;
            }            
        }
        return mean;
        
    }

}
