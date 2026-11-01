/* iqfeed: feed static ISP params blob to /dev/video9 and hold fd open.
 * Without this, rkisp MI never writes frames (no first iq setting).
 * Usage: iqfeed /path/to/params.bin
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <fcntl.h>
#include <unistd.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <linux/videodev2.h>
#include <stdint.h>


int main(int argc, char **argv)
{
    if (argc < 2) { fprintf(stderr, "usage: %s params.bin\n", argv[0]); return 1; }
    int bfd = open(argv[1], O_RDONLY);
    if (bfd < 0) { perror("open blob"); return 1; }
    static unsigned char blob[16384];
    ssize_t n = read(bfd, blob, sizeof blob);
    close(bfd);
    if (n <= 0) { perror("read blob"); return 1; }
    printf("blob %zd bytes\n", n);

    int fd = open("/dev/video9", O_RDWR);
    if (fd < 0) { perror("open video9"); return 1; }
    struct v4l2_format f;
    memset(&f, 0, sizeof f);
    f.type = V4L2_BUF_TYPE_META_OUTPUT;
    f.fmt.meta.dataformat = v4l2_fourcc('R','K','1','P');
    ioctl(fd, VIDIOC_S_FMT, &f);
    memset(&f, 0, sizeof f);
    f.type = V4L2_BUF_TYPE_META_OUTPUT;
    if (ioctl(fd, VIDIOC_G_FMT, &f) < 0 || !f.fmt.meta.buffersize) { perror("G_FMT"); return 1; }
    unsigned sz = f.fmt.meta.buffersize;
    struct v4l2_requestbuffers r;
    memset(&r, 0, sizeof r);
    r.count = 1; r.type = V4L2_BUF_TYPE_META_OUTPUT; r.memory = V4L2_MEMORY_MMAP;
    if (ioctl(fd, VIDIOC_REQBUFS, &r) < 0) { perror("REQBUFS"); return 1; }
    void *p = mmap(NULL, sz, PROT_READ|PROT_WRITE, MAP_SHARED, fd, 0);
    if (p == MAP_FAILED) { perror("mmap"); return 1; }
    memset(p, 0, sz);
    memcpy(p, blob, n);
    struct v4l2_buffer b;
    memset(&b, 0, sizeof b);
    b.type = V4L2_BUF_TYPE_META_OUTPUT; b.memory = V4L2_MEMORY_MMAP;
    b.index = 0; b.length = sz;
    if (ioctl(fd, VIDIOC_QBUF, &b) < 0) { perror("QBUF"); return 1; }
    int t = V4L2_BUF_TYPE_META_OUTPUT;
    if (ioctl(fd, VIDIOC_STREAMON, &t) < 0) { perror("STREAMON"); return 1; }
    printf("params fed, holding (pid %d)\n", getpid());
    fflush(stdout);
    while (1) pause();
    return 0;
}
