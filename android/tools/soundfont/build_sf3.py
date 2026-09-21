#!/usr/bin/env python3
"""Build a small SF3 (SF2 container with Ogg/Vorbis compressed samples) acoustic
grand piano from the Salamander Grand Piano V3 FLAC samples (CC-BY 3.0).

Output format matches exactly what TinySoundFont (tsf.h) + stb_vorbis.c expects:
  * RIFF/sfbk container, LIST INFO / LIST sdta / LIST pdta
  * each shdr has sampleType with bit 0x10 set (SF3 compression flag)
  * shdr.start / shdr.end are BYTE offsets of the Ogg stream inside the smpl chunk
  * shdr.startLoop / shdr.endLoop are sample-frame offsets relative to shdr.start
  * every stream is a complete mono Ogg/Vorbis bitstream beginning with "OggS"
"""
import io, os, struct, sys
import numpy as np
import soundfile as sf

HERE = os.path.dirname(os.path.abspath(__file__))
SAMPLES = os.path.join(HERE, "samples")
OUT = os.path.join(HERE, "piano.sf3")

VEL = "v13"
# Salamander is sampled in minor thirds starting at A0 (MIDI 21)
NOTE_NAMES = ["A0", "C1", "DS1", "FS1", "A1", "C2", "DS2", "FS2", "A2", "C3",
              "DS3", "FS3", "A3", "C4", "DS4", "FS4", "A4", "C5", "DS5", "FS5",
              "A5", "C6", "DS6", "FS6", "A6", "C7", "DS7", "FS7", "A7", "C8"]
ROOTS = [21 + 3 * i for i in range(30)]
assert ROOTS[-1] == 108 and len(NOTE_NAMES) == len(ROOTS)

SR = 48000
OGG_COMPRESSION = 0.05  # libsndfile: 0.0 = best quality / biggest, 1.0 = worst
FADE = 0.30             # seconds of fade-out at the tail


def duration_for(root):
    if root < 48:
        return 6.0
    if root < 72:
        return 5.0
    if root < 96:
        return 3.5
    return 2.5


def load_mono(path, seconds):
    data, sr = sf.read(path, always_2d=True, dtype="float32")
    assert sr == SR, (path, sr)
    mono = data.mean(axis=1)
    n = int(seconds * SR)
    mono = mono[:n]
    f = min(int(FADE * SR), len(mono))
    mono[len(mono) - f:] *= np.linspace(1.0, 0.0, f, dtype=np.float32)
    return mono


def measure_cents(mono, root):
    """Estimate how far the recording deviates from equal temperament, in cents.

    The Salamander recordings are of a real (stretch-tuned) piano, so the top
    and bottom octaves are noticeably sharp/flat relative to 12-TET. We bake the
    correction into shdr.pitchCorrection so the font plays in tune.
    """
    N = 1 << 17
    x = np.zeros(N)
    seg = mono[2000:2000 + N]
    x[:len(seg)] = seg
    S = np.abs(np.fft.rfft(x * np.hanning(N)))
    binhz = SR / float(N)
    exp = 440.0 * 2.0 ** ((root - 69) / 12.0)
    best, bestscore = exp, -1.0
    for c in exp * 2.0 ** (np.linspace(-300, 300, 1201) / 1200.0):
        score = sum(S[int(round(c * h / binhz))] for h in range(1, 9) if c * h < SR / 2 - binhz)
        if score > bestscore:
            bestscore, best = score, c
    return 1200.0 * np.log2(best / exp)


def encode_ogg(mono):
    buf = io.BytesIO()
    sf.write(buf, mono, SR, format="OGG", subtype="VORBIS",
             compression_level=OGG_COMPRESSION)
    return buf.getvalue()


def chunk(fourcc, payload):
    # tsf.h's tsf_riffchunk_read doesn't skip the RIFF word-alignment pad byte, so an
    # odd-sized chunk desyncs its parser. Pad the payload to even length and count the
    # pad in the chunk size, leaving no implicit pad byte.
    if len(payload) & 1:
        payload = payload + b"\0"
    return fourcc + struct.pack("<I", len(payload)) + payload


def zstr(s, n=20):
    b = s.encode("ascii", "replace")[: n - 1]
    return b + b"\0" * (n - len(b))


# SF2 generator operators
GEN_KEYRANGE = 43
GEN_VELRANGE = 44
GEN_SAMPLEMODES = 54
GEN_ROOTKEY = 58
GEN_SAMPLEID = 53
GEN_INSTRUMENT = 41
GEN_RELEASEVOL = 38
GEN_ATTENUATION = 48
GEN_DECAYVOL = 36
GEN_SUSTAINVOL = 37


def gen(op, amount):
    return struct.pack("<HH", op, amount & 0xFFFF)


def gen_range(op, lo, hi):
    return struct.pack("<HBB", op, lo, hi)


