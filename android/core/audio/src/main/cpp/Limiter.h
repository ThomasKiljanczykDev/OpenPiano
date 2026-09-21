#pragma once

#include <cstdint>

namespace openpiano {

/// Peak limiter: linked-stereo envelope follower that pulls the mix down to a fixed threshold
/// before it reaches the final hard clamp in SynthEngine::render. init() must run before the
/// stream starts; process() does no allocation, locking, or I/O, so it is safe on the audio
/// thread.
class Limiter {
public:
    /// Computes attack/release coefficients from sampleRate and resets the envelope.
    void init(int32_t sampleRate);

    /// Applies linked gain reduction to interleavedStereo in place.
    void process(float* interleavedStereo, int32_t numFrames);

private:
    float envelope_ = 0.0f;
    float attackCoeff_ = 0.0f;
    float releaseCoeff_ = 0.0f;
};

} // namespace openpiano
