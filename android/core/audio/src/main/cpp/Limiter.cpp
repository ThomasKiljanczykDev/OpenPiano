#include "Limiter.h"

#include <algorithm>
#include <cmath>

namespace openpiano {

namespace {

// Headroom below the final hard clamp so the limiter's attack lag has margin before any
// residual overshoot reaches that backstop.
constexpr float kThresholdDb = -2.0f;
constexpr float kAttackMs = 3.0f;
constexpr float kReleaseMs = 75.0f;

float dbToLinear(float db) { return std::pow(10.0f, db / 20.0f); }

float timeConstantToCoeff(float milliseconds, int32_t sampleRate) {
    return std::exp(-1.0f / (static_cast<float>(sampleRate) * (milliseconds / 1000.0f)));
}

} // namespace

void Limiter::init(int32_t sampleRate) {
    attackCoeff_ = timeConstantToCoeff(kAttackMs, sampleRate);
    releaseCoeff_ = timeConstantToCoeff(kReleaseMs, sampleRate);
    envelope_ = 0.0f;
}

void Limiter::process(float* interleavedStereo, int32_t numFrames) {
    const float thresholdLinear = dbToLinear(kThresholdDb);
    for (int32_t frame = 0; frame < numFrames; ++frame) {
        float& left = interleavedStereo[frame * 2];
        float& right = interleavedStereo[frame * 2 + 1];

        const float peak = std::max(std::fabs(left), std::fabs(right));
        const float coeff = peak > envelope_ ? attackCoeff_ : releaseCoeff_;
        envelope_ = coeff * envelope_ + (1.0f - coeff) * peak;

        const float gain = envelope_ > thresholdLinear ? thresholdLinear / envelope_ : 1.0f;
        left *= gain;
        right *= gain;
    }
}

} // namespace openpiano
