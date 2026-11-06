#include "native_ownership_lock.h"
#include <fcntl.h>
#include <sys/file.h>
#include <sys/stat.h>
#include <unistd.h>
int moon_lock_acquire(MoonLock *lock,const char *path) {
 if (!lock || !path || !*path || lock->fd != -1) return -1;
 int fd=open(path,O_CREAT|O_RDWR|O_CLOEXEC|O_NOFOLLOW,0600);
 if (fd<0) return -2;
 struct stat st;
 if (fstat(fd,&st) || !S_ISREG(st.st_mode) || st.st_uid!=geteuid() || (st.st_mode&077)) {close(fd);return -3;}
 if (flock(fd,LOCK_EX|LOCK_NB)) {close(fd);return -4;}
 lock->fd=fd;return 0;
}
void moon_lock_release(MoonLock *lock) {
 if (!lock || lock->fd<0) return;
 flock(lock->fd,LOCK_UN);close(lock->fd);lock->fd=-1;
}
