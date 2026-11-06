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
