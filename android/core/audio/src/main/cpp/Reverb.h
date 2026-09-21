#pragma once

#include <array>
#include <cstdint>

namespace openpiano {

/// Stereo Freeverb-style reverb: eight parallel comb filters per channel feeding four series
/// allpass filters, with delay lengths tuned at a 44.1 kHz reference and rescaled to the stream's
/// negotiated sample rate. Every delay line is a fixed-size array allocated once; init() must run
/// before the stream starts and must never be called from onAudioReady. process() does no
/// allocation, locking, or I/O, so it is safe on the audio thread.
class Reverb {
public:
    /// Computes each delay line's active length from sampleRate and clears filter state.
    /// sampleRate is clamped to kMaxSampleRate; a device that negotiates a higher rate gets
    /// shorter delay times rather than a buffer overrun.
    void init(int32_t sampleRate);

    /// Adds the wet signal to interleavedStereo in place. When enabled is false, no new signal is
    /// fed into the comb/allpass network, but the network still runs so any reflections already
    /// in flight keep decaying naturally instead of cutting off.
    void process(float* interleavedStereo, int32_t numFrames, bool enabled);

private:
    static constexpr int kNumCombs = 8;
    static constexpr int kNumAllPasses = 4;
    static constexpr int32_t kMaxCombSamples = 1800;
    static constexpr int32_t kMaxAllPassSamples = 650;

    struct Comb {
        std::array<float, kMaxCombSamples> buffer{};
        int32_t length = 1;
        int32_t index = 0;
        float filterStore = 0.0f;
    };

    struct AllPass {
        std::array<float, kMaxAllPassSamples> buffer{};
        int32_t length = 1;
        int32_t index = 0;
    };

    static float processComb(Comb& comb, float input);
    static float processAllPass(AllPass& allPass, float input);

    std::array<Comb, kNumCombs> combL_;
    std::array<Comb, kNumCombs> combR_;
    std::array<AllPass, kNumAllPasses> allPassL_;
    std::array<AllPass, kNumAllPasses> allPassR_;
    float wetGain_ = 1.0f;
};

} // namespace openpiano
