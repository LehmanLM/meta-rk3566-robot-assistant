/* camcat: proven robot-ui style rkisp selfpath capture -> raw NV12 on stdout.
 * Usage: camcat [/dev/video1] [> fifo]
 * Keeps ISP alive only if iqfeed already fed params (video9 global).
 */
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <errno.h>
#include <sys/mman.h>
#include <sys/ioctl.h>
#include <linux/videodev2.h>

#define W 640
#define H 480
#define NBUF 4

int main(int argc, char **argv)
{
    const char *dev = argc > 1 ? argv[1] : "/dev/video1";
    int fd = open(dev, O_RDWR);
    if (fd < 0) { perror("open"); return 1; }

    struct v4l2_format f;
    memset(&f, 0, sizeof f);
    f.type = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
    f.fmt.pix_mp.width = W;
    f.fmt.pix_mp.height = H;
    f.fmt.pix_mp.pixelformat = V4L2_PIX_FMT_NV12;
    f.fmt.pix_mp.num_planes = 1;
    f.fmt.pix_mp.plane_fmt[0].sizeimage = W * H * 3 / 2;
    if (ioctl(fd, VIDIOC_S_FMT, &f) < 0) { perror("S_FMT"); return 1; }

    struct v4l2_requestbuffers r;
    memset(&r, 0, sizeof r);
    r.count = NBUF;
    r.type = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
    r.memory = V4L2_MEMORY_MMAP;
    if (ioctl(fd, VIDIOC_REQBUFS, &r) < 0) { perror("REQBUFS"); return 1; }

    void *mm[NBUF];
    unsigned len[NBUF];
    struct v4l2_plane planes[1];
    for (unsigned i = 0; i < r.count; i++) {
        struct v4l2_buffer b;
        memset(&b, 0, sizeof b);
        memset(planes, 0, sizeof planes);
        b.type = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
        b.memory = V4L2_MEMORY_MMAP;
        b.index = i;
        b.length = 1;
        b.m.planes = planes;
        if (ioctl(fd, VIDIOC_QUERYBUF, &b) < 0) { perror("QUERYBUF"); return 1; }
        len[i] = planes[0].length;
        mm[i] = mmap(NULL, len[i], PROT_READ, MAP_SHARED, fd, planes[0].m.mem_offset);
        if (mm[i] == MAP_FAILED) { perror("mmap"); return 1; }
        if (ioctl(fd, VIDIOC_QBUF, &b) < 0) { perror("QBUF"); return 1; }
    }

    int t = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
    if (ioctl(fd, VIDIOC_STREAMON, &t) < 0) { perror("STREAMON"); return 1; }
    fprintf(stderr, "camcat: streaming %dx%d NV12\n", W, H);

    size_t frame = (size_t)W * H * 3 / 2;
    for (;;) {
        struct v4l2_buffer b;
        memset(&b, 0, sizeof b);
        memset(planes, 0, sizeof planes);
        b.type = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
        b.memory = V4L2_MEMORY_MMAP;
        b.length = 1;
        b.m.planes = planes;
        if (ioctl(fd, VIDIOC_DQBUF, &b) < 0) {
            if (errno == EINTR) continue;
            perror("DQBUF");
            return 1;
        }
        size_t sz = planes[0].bytesused > frame ? frame : planes[0].bytesused;
        if (sz == frame) {
            ssize_t wr = write(STDOUT_FILENO, mm[b.index], frame);
            (void)wr;
        }
        if (ioctl(fd, VIDIOC_QBUF, &b) < 0) { perror("reQBUF"); return 1; }
    }
    return 0;
}
