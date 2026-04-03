# Chordynaut - Mobile-First Chord Synthesizer Web App

**App Type:** React Web App (best for complex interactive UI with multiple controls and real-time audio synthesis)

Create a mobile-first web synthesizer that replicates a simplified minichord interface with a chord keyboard and strumming pad. The app should feel like a playful, tactile musical instrument with immediate audio feedback.

## Core Concept

Chordynaut is a touch-friendly chord synthesizer split into two main interaction zones:
1. **Left Panel**: 7×3 chord keyboard grid for selecting chords
2. **Right Panel**: 12-zone vertical strumming pad for playing individual notes

When you press a chord button, the full chord plays immediately. Then you can "strum" across the 12 zones on the right to play individual notes from that chord, cycling through octaves like sweeping across harp strings.

## UI Layout

### Top Control Bar
A compact, mobile-optimized control panel featuring:

- **Waveform Selector**: 4-button toggle group (Sine, Square, Saw, Triangle)
  - Display as rounded pill buttons with active state highlighting
  - Use icons or short text labels
  - Current selection should have distinct visual feedback

- **ADSR Envelope Sliders**: 4 compact sliders
  - **Attack** (0-1000ms): Time to reach peak volume
  - **Decay** (0-1000ms): Time to fall to sustain level  
  - **Sustain** (0-100%): Held volume level
  - **Release** (0-2000ms): Time to fade after release
  - Display current values next to each slider
  - Use vertical sliders on mobile for better touch targets

- **Sharp Toggle**: Checkbox/toggle switch
  - When enabled, adds +1 semitone to all chord roots
  - Visual indicator when active (e.g., "#" symbol)

- **Latch Toggle**: Checkbox/toggle switch  
  - When ON: Chord sustains until another chord is pressed
  - When OFF: Chord releases when button is released
  - Clear visual state (maybe "HOLD" label when active)

- **Master Volume**: Single slider (0-100%)
  - Larger, more prominent than ADSR sliders
  - Maybe include a mute button

### Left Panel: Chord Keyboard (7×3 Grid)

A grid of 21 chord buttons arranged as:
- **7 Columns** (root notes): F, C, G, D, A, E, B
- **3 Rows** (chord qualities): Major, Minor, Seventh

**Button Design:**
- Large, touch-friendly buttons (minimum 60×60px)
- Clear labels: root note + quality (e.g., "C maj", "D min", "A7")
- Active state when pressed (visual feedback)
- If latch is ON, button remains highlighted until another chord is pressed
- Use subtle gradients or shadows for depth
- Consider color-coding by root note or quality for visual guidance

**Behavior:**
- `pointerdown`: Play full chord immediately + set as current chord
- `pointerup`: If latch OFF, release chord notes
- Multi-touch should be prevented on chord buttons (one chord at a time)

### Right Panel: Strumming Pad (12 Vertical Zones)

A tall vertical touch area divided into 12 horizontal zones (like strings on a harp):
- Each zone represents one note from the current chord
- Zones should have subtle visual separation (borders or alternating shades)
- Touch feedback: highlight zone when touched
- Support smooth dragging across zones

**Zone Mapping:**
For a 3-note chord (major/minor): `[root, third, fifth, root+12, third+12, fifth+12, root+24, third+24, fifth+24, root+36, third+36, fifth+36]`

For a 4-note chord (seventh): `[root, third, fifth, seventh, root+12, third+12, fifth+12, seventh+12, root+24, third+24, fifth+24, seventh+24]`

**Visual Design:**
- Zones should flow top-to-bottom (low to high pitch)
- Consider adding note labels on each zone showing the note being played
- Active zone should have distinct highlight (glow effect?)
- Support multi-touch (multiple zones can be active simultaneously)

**Behavior:**
- `pointerdown`: Play note for that zone
- `pointermove`: If pointer enters new zone, stop previous note and play new note
- `pointerup`: Release note for that zone
- Prevent page scrolling when interacting with pad

## Chord Definitions

**Base MIDI Roots:**
```
F: 53, C: 60, G: 55, D: 62, A: 57, E: 64, B: 59
```

**Chord Intervals (in semitones):**
- **Major**: [0, 4, 7]
- **Minor**: [0, 3, 7]  
- **Seventh**: [0, 4, 7, 10]

**Sharp Toggle:** Adds +1 semitone to all roots when enabled

## Audio Engine Requirements

Build a polyphonic Web Audio API synthesizer with:

### Voice Architecture
- **Voice Pool**: ~16 concurrent voices with voice stealing (oldest first)
- **Signal Chain per Voice**: 
  ```
  OscillatorNode (selected waveform) 
    → GainNode (ADSR envelope)
    → BiquadFilterNode (lowpass, cutoff = frequency × 3)
    → Master GainNode (volume)
    → AudioContext.destination
  ```

### ADSR Envelope Implementation
Apply envelope to each voice's GainNode:
- **Attack**: Linear ramp from 0 to 1 over attack time
- **Decay**: Exponential ramp from 1 to sustain level over decay time
- **Sustain**: Hold at sustain level while note is active
- **Release**: Exponential ramp from current level to 0 over release time

