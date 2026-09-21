/* Verification harness for piano.sf3 with TinySoundFont + stb_vorbis.
 * Build: cc -O2 -o verify verify.c -lm
 * Run:   ./verify piano.sf3
 */
#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <string.h>

#define STB_VORBIS_NO_PUSHDATA_API
#include "stb_vorbis.c"

#define TSF_IMPLEMENTATION
#include "tsf.h"

#define SR 48000

static int render_note(tsf *f, int note, int vel, float seconds,
                       float *outPeak, float *outRms)
{
    int total = (int)(seconds * SR);
    int block = 512, done = 0, i;
    static float buf[512 * 2];
    double sumsq = 0.0;
    float peak = 0.0f;

    tsf_note_on(f, 0, note, vel / 127.0f);
    while (done < total) {
        int n = (total - done < block) ? (total - done) : block;
        tsf_render_float(f, buf, n, 0);
        for (i = 0; i < n * 2; i++) {
            float a = fabsf(buf[i]);
            if (a > peak) peak = a;
            sumsq += (double)buf[i] * (double)buf[i];
        }
        done += n;
        if (done >= (int)(seconds * 0.6f * SR)) tsf_note_off(f, 0, note);
    }
    *outPeak = peak;
    *outRms = (float)sqrt(sumsq / (double)(total * 2));
    return 0;
}

int main(int argc, char **argv)
{
    const char *path = (argc > 1) ? argv[1] : "piano.sf3";
    int notes[] = {21, 36, 48, 60, 72, 84, 96, 108};
    int i, fail = 0;

    tsf *f = tsf_load_filename(path);
    if (!f) { printf("FAIL: tsf_load_filename(%s) returned NULL\n", path); return 1; }

    printf("file          : %s\n", path);
    printf("preset count  : %d\n", tsf_get_presetcount(f));
    printf("preset[0] name: %s\n", tsf_get_presetname(f, 0));
    printf("bank/preset   : %d/%d\n", 0, 0);

    tsf_set_output(f, TSF_STEREO_INTERLEAVED, SR, 0);

    /* full sweep: which MIDI notes actually sound */
    {
        int lo = -1, hi = -1;
        for (i = 0; i < 128; i++) {
            float p, r;
            tsf_reset(f);
            render_note(f, i, 100, 0.5f, &p, &r);
            if (p > 0.001f) { if (lo < 0) lo = i; hi = i; }
        }
        printf("sounding range: MIDI %d .. %d\n", lo, hi);
    }

    for (i = 0; i < (int)(sizeof(notes) / sizeof(notes[0])); i++) {
        float p, r;
        tsf_reset(f);
        render_note(f, notes[i], 100, 3.0f, &p, &r);
        printf("note %3d  peak=%.6f  rms=%.6f  %s\n", notes[i], p, r,
               (p > 0.01f) ? "OK" : "SILENT <-- FAILURE");
        if (p <= 0.01f) fail = 1;
    }

    /* dump raw float32 stereo renders so pitch can be checked externally */
    for (i = 0; i < (int)(sizeof(notes) / sizeof(notes[0])); i++) {
        char fn[64]; FILE *fp; int done = 0; float buf[512 * 2];
        sprintf(fn, "dump_%d.f32", notes[i]);
        fp = fopen(fn, "wb");
        tsf_reset(f);
        tsf_note_on(f, 0, notes[i], 0.8f);
        while (done < SR * 2) { tsf_render_float(f, buf, 512, 0); fwrite(buf, 4, 1024, fp); done += 512; }
        fclose(fp);
    }

    tsf_close(f);
    printf(fail ? "RESULT: FAIL\n" : "RESULT: PASS\n");
    return fail;
}
