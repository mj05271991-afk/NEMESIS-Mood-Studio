#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <signal.h>
#include <unistd.h>
/* Moon Studio offline daemon harness. No MICOM or ALSA access yet. */
static volatile sig_atomic_t running=1;
static void stop(int sig){(void)sig;running=0;}
int main(int argc,char **argv){
 if(argc==2 && strcmp(argv[1],"--status")==0){puts("moonstudiod: staging; hardware transport disabled");return 0;}
 if(argc==2 && strcmp(argv[1],"--self-test")==0){puts("PASS: offline daemon entry point");return 0;}
 if(argc!=2 || strcmp(argv[1],"--offline")!=0){fputs("Usage: moonstudiod --status|--self-test|--offline\n",stderr);return 2;}
 signal(SIGTERM,stop);signal(SIGINT,stop);
 while(running) sleep(1);
 return 0;
}