def main():
    smpl = bytearray()
    shdrs = []
    raws = []
    for name, root in zip(NOTE_NAMES, ROOTS):
        path = os.path.join(SAMPLES, "%s%s.flac" % (name, VEL))
        raws.append(load_mono(path, duration_for(root)))
    peak = max(float(np.abs(m).max()) for m in raws)
    gain = 0.92 / peak
    print("global peak %.4f -> gain %.4f" % (peak, gain))

    for name, root, mono in zip(NOTE_NAMES, ROOTS, raws):
        dev = measure_cents(mono, root)
        corr = int(max(-99, min(99, round(-dev))))
        ogg = encode_ogg(mono * gain)
        assert ogg[:4] == b"OggS", name
        start = len(smpl)
        smpl += ogg
        end = len(smpl)
        nframes = len(mono)
        shdrs.append(dict(name="%s piano" % name, start=start, end=end,
                          startLoop=0, endLoop=max(nframes - 1, 0),
                          rate=SR, root=root, corr=corr, link=0,
                          # 1 = monoSample, 0x10 = SF3 Ogg/Vorbis compression
                          stype=0x0011))
        print("%-6s root=%3d frames=%6d ogg=%7d bytes  detune=%+6.1fc corr=%+3d"
              % (name, root, nframes, len(ogg), dev, corr))

    shdr_bytes = b""
    for s in shdrs:
        shdr_bytes += (zstr(s["name"]) +
                       struct.pack("<IIIIIBbHH", s["start"], s["end"],
                                   s["startLoop"], s["endLoop"], s["rate"],
                                   s["root"], s["corr"], s["link"], s["stype"]))
    # terminal EOS record
    shdr_bytes += zstr("EOS") + struct.pack("<IIIIIBbHH", 0, 0, 0, 0, 0, 0, 0, 0, 0)

    igen = b""
    ibag = b""
    imod = struct.pack("<HHhHH", 0, 0, 0, 0, 0)  # single terminal modulator
    for i, (s, root) in enumerate(zip(shdrs, ROOTS)):
        lo = 0 if i == 0 else root - 1
        hi = 127 if i == len(shdrs) - 1 else root + 1
        ibag += struct.pack("<HH", len(igen) // 4, 0)
        z = gen_range(GEN_KEYRANGE, lo, hi)
        z += gen(GEN_ROOTKEY, root)
        z += gen(GEN_SAMPLEMODES, 0)          # no loop, natural decay
        z += gen(GEN_RELEASEVOL, 0xFC18)      # -1000 timecents ~= 0.56 s
        z += gen(GEN_SAMPLEID, i)             # sampleID must be the last generator
        igen += z
    ibag += struct.pack("<HH", len(igen) // 4, 0)  # terminal ibag
    igen += struct.pack("<HH", 0, 0)               # terminal igen

    inst = zstr("Acoustic Grand") + struct.pack("<H", 0)
    inst += zstr("EOI") + struct.pack("<H", (len(ibag) // 4) - 1)

    pgen = gen(GEN_INSTRUMENT, 0)
    pbag = struct.pack("<HH", 0, 0)
    pbag += struct.pack("<HH", len(pgen) // 4, 0)  # terminal pbag
    pgen += struct.pack("<HH", 0, 0)               # terminal pgen
    pmod = struct.pack("<HHhHH", 0, 0, 0, 0, 0)

    phdr = zstr("Acoustic Grand") + struct.pack("<HHHIII", 0, 0, 0, 0, 0, 0)
    phdr += zstr("EOP") + struct.pack("<HHHIII", 0, 0, (len(pbag) // 4) - 1, 0, 0, 0)

    info = b"INFO"
    info += chunk(b"ifil", struct.pack("<HH", 2, 1))
    info += chunk(b"isng", b"EMU8000\0")
    info += chunk(b"INAM", b"OpenPiano Acoustic Grand Piano\0")
    info += chunk(b"IENG", b"Alexander Holm (Salamander Grand Piano V3)\0")
    info += chunk(b"ICOP", b"CC-BY 3.0 - https://creativecommons.org/licenses/by/3.0/\0")
    info += chunk(b"ICMT", b"Built 2026-09-04 from Salamander Grand Piano V3 "
                           b"(github.com/sfzinstruments/SalamanderGrandPiano), "
                           b"velocity layer 13, mono, Ogg/Vorbis SF3.\0")
    info += chunk(b"ISFT", b"build_sf3.py\0")

    sdta = b"sdta" + chunk(b"smpl", bytes(smpl))

    pdta = b"pdta"
    pdta += chunk(b"phdr", phdr)
    pdta += chunk(b"pbag", pbag)
    pdta += chunk(b"pmod", pmod)
    pdta += chunk(b"pgen", pgen)
    pdta += chunk(b"inst", inst)
    pdta += chunk(b"ibag", ibag)
    pdta += chunk(b"imod", imod)
    pdta += chunk(b"igen", igen)
    pdta += chunk(b"shdr", shdr_bytes)

    body = b"sfbk" + chunk(b"LIST", info) + chunk(b"LIST", sdta) + chunk(b"LIST", pdta)
    riff = b"RIFF" + struct.pack("<I", len(body)) + body

    with open(OUT, "wb") as f:
        f.write(riff)
    print("wrote %s: %d bytes (%.2f MB), %d samples" %
          (OUT, len(riff), len(riff) / 1048576.0, len(shdrs)))


if __name__ == "__main__":
    main()
