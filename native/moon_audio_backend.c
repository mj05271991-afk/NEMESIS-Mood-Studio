#include "moon_audio_backend.h"
#include <stdint.h>
#include <math.h>
int moon_audio_analyze(const void *data,size_t bytes,MoonAudioMeter *out){
 if(!data||!out||bytes==0||bytes%4) return -1;
 const unsigned char *p=(const unsigned char *)data;
 size_t samples=bytes/2;
 double sum=0,peak=0;
 for(size_t i=0;i<samples;i++){
  int16_t s=(int16_t)((unsigned)p[2*i]|((unsigned)p[2*i+1]<<8));
  double v=(double)s/32768.0;
  sum+=v*v;
  double a=fabs(v);if(a>peak)peak=a;
 }
 out->frames=(unsigned long)(bytes/4);
 out->rms=sqrt(sum/(double)samples);
 out->peak=peak;
 return 0;
}

/* Hann-windowed spectral energy, O(N^2) reference implementation.
   Offline correctness first; optimize FFT after verified tests. */
int moon_audio_bands(const void *data,size_t bytes,MoonAudioBands *out){
 if(!data||!out||bytes!=2048u*4u) return -1;
 const unsigned char *p=(const unsigned char *)data;
 double samples[2048],power[3]={0,0,0};
 for(int i=0;i<2048;i++){
  size_t k=(size_t)i*4;
  int16_t l=(int16_t)((unsigned)p[k]|((unsigned)p[k+1]<<8));
  int16_t r=(int16_t)((unsigned)p[k+2]|((unsigned)p[k+3]<<8));
  samples[i]=((double)l+(double)r)/65536.0*(0.5-0.5*cos(6.283185307179586*i/2047.0));
 }
 for(int bin=2;bin<=341;bin++){
  double hz=48000.0*bin/2048.0;
  int band=hz<250?0:hz<2000?1:2;
  double re=0,im=0;
  for(int i=0;i<2048;i++){
   double angle=6.283185307179586*(double)(bin*i)/2048.0;
   re+=samples[i]*cos(angle);im-=samples[i]*sin(angle);
  }
  power[band]+=(re*re+im*im)/(2048.0*2048.0);
 }
 out->bass=fmin(1.0,sqrt(power[0])*4.0);
 out->mid=fmin(1.0,sqrt(power[1])*4.0);
 out->treble=fmin(1.0,sqrt(power[2])*4.0);
 return 0;
}
