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

/* In-place radix-2 FFT: 2048 stereo frames, Hann window, 48kHz. */
int moon_audio_bands(const void *data,size_t bytes,MoonAudioBands *out){
 enum { N=2048 };
 if(!data||!out||bytes!=N*4u)return -1;
 const unsigned char *p=(const unsigned char *)data;
 double re[N],im[N]={0},power[3]={0};
 for(int i=0;i<N;i++){
  int k=i*4;
  int16_t l=(int16_t)((unsigned)p[k]|((unsigned)p[k+1]<<8));
  int16_t r=(int16_t)((unsigned)p[k+2]|((unsigned)p[k+3]<<8));
  re[i]=((double)l+(double)r)/65536.0*(0.5-0.5*cos(6.283185307179586*i/(N-1)));
 }
 for(unsigned i=1,j=0;i<N;i++){
  unsigned bit=N>>1;
  for(;j&bit;bit>>=1)j^=bit;
  j^=bit;
  if(i<j){double t=re[i];re[i]=re[j];re[j]=t;}
 }
 for(int len=2;len<=N;len<<=1){
  double a=-6.283185307179586/len,wr=cos(a),wi=sin(a);
  for(int i=0;i<N;i+=len){
   double cr=1,ci=0;
   for(int j=0;j<len/2;j++){
    int x=i+j,y=x+len/2;
    double tr=cr*re[y]-ci*im[y],ti=cr*im[y]+ci*re[y];
    re[y]=re[x]-tr;im[y]=im[x]-ti;
    re[x]+=tr;im[x]+=ti;
    double nr=cr*wr-ci*wi;ci=cr*wi+ci*wr;cr=nr;
   }
  }
 }
 for(int bin=2;bin<=341;bin++){
  double hz=48000.0*bin/N;
  int band=hz<250?0:hz<2000?1:2;
  power[band]+=(re[bin]*re[bin]+im[bin]*im[bin])/(N*(double)N);
 }
 out->bass=fmin(1.0,sqrt(power[0])*4.0);
 out->mid=fmin(1.0,sqrt(power[1])*4.0);
 out->treble=fmin(1.0,sqrt(power[2])*4.0);
 return 0;
}
