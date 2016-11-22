package ch.fhnw.tvver;

import java.util.ArrayList;
import java.util.Arrays;
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
    
    public boolean tone = false;
    
    private List<Float[]> spectralFlux = new ArrayList<>();
    private float[] spectrum = new float[3];
    private float[] last_spectrum = new float[3];
    int idx = 0;
    
    public OnSetDetection(FFT fft) {
        fft.addLast(this);
        this.fft = fft;
    }

    @Override
    protected void run(IAudioRenderTarget target) throws RenderCommandException {
        
        
        System.arraycopy(spectrum, 0, last_spectrum, 0, spectrum.length);
        for(int i=0; i < bands.length/2; i++) {
            spectrum[i] = fft.power(bands[i*2], bands[i*2+1]);
        }
        
        Float[] flux = calculateFlux();
        spectralFlux.add(flux);
                
        Float[] mean = calcualteTreshhold(Math.max(0, this.idx - 10), Math.min(spectralFlux.size() - 1, this.idx + 10));
       
        // Check if all are over treshholds
//        if (spectrum[0] > mean[0] && spectrum[1] > mean[1] ||
//            spectrum[0] > mean[0] && spectrum[2] > mean[2] ||
//            spectrum[1] > mean[1] && spectrum[2] > mean[2]) {
        
        for(int i = 0; i < 7; i++) { 
            clear();
        }
        
//        if(flux[0] > mean[0] && flux[1] > mean[1] && flux[2] > mean[2]) {
      if (flux[0] > mean[0] && flux[1] > mean[1] ||
          flux[0] > mean[0] && flux[2] > mean[2] ||
          flux[1] > mean[1] && flux[2] > mean[2]) {
            this.tone = true;
            bar(1, RGB.YELLOW);
        } else {
            this.tone = false;
            clear();
        }
        this.idx++;
    }
    
    
    private Float[] calculateFlux() {
        Float[] flux = new Float[] {0f, 0f, 0f};
        for (int i = 0; i < 3; i++) {
            float value = spectrum[i] - last_spectrum[i];
            flux[i] = value < 0 ? 0 : value;
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
            mean[i] /= (end - start);
            mean[i] *= 2f;
        }
        return mean;
        
    }

}
