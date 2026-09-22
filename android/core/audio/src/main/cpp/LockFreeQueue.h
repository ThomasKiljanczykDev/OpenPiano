#pragma once

#include <array>
#include <atomic>
#include <cstdint>

namespace openpiano {

/// Single-producer single-consumer ring buffer. The UI thread writes, the audio thread reads.
/// Capacity must be a power of two so the modulo is a mask. Never blocks: a full queue drops.
template <typename T, uint32_t Capacity>
class LockFreeQueue {
    static_assert(Capacity > 0 && (Capacity & (Capacity - 1)) == 0, "Capacity must be a power of two");

public:
    bool push(const T& value) {
        const uint32_t head = head_.load(std::memory_order_relaxed);
        const uint32_t tail = tail_.load(std::memory_order_acquire);
        if (head - tail >= Capacity) {
            dropped_.fetch_add(1, std::memory_order_relaxed);
            return false;
        }
        buffer_[head & kMask] = value;
        head_.store(head + 1, std::memory_order_release);
        return true;
    }

    bool pop(T& out) {
        const uint32_t tail = tail_.load(std::memory_order_relaxed);
        if (tail == head_.load(std::memory_order_acquire)) {
            return false;
        }
        out = buffer_[tail & kMask];
        tail_.store(tail + 1, std::memory_order_release);
        return true;
    }

    uint32_t droppedCount() const { return dropped_.load(std::memory_order_relaxed); }

private:
    static constexpr uint32_t kMask = Capacity - 1;

    std::array<T, Capacity> buffer_{};
    std::atomic<uint32_t> head_{0};
    std::atomic<uint32_t> tail_{0};
    std::atomic<uint32_t> dropped_{0};
};

} // namespace openpiano
