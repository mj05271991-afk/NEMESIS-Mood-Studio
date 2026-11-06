#include "moon_audio_backend.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <math.h>
#include <unistd.h>
static volatile sig_atomic_t running=1;
static void stop(int sig){(void)sig;running=0;}
static int analyze_file(const char *path){
 FILE *f=fopen(path,"rb");
 if(!f){perror("PCM input");return 3;}
 unsigned char pcm[4096];size_t n;unsigned long frames=0;double peak=0,sum=0;
 while((n=fread(pcm,1,sizeof pcm,f))>0){
  MoonAudioMeter m;
  if(moon_audio_analyze(pcm,n,&m)){fclose(f);return 4;}
  frames+=m.frames;sum+=m.rms*m.rms*(double)m.frames;
  if(m.peak>peak)peak=m.peak;
 }
 if(ferror(f)){fclose(f);return 5;}
 fclose(f);
 printf("frames=%lu rms=%.6f peak=%.6f\n",frames,frames?sqrt(sum/frames):0,peak);
 return frames?0:6;
}
int main(int argc,char **argv){
 if(argc==2 && strcmp(argv[1],"--status")==0){puts("moonstudiod: offline PCM mode; live ALSA/MICOM disabled");return 0;}
 if(argc==2 && strcmp(argv[1],"--self-test")==0){unsigned char pcm[8]={0,0,0,0,0,64,0,64};MoonAudioMeter m;if(moon_audio_analyze(pcm,sizeof pcm,&m)||m.frames!=2||m.peak<0.49)return 1;puts("PASS: offline PCM meter");return 0;}
 if(argc==3 && strcmp(argv[1],"--analyze")==0)return analyze_file(argv[2]);
 if(argc!=2 || strcmp(argv[1],"--offline")!=0){fputs("Usage: moonstudiod --status|--self-test|--analyze FILE.raw|--offline\n",stderr);return 2;}
 signal(SIGTERM,stop);signal(SIGINT,stop);
 while(running)sleep(1);
 return 0;
}
