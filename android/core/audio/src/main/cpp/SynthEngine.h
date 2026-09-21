#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>

#include "Limiter.h"
#include "MidiMessage.h"
#include "Reverb.h"

struct tsf;

namespace openpiano {

/// Owns the TinySoundFont instance. Every tsf_* call after load happens on the audio thread,
/// so TSF is touched by exactly one thread and needs no lock. load() runs with the stream stopped.
class SynthEngine {
public:
    SynthEngine() = default;
    ~SynthEngine();

    SynthEngine(const SynthEngine&) = delete;
    SynthEngine& operator=(const SynthEngine&) = delete;

    bool load(const void* data, size_t size);
    void unload();
    bool isLoaded() const { return tsf_ != nullptr; }

    void setOutput(int32_t sampleRate);
    void apply(const MidiMessage& message);
    void render(float* interleavedStereo, int32_t numFrames);
    void allNotesOff();

    /// Safe to call from any thread. Toggling off stops feeding new signal into the reverb but
    /// leaves its delay lines alone, so an in-flight tail decays naturally.
    void setReverbEnabled(bool enabled) {
        reverbEnabled_.store(enabled, std::memory_order_relaxed);
    }

private:
    tsf* tsf_ = nullptr;
    int32_t sampleRate_ = 0;
    Reverb reverb_;
    Limiter limiter_;
    std::atomic<bool> reverbEnabled_{true};
};

constexpr int kMaxVoices = 64;

/// Headroom. TSF sums voices with no limiter: at unity gain the shipped bank peaks at 1.04 for a
/// five-note chord and 2.23 for a 25-note cluster, and everything past 1.0 is clipped as a crack.
constexpr float kGlobalGainDb = -8.0f;

} // namespace openpiano
