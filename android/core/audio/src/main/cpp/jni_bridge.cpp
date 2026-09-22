#include <jni.h>

#include <algorithm>
#include <cmath>
#include <memory>
#include <string>
#include <thread>
#include <vector>

#include "AudioEngine.h"
#include "LockFreeQueue.h"
#include "SynthEngine.h"

namespace {

openpiano::AudioEngine* engineOf(jlong handle) {
    return reinterpret_cast<openpiano::AudioEngine*>(handle);
}

/// Diagnostics below back NativeQueueSelfTest. They return null on success and a failure
/// description otherwise; nothing in the audio path calls them.
jstring failure(JNIEnv* env, const std::string& message) {
    return env->NewStringUTF(message.c_str());
}

constexpr uint32_t kSelfTestCapacity = 8;
constexpr int kLoadTestItems = 100000;

constexpr int32_t kProbeSampleRate = 48000;
constexpr int32_t kProbeBurstFrames = 192;
constexpr int32_t kProbeBursts = kProbeSampleRate / kProbeBurstFrames;
constexpr int kProbeVelocity = 100;

/// Loads a soundfont into an offline synth at the probe sample rate. Returns false if decoding
/// failed.
bool loadProbeSynth(JNIEnv* env, jbyteArray soundFont, openpiano::SynthEngine& synth) {
    const jsize size = env->GetArrayLength(soundFont);
    jbyte* bytes = env->GetByteArrayElements(soundFont, nullptr);
    const bool loaded = synth.load(bytes, static_cast<size_t>(size));
    env->ReleaseByteArrayElements(soundFont, bytes, JNI_ABORT);
    if (loaded) {
        synth.setOutput(kProbeSampleRate);
    }
    return loaded;
}

void sendNoteMessages(JNIEnv* env, openpiano::SynthEngine& synth, jintArray notes, int status) {
    const jsize noteCount = env->GetArrayLength(notes);
    jint* noteValues = env->GetIntArrayElements(notes, nullptr);
    for (jsize i = 0; i < noteCount; ++i) {
        openpiano::MidiMessage message{(status << 16) | (noteValues[i] << 8) | kProbeVelocity};
        synth.apply(message);
    }
    env->ReleaseIntArrayElements(notes, noteValues, JNI_ABORT);
}

/// Renders numBursts offline through the given synth and returns the loudest sample.
float renderPeakAmplitude(openpiano::SynthEngine& synth, int32_t numBursts) {
    std::vector<float> buffer(static_cast<size_t>(kProbeBurstFrames) * 2);
    float peak = 0.0f;
    for (int32_t burst = 0; burst < numBursts; ++burst) {
        std::fill(buffer.begin(), buffer.end(), 0.0f);
        synth.render(buffer.data(), kProbeBurstFrames);
        for (const float sample : buffer) {
            peak = std::max(peak, std::abs(sample));
        }
    }
    return peak;
}

/// Renders one second offline and reports the loudest sample. An empty note list must be silence.
float peakAmplitude(JNIEnv* env, jbyteArray soundFont, jintArray notes, bool reverbEnabled) {
    openpiano::SynthEngine synth;
    if (!loadProbeSynth(env, soundFont, synth)) {
        return -1.0f;
    }
    synth.setReverbEnabled(reverbEnabled);
    sendNoteMessages(env, synth, notes, openpiano::kNoteOn);
    return renderPeakAmplitude(synth, kProbeBursts);
}

/// Holds notes down for sustainBursts, releases them, then reports the loudest sample rendered
/// over the following releaseBursts.
float releasePeakAmplitude(JNIEnv* env, jbyteArray soundFont, jintArray notes,
                           int32_t sustainBursts, int32_t releaseBursts, bool reverbEnabled) {
    openpiano::SynthEngine synth;
    if (!loadProbeSynth(env, soundFont, synth)) {
        return -1.0f;
    }
    synth.setReverbEnabled(reverbEnabled);
    sendNoteMessages(env, synth, notes, openpiano::kNoteOn);
    renderPeakAmplitude(synth, sustainBursts);
    sendNoteMessages(env, synth, notes, openpiano::kNoteOff);
    return renderPeakAmplitude(synth, releaseBursts);
}

} // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_OboeAudioEngine_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new openpiano::AudioEngine());
}

JNIEXPORT void JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_OboeAudioEngine_nativeDestroy(JNIEnv*, jobject,
                                                                             jlong handle) {
    delete engineOf(handle);
}

JNIEXPORT jboolean JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_OboeAudioEngine_nativeLoadSoundFont(
    JNIEnv* env, jobject, jlong handle, jbyteArray data) {
    const jsize size = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    const bool loaded = engineOf(handle)->loadSoundFont(bytes, static_cast<size_t>(size));
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
    return static_cast<jboolean>(loaded);
}

JNIEXPORT jboolean JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_OboeAudioEngine_nativeStart(JNIEnv*, jobject,
                                                                           jlong handle) {
    return static_cast<jboolean>(engineOf(handle)->start());
}

JNIEXPORT void JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_OboeAudioEngine_nativeStop(JNIEnv*, jobject,
                                                                          jlong handle) {
    engineOf(handle)->stop();
}

JNIEXPORT void JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_OboeAudioEngine_nativeSend(JNIEnv*, jobject,
                                                                          jlong handle,
                                                                          jint packed) {
    engineOf(handle)->send(packed);
}