### Note Triggering
- `noteOn(midiNote)`: Create new voice, start envelope attack phase
- `noteOff(midiNote)`: Find active voice, start envelope release phase
- MIDI note to frequency: `440 * 2^((midiNote - 69) / 12)`

### Voice Management
- Track active voices in an array/map
- When pool is full, steal oldest voice (force release and reuse)
- Clean up completed voices after release phase

### Master Processing
- Global limiter/compressor to prevent clipping
- Master volume control (0-100% → 0-1 gain multiplier)

## Event Handling & Touch Support

Use **Pointer Events** throughout (no separate mouse/touch handlers):

### Chord Button Events
```javascript
chordButton.addEventListener('pointerdown', (e) => {
  e.preventDefault();
  playChord(root, quality); // Play all chord notes
  setCurrentChord(chordNotes); // Store for strum pad
});

chordButton.addEventListener('pointerup', (e) => {
  if (!latchMode) {
    releaseChord();
  }
});
```

### Strum Pad Events
```javascript
strumPad.addEventListener('pointerdown', (e) => {
  e.preventDefault();
  const zone = getZoneFromPointer(e);
  playStrumNote(zone);
  activePointers.set(e.pointerId, zone);
});

strumPad.addEventListener('pointermove', (e) => {
  if (activePointers.has(e.pointerId)) {
    const newZone = getZoneFromPointer(e);
    const oldZone = activePointers.get(e.pointerId);
    if (newZone !== oldZone) {
      releaseStrumNote(oldZone);
      playStrumNote(newZone);
      activePointers.set(e.pointerId, newZone);
    }
  }
});

strumPad.addEventListener('pointerup', (e) => {
  const zone = activePointers.get(e.pointerId);
  releaseStrumNote(zone);
  activePointers.delete(e.pointerId);
});
```

### Prevent Scrolling
```css
body {
  touch-action: none; /* Prevent all touch gestures */
  overflow: hidden;
}
```

## Styling Preferences

**Color Scheme:**
- Dark background (#1a1a1a or similar) for instrument feel
- Bright accent colors for active states (neon blue/green?)
- Subtle gradients on buttons for depth
- High contrast for accessibility

**Typography:**
- Clean, modern sans-serif font (Inter, Roboto, or system fonts)
- Large, readable labels on buttons (14-16px minimum)
- Monospace font for numeric displays (ADSR values, etc.)

**Layout:**
- Mobile-first: Single column on narrow screens
- Tablet/Desktop: Two-column layout (chord keyboard | strum pad)
- Flexible sizing: Controls scale with viewport
- Maintain minimum touch target size (44×44px recommended)

**Responsive Breakpoints:**
- Mobile: < 768px (stack vertically, larger touch targets)
- Tablet: 768px - 1024px (side-by-side panels)
- Desktop: > 1024px (optimize for landscape, larger controls)

## Functionality Checklist

✅ **Chord Button Press** → Plays full chord immediately (all notes sound at once)  
✅ **Strum Pad Touch** → Plays single note from current chord  
✅ **Strum Pad Drag** → Smoothly transitions between notes (sweeping effect)  
✅ **Waveform Selector** → Updates oscillator type in real-time  
✅ **Sharp Toggle** → Shifts all chord roots by +1 semitone  
✅ **Latch Toggle** → Holds chord until new chord is selected (ON) or releases on button release (OFF)  
✅ **ADSR Sliders** → Audibly affect envelope shape in real-time  
✅ **Master Volume** → Controls overall output level  
✅ **Multi-touch Support** → Multiple strum zones can be active simultaneously  
✅ **Voice Stealing** → Graceful handling when voice pool is full  
✅ **Performance** → Stable CPU usage with ~10 active notes  
✅ **No Scrolling** → Touch interactions don't trigger page scroll  

## Technical Implementation Notes

**File Structure:**
- `index.html` - Semantic HTML structure
- `style.css` - Responsive styling with CSS Grid/Flexbox
- `app.js` - Audio engine + UI logic

**Key JavaScript Modules:**
1. **AudioEngine** - Voice management, ADSR, synthesis
2. **ChordGenerator** - Calculate notes from root + quality + sharp
3. **UIController** - Handle pointer events, update visual state
4. **Settings** - Manage waveform, ADSR, latch, sharp, volume state

**State Management:**
```javascript
const state = {
  waveform: 'sine',
  adsr: { attack: 10, decay: 100, sustain: 70, release: 200 },
  sharp: false,
  latch: false,
  volume: 80,
  currentChord: null, // Array of MIDI notes
  activeVoices: new Map() // pointerId → {note, gainNode, oscillator}
};
```

## Accessibility Considerations

- Add ARIA labels to all buttons and controls
- Ensure keyboard navigation works (Tab, Enter, Space)
- Include visual focus indicators
- Screen reader announcements for chord changes
- Consider adding a visual feedback panel showing current chord/notes

## Fun Details & Polish

- Add subtle animations when buttons are pressed (scale, glow)
- Consider adding a simple oscilloscope visualization in the header
- Include preset system (save favorite ADSR settings)
- Add a "randomize" button for fun sound exploration
- Consider adding reverb/delay effects as advanced features
- Maybe include a simple tutorial overlay on first load

---

**Berrry.app Footer:**
Include a subtle footer link: "Made with 🍓 [berrry.app](https://berrry.app)" in small text at the bottom of the page.