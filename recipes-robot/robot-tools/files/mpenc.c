/* mpenc: rkisp selfpath NV12 capture + MPP hardware H.264 encode -> Annex-B stdout.
 * Usage: mpenc [/dev/video1] [> fifo]
 * Requires iqfeed to have fed ISP params (video9, kept open elsewhere).
 * Env: MPENC_BPS (default 1500000), MPENC_GOP (default 60).
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
#include <rockchip/rk_mpi.h>

#define W 640
#define H 480
#define NBUF 4
#define FRAME_SZ ((size_t)W * H * 3 / 2)

struct vctx {
    int fd;
    void *mm[NBUF];
    unsigned len[NBUF];
    struct v4l2_plane planes[1];
};

/* ---- SW-AE (ported from robot-ui, same sensor cid) ---- */
#define CAM_CID_EXPOSURE 9963793u
#define CAM_CID_GAIN    0x009e0903u
static int cam_sd_fd = -1;
static int cam_exp_max = 2020, cam_gain_min = 16, cam_gain_max = 248;
static int cam_exp_cur = 1104, cam_gain_cur = 16;

static int cam_ctrl_query(int fd, unsigned id, int *mn, int *mx)
{
    struct v4l2_queryctrl q;
    memset(&q, 0, sizeof q);
    q.id = id;
    if (ioctl(fd, VIDIOC_QUERYCTRL, &q) < 0) return -1;
    *mn = q.minimum;
    *mx = q.maximum;
    return 0;
}

static void cam_ctrl_set(unsigned id, int val)
{
    struct v4l2_control c;
    memset(&c, 0, sizeof c);
    c.id = id;
    c.value = val;
    ioctl(cam_sd_fd, VIDIOC_S_CTRL, &c);
}

static int cam_subdev_open(void)
{
    for (int i = 0; i < 16; i++) {
        char p[64], nm[64];
        snprintf(p, sizeof p, "/sys/class/video4linux/v4l-subdev%d/name", i);
        FILE *fp = fopen(p, "r");
        if (!fp) continue;
        if (fgets(nm, sizeof nm, fp) && strstr(nm, "ov5695")) {
            fclose(fp);
            snprintf(p, sizeof p, "/dev/v4l-subdev%d", i);
            int fd = open(p, O_RDWR);
            if (fd >= 0) {
                int mn, mx;
                if (cam_ctrl_query(fd, CAM_CID_EXPOSURE, &mn, &mx) == 0)
                    cam_exp_max = mx;
                if (cam_ctrl_query(fd, CAM_CID_GAIN, &mn, &mx) == 0) {
                    cam_gain_min = mn;
                    cam_gain_max = mx;
                    cam_gain_cur = mn;
                }
                cam_exp_cur = cam_exp_max / 2;
                cam_ctrl_set(CAM_CID_EXPOSURE, cam_exp_cur);
                cam_ctrl_set(CAM_CID_GAIN, cam_gain_cur);
                return fd;
            }
            return -1;
        }
        fclose(fp);
    }
    return -1;
}

static int clampi(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }

static void cam_ae(int avg_y)
{
    if (cam_sd_fd < 0) return;
    const int target = 145;
    if (avg_y > target - 12 && avg_y < target + 12) return;
    if (avg_y < target) {
        if (cam_exp_cur < cam_exp_max)
            cam_exp_cur += (target - avg_y) * 24 + 32;
        else if (cam_gain_cur < cam_gain_max)
            cam_gain_cur += (cam_gain_max - cam_gain_min) / 32 + 1;
        else return;
    } else {
        if (cam_gain_cur > cam_gain_min)
            cam_gain_cur -= (cam_gain_max - cam_gain_min) / 32 + 1;
        else if (cam_exp_cur > 4)
            cam_exp_cur -= (avg_y - target) * 24 + 32;
        else return;
    }
    cam_exp_cur = clampi(cam_exp_cur, 4, cam_exp_max);
    cam_gain_cur = clampi(cam_gain_cur, cam_gain_min, cam_gain_max);
    cam_ctrl_set(CAM_CID_EXPOSURE, cam_exp_cur);
    cam_ctrl_set(CAM_CID_GAIN, cam_gain_cur);
}

