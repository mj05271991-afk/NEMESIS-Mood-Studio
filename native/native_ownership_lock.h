#ifndef MOON_NATIVE_OWNERSHIP_LOCK_H
#define MOON_NATIVE_OWNERSHIP_LOCK_H
typedef struct { int fd; } MoonLock;
int moon_lock_acquire(MoonLock *lock, const char *path);
void moon_lock_release(MoonLock *lock);
#endif