JNIEXPORT void JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_OboeAudioEngine_nativeSetReverbEnabled(
    JNIEnv*, jobject, jlong handle, jboolean enabled) {
    engineOf(handle)->setReverbEnabled(enabled == JNI_TRUE);
}

JNIEXPORT jintArray JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_OboeAudioEngine_nativeStats(JNIEnv* env, jobject,
                                                                           jlong handle) {
    openpiano::AudioEngine* engine = engineOf(handle);
    jint values[]{engine->sampleRate(), engine->framesPerBurst(),
                  static_cast<jint>(engine->isExclusive()), engine->xRunCount(),
                  static_cast<jint>(engine->droppedEvents())};
    jintArray array = env->NewIntArray(5);
    env->SetIntArrayRegion(array, 0, 5, values);
    return array;
}

JNIEXPORT jstring JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_NativeQueueSelfTest_nativeFifoOrder(JNIEnv* env,
                                                                                   jobject) {
    openpiano::LockFreeQueue<int32_t, kSelfTestCapacity> queue;
    for (int32_t i = 0; i < static_cast<int32_t>(kSelfTestCapacity); ++i) {
        if (!queue.push(i)) {
            return failure(env, "push rejected item " + std::to_string(i));
        }
    }
    for (int32_t i = 0; i < static_cast<int32_t>(kSelfTestCapacity); ++i) {
        int32_t value = -1;
        if (!queue.pop(value)) {
            return failure(env, "pop failed at index " + std::to_string(i));
        }
        if (value != i) {
            return failure(env, "expected " + std::to_string(i) + " got " + std::to_string(value));
        }
    }
    int32_t drained = -1;
    if (queue.pop(drained)) {
        return failure(env, "pop succeeded on an empty queue");
    }
    return nullptr;
}

JNIEXPORT jstring JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_NativeQueueSelfTest_nativeDropWhenFull(JNIEnv* env,
                                                                                      jobject) {
    openpiano::LockFreeQueue<int32_t, kSelfTestCapacity> queue;
    for (int32_t i = 0; i < static_cast<int32_t>(kSelfTestCapacity); ++i) {
        if (!queue.push(i)) {
            return failure(env, "push rejected item " + std::to_string(i));
        }
    }
    if (queue.push(static_cast<int32_t>(kSelfTestCapacity))) {
        return failure(env, "push succeeded past capacity");
    }
    if (queue.droppedCount() != 1) {
        return failure(env, "dropped counter is " + std::to_string(queue.droppedCount()));
    }
    int32_t value = -1;
    if (!queue.pop(value) || value != 0) {
        return failure(env, "the oldest item was not preserved");
    }
    return nullptr;
}

JNIEXPORT jstring JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_NativeQueueSelfTest_nativeNoDropUnderLoad(
    JNIEnv* env, jobject) {
    auto queue = std::make_unique<openpiano::LockFreeQueue<int32_t, openpiano::kQueueCapacity>>();
    std::atomic<int32_t> consumed{0};
    std::atomic<bool> rejected{false};

    // Waiting for the consumer to fall behind by less than a capacity keeps every push accepted,
    // so a non-zero dropped counter means the queue lost an item rather than the producer racing.
    std::thread producer([&] {
        for (int32_t i = 0; i < kLoadTestItems; ++i) {
            while (i - consumed.load(std::memory_order_acquire) >=
                   static_cast<int32_t>(openpiano::kQueueCapacity)) {
                std::this_thread::yield();
            }
            if (!queue->push(i)) {
                rejected.store(true, std::memory_order_release);
                return;
            }
        }
    });

    std::string error;
    int32_t expected = 0;
    while (expected < kLoadTestItems && !rejected.load(std::memory_order_acquire)) {
        int32_t value = -1;
        if (!queue->pop(value)) {
            std::this_thread::yield();
            continue;
        }
        if (value != expected && error.empty()) {
            error = "expected " + std::to_string(expected) + " got " + std::to_string(value);
        }
        ++expected;
        consumed.store(expected, std::memory_order_release);
    }
    producer.join();

    if (rejected.load(std::memory_order_acquire)) {
        return failure(env, "push rejected while the queue had room");
    }

    if (!error.empty()) {
        return failure(env, error);
    }
    if (expected != kLoadTestItems) {
        return failure(env, "consumed only " + std::to_string(expected) + " items");
    }
    if (queue->droppedCount() != 0) {
        return failure(env, "dropped " + std::to_string(queue->droppedCount()) + " under load");
    }
    return nullptr;
}

JNIEXPORT jfloat JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_NativeSynthProbe_nativePeakAmplitude(
    JNIEnv* env, jobject, jbyteArray soundFont, jintArray notes, jboolean reverbEnabled) {
    return peakAmplitude(env, soundFont, notes, reverbEnabled == JNI_TRUE);
}

JNIEXPORT jfloat JNICALL
Java_dev_thomas_1kiljanczyk_openpiano_core_audio_NativeSynthProbe_nativeReleasePeakAmplitude(
    JNIEnv* env, jobject, jbyteArray soundFont, jintArray notes, jint sustainBursts,
    jint releaseBursts, jboolean reverbEnabled) {
    return releasePeakAmplitude(env, soundFont, notes, sustainBursts, releaseBursts,
                                reverbEnabled == JNI_TRUE);
}

} // extern "C"
