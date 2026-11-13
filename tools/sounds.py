"""Generates Portal Dive sounds (mono 44.1k ogg) with numpy + ffmpeg."""
import numpy as np, subprocess, wave, os, sys
SR = 44100
rng = np.random.default_rng(7)
OUT = sys.argv[1] if len(sys.argv) > 1 else "src/main/resources/assets/portaldive/sounds"

def lp(x, cut):
    """one-pole lowpass, cut may be array (Hz)"""
    cut = np.broadcast_to(np.asarray(cut, float), x.shape)
    a = 1 - np.exp(-2 * np.pi * cut / SR)
    y = np.empty_like(x); s = 0.0
    for i in range(len(x)):
        s += a[i] * (x[i] - s); y[i] = s
    return y

def bp(x, lo, hi):
    return lp(x, hi) - lp(lp(x, hi), lo)

def sweep(f, t):
    return np.sin(2 * np.pi * np.cumsum(f) / SR)

def save(name, x):
    x = x / (np.max(np.abs(x)) + 1e-9) * 0.9
    wav = f"/tmp/{name}.wav"
    with wave.open(wav, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes((x * 32767).astype("<i2").tobytes())
    os.makedirs(OUT, exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5",
                    "-map_metadata", "-1", f"{OUT}/{name}.ogg"], check=True)
    os.remove(wav)

# jump: noise swell rising in pitch + rising tone, ends in a thump
d = 2.2; t = np.arange(int(SR * d)) / SR; p = t / d
n = rng.standard_normal(len(t))
air = bp(n, 200 + 2500 * p**2, 600 + 7000 * p**2) * (p**2.2) * 1.4
tone = (sweep(60 + 500 * p**3, t) * 0.25 + sweep(120 + 1000 * p**3, t) * 0.12) * p**2
sub = sweep(40 + 20 * p, t) * 0.5 * p**1.5
x = air + tone + sub
x[-int(0.04 * SR):] *= np.linspace(1, 0, int(0.04 * SR))
save("jump", x)

# cruise: loopable rumble + airy hiss, 4s, seamless (crossfade)
d = 4.0; L = int(SR * d); t = np.arange(L + SR) / SR
n = rng.standard_normal(len(t))
lfo = 0.8 + 0.2 * np.sin(2 * np.pi * t / d * 2)
x = lp(n, 120) * 3.0 * lfo + bp(n, 900, 4000) * 0.35 + np.sin(2 * np.pi * 55 * t) * 0.18 + np.sin(2 * np.pi * 82.5 * t) * 0.08
head, tail = x[:SR], x[L:L + SR]; f = np.linspace(0, 1, SR)
loop = x[:L].copy(); loop[:SR] = head * f + tail * (1 - f)
save("cruise", loop)

# brake: downward screech/whoosh + heavy boom + rumble tail
d = 2.6; t = np.arange(int(SR * d)) / SR; p = t / d
n = rng.standard_normal(len(t))
whoosh = bp(n, 300 + 4000 * (1 - p)**3, 900 + 9000 * (1 - p)**3) * np.exp(-t * 3.5) * 1.6
whine = sweep(900 * (1 - p)**2 + 60, t) * np.exp(-t * 4) * 0.25
boom = sweep(70 * np.exp(-t * 2) + 28, t) * np.exp(-t * 2.2) * 1.2
rumble = lp(n, 90) * 4 * np.exp(-t * 1.6)
x = whoosh + whine + boom + rumble
x[:200] *= np.linspace(0, 1, 200)
x[-int(0.2 * SR):] *= np.linspace(1, 0, int(0.2 * SR))
save("brake", x)
print("ok")
