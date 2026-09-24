#include "AudioEngine.h"

#include <android/log.h>

namespace openpiano {

namespace {

constexpr const char* kTag = "OpenPianoAudio";

} // namespace

AudioEngine::~AudioEngine() {
    stop();
}

bool AudioEngine::loadSoundFont(const void* data, size_t size) {
    std::lock_guard<std::mutex> lock(streamLock_);
    if (stream_ != nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, kTag,
                            "loadSoundFont called while the stream is open");
        return false;
    }
    return synth_.load(data, size);
}

bool AudioEngine::openStream() {
    oboe::AudioStreamBuilder builder;
    builder.setPerformanceMode(oboe::PerformanceMode::LowLatency)
        ->setSharingMode(oboe::SharingMode::Exclusive)
        ->setFormat(oboe::AudioFormat::Float)
        ->setChannelCount(oboe::ChannelCount::Stereo)
        ->setUsage(oboe::Usage::Game)
        ->setSampleRateConversionQuality(oboe::SampleRateConversionQuality::Medium)
        ->setDataCallback(this)
        ->setErrorCallback(this);

    const oboe::Result result = builder.openStream(stream_);
    if (result != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "openStream failed: %s",
                            oboe::convertToText(result));
        stream_.reset();
        return false;
    }

    latencyTuner_ = std::make_unique<oboe::LatencyTuner>(*stream_);
    synth_.setOutput(stream_->getSampleRate());
    __android_log_print(ANDROID_LOG_INFO, kTag,
                        "stream open: rate=%d burst=%d sharing=%s perf=%s format=%s",
                        stream_->getSampleRate(), stream_->getFramesPerBurst(),
                        oboe::convertToText(stream_->getSharingMode()),
                        oboe::convertToText(stream_->getPerformanceMode()),
                        oboe::convertToText(stream_->getFormat()));

    const oboe::Result started = stream_->requestStart();
    if (started != oboe::Result::OK) {
        __android_log_print(ANDROID_LOG_ERROR, kTag, "requestStart failed: %s",
                            oboe::convertToText(started));
        closeStream();
        return false;
    }
    return true;
}

void AudioEngine::closeStream() {
    if (stream_ == nullptr) {
        return;
    }
    stream_->stop();
    stream_->close();
    latencyTuner_.reset();
    stream_.reset();
}

bool AudioEngine::start() {
    std::lock_guard<std::mutex> lock(streamLock_);
    if (stream_ != nullptr) {
        return true;
    }
    const bool opened = openStream();
    running_.store(opened, std::memory_order_release);
    return opened;
}

void AudioEngine::stop() {
    std::lock_guard<std::mutex> lock(streamLock_);
    running_.store(false, std::memory_order_release);
    closeStream();
    MidiMessage discarded;
    while (queue_.pop(discarded)) {
    }
    soundsOffPending_.store(true, std::memory_order_release);
}

void AudioEngine::send(int32_t packed) {
    queue_.push(MidiMessage{packed});
}

int32_t AudioEngine::sampleRate() const {
    std::lock_guard<std::mutex> lock(streamLock_);
    return stream_ == nullptr ? 0 : stream_->getSampleRate();
}

int32_t AudioEngine::framesPerBurst() const {
    std::lock_guard<std::mutex> lock(streamLock_);
    return stream_ == nullptr ? 0 : stream_->getFramesPerBurst();
}

bool AudioEngine::isExclusive() const {
    std::lock_guard<std::mutex> lock(streamLock_);
    return stream_ != nullptr && stream_->getSharingMode() == oboe::SharingMode::Exclusive;
}

int32_t AudioEngine::xRunCount() const {
    std::lock_guard<std::mutex> lock(streamLock_);
    if (stream_ == nullptr) {
        return 0;
    }
    const auto result = stream_->getXRunCount();
    return result ? result.value() : 0;
}

oboe::DataCallbackResult AudioEngine::onAudioReady(oboe::AudioStream*, void* audioData,
                                                   int32_t numFrames) {
    if (soundsOffPending_.exchange(false, std::memory_order_acq_rel)) {
        synth_.allSoundsOff();
    }
    MidiMessage message;
    while (queue_.pop(message)) {
        synth_.apply(message);
    }
    synth_.render(static_cast<float*>(audioData), numFrames);
    latencyTuner_->tune();
    return oboe::DataCallbackResult::Continue;
}

void AudioEngine::onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) {
    __android_log_print(ANDROID_LOG_WARN, kTag, "stream error after close: %s",
                        oboe::convertToText(error));
    std::lock_guard<std::mutex> lock(streamLock_);
    // Ignore errors for a stream that stop() already closed or replaced.
    if (!running_.load(std::memory_order_acquire) || stream != stream_.get()) {
        return;
    }
    latencyTuner_.reset();
    stream_.reset();
    // Voices and release tails held across the reopen are pitched for the old sample rate.
    soundsOffPending_.store(true, std::memory_order_release);
    if (!openStream()) {
        running_.store(false, std::memory_order_release);
    }
}

} // namespace openpiano
