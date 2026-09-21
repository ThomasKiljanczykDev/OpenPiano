#include "Reverb.h"

#include <algorithm>
#include <cmath>

namespace openpiano {

namespace {

// Delay-line lengths in samples, tuned at Freeverb's 44.1 kHz reference rate.
constexpr int32_t kReferenceSampleRate = 44100;
constexpr int32_t kMaxSampleRate = 48000;
constexpr int32_t kStereoSpread = 23;

constexpr std::array<int32_t, 8> kCombTuningL{1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
constexpr std::array<int32_t, 4> kAllPassTuningL{556, 441, 341, 225};

constexpr float kCombFeedback = 0.87f;
constexpr float kCombDamp1 = 0.2f;
constexpr float kCombDamp2 = 1.0f - kCombDamp1;
constexpr float kAllPassFeedback = 0.5f;
constexpr float kFixedGain = 0.015f;

// Independent of SynthEngine's kGlobalGainDb: TSF sums voices unclamped, so the wet mix
// needs its own headroom to avoid pushing a dense chord's tail past the final hard clamp.
constexpr float kWetGainDb = -11.0f;

float dbToLinear(float db) { return std::pow(10.0f, db / 20.0f); }

int32_t scaledLength(int32_t referenceSamples, int32_t sampleRate, int32_t maxSamples) {
    const int32_t clampedRate = std::min(sampleRate, kMaxSampleRate);
    const float scale = static_cast<float>(clampedRate) / static_cast<float>(kReferenceSampleRate);
    const auto length = static_cast<int32_t>(std::lround(referenceSamples * scale));
    return std::clamp(length, 1, maxSamples);
}

} // namespace

float Reverb::processComb(Comb& comb, float input) {
    const float output = comb.buffer[comb.index];
    comb.filterStore = (output * kCombDamp1) + (comb.filterStore * kCombDamp2);
    comb.buffer[comb.index] = input + (comb.filterStore * kCombFeedback);
    comb.index = (comb.index + 1 == comb.length) ? 0 : comb.index + 1;
    return output;
}

float Reverb::processAllPass(AllPass& allPass, float input) {
    const float bufferOut = allPass.buffer[allPass.index];
    const float output = -input + bufferOut;
    allPass.buffer[allPass.index] = input + (bufferOut * kAllPassFeedback);
    allPass.index = (allPass.index + 1 == allPass.length) ? 0 : allPass.index + 1;
    return output;
}

void Reverb::init(int32_t sampleRate) {
    for (int i = 0; i < kNumCombs; ++i) {
        combL_[i].buffer.fill(0.0f);
        combL_[i].index = 0;
        combL_[i].filterStore = 0.0f;
        combL_[i].length = scaledLength(kCombTuningL[i], sampleRate, kMaxCombSamples);

        combR_[i].buffer.fill(0.0f);
        combR_[i].index = 0;
        combR_[i].filterStore = 0.0f;
        combR_[i].length =
            scaledLength(kCombTuningL[i] + kStereoSpread, sampleRate, kMaxCombSamples);
    }
    for (int i = 0; i < kNumAllPasses; ++i) {
        allPassL_[i].buffer.fill(0.0f);
        allPassL_[i].index = 0;
        allPassL_[i].length = scaledLength(kAllPassTuningL[i], sampleRate, kMaxAllPassSamples);

        allPassR_[i].buffer.fill(0.0f);
        allPassR_[i].index = 0;
        allPassR_[i].length =
            scaledLength(kAllPassTuningL[i] + kStereoSpread, sampleRate, kMaxAllPassSamples);
    }
    wetGain_ = dbToLinear(kWetGainDb);
}

void Reverb::process(float* interleavedStereo, int32_t numFrames, bool enabled) {
    for (int32_t frame = 0; frame < numFrames; ++frame) {
        const float inputL = interleavedStereo[frame * 2];
        const float inputR = interleavedStereo[frame * 2 + 1];
        const float input = enabled ? (inputL + inputR) * kFixedGain : 0.0f;

        float wetL = 0.0f;
        float wetR = 0.0f;
        for (int i = 0; i < kNumCombs; ++i) {
            wetL += processComb(combL_[i], input);
            wetR += processComb(combR_[i], input);
        }
        for (int i = 0; i < kNumAllPasses; ++i) {
            wetL = processAllPass(allPassL_[i], wetL);
            wetR = processAllPass(allPassR_[i], wetR);
        }

        interleavedStereo[frame * 2] = inputL + (wetL * wetGain_);
        interleavedStereo[frame * 2 + 1] = inputR + (wetR * wetGain_);
    }
}

} // namespace openpiano
