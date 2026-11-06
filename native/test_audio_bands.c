#include "moon_audio_backend.h"
#include <math.h>
#include <stdint.h>
#include <stdio.h>
#define N 2048
static unsigned char pcm[N*4];
static int check(double hz,int expected){
 for(int i=0;i<N;i++){
  int16_t v=(int16_t)(12000*sin(6.283185307179586*hz*i/48000.0));
  for(int ch=0;ch<2;ch++){int k=i*4+ch*2;pcm[k]=(unsigned char)v;pcm[k+1]=(unsigned char)((uint16_t)v>>8);}
 }
 MoonAudioBands b;
 if(moon_audio_bands(pcm,sizeof pcm,&b))return 1;
 double x[3]={b.bass,b.mid,b.treble};
 printf("%.0fHz bass=%.6f mid=%.6f treble=%.6f\n",hz,x[0],x[1],x[2]);
 return !(x[expected]>0.05 && x[expected]>x[(expected+1)%3]*4 && x[expected]>x[(expected+2)%3]*4);
}
int main(void){return check(100,0)||check(1000,1)||check(5000,2);}