static int cam_open(struct vctx *c, const char *dev)
{
    c->fd = open(dev, O_RDWR);
    if (c->fd < 0) { perror("open video"); return -1; }

    struct v4l2_format f;
    memset(&f, 0, sizeof f);
    f.type = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
    f.fmt.pix_mp.width = W;
    f.fmt.pix_mp.height = H;
    f.fmt.pix_mp.pixelformat = V4L2_PIX_FMT_NV12;
    f.fmt.pix_mp.num_planes = 1;
    f.fmt.pix_mp.plane_fmt[0].sizeimage = W * H * 3 / 2;
    if (ioctl(c->fd, VIDIOC_S_FMT, &f) < 0) { perror("S_FMT"); return -1; }

    struct v4l2_requestbuffers r;
    memset(&r, 0, sizeof r);
    r.count = NBUF;
    r.type = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
    r.memory = V4L2_MEMORY_MMAP;
    if (ioctl(c->fd, VIDIOC_REQBUFS, &r) < 0) { perror("REQBUFS"); return -1; }

    for (unsigned i = 0; i < r.count; i++) {
        struct v4l2_buffer b;
        memset(&b, 0, sizeof b);
        memset(c->planes, 0, sizeof c->planes);
        b.type = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
        b.memory = V4L2_MEMORY_MMAP;
        b.index = i;
        b.length = 1;
        b.m.planes = c->planes;
        if (ioctl(c->fd, VIDIOC_QUERYBUF, &b) < 0) { perror("QUERYBUF"); return -1; }
        c->len[i] = c->planes[0].length;
        c->mm[i] = mmap(NULL, c->len[i], PROT_READ, MAP_SHARED, c->fd, c->planes[0].m.mem_offset);
        if (c->mm[i] == MAP_FAILED) { perror("mmap"); return -1; }
        if (ioctl(c->fd, VIDIOC_QBUF, &b) < 0) { perror("QBUF"); return -1; }
    }
    int t = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
    if (ioctl(c->fd, VIDIOC_STREAMON, &t) < 0) { perror("STREAMON"); return -1; }
    return 0;
}

static int enc_init(MppCtx *ctx, MppApi **mpi, MppEncCfg *cfg,
                    MppBufferGroup *grp, MppBuffer *frm, MppBuffer *pkt)
{
    RK_U32 bps = 1500000, gop = 60;
    char *e;
    if ((e = getenv("MPENC_BPS")) != NULL) bps = atoi(e);
    if ((e = getenv("MPENC_GOP")) != NULL) gop = atoi(e);

    if (mpp_create(ctx, mpi) < 0 || *ctx == NULL) { fprintf(stderr, "mpp_create\n"); return -1; }
    if (mpp_init(*ctx, MPP_CTX_ENC, MPP_VIDEO_CodingAVC) < 0) { fprintf(stderr, "mpp_init ENC\n"); return -1; }
    if (mpp_enc_cfg_init(cfg) < 0) { fprintf(stderr, "cfg_init\n"); return -1; }

    mpp_enc_cfg_set_s32(*cfg, "prep:width", W);
    mpp_enc_cfg_set_s32(*cfg, "prep:height", H);
    mpp_enc_cfg_set_s32(*cfg, "prep:hor_stride", W);
    mpp_enc_cfg_set_s32(*cfg, "prep:ver_stride", H);
    mpp_enc_cfg_set_s32(*cfg, "prep:format", MPP_FMT_YUV420SP);

    mpp_enc_cfg_set_s32(*cfg, "rc:mode", MPP_ENC_RC_MODE_CBR);
    mpp_enc_cfg_set_s32(*cfg, "rc:bps_target", bps);
    mpp_enc_cfg_set_s32(*cfg, "rc:bps_max", bps * 17 / 16);
    mpp_enc_cfg_set_s32(*cfg, "rc:bps_min", bps * 15 / 16);
    mpp_enc_cfg_set_s32(*cfg, "rc:fps_in_flex", 0);
    mpp_enc_cfg_set_s32(*cfg, "rc:fps_in_num", 30);
    mpp_enc_cfg_set_s32(*cfg, "rc:fps_in_denom", 1);
    mpp_enc_cfg_set_s32(*cfg, "rc:fps_out_flex", 0);
    mpp_enc_cfg_set_s32(*cfg, "rc:fps_out_num", 30);
    mpp_enc_cfg_set_s32(*cfg, "rc:fps_out_denom", 1);
    mpp_enc_cfg_set_s32(*cfg, "rc:gop", gop);
    mpp_enc_cfg_set_s32(*cfg, "rc:drop_mode", MPP_ENC_RC_DROP_FRM_DISABLED);

    mpp_enc_cfg_set_s32(*cfg, "codec:type", MPP_VIDEO_CodingAVC);
    mpp_enc_cfg_set_s32(*cfg, "h264:profile", 66);
    mpp_enc_cfg_set_s32(*cfg, "h264:level", 30);
    mpp_enc_cfg_set_s32(*cfg, "h264:cabac_en", 0);
    mpp_enc_cfg_set_s32(*cfg, "h264:trans8x8", 0);

    if ((*mpi)->control(*ctx, MPP_ENC_SET_CFG, *cfg) < 0) {
        fprintf(stderr, "MPP_ENC_SET_CFG\n"); return -1;
    }

    MppEncHeaderMode hm = MPP_ENC_HEADER_MODE_EACH_IDR;
    if ((*mpi)->control(*ctx, MPP_ENC_SET_HEADER_MODE, &hm) < 0)
        fprintf(stderr, "warn: header mode not set\n");

    if (mpp_buffer_group_get_internal(grp, MPP_BUFFER_TYPE_DRM | MPP_BUFFER_FLAGS_CACHABLE) < 0) {
        fprintf(stderr, "buf grp\n"); return -1;
    }
    if (mpp_buffer_get(*grp, frm, FRAME_SZ + 4096) < 0 ||
        mpp_buffer_get(*grp, pkt, FRAME_SZ + 4096) < 0) {
        fprintf(stderr, "buf get\n"); return -1;
    }
    return 0;
}

