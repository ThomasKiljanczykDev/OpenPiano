#include "SynthEngine.h"

#include <algorithm>
#include <cstring>

#include "third_party/stb_vorbis.c"

#define TSF_IMPLEMENTATION
#include "third_party/tsf.h"

namespace openpiano {

namespace {
constexpr int kChannel = 0;
} // namespace

SynthEngine::~SynthEngine() { unload(); }

bool SynthEngine::load(const void* data, size_t size) {
    unload();
    tsf_ = tsf_load_memory(data, static_cast<int>(size));
    if (tsf_ == nullptr) {
        return false;
    }
    tsf_set_max_voices(tsf_, kMaxVoices);
    tsf_channel_set_presetindex(tsf_, kChannel, 0);
    if (sampleRate_ > 0) {
        setOutput(sampleRate_);
    }
    return true;
}

void SynthEngine::unload() {
    if (tsf_ != nullptr) {
        tsf_close(tsf_);
        tsf_ = nullptr;
    }
}

void SynthEngine::setOutput(int32_t sampleRate) {
    sampleRate_ = sampleRate;
    reverb_.init(sampleRate);
    limiter_.init(sampleRate);
    if (tsf_ != nullptr) {
        tsf_set_output(tsf_, TSF_STEREO_INTERLEAVED, sampleRate, kGlobalGainDb);
    }
}

void SynthEngine::apply(const MidiMessage& message) {
    if (tsf_ == nullptr) {
        return;
    }
    switch (message.status()) {
    case kNoteOn:
        if (message.data2() > 0) {
            tsf_channel_note_on(tsf_, message.channel(), message.data1(), message.data2() / 127.0f);
        } else {
            tsf_channel_note_off(tsf_, message.channel(), message.data1());
        }
        break;
    case kNoteOff:
        tsf_channel_note_off(tsf_, message.channel(), message.data1());
        break;
    default:
        break;
    }
}

void SynthEngine::render(float* interleavedStereo, int32_t numFrames) {
    const size_t samples = static_cast<size_t>(numFrames) * 2;
    if (tsf_ == nullptr) {
        std::memset(interleavedStereo, 0, samples * sizeof(float));
        return;
    }
    tsf_render_float(tsf_, interleavedStereo, numFrames, 0);
    reverb_.process(interleavedStereo, numFrames, reverbEnabled_.load(std::memory_order_relaxed));
    limiter_.process(interleavedStereo, numFrames);
    // Backstop for any residual overshoot the limiter's attack lag lets through.
    for (size_t i = 0; i < samples; ++i) {
        interleavedStereo[i] = std::clamp(interleavedStereo[i], -1.0f, 1.0f);
    }
}

void SynthEngine::allNotesOff() {
    if (tsf_ != nullptr) {
        tsf_note_off_all(tsf_);
    }
}

} // namespace openpiano
