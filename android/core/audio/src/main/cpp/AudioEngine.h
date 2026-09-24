#pragma once

#include <atomic>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <mutex>

#include <oboe/LatencyTuner.h>
#include <oboe/Oboe.h>

#include "LockFreeQueue.h"
#include "MidiMessage.h"
#include "SynthEngine.h"

namespace openpiano {

constexpr uint32_t kQueueCapacity = 1024;

class AudioEngine : public oboe::AudioStreamDataCallback, public oboe::AudioStreamErrorCallback {
public:
    AudioEngine() = default;
    ~AudioEngine() override;

    AudioEngine(const AudioEngine&) = delete;
    AudioEngine& operator=(const AudioEngine&) = delete;

    /// Blocking. Decodes the soundfont; call from a background thread with the stream stopped.
    bool loadSoundFont(const void* data, size_t size);

    bool start();
    void stop();

    /// Enqueue a packed MIDI 1.0 channel message. Never blocks; a full queue drops.
    void send(int32_t packed);

    /// Safe to call from any thread.
    void setReverbEnabled(bool enabled) { synth_.setReverbEnabled(enabled); }

    int32_t sampleRate() const;
    int32_t framesPerBurst() const;
    bool isExclusive() const;
    int32_t xRunCount() const;
    uint32_t droppedEvents() const { return queue_.droppedCount(); }

    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream, void* audioData,
                                          int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

private:
    bool openStream();
    void closeStream();

    /// Guards stream lifecycle only. Never taken on the audio thread.
    mutable std::mutex streamLock_;
    std::shared_ptr<oboe::AudioStream> stream_;
    /// Grows the buffer after an underrun. Only touched while the stream is stopped
    /// or from the audio thread.
    std::unique_ptr<oboe::LatencyTuner> latencyTuner_;
    SynthEngine synth_;
    LockFreeQueue<MidiMessage, kQueueCapacity> queue_;
    std::atomic<bool> running_{false};
    /// Set off the audio thread; consumed by the audio thread before the next render.
    std::atomic<bool> soundsOffPending_{false};
};

} // namespace openpiano
