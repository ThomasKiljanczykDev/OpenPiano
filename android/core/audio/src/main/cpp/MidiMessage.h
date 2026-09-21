#pragma once

#include <cstdint>

namespace openpiano {

/// MIDI 1.0 channel message packed as (status << 16) | (data1 << 8) | data2.
/// Identical to the shape a future AMidi adapter emits, so the bus needs no change.
struct MidiMessage {
    int32_t packed = 0;

    int status() const { return (packed >> 16) & 0xF0; }
    int channel() const { return (packed >> 16) & 0x0F; }
    int data1() const { return (packed >> 8) & 0x7F; }
    int data2() const { return packed & 0x7F; }
};

constexpr int kNoteOff = 0x80;
constexpr int kNoteOn = 0x90;

} // namespace openpiano