static int write_hdr(MppCtx ctx, MppApi *mpi, MppBuffer pktbuf)
{
    MppPacket p = NULL;
    mpp_packet_init_with_buffer(&p, pktbuf);
    mpp_packet_set_length(p, 0);
    if (mpi->control(ctx, MPP_ENC_GET_HDR_SYNC, p) < 0) {
        fprintf(stderr, "GET_HDR_SYNC\n");
        mpp_packet_deinit(&p);
        return -1;
    }
    void *ptr = mpp_packet_get_pos(p);
    size_t len = mpp_packet_get_length(p);
    fwrite(ptr, 1, len, stdout);
    mpp_packet_deinit(&p);
    return 0;
}

int main(int argc, char **argv)
{
    const char *dev = argc > 1 ? argv[1] : "/dev/video1";
    struct vctx c;
    MppCtx ctx = NULL;
    MppApi *mpi = NULL;
    MppEncCfg cfg = NULL;
    MppBufferGroup grp = NULL;
    MppBuffer frm = NULL, pktbuf = NULL;

    if (cam_open(&c, dev) < 0) return 1;
    cam_sd_fd = cam_subdev_open();
    fprintf(stderr, "mpenc: subdev fd %d exp_max %d\n", cam_sd_fd, cam_exp_max);
    if (enc_init(&ctx, &mpi, &cfg, &grp, &frm, &pktbuf) < 0) return 1;
    if (write_hdr(ctx, mpi, pktbuf) < 0) return 1;
    fflush(stdout);
    fprintf(stderr, "mpenc: streaming %dx%d H.264\n", W, H);

    long frames = 0;
    for (;;) {
        struct v4l2_buffer b;
        memset(&b, 0, sizeof b);
        memset(c.planes, 0, sizeof c.planes);
        b.type = V4L2_BUF_TYPE_VIDEO_CAPTURE_MPLANE;
        b.memory = V4L2_MEMORY_MMAP;
        b.length = 1;
        b.m.planes = c.planes;
        if (ioctl(c.fd, VIDIOC_DQBUF, &b) < 0) {
            if (errno == EINTR) continue;
            perror("DQBUF");
            return 1;
        }

        if (c.planes[0].bytesused >= FRAME_SZ) {
            /* AE: sample Y plane brightness */
            const unsigned char *y = c.mm[b.index];
            long sum = 0;
            int n = 0;
            for (size_t i = 0; i < (size_t)W * H; i += 67) { sum += y[i]; n++; }
            cam_ae((int)(sum / (n ? n : 1)));

            void *dst = mpp_buffer_get_ptr(frm);
            mpp_buffer_sync_begin(frm);
            memcpy(dst, c.mm[b.index], FRAME_SZ);
            mpp_buffer_sync_end(frm);

            MppFrame frame = NULL;
            MppPacket packet = NULL;
            mpp_frame_init(&frame);
            mpp_frame_set_width(frame, W);
            mpp_frame_set_height(frame, H);
            mpp_frame_set_hor_stride(frame, W);
            mpp_frame_set_ver_stride(frame, H);
            mpp_frame_set_fmt(frame, MPP_FMT_YUV420SP);
            mpp_frame_set_buffer(frame, frm);

            MppMeta meta = mpp_frame_get_meta(frame);
            mpp_packet_init_with_buffer(&packet, pktbuf);
            mpp_packet_set_length(packet, 0);
            mpp_meta_set_packet(meta, KEY_OUTPUT_PACKET, packet);

            if (mpi->encode_put_frame(ctx, frame) < 0) {
                fprintf(stderr, "put_frame\n");
                mpp_frame_deinit(&frame);
                return 1;
            }
            mpp_frame_deinit(&frame);

            do {
                if (mpi->encode_get_packet(ctx, &packet) < 0) {
                    fprintf(stderr, "get_packet\n");
                    return 1;
                }
                if (packet) {
                    void *ptr = mpp_packet_get_pos(packet);
                    size_t len = mpp_packet_get_length(packet);
                    fwrite(ptr, 1, len, stdout);
                    mpp_packet_deinit(&packet);
                    frames++;
                }
            } while (0);
            if ((frames & 63) == 0) fflush(stdout);
        }

        if (ioctl(c.fd, VIDIOC_QBUF, &b) < 0) { perror("reQBUF"); return 1; }
    }
    return 0;
}
